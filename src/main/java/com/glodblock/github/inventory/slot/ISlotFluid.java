package com.glodblock.github.inventory.slot;

import javax.annotation.Nullable;

import appeng.api.storage.data.IAEItemStack;

public interface ISlotFluid {

    @Nullable
    IAEItemStack getAeStack();

    void setAeStack(@Nullable IAEItemStack stack, boolean sync);
}
