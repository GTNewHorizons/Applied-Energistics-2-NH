package com.glodblock.github.inventory.item;

import net.minecraft.inventory.IInventory;

public interface IItemTerminal {

    IInventory getInventoryByName(final String name);

    default void saveSettings() {}

}
