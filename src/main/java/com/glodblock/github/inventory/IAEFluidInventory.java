package com.glodblock.github.inventory;

public interface IAEFluidInventory {

    void onFluidInventoryChanged(final IAEFluidTank inv, final int slot);

    AEFluidInventory getInternalFluid();
}
