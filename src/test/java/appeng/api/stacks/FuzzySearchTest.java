package appeng.api.stacks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import appeng.api.config.FuzzyMode;

public class FuzzySearchTest {

    @Test
    public void damageLevelRisesJustAboveEachBreakPoint() {
        assertLevel(0, 0, 100);
        assertLevel(1, 1, 100);
        assertLevel(1, 25, 100);
        assertLevel(2, 26, 100);
        assertLevel(2, 50, 100);
        assertLevel(3, 51, 100);
        assertLevel(3, 75, 100);
        assertLevel(4, 76, 100);
        assertLevel(4, 90, 100);
        assertLevel(5, 91, 100);
        assertLevel(5, 99, 100);
        assertLevel(6, 100, 100);
        assertEquals(7, FuzzySearch.LEVEL_COUNT);
    }

    @Test
    public void damageLevelOfMaxThatBreakPointsDoNotDivide() {
        assertLevel(0, 0, 3);
        assertLevel(2, 1, 3);
        assertLevel(3, 2, 3);
        assertLevel(6, 3, 3);
    }

    @Test
    public void damageAboveMaxIsFullyDamaged() {
        assertLevel(6, 150, 100);
        assertLevel(6, Integer.MAX_VALUE, 1);
    }

    @Test
    public void keyWithoutMaxIsUndamaged() {
        assertLevel(0, 50, 0);
        assertLevel(0, 50, -1);
    }

    @Test
    public void negativeDamageIsUndamaged() {
        assertLevel(0, -5, 100);
    }

    @Test
    public void damageLevelOfLargeValuesDoesNotOverflow() {
        assertLevel(2, 1073741823, Integer.MAX_VALUE);
        assertLevel(3, 1073741824, Integer.MAX_VALUE);
        assertLevel(6, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void breakPointOfEachModeIsUndamaged() {
        int undamaged = FuzzySearch.getDamageLevel(0, 1000);
        for (FuzzyMode mode : FuzzyMode.values()) {
            if (mode == FuzzyMode.IGNORE_ALL) {
                continue;
            }
            int breakPoint = (int) mode.percentage * 10;
            assertTrue(mode.name(), FuzzySearch.matches(undamaged, mode, FuzzySearch.getDamageLevel(breakPoint, 1000)));
            assertFalse(
                    mode.name(),
                    FuzzySearch.matches(undamaged, mode, FuzzySearch.getDamageLevel(breakPoint + 1, 1000)));
        }
    }

    @Test
    public void matchesComparesDamagePercentageWithBreakPoint() {
        for (int max : new int[] { 1, 3, 7, 100 }) {
            for (FuzzyMode mode : FuzzyMode.values()) {
                for (int filterValue = 0; filterValue <= max + 1; filterValue++) {
                    int filterLevel = FuzzySearch.getDamageLevel(filterValue, max);
                    for (int value = 0; value <= max + 1; value++) {
                        boolean expected = mode == FuzzyMode.IGNORE_ALL
                                || isDamaged(filterValue, max, mode) == isDamaged(value, max, mode);
                        assertEquals(
                                mode + " " + filterValue + "/" + max + " " + value + "/" + max,
                                expected,
                                FuzzySearch.matches(filterLevel, mode, FuzzySearch.getDamageLevel(value, max)));
                    }
                }
            }
        }
    }

    @Test
    public void levelRangeContainsExactlyMatchingLevels() {
        for (FuzzyMode mode : FuzzyMode.values()) {
            for (int filterLevel = 0; filterLevel < FuzzySearch.LEVEL_COUNT; filterLevel++) {
                int lowest = FuzzySearch.getLowestLevel(filterLevel, mode);
                int highest = FuzzySearch.getHighestLevel(filterLevel, mode);
                for (int level = 0; level < FuzzySearch.LEVEL_COUNT; level++) {
                    assertEquals(
                            mode + " filter level " + filterLevel + " level " + level,
                            FuzzySearch.matches(filterLevel, mode, level),
                            lowest <= level && level <= highest);
                }
            }
        }
    }

    @Test
    public void ignoreAllSelectsEveryLevel() {
        for (int level = 0; level < FuzzySearch.LEVEL_COUNT; level++) {
            assertEquals(0, FuzzySearch.getLowestLevel(level, FuzzyMode.IGNORE_ALL));
            assertEquals(FuzzySearch.LEVEL_COUNT - 1, FuzzySearch.getHighestLevel(level, FuzzyMode.IGNORE_ALL));
        }
    }

    @Test
    public void matchesIsSymmetric() {
        for (FuzzyMode mode : FuzzyMode.values()) {
            for (int a = 0; a < FuzzySearch.LEVEL_COUNT; a++) {
                for (int b = 0; b < FuzzySearch.LEVEL_COUNT; b++) {
                    assertEquals(FuzzySearch.matches(a, mode, b), FuzzySearch.matches(b, mode, a));
                }
            }
        }
    }

    private static void assertLevel(int expected, int value, int maxValue) {
        assertEquals(value + "/" + maxValue, expected, FuzzySearch.getDamageLevel(value, maxValue));
    }

    private static boolean isDamaged(int value, int maxValue, FuzzyMode mode) {
        return (long) value * 100 > (long) mode.percentage * maxValue;
    }
}
