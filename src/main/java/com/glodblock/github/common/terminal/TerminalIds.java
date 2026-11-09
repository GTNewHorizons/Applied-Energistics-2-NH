package com.glodblock.github.common.terminal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Stable identifiers of the terminal types registered by AE2NH and its bundled addons.
 */
public final class TerminalIds {

    public static final String AE2_WIRELESS = "appliedenergistics2:wireless";
    public static final String AE2FC_CRAFTING = "ae2fc:crafting";
    public static final String AE2FC_PATTERN = "ae2fc:pattern";
    public static final String AE2FC_PATTERN_EX = "ae2fc:pattern_ex";
    public static final String AE2FC_INTERFACE = "ae2fc:interface";
    public static final String AE2FC_LEVEL = "ae2fc:level";
    public static final String WCT_CRAFTING = "ae2wct:crafting";

    /**
     * Ore dictionary name that contains the wireless terminals which can be combined into the ultra terminal.
     */
    public static final String TERMINAL_OREDICT = "ae2fcUltraTerminal";

    /**
     * Ordinal order of the removed {@code UltraTerminalModes} enum, used to migrate old items.
     */
    private static final String[] LEGACY_ORDER = { AE2FC_CRAFTING, AE2FC_PATTERN, AE2FC_PATTERN_EX, AE2FC_INTERFACE,
            AE2FC_LEVEL };

    /**
     * Terminals the old enum based terminal could open together with a pattern terminal.
     */
    private static final List<String> LEGACY_PATTERNS = Collections
            .unmodifiableList(Arrays.asList(AE2FC_PATTERN, AE2FC_PATTERN_EX));

    /**
     * @return the terminal id stored by the old enum based implementation, or null if the ordinal is unknown.
     */
    @Nullable
    public static String fromLegacyOrdinal(final int ordinal) {
        return ordinal < 0 || ordinal >= LEGACY_ORDER.length ? null : LEGACY_ORDER[ordinal];
    }

    /**
     * @return all terminals the old enum based terminal could open while it stored the given terminal, or null if the
     *         terminal has no legacy group.
     */
    @Nullable
    public static List<String> legacyGroup(final @Nullable String id) {
        return AE2FC_PATTERN.equals(id) || AE2FC_PATTERN_EX.equals(id) ? LEGACY_PATTERNS : null;
    }

    private TerminalIds() {}
}
