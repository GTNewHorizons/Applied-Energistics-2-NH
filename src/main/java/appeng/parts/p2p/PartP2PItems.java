/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.parts.p2p;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkBootingStatusChange;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartHost;
import appeng.core.settings.TickRates;
import appeng.hooks.TickHandler;
import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.integration.abstraction.IBuildCraftTransport;
import appeng.me.GridAccessException;
import appeng.me.cache.helpers.TunnelCollection;
import appeng.tile.inventory.AppEngNullInventory;
import appeng.transformer.annotations.Integration.Interface;
import appeng.transformer.annotations.Integration.Method;
import appeng.util.Platform;
import appeng.util.inv.GuardedInventory;
import appeng.util.inv.IInventoryWrapper;
import appeng.util.inv.WrapperBCPipe;
import appeng.util.inv.WrapperChainedInventory;
import appeng.util.inv.WrapperMCISidedInventory;
import appeng.util.target.TileTarget;
import appeng.util.target.TileTargetCache;
import appeng.util.p2p.ArrayInventoryRouter;
import appeng.util.p2p.IInventoryRouter;
import appeng.util.p2p.IRoutedTunnel;
import appeng.util.p2p.InventoryTarget;
import appeng.util.p2p.P2PTransportPolicy;
import buildcraft.api.transport.IPipeConnection;
import buildcraft.api.transport.IPipeTile.PipeType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Interface(iface = "buildcraft.api.transport.IPipeConnection", iname = IntegrationType.BuildCraftTransport)
public class PartP2PItems extends PartP2PRoutedTunnel<PartP2PItems>
        implements IPipeConnection, ISidedInventory, IGridTickable, IRoutedTunnel {

    private final LinkedList<IInventory> which = new LinkedList<>();
    private int oldSize = 0;
    private boolean requested;
    /** Input only: the destination, or null if it must be rebuilt on the next access. */
    private @Nullable IInventory cachedInv;
    /** Input only: the outputs included in {@link #cachedInv}. */
    private final List<PartP2PItems> boundOutputs = new ArrayList<>();
    /** Output only: the target wrapped into the input's destination, or null if none was. */
    private @Nullable TileTarget boundTarget;
    /**
     * Output only: the slot layout that the wrapper of {@link #boundTarget} fixed, as {@link #readLayout} returns it.
     * Null if the layout is not tracked.
     */
    private int @Nullable [] boundLayout;
    private final TileTargetCache targetCache = new TileTargetCache();

    private boolean viewed;
    private PartP2PItems owner;
    private final IInventoryRouter router = new ArrayInventoryRouter(this, getTransportPolicy());
    private final IInventoryRouter.SlotView slotView = new IInventoryRouter.SlotView();
    private long invOffset;
    private boolean busy;
    private boolean commitQueued;

    public PartP2PItems(final ItemStack is) {
        super(is);
    }

    @Override
    public P2PTransportPolicy getTransportPolicy() {
        return P2PTransportPolicy.ONE_WAY_PUSH;
    }

    @Override
    public void onNeighborChanged() {
        this.targetCache.markStale();
        if (!this.isOutput()) return;
        final PartP2PItems input = this.getInput();
        // Also called for every markDirty of the adjacent inventory, so the input is rebuilt only if what this output
        // wrapped no longer matches.
        if (input != null && input.cachedInv != null && this.hasTargetChanged()) input.invalidateDestination();

        refreshTarget();
        super.onNeighborChanged();
    }

    /** Rebuilds the destination now and tells the neighbors if its size changed. */
    private void refreshDestination() {
        this.cachedInv = null;
        this.notifySizeChange();
    }

    /** Tells the neighbors, which cache the slot count of the cable bus, if the size of the destination changed. */
    private void notifySizeChange() {
        // A rebuild is not a request from outside, so it must not speed up the tick or cycle the order.
        final int size = this.buildDestination().getSizeInventory();
        if (size != this.oldSize) {
            this.oldSize = size;
            this.getHost().notifyNeighbors();
        }
    }

    /**
     * Rebuilds the destination on its next access, so that a burst of changes reported by outputs, such as P2PCache
     * notifying every output of a frequency, rebuilds once. Neighbors are told about a new size on the next tick.
     */
    private void invalidateDestination() {
        if (this.cachedInv == null) return;
        this.cachedInv = null;
        try {
            this.getProxy().getTick().alertDevice(this.getProxy().getNode());
        } catch (final GridAccessException ignored) {}
    }

    /** Whether the adjacent tile or its slot layout differs from what was wrapped into the input's destination. */
    private boolean hasTargetChanged() {
        final TileTarget target = this.resolveOutputTarget();
        if (target != this.boundTarget) return true;
        return target != null && this.boundLayout != null
                && target.tile() instanceof IInventory inventory
                && !Arrays.equals(this.boundLayout, this.readLayout(inventory));
    }

    /**
     * Detects the changes that nothing reports to the tunnel: the wrapped target expired or its double chest pair
     * changed, or the chunk of an absent target has loaded.
     */
    private boolean hasTargetExpired() {
        final TileTarget target = this.boundTarget;
        if (target != null) return !target.isCompositionCurrent();
        return this.targetCache.isWaitingForChunk() && this.resolveOutputTarget() != null;
    }

    /**
     * The slot layout that the output wrapper fixes: the accessible slots of a sided inventory, or the size of another.
     * The returned array may be owned by the tile.
     */
    private int[] readLayout(final IInventory inventory) {
        if (inventory instanceof ISidedInventory sided) {
            return sided.getAccessibleSlotsFromSide(this.getSide().getOpposite().ordinal());
        }
        return new int[] { inventory.getSizeInventory() };
    }

    private @Nullable TileTarget resolveOutputTarget() {
        return this.getProxy().isActive() ? this.targetCache.get(this.getTile(), this.getSide()) : null;
    }

    private IInventory getDestination() {
        this.requested = true;
        return this.buildDestination();
    }

    private IInventory buildDestination() {
        if (this.cachedInv != null) {
            return this.cachedInv;
        }

        final List<IInventory> outs = new LinkedList<>();
        this.boundOutputs.clear();
        final TunnelCollection<PartP2PItems> itemTunnels;

        try {
            itemTunnels = this.getOutputs();
        } catch (final GridAccessException e) {
            return new AppEngNullInventory();
        }

        for (final PartP2PItems t : itemTunnels) {
            if (t.loopsBackTo(this, new HashSet<>())) {
                continue;
            }
            this.boundOutputs.add(t);
            final IInventory inv = t.getOutputInv();
            if (inv != null) {
                if (Platform.getRandomInt() % 2 == 0) {
                    outs.add(inv);
                } else {
                    outs.add(0, inv);
                }
            }
        }

        return this.cachedInv = new WrapperChainedInventory(outs);
    }

    private @Nullable IInventory getOutputInv() {
        IInventory output = null;
        final TileTarget target = this.resolveOutputTarget();
        this.boundTarget = target;
        this.boundLayout = null;

        if (target != null) {
            final TileEntity te = target.tile();

            if (this.which.contains(this)) {
                return null;
            }

            this.which.add(this);

            if (IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.BuildCraftTransport)) {
                final IBuildCraftTransport buildcraft = (IBuildCraftTransport) IntegrationRegistry.INSTANCE
                        .getInstance(IntegrationType.BuildCraftTransport);
                if (buildcraft.isPipe(te, this.getSide().getOpposite())) {
                    try {
                        output = new WrapperBCPipe(te, this.getSide().getOpposite());
                    } catch (final Throwable ignore) {}
                }
            }

            /*
             * if ( AppEng.INSTANCE.isIntegrationEnabled( "TE" ) ) { ITE thermal = (ITE) AppEng.INSTANCE.getIntegration(
             * "TE" ); if ( thermal != null ) { if ( thermal.isPipe( te, side.getOpposite() ) ) { try { output = new
             * WrapperTEPipe( te, side.getOpposite() ); } catch (Throwable ignore) { } } } }
             */

            if (output == null) {
                if (te instanceof TileEntityChest) {
                    output = target.chestInventory();
                } else if (te instanceof IInventory inventory) {
                    output = inventory instanceof ISidedInventory sided
                            ? new WrapperMCISidedInventory(sided, this.getSide().getOpposite())
                            : inventory;
                    // The wrappers fix this layout. A double chest pair is tracked by the target instead.
                    this.boundLayout = this.readLayout(inventory).clone();
                }
            }

            this.which.pop();
            if (output != null) {
                final TileEntity self = this.getTile();
                // Evaluated for every slot access, so only cheap checks belong here.
                output = new GuardedInventory(output, () -> target.isValid() && !self.isInvalid());
            }
        }

        return output;
    }

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        return new TickingRequest(TickRates.ItemTunnel.getMin(), TickRates.ItemTunnel.getMax(), false, true);
    }

    @Override
    public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
        final boolean wasReq = this.requested;
        if (!this.isOutput()) {
            for (final PartP2PItems output : this.boundOutputs) {
                if (output.hasTargetExpired()) {
                    this.cachedInv = null;
                    break;
                }
            }
            // Also covers a destination that was invalidated and then rebuilt by an access before this tick.
            this.notifySizeChange();
        }

        final boolean wasViewed = this.viewed;

        if (this.viewed) {
            this.invOffset++;
        }

        this.viewed = false;
        return wasViewed ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    @MENetworkEventSubscribe
    public void changeStateA(final MENetworkBootingStatusChange bs) {
        if (!this.isOutput()) {
            this.refreshDestination();
        }
        syncRouter();
    }

    @MENetworkEventSubscribe
    public void changeStateB(final MENetworkChannelsChanged bs) {
        if (!this.isOutput()) {
            this.refreshDestination();
        }
        syncRouter();
    }

    @MENetworkEventSubscribe
    public void changeStateC(final MENetworkPowerStatusChange bs) {
        if (!this.isOutput()) {
            this.refreshDestination();
        }
        syncRouter();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getTypeTexture() {
        return Blocks.hopper.getBlockTextureFromSide(0);
    }

    @Override
    public void onTunnelNetworkChange() {
        super.onTunnelNetworkChange();
        syncRouter();
        if (!this.isOutput()) {
            this.refreshDestination();
        } else {
            // P2PCache tells only the output when it joins or leaves the frequency.
            final PartP2PItems input = this.getInput();
            if (input != null) input.invalidateDestination();
        }
    }

    @Override
    public void removeFromWorld() {
        this.boundOutputs.clear();
        this.boundTarget = null;
        this.boundLayout = null;
        this.cachedInv = null;
        super.removeFromWorld();
    }

    @Override
    public int[] getAccessibleSlotsFromSide(final int var1) {
        final int[] slots = new int[this.getSizeInventory()];
        for (int x = 0; x < this.getSizeInventory(); x++) {
            slots[x] = x;
        }
        return slots;
    }

    @Override
    public int getSizeInventory() {
        final IInventoryRouter router = getOwnerRouter();
        return router == null ? 0 : router.getViewSize(this);
    }

    @Override
    public ItemStack getStackInSlot(final int i) {
        if (this.busy) {
            return null;
        }
        this.busy = true;
        try {
            IInventoryRouter.SlotView view = getSlotView(i);
            return view == null ? null : view.inv.getStackInSlot(view.slot);
        } finally {
            this.busy = false;
        }
    }

    @Override
    public ItemStack decrStackSize(final int i, final int j) {
        if (this.busy) {
            return null;
        }
        this.busy = true;
        try {
            IInventoryRouter.SlotView view = getSlotView(i);
            if (view == null || !view.canExtract) {
                return null;
            }
            if (view.inv instanceof IInventoryWrapper w
                    && !w.canRemoveItemFromSlot(view.slot, view.inv.getStackInSlot(view.slot))) {
                return null;
            }
            return view.inv.decrStackSize(view.slot, j);
        } finally {
            this.busy = false;
        }
    }

    @Override
    public ItemStack getStackInSlotOnClosing(final int i) {
        return null;
    }

    @Override
    public void setInventorySlotContents(final int i, final ItemStack itemstack) {
        if (this.busy) {
            return;
        }
        this.busy = true;
        try {
            final IInventoryRouter.SlotView view = getSlotView(i);
            if (view == null) {
                return;
            }
            final ItemStack storedStack = view.inv.getStackInSlot(view.slot);
            final boolean same = storedStack != null && itemstack != null
                    && storedStack.isItemEqual(itemstack)
                    && ItemStack.areItemStackTagsEqual(storedStack, itemstack);
            final boolean adds = itemstack != null && (!same || itemstack.stackSize > storedStack.stackSize);
            final boolean removes = storedStack != null && (!same || itemstack.stackSize < storedStack.stackSize);

            if (adds && !(view.canInsert && view.inv.isItemValidForSlot(view.slot, itemstack))) {
                return;
            }
            if (removes) {
                if (!view.canExtract) {
                    return;
                }
                if (view.inv instanceof IInventoryWrapper w && !w.canRemoveItemFromSlot(view.slot, storedStack)) {
                    return;
                }
            }
            view.inv.setInventorySlotContents(view.slot, itemstack);
        } finally {
            this.busy = false;
        }
    }

    @Override
    public String getInventoryName() {
        return null;
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        final IInventoryRouter router = getOwnerRouter();
        return router == null ? 64 : router.getViewStackLimit(this);
    }

    @Override
    public void markDirty() {
        // eh?
    }

    @Override
    public boolean isUseableByPlayer(final EntityPlayer entityplayer) {
        return false;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(final int i, final ItemStack itemstack) {
        if (this.busy) {
            return false;
        }
        this.busy = true;
        try {
            IInventoryRouter.SlotView view = getSlotView(i);
            return view != null && view.canInsert && view.inv.isItemValidForSlot(view.slot, itemstack);
        } finally {
            this.busy = false;
        }
    }

    @Override
    public boolean canInsertItem(final int i, final ItemStack itemstack, final int j) {
        return isItemValidForSlot(i, itemstack);
    }

    @Override
    public boolean canExtractItem(final int i, final ItemStack itemstack, final int j) {
        if (this.busy) {
            return false;
        }
        this.busy = true;
        try {
            IInventoryRouter.SlotView view = getSlotView(i);
            if (view == null || !view.canExtract) {
                return false;
            }
            return !(view.inv instanceof IInventoryWrapper w) || w.canRemoveItemFromSlot(view.slot, itemstack);
        } finally {
            this.busy = false;
        }
    }

    public float getPowerDrainPerTick() {
        return 2.0f;
    }

    @Override
    @Method(iname = IntegrationType.BuildCraftTransport)
    public ConnectOverride overridePipeConnection(final PipeType type, final ForgeDirection with) {
        return this.getSide() == with && type == PipeType.ITEM ? ConnectOverride.CONNECT : ConnectOverride.DEFAULT;
    }

    @Override
    public void onInvalidTarget() {
        refreshTarget();
    }

    @Override
    public void onViewSizeChange() {
        final IPartHost host = this.getHost();
        if (host != null) {
            host.notifyNeighbors();
        }
    }

    private boolean loopsBackTo(final PartP2PItems tunnel, final Set<PartP2PItems> visited) {
        final TileEntity te = Platform.getLoadedTileEntity(
                this.getTile().getWorldObj(),
                this.getTile().xCoord + this.getSide().offsetX,
                this.getTile().yCoord + this.getSide().offsetY,
                this.getTile().zCoord + this.getSide().offsetZ);
        if (Platform.getPartFromTE(te, this.getSide().getOpposite()) instanceof PartP2PItems next) {
            if (!visited.contains(next)) {
                visited.add(next);
                for (final PartP2PItems t : next.getInsertTargets()) {
                    if (t == tunnel || t.loopsBackTo(tunnel, visited)) {
                        return true;
                    }
                }
                for (final PartP2PItems t : next.getExtractTargets()) {
                    if (t == tunnel || t.loopsBackTo(tunnel, visited)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private IInventoryRouter getOwnerRouter() {
        return owner == null ? null : owner.router;
    }

    private IInventoryRouter.SlotView getSlotView(final int slot) {
        this.viewed = true;

        final IInventoryRouter router = getOwnerRouter();
        final TileEntity te = this.getTile();
        if (router == null || te == null || te.getWorldObj() == null) {
            return null;
        }
        return router.resolve(this, slot, this.invOffset, this.slotView) ? this.slotView : null;
    }

    private void syncRouter() {
        final PartP2PItems newOwner = findOwner();
        if (newOwner != this.owner) {
            if (this.owner == this) {
                // I was an input P2P, but am no longer
                this.clearRouteMembers();
            } else if (this.owner != null) {
                // I was an output P2P who left the router
                this.owner.removeRouteMember(this);
            }
            this.owner = newOwner;
            if (this.owner != this && this.owner != null) {
                // I am an output P2P who has joined a new router
                this.owner.addRouteMember(this);
            }
            this.refreshTarget();
        }
    }

    private PartP2PItems findOwner() {
        if (!this.getProxy().isActive() || this.getFrequency() == 0) {
            return null;
        }
        try {
            if (!this.getProxy().getP2P().isRegistered(this)) {
                return null;
            }
        } catch (final GridAccessException e) {
            return null;
        }
        final PartP2PItems input = this.isOutput() ? this.getInput() : this;
        return input != null && input.getProxy().isActive() ? input : null;
    }

    private void scheduleCommit() {
        if (this.commitQueued) {
            return;
        }
        this.commitQueued = true;
        TickHandler.INSTANCE.addCallable(null, world -> {
            this.commitQueued = false;
            this.router.commit();
            return null;
        });
    }

    private void addRouteMember(PartP2PItems t) {
        this.router.addMember(t);
        this.scheduleCommit();
    }

    private void removeRouteMember(PartP2PItems t) {
        this.router.removeMember(t);
        this.scheduleCommit();
    }

    private void clearRouteMembers() {
        this.router.clearMembers();
        this.scheduleCommit();
    }

    private void setRouteTarget(PartP2PItems t, InventoryTarget inv) {
        if (this.router.setTarget(t, inv)) {
            this.scheduleCommit();
        }
    }

    private void refreshTarget() {
        if (this.owner == null) {
            return;
        }
        InventoryTarget newTarget = getTargetInv();
        if (newTarget == null || !newTarget.isValid()) {
            this.owner.setRouteTarget(this, null);
        } else {
            this.owner.setRouteTarget(this, newTarget);
        }
    }
}
