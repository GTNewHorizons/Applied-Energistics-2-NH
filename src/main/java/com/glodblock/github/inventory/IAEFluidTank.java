package com.glodblock.github.inventory;

import net.minecraftforge.fluids.IFluidHandler;

import appeng.api.storage.data.IAEFluidStack;

public interface IAEFluidTank extends IFluidHandler {

    void setFluidInSlot(final int slot, final IAEFluidStack fluid);

    IAEFluidStack getFluidInSlot(final int slot);

    int getSlots();
}
