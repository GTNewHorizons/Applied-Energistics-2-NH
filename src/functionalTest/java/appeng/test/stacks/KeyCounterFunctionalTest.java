package appeng.test.stacks;

import static appeng.test.stacks.FuzzyTestItems.TOOL;
import static appeng.test.stacks.FuzzyTestItems.VARIANTS;
import static appeng.test.stacks.KeyCounterAssertions.assertFindFuzzyMatchesFuzzyEquals;
import static appeng.test.stacks.KeyCounterAssertions.assertFound;
import static appeng.test.stacks.KeyCounterAssertions.counterOf;
import static appeng.test.stacks.KeyCounterAssertions.keys;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

public class KeyCounterFunctionalTest {

    /** Damages out of 100 with one in each fuzzy damage level, from least to most damaged. */
    private static final int[] LEVEL_DAMAGES = { 0, 1, 26, 51, 76, 91, 100 };

    @Test
    public void emptyCounterHasNoEntries() {
        KeyCounter counter = new KeyCounter();

        assertEmpty(counter);
        for (AEKey key : keysOfEveryKind()) {
            assertEquals(0, counter.get(key), key::toString);
            assertEquals(0, counter.remove(key), key::toString);
            assertTrue(counter.findFuzzy(key, FuzzyMode.IGNORE_ALL).isEmpty(), key::toString);
        }
        assertEmpty(counter);
    }

    @Test
    public void addAccumulatesAmountsPerKey() {
        List<AEKey> keys = keysOfEveryKind();
        KeyCounter counter = new KeyCounter();
        for (int i = 0; i < keys.size(); i++) {
            counter.add(keys.get(i), i + 1);
            counter.add(keys.get(i), 10);
        }

        assertFalse(counter.isEmpty());
        assertEquals(keys.size(), counter.size());
        for (int i = 0; i < keys.size(); i++) {
            assertEquals(i + 11, counter.get(keys.get(i)), keys.get(i)::toString);
        }
    }

    @Test
    public void removeAmountKeepsKeyWithNegativeAmount() {
        for (AEKey key : keysOfEveryKind()) {
            KeyCounter counter = new KeyCounter();
            counter.add(key, 5);
            counter.remove(key, 8);
            assertEquals(-3, counter.get(key), key::toString);
            assertEquals(1, counter.size(), key::toString);

            counter.remove(key, 2);
            assertEquals(-5, counter.get(key), key::toString);
        }
    }

    @Test
    public void setReplacesAmount() {
        List<AEKey> keys = keysOfEveryKind();
        KeyCounter counter = new KeyCounter();
        for (AEKey key : keys) {
            counter.set(key, 5);
            counter.add(key, 1);
            counter.set(key, 2);
        }
        for (AEKey key : keys) {
            assertEquals(2, counter.get(key), key::toString);
        }

        for (AEKey key : keys) {
            counter.set(key, 0);
        }
        assertEquals(keys.size(), counter.size());
        assertEquals(new HashSet<>(keys), counter.keySet());
    }

    @Test
    public void removeKeyReturnsAmountAndKeepsOtherKeys() {
        List<AEKey> keys = keysOfEveryKind();
        assertRemovesOneByOne(keys);

        List<AEKey> reversed = new ArrayList<>(keys);
        Collections.reverse(reversed);
        assertRemovesOneByOne(reversed);
    }

    @Test
    public void iterationVisitsEveryEntryOnce() {
        Map<AEKey, Long> expected = amountsOfEveryKind();
        KeyCounter counter = counterWith(expected);

        Map<AEKey, Long> iterated = new HashMap<>();
        int count = 0;
        for (Object2LongMap.Entry<AEKey> entry : counter) {
            iterated.put(entry.getKey(), entry.getLongValue());
            count++;
        }
        assertEquals(expected, iterated);
        assertEquals(expected.size(), count);

        Map<AEKey, Long> visited = new HashMap<>();
        counter.forEachEntry((key, amount) -> assertNull(visited.put(key, amount), key::toString));
        assertEquals(expected, visited);

        assertEquals(expected.keySet(), counter.keySet());
    }

