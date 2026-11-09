package appeng.test.stacks;

import static appeng.test.stacks.FuzzyTestItems.MAX_WEAR;
import static appeng.test.stacks.FuzzyTestItems.ORE_TOOL;
import static appeng.test.stacks.FuzzyTestItems.SHARED_ORE_ITEM;
import static appeng.test.stacks.FuzzyTestItems.TOOL;
import static appeng.test.stacks.FuzzyTestItems.VARIANTS;
import static appeng.test.stacks.FuzzyTestItems.WEAR;
import static appeng.test.stacks.FuzzyTestItems.WILDCARD_ORE_ITEM;
import static appeng.test.stacks.KeyCounterAssertions.assertFindFuzzyMatchesFuzzyEquals;
import static appeng.test.stacks.KeyCounterAssertions.assertFound;
import static appeng.test.stacks.KeyCounterAssertions.counterOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;

public class AEItemKeyFuzzyFunctionalTest {

    /** Damages of {@link FuzzyTestItems#TOOL} at and just above each break point. */
    private static final int[] TOOL_DAMAGES = { 0, 1, 25, 26, 50, 51, 75, 76, 90, 91, 99, 100, 101 };

    @Test
    public void keyFromStackHasSameFuzzyValuesAsKeyFromDamage() {
        for (int damage = 0; damage <= 101; damage++) {
            assertSameFuzzyValues(new ItemStack(TOOL, 1, damage));
        }
        for (int damage = 0; damage <= 251; damage++) {
            assertSameFuzzyValues(new ItemStack(Items.iron_sword, 1, damage));
        }
    }

    @Test
    public void keyFromStackHasSameFuzzyValuesAsKeyFromTag() {
        for (int wear = 0; wear <= 250; wear++) {
            assertSameFuzzyValues(worn(wear, 100));
            assertSameFuzzyValues(worn(wear, 250));
        }
    }

    @Test
    public void fuzzyValuesComeFromDisplayDamage() {
        AEItemKey fromStack = key(worn(60, 250));
        AEItemKey fromTag = lazyKey(worn(60, 250));
        for (AEItemKey key : Arrays.asList(fromStack, fromTag)) {
            assertEquals(0, key.getItemDamage());
            assertEquals(60, key.getFuzzySearchValue());
            assertEquals(250, key.getFuzzySearchMaxValue());
        }
    }

    @Test
    public void findFuzzyMatchesFuzzyEqualsForDamageableItems() {
        List<AEItemKey> keys = new ArrayList<>();
        for (int damage : TOOL_DAMAGES) {
            keys.add(AEItemKey.of(TOOL, damage));
        }
        for (int wear : new int[] { 0, 62, 63, 125, 126, 250 }) {
            keys.add(key(worn(wear, 250)));
        }
        keys.add(AEItemKey.of(VARIANTS, 0));
        KeyCounter counter = counterOf(keys);

        List<AEItemKey> filters = new ArrayList<>(keys);
        filters.add(AEItemKey.of(TOOL, 30));
        filters.add(AEItemKey.of(TOOL, 70));
        for (FuzzyMode mode : FuzzyMode.values()) {
            for (AEItemKey filter : filters) {
                assertFindFuzzyMatchesFuzzyEquals(counter, filter, mode);
            }
        }
    }

    @Test
    public void breakPointOfEachModeIsUndamaged() {
        List<AEItemKey> keys = new ArrayList<>();
        for (int damage : TOOL_DAMAGES) {
            keys.add(AEItemKey.of(TOOL, damage));
        }
        KeyCounter counter = counterOf(keys);
        AEItemKey undamaged = AEItemKey.of(TOOL, 0);

        for (FuzzyMode mode : FuzzyMode.values()) {
            if (mode == FuzzyMode.IGNORE_ALL) {
                continue;
            }
            int breakPoint = (int) mode.percentage;
            Set<AEKey> found = assertFindFuzzyMatchesFuzzyEquals(counter, undamaged, mode);
            assertTrue(found.contains(AEItemKey.of(TOOL, breakPoint)), mode::name);
            assertFalse(found.contains(AEItemKey.of(TOOL, breakPoint + 1)), mode::name);
        }
    }

    @Test
    public void breakPointOfMaxDamageThatModesDoNotDivide() {
        KeyCounter counter = counterOf(
                Arrays.asList(key(worn(62, 250)), key(worn(63, 250)), key(worn(125, 250)), key(worn(126, 250))));
        AEItemKey undamaged = key(worn(0, 250));

        assertFound(counter, undamaged, FuzzyMode.PERCENT_75, key(worn(62, 250)));
        assertFound(
                counter,
                undamaged,
                FuzzyMode.PERCENT_50,
                key(worn(62, 250)),
                key(worn(63, 250)),
                key(worn(125, 250)));
    }

