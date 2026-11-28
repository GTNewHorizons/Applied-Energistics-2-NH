package appeng.test.stacks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraftforge.fluids.FluidRegistry;

import org.junit.jupiter.api.Test;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEFluidKey;

public class AEKeyFuzzyEqualsFunctionalTest {

    @Test
    public void keysWithFuzzyRangeSearchMatchBySamePrimaryKeyAndPartition() {
        TestKey filter = TestKey.fuzzy("a", 10, 100);
        TestKey otherVariant = TestKey.fuzzy("b", 40, 100);

        assertTrue(filter.fuzzyEquals(filter, FuzzyMode.PERCENT_1));
        assertTrue(filter.fuzzyEquals(otherVariant, FuzzyMode.PERCENT_50));
        assertFalse(filter.fuzzyEquals(otherVariant, FuzzyMode.PERCENT_75));
        assertTrue(filter.fuzzyEquals(otherVariant, FuzzyMode.IGNORE_ALL));
        assertFalse(filter.fuzzyEquals(filter.withOtherPrimaryKey(), FuzzyMode.IGNORE_ALL));
        assertFalse(filter.fuzzyEquals(AEFluidKey.of(FluidRegistry.WATER), FuzzyMode.IGNORE_ALL));
        assertFalse(filter.fuzzyEquals(null, FuzzyMode.IGNORE_ALL));
    }

    @Test
    public void keysWithoutFuzzyRangeSearchIgnoreDamage() {
        TestKey filter = TestKey.plain("a", 0, 100);
        TestKey broken = TestKey.plain("b", 100, 100);

        for (FuzzyMode mode : FuzzyMode.values()) {
            assertTrue(filter.fuzzyEquals(broken, mode), mode::name);
            assertFalse(filter.fuzzyEquals(broken.withOtherPrimaryKey(), mode), mode::name);
        }
    }

    @Test
    public void fluidKeysMatchOnlyTheSameFluid() {
        AEFluidKey water = AEFluidKey.of(FluidRegistry.WATER);

        for (FuzzyMode mode : FuzzyMode.values()) {
            assertTrue(water.fuzzyEquals(AEFluidKey.of(FluidRegistry.WATER), mode), mode::name);
            assertFalse(water.fuzzyEquals(AEFluidKey.of(FluidRegistry.LAVA), mode), mode::name);
        }
    }
}
