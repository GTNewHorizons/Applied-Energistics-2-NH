package com.glodblock.github.client.gui;

import net.minecraft.entity.player.InventoryPlayer;

import com.glodblock.github.client.gui.container.ContainerLevelWireless;
import com.glodblock.github.inventory.item.IWirelessTerminal;

import appeng.container.slot.AppEngSlot;

public class GuiLevelWireless extends GuiLevelTerminal {

    public GuiLevelWireless(final InventoryPlayer inventoryPlayer, final IWirelessTerminal te) {
        super(inventoryPlayer, new ContainerLevelWireless(inventoryPlayer, te));
    }

    @Override
    protected void repositionSlots() {
        for (final Object obj : this.inventorySlots.inventorySlots) {
            if (obj instanceof final AppEngSlot slot) {
                slot.yDisplayPosition = this.ySize + slot.getY() - 78 - 4;
            }
        }
    }
}
