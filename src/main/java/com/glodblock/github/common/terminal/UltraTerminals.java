package com.glodblock.github.common.terminal;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.common.item.ItemWirelessUltraTerminal;

/**
 * Helpers around the ultra terminal item.
 */
public final class UltraTerminals {

    public static boolean isUltraTerminal(final @Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemWirelessUltraTerminal;
    }

    private UltraTerminals() {}
}
