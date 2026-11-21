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
     * Ordinal order of the removed {@code UltraTerminalModes} enum, used to migrate old items.
     */
    private static final String[] LEGACY_ORDER = { AE2FC_CRAFTING, AE2FC_PATTERN, AE2FC_PATTERN_EX, AE2FC_INTERFACE,
            AE2FC_LEVEL };

    private static final List<String> LEGACY_TYPES = Collections.unmodifiableList(Arrays.asList(LEGACY_ORDER));

    /**
     * @return the terminal id stored by the old enum based implementation, or null if the ordinal is unknown.
     */
    @Nullable
    public static String fromLegacyOrdinal(final int ordinal) {
        return ordinal < 0 || ordinal >= LEGACY_ORDER.length ? null : LEGACY_ORDER[ordinal];
    }

    /**
     * @return all five terminals supported by every old enum based ultra terminal, regardless of its selected mode.
     */
    public static List<String> legacyTypes() {
        return LEGACY_TYPES;
    }

    private TerminalIds() {}
}
