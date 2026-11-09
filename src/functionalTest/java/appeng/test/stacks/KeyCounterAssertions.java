package appeng.test.stacks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

final class KeyCounterAssertions {

    private KeyCounterAssertions() {}

    static List<AEKey> keys(Iterable<Object2LongMap.Entry<AEKey>> entries) {
        List<AEKey> keys = new ArrayList<>();
        for (Object2LongMap.Entry<AEKey> entry : entries) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    static KeyCounter counterOf(List<? extends AEKey> keys) {
        KeyCounter counter = new KeyCounter();
        for (AEKey key : keys) {
            counter.add(key, 1);
        }
        return counter;
    }

    static void assertFound(KeyCounter counter, AEKey filter, FuzzyMode mode, AEKey... expected) {
        Set<AEKey> found = assertFindFuzzyMatchesFuzzyEquals(counter, filter, mode);
        assertEquals(new HashSet<>(Arrays.asList(expected)), found, () -> mode + " " + filter);
    }

    /**
     * Asserts that {@link KeyCounter#findFuzzy} finds each key once, exactly the keys that {@link AEKey#fuzzyEquals}
     * accepts, and reports their number as its size.
     *
     * @return the keys found
     */
    static Set<AEKey> assertFindFuzzyMatchesFuzzyEquals(KeyCounter counter, AEKey filter, FuzzyMode mode) {
        List<Object2LongMap.Entry<AEKey>> entries = new ArrayList<>(counter.findFuzzy(filter, mode));
        List<AEKey> found = keys(entries);
        Set<AEKey> expected = new HashSet<>();
        for (AEKey key : counter.keySet()) {
            if (filter.fuzzyEquals(key, mode)) {
                expected.add(key);
            }
        }

        Set<AEKey> distinct = new HashSet<>(found);
        assertEquals(found.size(), distinct.size(), () -> mode + " " + filter + " found duplicates in " + found);
        assertEquals(expected, distinct, () -> mode + " " + filter);
        assertEquals(found.size(), counter.findFuzzy(filter, mode).size(), () -> mode + " " + filter + " size");
        return distinct;
    }
}