    @Test
    public void iteratorEntriesWriteThrough() {
        Map<AEKey, Long> amounts = amountsOfEveryKind();
        KeyCounter counter = counterWith(amounts);

        for (Object2LongMap.Entry<AEKey> entry : counter) {
            entry.setValue(entry.getLongValue() * 2);
        }
        for (Map.Entry<AEKey, Long> entry : amounts.entrySet()) {
            assertEquals(entry.getValue() * 2, counter.get(entry.getKey()), entry.getKey()::toString);
        }
    }

    @Test
    public void iteratorRemovesEntries() {
        Map<AEKey, Long> amounts = amountsOfEveryKind();
        KeyCounter counter = counterWith(amounts);

        Iterator<Object2LongMap.Entry<AEKey>> iterator = counter.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getLongValue() % 2 == 0) {
                iterator.remove();
            }
        }

        int remaining = 0;
        for (Map.Entry<AEKey, Long> entry : amounts.entrySet()) {
            long expected = entry.getValue() % 2 == 0 ? 0 : entry.getValue();
            assertEquals(expected, counter.get(entry.getKey()), entry.getKey()::toString);
            if (expected != 0) {
                remaining++;
            }
        }
        assertEquals(remaining, counter.size());
    }

    @Test
    public void getFirstEntryFindsKeyOfClass() {
        AEItemKey item = AEItemKey.of(VARIANTS, 0);
        AEFluidKey water = AEFluidKey.of(FluidRegistry.WATER);
        TestKey testKey = TestKey.fuzzy("a", 0, 100);
        KeyCounter counter = new KeyCounter();
        counter.add(item, 1);
        counter.add(water, 2);
        counter.add(testKey, 3);

        assertEquals(item, counter.getFirstKey(AEItemKey.class));
        assertEquals(water, counter.getFirstKey(AEFluidKey.class));
        assertEquals(testKey, counter.getFirstKey(TestKey.class));
        Object2LongMap.Entry<AEKey> waterEntry = counter.getFirstEntry(AEFluidKey.class);
        assertNotNull(waterEntry);
        assertEquals(2, waterEntry.getLongValue());

        AEKey first = counter.getFirstKey();
        assertNotNull(first);
        assertTrue(counter.keySet().contains(first));
        Object2LongMap.Entry<AEKey> firstEntry = counter.getFirstEntry();
        assertNotNull(firstEntry);
        assertEquals(counter.get(firstEntry.getKey()), firstEntry.getLongValue());

        counter.remove(water);
        assertNull(counter.getFirstKey(AEFluidKey.class));
        assertNull(counter.getFirstEntry(AEFluidKey.class));
    }

    @Test
    public void addAllAddsAmountsOfOtherCounter() {
        List<AEKey> keys = keysOfEveryKind();
        KeyCounter target = new KeyCounter();
        KeyCounter source = new KeyCounter();
        for (int i = 0; i < keys.size(); i++) {
            if (i % 2 == 0) {
                target.add(keys.get(i), i + 1);
            }
            source.add(keys.get(i), 100 + i);
        }

        target.addAll(source);
        for (int i = 0; i < keys.size(); i++) {
            long expected = (i % 2 == 0 ? i + 1 : 0) + 100 + i;
            assertEquals(expected, target.get(keys.get(i)), keys.get(i)::toString);
            assertEquals(100 + i, source.get(keys.get(i)), keys.get(i)::toString);
        }
        assertEquals(keys.size(), target.size());
    }

    @Test
    public void removeAllSubtractsAmountsOfOtherCounter() {
        List<AEKey> keys = keysOfEveryKind();
        KeyCounter target = new KeyCounter();
        KeyCounter source = new KeyCounter();
        for (int i = 0; i < keys.size(); i++) {
            if (i % 2 == 0) {
                target.add(keys.get(i), 1000 + i);
            }
            source.add(keys.get(i), 100 + i);
        }

        target.removeAll(source);
        for (int i = 0; i < keys.size(); i++) {
            long expected = (i % 2 == 0 ? 1000 + i : 0) - (100 + i);
            assertEquals(expected, target.get(keys.get(i)), keys.get(i)::toString);
            assertEquals(100 + i, source.get(keys.get(i)), keys.get(i)::toString);
        }
        assertEquals(keys.size(), target.size());
    }

    @Test
    public void mergedCountersStayIndependent() {
        Map<AEKey, Long> amounts = amountsOfEveryKind();
        for (boolean subtract : new boolean[] { false, true }) {
            KeyCounter source = counterWith(amounts);
            KeyCounter target = new KeyCounter();
            if (subtract) {
                target.removeAll(source);
            } else {
                target.addAll(source);
            }

            for (AEKey key : amounts.keySet()) {
                target.add(key, 1_000_000);
                AEKey variant = variantOf(key);
                if (variant != null) {
                    target.add(variant, 1);
                }
            }
            assertEquals(amounts, amountsOf(source), () -> "source after changing target, subtract: " + subtract);

            for (AEKey key : amounts.keySet()) {
                source.add(key, 1);
            }
            for (Map.Entry<AEKey, Long> entry : amounts.entrySet()) {
                long merged = subtract ? -entry.getValue() : entry.getValue();
                assertEquals(merged + 1_000_000, target.get(entry.getKey()), entry.getKey()::toString);
            }
        }
    }

    @Test
    public void mergedCountersKeepFuzzySearch() {
        List<AEKey> keys = keysOfEveryKind();
        KeyCounter source = counterOf(keys);
        KeyCounter target = new KeyCounter();
        target.addAll(source);

        for (FuzzyMode mode : FuzzyMode.values()) {
            for (AEKey filter : keys) {
                assertEquals(
                        assertFindFuzzyMatchesFuzzyEquals(source, filter, mode),
                        assertFindFuzzyMatchesFuzzyEquals(target, filter, mode));
            }
        }
    }

    @Test
    public void resetKeepsKeysWithZeroAmounts() {
        Map<AEKey, Long> amounts = amountsOfEveryKind();
        KeyCounter counter = counterWith(amounts);

        counter.reset();
        assertEquals(amounts.size(), counter.size());
        for (Object2LongMap.Entry<AEKey> entry : counter) {
            assertEquals(0, entry.getLongValue(), entry.getKey()::toString);
        }

        counter.removeZeros();
        assertEmpty(counter);
    }

    @Test
    public void clearedCounterStaysUsable() {
        List<AEKey> keys = keysOfEveryKind();
        KeyCounter counter = counterOf(keys);

        counter.clear();
        assertEmpty(counter);
        for (AEKey key : keys) {
            assertEquals(0, counter.get(key), key::toString);
            assertTrue(counter.findFuzzy(key, FuzzyMode.IGNORE_ALL).isEmpty(), key::toString);
        }

        for (int i = 0; i < keys.size(); i++) {
            counter.add(keys.get(i), i + 1);
        }
        assertEquals(keys.size(), counter.size());
        for (int i = 0; i < keys.size(); i++) {
            AEKey key = keys.get(i);
            assertEquals(i + 1, counter.get(key), key::toString);
            assertTrue(keys(counter.findFuzzy(key, FuzzyMode.PERCENT_99)).contains(key), key::toString);
        }
    }

    @Test
    public void removeZerosDropsOnlyZeroAmounts() {
        List<AEKey> keys = keysOfEveryKind();
        KeyCounter counter = new KeyCounter();
        Map<AEKey, Long> expected = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            long amount = i % 3 == 0 ? 0 : i % 3 == 1 ? -i : i;
            counter.set(keys.get(i), amount);
            if (amount != 0) {
                expected.put(keys.get(i), amount);
            }
        }

        counter.removeZeros();
        assertEquals(expected, amountsOf(counter));
        assertEquals(expected.size(), counter.size());
    }

    @Test
    public void removeEmptySubmapsKeepsZeroAmounts() {
        List<AEKey> keys = keysOfEveryKind();
        KeyCounter counter = counterOf(keys);
        for (AEKey key : keys) {
            counter.set(key, 0);
        }

        counter.removeEmptySubmaps();
        assertEquals(keys.size(), counter.size());
        assertEquals(new HashSet<>(keys), counter.keySet());

        Iterator<Object2LongMap.Entry<AEKey>> iterator = counter.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
        counter.removeEmptySubmaps();
        assertEmpty(counter);

        for (AEKey key : keys) {
            counter.add(key, 4);
        }
        for (AEKey key : keys) {
            assertEquals(4, counter.get(key), key::toString);
        }
    }

    @Test
    public void findFuzzyOfFluidFindsOnlyThatFluid() {
        AEFluidKey water = AEFluidKey.of(FluidRegistry.WATER);
        AEFluidKey lava = AEFluidKey.of(FluidRegistry.LAVA);
        KeyCounter counter = counterOf(Arrays.asList(water, AEItemKey.of(VARIANTS, 0), TestKey.fuzzy("a", 0, 100)));

        for (FuzzyMode mode : FuzzyMode.values()) {
            assertFound(counter, water, mode, water);
            assertFound(counter, lava, mode);
        }
    }

    @Test
    public void findFuzzyOfFuzzyKeysSelectsDamagePartition() {
        List<AEKey> keys = new ArrayList<>();
        for (int damage : LEVEL_DAMAGES) {
            keys.add(TestKey.fuzzy("a", damage, 100));
            keys.add(TestKey.fuzzy("b", damage, 100));
        }
        TestKey otherPrimary = TestKey.fuzzy("a", 0, 100).withOtherPrimaryKey();
        keys.add(otherPrimary);
        KeyCounter counter = counterOf(keys);

        List<AEKey> filters = new ArrayList<>(keys);
        filters.add(TestKey.fuzzy("filter", 10, 100));
        filters.add(TestKey.fuzzy("filter", 60, 100));
        for (FuzzyMode mode : FuzzyMode.values()) {
            for (AEKey filter : filters) {
                assertFindFuzzyMatchesFuzzyEquals(counter, filter, mode);
            }
        }

        assertFound(
                counter,
                TestKey.fuzzy("filter", 10, 100),
                FuzzyMode.PERCENT_50,
                TestKey.fuzzy("a", 0, 100),
                TestKey.fuzzy("b", 0, 100),
                TestKey.fuzzy("a", 1, 100),
                TestKey.fuzzy("b", 1, 100),
                TestKey.fuzzy("a", 26, 100),
                TestKey.fuzzy("b", 26, 100));
        assertFound(counter, otherPrimary, FuzzyMode.IGNORE_ALL, otherPrimary);
    }

    @Test
    public void findFuzzyOfKeysWithoutFuzzyRangeSearchFindsEveryVariant() {
        TestKey undamaged = TestKey.plain("a", 0, 100);
        TestKey damaged = TestKey.plain("b", 60, 100);
        TestKey broken = TestKey.plain("c", 100, 100);
        KeyCounter counter = counterOf(Arrays.asList(undamaged, damaged, broken, undamaged.withOtherPrimaryKey()));

        for (FuzzyMode mode : FuzzyMode.values()) {
            assertFound(counter, undamaged, mode, undamaged, damaged, broken);
            assertFound(counter, broken, mode, undamaged, damaged, broken);
        }
    }

    @Test
    public void findFuzzyEntriesStayDistinctAndWriteThrough() {
        List<AEKey> keys = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            keys.add(AEItemKey.of(TOOL, 10 + i));
            keys.add(AEItemKey.of(TOOL, 60 + i));
            keys.add(TestKey.fuzzy("a" + i, 10, 100));
            keys.add(TestKey.fuzzy("b" + i, 60, 100));
            keys.add(AEItemKey.of(VARIANTS, 1, tagged("variant" + i)));
        }
        KeyCounter counter = counterOf(keys);

        for (AEKey filter : Arrays
                .asList(AEItemKey.of(TOOL, 0), TestKey.fuzzy("filter", 0, 100), AEItemKey.of(VARIANTS, 1))) {
            List<Object2LongMap.Entry<AEKey>> entries = new ArrayList<>(
                    counter.findFuzzy(filter, FuzzyMode.IGNORE_ALL));
            assertEquals(entries.size(), new HashSet<>(keys(entries)).size(), filter::toString);
            for (Object2LongMap.Entry<AEKey> entry : entries) {
                assertEquals(counter.get(entry.getKey()), entry.getLongValue(), entry.getKey()::toString);
                entry.setValue(7);
            }
            for (Object2LongMap.Entry<AEKey> entry : entries) {
                assertEquals(7, counter.get(entry.getKey()), entry.getKey()::toString);
            }
        }
    }

    @Test
    public void findFuzzyOfLevelsWithoutKeysIsEmpty() {
        AEItemKey brokenTool = AEItemKey.of(TOOL, 100);
        TestKey brokenTestKey = TestKey.fuzzy("a", 100, 100);
        KeyCounter counter = counterOf(Arrays.asList(brokenTool, brokenTestKey));

        assertFound(counter, AEItemKey.of(TOOL, 0), FuzzyMode.PERCENT_50);
        assertFound(counter, TestKey.fuzzy("b", 0, 100), FuzzyMode.PERCENT_50);

        counter.remove(brokenTool);
        counter.remove(brokenTestKey);
        assertFound(counter, brokenTool, FuzzyMode.IGNORE_ALL);
        assertFound(counter, brokenTestKey, FuzzyMode.IGNORE_ALL);
    }

    /**
     * Distinct keys of every kind of index: items with and without durability, with several damage values and tags,
     * fluids, and non-item keys with and without fuzzy range search.
     */
    private static List<AEKey> keysOfEveryKind() {
        return Arrays.asList(
                AEItemKey.of(VARIANTS, 0),
                AEItemKey.of(VARIANTS, 1),
                AEItemKey.of(VARIANTS, 1, tagged("variant")),
                AEItemKey.of(VARIANTS, 2),
                AEItemKey.of(TOOL, 0),
                AEItemKey.of(TOOL, 0, tagged("tool")),
                AEItemKey.of(TOOL, 60),
                AEFluidKey.of(FluidRegistry.WATER),
                AEFluidKey.of(FluidRegistry.LAVA),
                TestKey.fuzzy("a", 0, 100),
                TestKey.fuzzy("b", 60, 100),
                TestKey.fuzzy("a", 0, 100).withOtherPrimaryKey(),
                TestKey.plain("a", 0, 100),
                TestKey.plain("b", 60, 100));
    }

    private static Map<AEKey, Long> amountsOfEveryKind() {
        List<AEKey> keys = keysOfEveryKind();
        Map<AEKey, Long> amounts = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            amounts.put(keys.get(i), (long) i + 1);
        }
        return amounts;
    }

    /**
     * @return a key that is not one of {@link #keysOfEveryKind()} but goes to the same sub-index as {@code key}, or
     *         null if {@code key} has no own sub-index
     */
    private static @Nullable AEKey variantOf(AEKey key) {
        if (key instanceof AEItemKey itemKey) {
            return AEItemKey.of(itemKey.getItem(), itemKey.getItemDamage(), tagged("other"));
        }
        if (key instanceof TestKey testKey) {
            return testKey.renamed("other");
        }
        return null;
    }

    private static NBTTagCompound tagged(String name) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", name);
        return tag;
    }

    private static KeyCounter counterWith(Map<AEKey, Long> amounts) {
        KeyCounter counter = new KeyCounter();
        for (Map.Entry<AEKey, Long> entry : amounts.entrySet()) {
            counter.add(entry.getKey(), entry.getValue());
        }
        return counter;
    }

    private static Map<AEKey, Long> amountsOf(KeyCounter counter) {
        Map<AEKey, Long> amounts = new HashMap<>();
        counter.forEachEntry(amounts::put);
        return amounts;
    }

    private static void assertRemovesOneByOne(List<AEKey> keys) {
        KeyCounter counter = new KeyCounter();
        for (int i = 0; i < keys.size(); i++) {
            counter.add(keys.get(i), i + 1);
        }

        for (int i = 0; i < keys.size(); i++) {
            AEKey removed = keys.get(i);
            assertEquals(i + 1, counter.remove(removed), removed::toString);
            assertEquals(0, counter.get(removed), removed::toString);
            assertEquals(0, counter.remove(removed), removed::toString);
            assertEquals(keys.size() - i - 1, counter.size(), removed::toString);
            for (int j = i + 1; j < keys.size(); j++) {
                assertEquals(j + 1, counter.get(keys.get(j)), () -> removed + " removed, " + keys);
            }
        }
        assertEmpty(counter);

        AEKey first = keys.get(0);
        counter.add(first, 3);
        assertEquals(3, counter.get(first));
        assertSame(first, counter.getFirstKey());
    }

    private static void assertEmpty(KeyCounter counter) {
        assertTrue(counter.isEmpty());
        assertEquals(0, counter.size());
        assertFalse(counter.iterator().hasNext());
        assertNull(counter.getFirstKey());
        assertNull(counter.getFirstEntry());
        assertTrue(counter.keySet().isEmpty());
    }
}
