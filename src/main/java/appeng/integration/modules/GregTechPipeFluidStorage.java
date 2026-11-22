package appeng.integration.modules;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import appeng.me.storage.MEMonitorIFluidHandler;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.implementations.MTEFluidPipe;

/** Gives AE2 access to the adjacent pipe block's lanes without opening a transport connection. */
final class GregTechPipeFluidStorage implements IFluidHandler {

    private final BaseMetaPipeEntity tile;
    private final ForgeDirection side;

    private GregTechPipeFluidStorage(BaseMetaPipeEntity tile, ForgeDirection side) {
        this.tile = tile;
        this.side = side;
    }

    static MEMonitorIFluidHandler createMonitor(BaseMetaPipeEntity tile, ForgeDirection side) {
        final var storage = new GregTechPipeFluidStorage(tile, side);
        return new MEMonitorIFluidHandler(storage, side) {

            @Override
            protected boolean canExtractFromTank(FluidTankInfo tank) {
                if (tank.fluid == null || tank.fluid.amount <= 0) return false;
                final FluidStack probe = tank.fluid.copy();
                probe.amount = 1;
                return storage.drain(side, probe, false) != null;
            }
        };
    }

    private MTEFluidPipe getPipe(ForgeDirection from) {
        if (from == this.side && this.tile.canAccessData()
                && this.tile.getMetaTileEntity() instanceof MTEFluidPipe pipe) {
            return pipe;
        }
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return fluid != null && this.fill(from, new FluidStack(fluid, 1), false) == 1;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return fluid != null && this.drain(from, new FluidStack(fluid, 1), false) != null;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        final MTEFluidPipe pipe = this.getPipe(from);
        if (pipe == null || resource == null || resource.amount <= 0 || resource.getFluid() == null
                || !pipe.isLiquidInput(from) || !this.tile.getCoverAtSide(from).letsFluidIn(resource.getFluid())) {
            return 0;
        }

        // The pipe multiplies the incoming amount by its lane count before checking capacity.
        final int maxOffer = Integer.MAX_VALUE / pipe.mPipeAmount;
        if (resource.amount > maxOffer) {
            resource = resource.copy();
            resource.amount = maxOffer;
        }
        return pipe.fill_default(from, resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        final MTEFluidPipe pipe = this.getPipe(from);
        if (pipe == null || resource == null || resource.amount <= 0 || resource.getFluid() == null
                || !pipe.isLiquidOutput(from) || !this.tile.getCoverAtSide(from).letsFluidOut(resource.getFluid())) {
            return null;
        }
        return pipe.drain(from, resource, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) return null;
        for (FluidTankInfo tank : this.getTankInfo(from)) {
            if (tank.fluid == null || tank.fluid.amount <= 0) continue;
            final FluidStack request = tank.fluid.copy();
            request.amount = Math.min(request.amount, maxDrain);
            final FluidStack drained = this.drain(from, request, doDrain);
            if (drained != null) return drained;
        }
        return null;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return this.getPipe(from) == null ? new FluidTankInfo[0] : this.tile.getTankInfo(from);
    }
}
