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

import java.util.HashSet;
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
import appeng.transformer.annotations.Integration.Interface;
import appeng.transformer.annotations.Integration.Method;
import appeng.util.Platform;
import appeng.util.inv.GuardedInventory;
import appeng.util.inv.IInventoryWrapper;
import appeng.util.inv.WrapperBCPipe;
import appeng.util.inv.WrapperMCISidedInventory;
import appeng.util.p2p.ArrayInventoryRouter;
import appeng.util.p2p.IInventoryRouter;
import appeng.util.p2p.IRoutedTunnel;
import appeng.util.p2p.P2PTransportPolicy;
import appeng.util.target.InventoryTarget;
import appeng.util.target.TileTarget;
import appeng.util.target.TileTargetCache;
import buildcraft.api.transport.IPipeConnection;
import buildcraft.api.transport.IPipeTile.PipeType;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Interface(iface = "buildcraft.api.transport.IPipeConnection", iname = IntegrationType.BuildCraftTransport)
public class PartP2PItems extends PartP2PRoutedTunnel<PartP2PItems>
        implements IPipeConnection, ISidedInventory, IGridTickable, IRoutedTunnel {

    /** Output only: the target wrapped into the input's destination, or null if none was. */
    private @Nullable TileTarget boundTarget;
    private final TileTargetCache targetCache = new TileTargetCache();

    private boolean viewed;
    private PartP2PItems owner;
    private final IInventoryRouter router = new ArrayInventoryRouter(this, getTransportPolicy());
    private final IInventoryRouter.SlotView slotView = new IInventoryRouter.SlotView();
    private long invOffset;
    private boolean busy;
    private boolean rebuildQueued;

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

        refreshTarget();
        super.onNeighborChanged();
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

    private @Nullable TileTarget resolveOutputTarget() {
        return this.getProxy().isActive() ? this.targetCache.get(this.getTile(), this.getSide()) : null;
    }

    private @Nullable InventoryTarget getTargetInv() {
        if (loopsBackToThis()) {
            return null;
        }

        IInventory output = null;
        final TileTarget target = this.resolveOutputTarget();
        this.boundTarget = target;

        if (target != null) {
            final TileEntity te = target.tile();

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
                }
            }

            if (output != null) {
                final TileEntity self = this.getTile();
                // Evaluated for every slot access, so only cheap checks belong here.
                output = new GuardedInventory(output, () -> target.isValid() && !self.isInvalid());
            }
        }

        return output == null ? null : new InventoryTarget(output, target);
    }

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        return new TickingRequest(TickRates.ItemTunnel.getMin(), TickRates.ItemTunnel.getMax(), false, true);
    }

    @Override
    public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
        final boolean wasViewed = this.viewed;

        if (this.hasTargetExpired()) {
            this.refreshTarget();
        }

        if (this.viewed) {
            this.invOffset++;
        }

        this.viewed = false;
        return wasViewed ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    @MENetworkEventSubscribe
    public void changeStateA(final MENetworkBootingStatusChange bs) {
        syncRouter();
    }

    @MENetworkEventSubscribe
    public void changeStateB(final MENetworkChannelsChanged bs) {
        syncRouter();
    }

    @MENetworkEventSubscribe
    public void changeStateC(final MENetworkPowerStatusChange bs) {
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
    }

    @Override
    public void removeFromWorld() {
        this.boundTarget = null;
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

    private boolean loopsBackToThis() {
        return loopsBackTo(this, new HashSet<>());
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

    private void scheduleRebuild() {
        if (this.rebuildQueued) {
            return;
        }
        this.rebuildQueued = true;
        TickHandler.INSTANCE.addCallable(null, world -> {
            this.rebuildQueued = false;
            this.router.rebuildLayout();
            return null;
        });
    }

    private void addRouteMember(PartP2PItems t) {
        this.router.addMember(t);
        this.scheduleRebuild();
    }

    private void removeRouteMember(PartP2PItems t) {
        this.router.removeMember(t);
        this.scheduleRebuild();
    }

    private void clearRouteMembers() {
        this.router.clearMembers();
        this.scheduleRebuild();
    }

    private void setRouteTarget(PartP2PItems t, InventoryTarget inv) {
        if (this.router.setTarget(t, inv)) {
            this.scheduleRebuild();
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
