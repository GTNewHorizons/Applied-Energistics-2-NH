package appeng.api.stacks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import appeng.api.config.FuzzyMode;

/**
 * Damage levels of keys that support fuzzy range search, and the rules of their fuzzy partitions.
 * <p>
 * The break points of the fuzzy modes are nested, so a key is described by its damage level: the number of break points
 * its damage exceeds. A fuzzy mode splits the levels into two contiguous ranges, the keys damaged more than its break
 * point and all others, and a key matches a filter if both are in the same range.
 */
final class FuzzySearch {

    /** The fuzzy modes with a break point, by ascending break point. */
    private static final FuzzyMode[] BREAK_POINT_MODES;
    /** For each fuzzy mode, its index in {@link #BREAK_POINT_MODES}, or -1 for {@link FuzzyMode#IGNORE_ALL}. */
    private static final int[] BREAK_POINT_INDEX = new int[FuzzyMode.values().length];

    static final int LEVEL_COUNT;

    static {
        ArrayList<FuzzyMode> modes = new ArrayList<>();
        for (FuzzyMode mode : FuzzyMode.values()) {
            if (mode != FuzzyMode.IGNORE_ALL) {
                modes.add(mode);
            }
        }
        modes.sort(Comparator.comparingDouble(mode -> mode.percentage));
        BREAK_POINT_MODES = modes.toArray(new FuzzyMode[0]);

        Arrays.fill(BREAK_POINT_INDEX, -1);
        for (int i = 0; i < BREAK_POINT_MODES.length; i++) {
            BREAK_POINT_INDEX[BREAK_POINT_MODES[i].ordinal()] = i;
        }
        LEVEL_COUNT = BREAK_POINT_MODES.length + 1;
    }

    private FuzzySearch() {}

    static int getDamageLevel(int value, int maxValue) {
        if (maxValue <= 0) {
            return 0;
        }
        int level = 0;
        while (level < BREAK_POINT_MODES.length && isDamaged(value, maxValue, BREAK_POINT_MODES[level])) {
            level++;
        }
        return level;
    }

    private static boolean isDamaged(int value, int maxValue, FuzzyMode mode) {
        return value * 100.0 > mode.percentage * (double) maxValue;
    }

    static int getLowestLevel(int filterLevel, FuzzyMode fuzzy) {
        int index = BREAK_POINT_INDEX[fuzzy.ordinal()];
        if (index < 0) {
            return 0;
        }
        return filterLevel > index ? index + 1 : 0;
    }

    static int getHighestLevel(int filterLevel, FuzzyMode fuzzy) {
        int index = BREAK_POINT_INDEX[fuzzy.ordinal()];
        if (index < 0) {
            return LEVEL_COUNT - 1;
        }
        return filterLevel > index ? LEVEL_COUNT - 1 : index;
    }

    static boolean matches(int filterLevel, FuzzyMode fuzzy, int candidateLevel) {
        int index = BREAK_POINT_INDEX[fuzzy.ordinal()];
        return index < 0 || (filterLevel > index) == (candidateLevel > index);
    }
}
