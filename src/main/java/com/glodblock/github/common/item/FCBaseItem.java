package com.glodblock.github.common.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.glodblock.github.loader.IRegister;

public abstract class FCBaseItem extends Item implements IRegister<FCBaseItem> {

    public ItemStack stack(int size, int meta) {
        return new ItemStack(this, size, meta);
    }

    public ItemStack stack(int size) {
        return new ItemStack(this, size);
    }

    public ItemStack stack() {
        return new ItemStack(this, 1);
    }
}
