package com.projecturanus.betterp2p.client.gui.widget;

import java.util.function.IntUnaryOperator;

public enum GuiScale {

    SMALL(availableHeight -> 4, 0, "gui.advanced_memory_card.gui_scale.small"),
    NORMAL(availableHeight -> 6, 326, "gui.advanced_memory_card.gui_scale.normal"),
    LARGE(availableHeight -> 8, 409, "gui.advanced_memory_card.gui_scale.large"),
    DYNAMIC(availableHeight -> availableHeight / P2PEntryConstants.HEIGHT, 0,
            "gui.advanced_memory_card.gui_scale.dynamic");

    private static final GuiScale[] VALUES = values();

    private final IntUnaryOperator size;
    public final int minHeight;
    public final String unlocalizedName;

    GuiScale(IntUnaryOperator size, int minHeight, String unlocalizedName) {
        this.size = size;
        this.minHeight = minHeight;
        this.unlocalizedName = unlocalizedName;
    }

    public int size(int availableHeight) {
        return size.applyAsInt(availableHeight);
    }

    public static GuiScale fromOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : DYNAMIC;
    }

    public GuiScale next(boolean reverse) {
        return VALUES[Math.floorMod(ordinal() + (reverse ? -1 : 1), VALUES.length)];
    }
}
