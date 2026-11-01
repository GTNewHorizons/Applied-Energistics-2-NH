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

import java.util.List;
import java.util.Stack;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.config.PowerUnits;
import appeng.integration.IntegrationType;
import appeng.integration.modules.helpers.NullRFHandler;
import appeng.transformer.annotations.Integration.Interface;
import appeng.transformer.annotations.Integration.InterfaceList;
import appeng.util.Platform;
import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@InterfaceList(
        value = { @Interface(iface = "cofh.api.energy.IEnergyReceiver", iname = IntegrationType.RF),
                @Interface(iface = "cofh.api.energy.IEnergyProvider", iname = IntegrationType.RF) })
public final class PartP2PRFPower extends PartP2PRoutedTunnel<PartP2PRFPower>
        implements IEnergyReceiver, IEnergyProvider {

    private static final ThreadLocal<Stack<PartP2PRFPower>> THREAD_STACK = new ThreadLocal<>();
    /**
     * Default element based on the null element pattern
     */
    private static final IEnergyHandler NULL_HANDLER = new NullRFHandler();

    private boolean cachedReceiver = false;
    private IEnergyReceiver receiverTarget;

    private boolean cachedProvider = false;
    private IEnergyProvider providerTarget;

    public PartP2PRFPower(final ItemStack is) {
        super(is);
    }

    @Override
    protected P2PTransportPolicy getTransportPolicy() {
        return P2PTransportPolicy.ONE_WAY_PUSH;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getTypeTexture() {
        return Blocks.iron_block.getBlockTextureFromSide(0);
    }

    @Override
    public void onTunnelNetworkChange() {
        super.onTunnelNetworkChange();
        this.getHost().notifyNeighbors();
    }

    @Override
    public void onNeighborChanged() {
        this.cachedReceiver = false;
        this.cachedProvider = false;
        super.onNeighborChanged();
    }

    @Override
    public int receiveEnergy(final ForgeDirection from, int maxReceive, final boolean simulate) {
        if (!this.isActive() || this.isBusy()) {
            return 0;
        }

        final Stack<PartP2PRFPower> stack = this.getDepth();

        stack.push(this);

        try {
            final List<PartP2PRFPower> targets = this.getInsertTargets();

            int total = 0;

            for (final PartP2PRFPower t : targets) {
                if (Platform.getRandomInt() % 2 > 0) {
                    final int receiver = t.getReceiver().receiveEnergy(t.getSide().getOpposite(), maxReceive, simulate);
                    maxReceive -= receiver;
                    total += receiver;

                    if (maxReceive <= 0) {
                        break;
                    }
                }
            }

            if (maxReceive > 0) {
                for (final PartP2PRFPower t : targets) {
                    final int receiver = t.getReceiver().receiveEnergy(t.getSide().getOpposite(), maxReceive, simulate);
                    maxReceive -= receiver;
                    total += receiver;

                    if (maxReceive <= 0) {
                        break;
                    }
                }
            }

            if (!simulate && total > 0) {
                this.queueTunnelDrain(PowerUnits.RF, total);
            }

            return total;
        } finally {
            stack.pop();
        }
    }

    @Override
    public int extractEnergy(final ForgeDirection from, final int maxExtract, final boolean simulate) {
        if (!this.isActive() || this.isBusy()) {
            return 0;
        }

        final Stack<PartP2PRFPower> stack = this.getDepth();
        stack.push(this);

        try {
            int total = 0;
            for (PartP2PRFPower t : this.getExtractTargets()) {
                final IEnergyProvider provider = t.getProvider();
                total += provider.extractEnergy(t.getSide().getOpposite(), maxExtract - total, simulate);
                if (total >= maxExtract) {
                    break;
                }
            }
            if (!simulate && total > 0) {
                this.queueTunnelDrain(PowerUnits.RF, total);
            }
            return total;
        } finally {
            stack.pop();
        }
    }

    private Stack<PartP2PRFPower> getDepth() {
        Stack<PartP2PRFPower> s = THREAD_STACK.get();

        if (s == null) {
            THREAD_STACK.set(s = new Stack<>());
        }

        return s;
    }

    private IEnergyReceiver getReceiver() {
        if (!this.cachedReceiver) {
            final TileEntity self = this.getTile();
            final TileEntity te = self.getWorldObj().getTileEntity(
                    self.xCoord + this.getSide().offsetX,
                    self.yCoord + this.getSide().offsetY,
                    self.zCoord + this.getSide().offsetZ);
            this.receiverTarget = te instanceof IEnergyReceiver ? (IEnergyReceiver) te : null;
            this.cachedReceiver = true;
        }

        if (this.receiverTarget == null || !this.receiverTarget.canConnectEnergy(this.getSide().getOpposite())) {
            return NULL_HANDLER;
        }

        return this.receiverTarget;
    }

    private IEnergyProvider getProvider() {
        if (!this.cachedProvider) {
            final TileEntity self = this.getTile();
            final TileEntity te = self.getWorldObj().getTileEntity(
                    self.xCoord + this.getSide().offsetX,
                    self.yCoord + this.getSide().offsetY,
                    self.zCoord + this.getSide().offsetZ);
            this.providerTarget = te instanceof IEnergyProvider ? (IEnergyProvider) te : null;
            this.cachedProvider = true;
        }

        if (this.providerTarget == null || !this.providerTarget.canConnectEnergy(this.getSide().getOpposite())) {
            return NULL_HANDLER;
        }

        return this.providerTarget;
    }

    @Override
    public int getEnergyStored(final ForgeDirection from) {
        if (!this.isActive() || this.isBusy()) {
            return 0;
        }

        final Stack<PartP2PRFPower> stack = this.getDepth();

        stack.push(this);

        try {
            int total = 0;
            for (final PartP2PRFPower t : this.getExtractTargets()) {
                total += t.getProvider().getEnergyStored(t.getSide().getOpposite());
            }

            return total;
        } finally {
            stack.pop();
        }
    }

    @Override
    public int getMaxEnergyStored(final ForgeDirection from) {
        if (!this.isActive() || this.isBusy()) {
            return 0;
        }

        final Stack<PartP2PRFPower> stack = this.getDepth();

        stack.push(this);

        try {
            int total = 0;
            for (final PartP2PRFPower t : this.getExtractTargets()) {
                total += t.getProvider().getMaxEnergyStored(t.getSide().getOpposite());
            }
            return total;
        } finally {
            stack.pop();
        }
    }

    @Override
    public boolean canConnectEnergy(final ForgeDirection from) {
        return true;
    }

    private boolean isBusy() {
        for (final PartP2PRFPower t : this.getDepth()) {
            if (t == this) {
                return true;
            }
        }
        return false;
    }
}