    @Test
    public void displayDamageDecidesPartition() {
        AEItemKey slightlyWorn = key(worn(10, 100));
        AEItemKey badlyWorn = key(worn(90, 100));
        KeyCounter counter = counterOf(Arrays.asList(slightlyWorn, badlyWorn));

        assertFound(counter, AEItemKey.of(TOOL, 0), FuzzyMode.PERCENT_50, slightlyWorn);
        assertFound(counter, AEItemKey.of(TOOL, 0), FuzzyMode.PERCENT_99);
        assertFound(counter, AEItemKey.of(TOOL, 100), FuzzyMode.PERCENT_25, badlyWorn);
    }

    @Test
    public void itemWithoutDurabilityMatchesExactDamageInEveryMode() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Name", "variant");
        AEItemKey variant = AEItemKey.of(VARIANTS, 1);
        AEItemKey variantWithTag = AEItemKey.of(VARIANTS, 1, tag);
        KeyCounter counter = counterOf(
                Arrays.asList(AEItemKey.of(VARIANTS, 0), variant, variantWithTag, AEItemKey.of(VARIANTS, 2)));

        for (FuzzyMode mode : FuzzyMode.values()) {
            assertFound(counter, variant, mode, variant, variantWithTag);
        }
    }

    @Test
    public void wildcardOreMatchesEveryDamageOfItsItems() {
        AEItemKey wildcard0 = AEItemKey.of(WILDCARD_ORE_ITEM, 0);
        AEItemKey wildcard1 = AEItemKey.of(WILDCARD_ORE_ITEM, 1);
        AEItemKey wildcard2 = AEItemKey.of(WILDCARD_ORE_ITEM, 2);
        AEItemKey shared0 = AEItemKey.of(SHARED_ORE_ITEM, 0);
        KeyCounter counter = counterOf(
                Arrays.asList(
                        wildcard0,
                        wildcard1,
                        wildcard2,
                        shared0,
                        AEItemKey.of(SHARED_ORE_ITEM, 1),
                        AEItemKey.of(VARIANTS, 0)));

        for (FuzzyMode mode : FuzzyMode.values()) {
            assertFound(counter, wildcard1, mode, wildcard0, wildcard1, wildcard2, shared0);
            assertFound(counter, AEItemKey.of(WILDCARD_ORE_ITEM, 5), mode, wildcard0, wildcard1, wildcard2, shared0);
        }
    }

    @Test
    public void wildcardOreOfDamageableItemIgnoresDamage() {
        AEItemKey undamaged = AEItemKey.of(ORE_TOOL, 0);
        AEItemKey halfDamaged = AEItemKey.of(ORE_TOOL, 50);
        AEItemKey broken = AEItemKey.of(ORE_TOOL, 100);
        KeyCounter counter = counterOf(Arrays.asList(undamaged, halfDamaged, broken, AEItemKey.of(TOOL, 0)));

        for (FuzzyMode mode : FuzzyMode.values()) {
            assertFound(counter, undamaged, mode, undamaged, halfDamaged, broken);
            assertFound(counter, broken, mode, undamaged, halfDamaged, broken);
        }
    }

    @Test
    public void keyInSeveralOresIsFoundOnce() {
        AEItemKey shared0 = AEItemKey.of(SHARED_ORE_ITEM, 0);
        AEItemKey shared1 = AEItemKey.of(SHARED_ORE_ITEM, 1);
        AEItemKey wildcard0 = AEItemKey.of(WILDCARD_ORE_ITEM, 0);
        AEItemKey wildcard1 = AEItemKey.of(WILDCARD_ORE_ITEM, 1);
        KeyCounter counter = counterOf(Arrays.asList(shared0, shared1, wildcard0, wildcard1));

        for (FuzzyMode mode : FuzzyMode.values()) {
            assertFound(counter, shared0, mode, shared0, wildcard0, wildcard1);
            assertFound(counter, shared1, mode, shared1);
        }
    }

    private static void assertSameFuzzyValues(ItemStack stack) {
        AEItemKey fromStack = key(stack);
        KeyCounter counter = new KeyCounter();
        counter.add(fromStack, 1);
        // Counters select the bucket of a key by its fuzzy damage level, which a fresh key computes on lookup.
        assertEquals(1, counter.get(lazyKey(stack)), stack::toString);

        AEItemKey lazy = lazyKey(stack);
        assertEquals(fromStack, lazy, stack::toString);
        assertEquals(fromStack.getFuzzySearchValue(), lazy.getFuzzySearchValue(), stack::toString);
        assertEquals(fromStack.getFuzzySearchMaxValue(), lazy.getFuzzySearchMaxValue(), stack::toString);
    }

    private static ItemStack worn(int wear, int maxWear) {
        ItemStack stack = new ItemStack(TOOL);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(WEAR, wear);
        tag.setInteger(MAX_WEAR, maxWear);
        stack.setTagCompound(tag);
        return stack;
    }

    private static AEItemKey key(ItemStack stack) {
        return Objects.requireNonNull(AEItemKey.of(stack));
    }

    /**
     * @return a key whose fuzzy values are computed on first use, unlike {@link #key}
     */
    private static AEItemKey lazyKey(ItemStack stack) {
        Item item = stack.getItem();
        return AEItemKey.of(item, stack.getItemDamage(), stack.getTagCompound());
    }
}
