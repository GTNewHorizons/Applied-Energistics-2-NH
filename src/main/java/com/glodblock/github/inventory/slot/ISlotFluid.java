package com.glodblock.github.inventory.slot;

import org.jspecify.annotations.Nullable;

import appeng.api.storage.data.IAEItemStack;

public interface ISlotFluid {

    @Nullable
    IAEItemStack getAeStack();

    void setAeStack(@Nullable IAEItemStack stack, boolean sync);
}
