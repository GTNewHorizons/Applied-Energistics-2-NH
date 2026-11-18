package com.projecturanus.betterp2p.item;

public enum BetterMemoryCardModes {

    OUTPUT("gui.advanced_memory_card.mode.output", "gui.advanced_memory_card.mode.output.desc.1",
            "gui.advanced_memory_card.mode.output.desc.2", "gui.advanced_memory_card.mode.output.desc.3"),
    INPUT("gui.advanced_memory_card.mode.input", "gui.advanced_memory_card.mode.input.desc.1",
            "gui.advanced_memory_card.mode.input.desc.2", "gui.advanced_memory_card.mode.input.desc.3"),
    COPY("gui.advanced_memory_card.mode.copy", "gui.advanced_memory_card.mode.copy.desc.1",
            "gui.advanced_memory_card.mode.copy.desc.2", "gui.advanced_memory_card.mode.copy.desc.3",
            "gui.advanced_memory_card.mode.copy.desc.4"),
    UNBIND("gui.advanced_memory_card.mode.unbind", "gui.advanced_memory_card.mode.unbind.desc.1",
            "gui.advanced_memory_card.mode.unbind.desc.2");

    public static final int MAX_TOOLTIP_LENGTH = 40;

    private static final BetterMemoryCardModes[] VALUES = values();

    public final String unlocalizedName;
    public final String[] unlocalizedDesc;

    BetterMemoryCardModes(String unlocalizedName, String... unlocalizedDesc) {
        this.unlocalizedName = unlocalizedName;
        this.unlocalizedDesc = unlocalizedDesc;
    }

    public BetterMemoryCardModes next(boolean reverse) {
        return VALUES[Math.floorMod(ordinal() + (reverse ? -1 : 1), VALUES.length)];
    }
}
