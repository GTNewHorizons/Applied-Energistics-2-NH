/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.parts.p2p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import appeng.me.GridAccessException;
import appeng.util.Platform;
import appeng.util.item.FluidKey;
import appeng.util.p2p.P2PRoute;
import appeng.util.p2p.P2PTransportPolicy;
import appeng.util.target.TileTargetCache;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class PartP2PLiquids extends PartP2PTunnelRouted<PartP2PLiquids> implements IFluidHandler {

    private static final ThreadLocal<Stack<PartP2PLiquids>> DEPTH = new ThreadLocal<>();
    private static final FluidTankInfo[] ACTIVE_TANK = { new FluidTankInfo(null, 10000) };
    private static final FluidTankInfo[] INACTIVE_TANK = { new FluidTankInfo(null, 0) };
    private final TileTargetCache targetCache = new TileTargetCache();
    private int tmpUsed;

    private long fluidSelector;

    public PartP2PLiquids(final ItemStack is) {
        super(is);
    }

    @Override
    public P2PTransportPolicy getTransportPolicy() {
        return P2PTransportPolicy.TWO_WAY_PUSH;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getTypeTexture() {
        return Blocks.lapis_block.getBlockTextureFromSide(0);
    }

    @Override
    public void onTunnelNetworkChange() {
        this.targetCache.markStale();
    }

    @Override
    public void onNeighborChanged() {
        this.targetCache.markStale();
    }

    @Override
    public int fill(final ForgeDirection from, final FluidStack resource, final boolean doFill) {
        if (resource == null || resource.getFluid() == null || isBusy()) return 0;

        final Stack<PartP2PLiquids> stack = this.getDepth();

        stack.push(this);

        try {
            final List<PartP2PLiquids> list = this.getInsertTargets(resource.getFluid());

            long requestTotal = 0;

            Iterator<PartP2PLiquids> i = list.iterator();
            while (i.hasNext()) {
                final PartP2PLiquids l = i.next();
                final IFluidHandler tank = l.getTarget();
                if (tank != null) {
                    l.tmpUsed = tank.fill(l.getSide().getOpposite(), resource.copy(), false);
                } else {
                    l.tmpUsed = 0;
                }

                if (l.tmpUsed <= 0) {
                    i.remove();
                } else {
                    requestTotal += l.tmpUsed;
                }
            }

            if (requestTotal <= 0) {
                return 0;
            }

            if (!doFill) {
                return (int) Math.min(resource.amount, requestTotal);
            }

            int available = resource.amount;

            i = list.iterator();
            int used = 0;
            while (i.hasNext()) {
                final PartP2PLiquids l = i.next();

                final FluidStack insert = resource.copy();
                insert.amount = (int) Math.ceil(insert.amount * ((double) l.tmpUsed / (double) requestTotal));
                if (insert.amount > available) {
                    insert.amount = available;
                }

                final IFluidHandler tank = l.getTarget();
                if (tank != null) {
                    l.tmpUsed = tank.fill(l.getSide().getOpposite(), insert, true);
                } else {
                    l.tmpUsed = 0;
                }

                available -= l.tmpUsed;
                used += l.tmpUsed;
                if (available == 0) break;
            }

            return used;
        } finally {
            stack.pop();
        }
    }

    private Stack<PartP2PLiquids> getDepth() {
        Stack<PartP2PLiquids> s = DEPTH.get();

        if (s == null) {
            DEPTH.set(s = new Stack<>());
        }

        return s;
    }

    private List<PartP2PLiquids> getInsertTargets(final Fluid input) {
        final List<PartP2PLiquids> tunnels = new LinkedList<>();
        if (input == null) return tunnels;
        for (final PartP2PLiquids l : this.getInsertTargets()) {
            final IFluidHandler handler = l.getTarget();
            if (handler != null && handler.canFill(l.getSide().getOpposite(), input)) {
                tunnels.add(l);
            }
        }

        return tunnels;
    }

    private List<PartP2PLiquids> getExtractTargets(final Fluid fluid) {
        final List<PartP2PLiquids> tunnels = new LinkedList<>();
        if (fluid == null) return tunnels;
        for (final PartP2PLiquids l : this.getExtractTargets()) {
            final IFluidHandler handler = l.getTarget();
            if (handler != null && handler.canDrain(l.getSide().getOpposite(), fluid)) {
                tunnels.add(l);
            }
        }
        return tunnels;
    }

    public IFluidHandler getTarget() {
        if (!this.getProxy().isActive()) {
            return null;
        }

        var target = this.targetCache.get(this.getTile(), this.getSide());
        return target != null && target.tile() instanceof IFluidHandler handler ? handler : null;
    }

    @Override
    public FluidStack drain(final ForgeDirection from, final FluidStack resource, final boolean doDrain) {
        if (resource == null || resource.getFluid() == null || resource.amount <= 0) {
            return null;
        }
        return this.drainTargets(resource, resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(final ForgeDirection from, final int maxDrain, final boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }
        return this.drainTargets(null, maxDrain, doDrain);
    }

    private FluidStack drainTargets(final FluidStack filter, final int maxDrain, final boolean doDrain) {
        if (isBusy()) {
            return null;
        }

        final Stack<PartP2PLiquids> stack = this.getDepth();
        stack.push(this);
        try {
            final List<PartP2PLiquids> list = filter == null ? this.getExtractTargets()
                    : this.getExtractTargets(filter.getFluid());
            final List<FluidKey> keys = new ArrayList<>();
            final Object2IntMap<FluidKey> fluidIndex = new Object2IntOpenHashMap<>();
            final LongList fluidAmounts = new LongArrayList();
            final List<List<FluidProvider>> providers = new ArrayList<>();
            long totalAmounts = 0;

            for (final PartP2PLiquids l : list) {
                final IFluidHandler tank = l.getTarget();
                final ForgeDirection side = l.getSide().getOpposite();
                if (tank == null) {
                    continue;
                }
                FluidStack drained = filter == null ? tank.drain(side, maxDrain, false)
                        : tank.drain(side, filter, false);
                if (drained == null || drained.amount <= 0 || (filter != null && !filter.isFluidEqual(drained))) {
                    continue;
                }
                totalAmounts += drained.amount;
                FluidKey key = FluidKey.of(drained);
                if (!fluidIndex.containsKey(key)) {
                    fluidIndex.put(key, fluidIndex.size());
                    keys.add(key);
                    fluidAmounts.add(0);
                    providers.add(new ArrayList<>());
                }
                int index = fluidIndex.getInt(key);
                fluidAmounts.set(index, fluidAmounts.getLong(index) + drained.amount);
                providers.get(index).add(new FluidProvider(l, tank, drained.amount));
            }

            if (totalAmounts == 0) {
                return null;
            }

            long fluidOffset = Math.floorMod(this.fluidSelector, totalAmounts);

            int targetIndex = 0;
            long totalOffset = 0;
            for (; targetIndex < fluidAmounts.size(); targetIndex++) {
                totalOffset += fluidAmounts.getLong(targetIndex);
                if (fluidOffset < totalOffset) {
                    break;
                }
            }

            final FluidKey targetKey = keys.get(targetIndex);
            final List<FluidProvider> targetProviders = providers.get(targetIndex);
            final long targetAmount = fluidAmounts.getLong(targetIndex);

            if (!doDrain) {
                return targetKey.toStack((int) Math.min(targetAmount, maxDrain));
            }

            int drainedAmount = 0;
            int available = maxDrain;

            for (final FluidProvider provider : targetProviders) {
                final PartP2PLiquids l = provider.tunnel;
                final IFluidHandler tank = provider.tank;
                final ForgeDirection side = l.getSide().getOpposite();

                int amount = (int) Math.ceil(maxDrain * ((double) provider.amount / (double) targetAmount));
                if (amount > available) {
                    amount = available;
                }

                FluidStack targetStack = targetKey.toStack(amount);
                FluidStack drained = tank.drain(side, targetStack, true);
                if (drained == null && filter == null) {
                    // Bad tank...
                    final FluidStack wouldDrain = tank.drain(side, amount, false);
                    if (targetStack.isFluidEqual(wouldDrain)) {
                        drained = tank.drain(side, amount, true);
                    }
                }

                if (drained != null && targetStack.isFluidEqual(drained)) {
                    amount = Math.min(amount, drained.amount);
                    drainedAmount += amount;
                    available -= amount;
                    if (available <= 0) {
                        break;
                    }
                }
            }

            if (drainedAmount <= 0) {
                return null;
            }

            fluidSelector = Platform.getRandom().nextLong();

            return targetKey.toStack(drainedAmount);
        } finally {
            stack.pop();
        }
    }

    @Override
    public boolean canFill(final ForgeDirection from, final Fluid fluid) {
        if (from != this.getSide() || isBusy()) {
            return false;
        }

        final Stack<PartP2PLiquids> stack = this.getDepth();
        stack.push(this);
        try {
            return !this.getInsertTargets(fluid).isEmpty();
        } finally {
            stack.pop();
        }
    }

    @Override
    public boolean canDrain(final ForgeDirection from, final Fluid fluid) {
        if (from != this.getSide() || isBusy()) {
            return false;
        }
        final Stack<PartP2PLiquids> stack = this.getDepth();
        stack.push(this);
        try {
            return !this.getExtractTargets(fluid).isEmpty();
        } finally {
            stack.pop();
        }
    }

    @Override
    public FluidTankInfo[] getTankInfo(final ForgeDirection from) {
        if (from != this.getSide()) {
            return new FluidTankInfo[0];
        }

        if (this.getSideAccess().extract() != P2PRoute.NONE && !this.isBusy()) {
            final Stack<PartP2PLiquids> stack = this.getDepth();
            stack.push(this);
            try {
                final List<FluidTankInfo> infos = new LinkedList<>();
                for (final PartP2PLiquids t : this.getExtractTargets()) {
                    final IFluidHandler tank = t.getTarget();
                    final FluidTankInfo[] targetInfos = tank != null ? tank.getTankInfo(t.getSide().getOpposite())
                            : null;
                    if (targetInfos != null) {
                        Collections.addAll(infos, targetInfos);
                    }
                }
                if (!infos.isEmpty()) {
                    return infos.toArray(new FluidTankInfo[0]);
                }
            } finally {
                stack.pop();
            }
        }
        return this.getTank();
    }

    private FluidTankInfo[] getTank() {
        if (this.isOutput()) {
            final PartP2PLiquids tun = this.getInput();
            if (tun != null) {
                return ACTIVE_TANK;
            }
        } else {
            try {
                if (!this.getOutputs().isEmpty()) {
                    return ACTIVE_TANK;
                }
            } catch (final GridAccessException e) {
                // :(
            }
        }
        return INACTIVE_TANK;
    }

    private boolean isBusy() {
        for (final PartP2PLiquids t : this.getDepth()) {
            if (t == this) {
                return true;
            }
        }
        return false;
    }

    private static class FluidProvider {

        final PartP2PLiquids tunnel;
        final IFluidHandler tank;
        final int amount;

        public FluidProvider(PartP2PLiquids l, IFluidHandler tank, int amount) {
            this.tunnel = l;
            this.tank = tank;
            this.amount = amount;
        }
    }
}
