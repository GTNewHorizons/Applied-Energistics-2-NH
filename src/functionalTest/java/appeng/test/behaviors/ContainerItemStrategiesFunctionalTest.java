package appeng.test.behaviors;

import static appeng.test.behaviors.BehaviorAssertions.assertNone;
import static appeng.test.behaviors.BehaviorAssertions.assertResult;
import static appeng.test.behaviors.BehaviorAssertions.assertStack;
import static appeng.test.behaviors.TestContainers.LAVA;
import static appeng.test.behaviors.TestContainers.WATER;
import static appeng.test.behaviors.TestContainers.cells;
import static appeng.test.behaviors.TestContainers.emptyBuckets;
import static appeng.test.behaviors.TestContainers.emptyCells;
import static appeng.test.behaviors.TestContainers.waterBuckets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import appeng.api.behaviors.ContainerItemResult;
import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.util.Platform;
import codechicken.nei.item.ItemFluidDisplay;

public class ContainerItemStrategiesFunctionalTest {

    @Test
    void recognizesFluidContainers() {
        assertFalse(ContainerItemStrategies.isContainer(null));
        assertFalse(ContainerItemStrategies.isContainer(new ItemStack(Items.stick)));

        assertTrue(ContainerItemStrategies.isContainer(emptyBuckets(1)));
        assertTrue(ContainerItemStrategies.isContainer(waterBuckets(1)));
        assertTrue(ContainerItemStrategies.isContainer(emptyCells(1)));
        assertTrue(ContainerItemStrategies.isContainer(cells(WATER, 500, 1)));
        assertTrue(ContainerItemStrategies.getSupportedKeyTypes().contains(AEKeyType.fluids()));
    }

    @Test
    void recognizesContainersOnlyForTheirKeyTypes() {
        assertTrue(ContainerItemStrategies.isContainer(emptyBuckets(1), AEKeyType.fluids()));
        assertFalse(ContainerItemStrategies.isContainer(emptyBuckets(1), AEKeyType.items()));
        assertFalse(ContainerItemStrategies.isContainer(null, AEKeyType.fluids()));

        assertTrue(ContainerItemStrategies.isContainer(emptyBuckets(1), type -> type == AEKeyType.fluids()));
        assertFalse(ContainerItemStrategies.isContainer(emptyBuckets(1), type -> type != AEKeyType.fluids()));
    }

    @Test
    void fluidDisplayItemsAreNotContainers() {
        assumeTrue(Platform.isNEILoaded);
        // NEI's display item implements IFluidContainerItem, but only shows a fluid.
        final ItemStack display = ItemFluidDisplay.createStack(FluidRegistry.WATER, 1000);

        assertFalse(ContainerItemStrategies.isContainer(display));
        assertNull(ContainerItemStrategies.getContainedStack(display));
        assertNone(ContainerItemStrategies.drain(display, WATER, 1000));
    }

    @Test
    void getsContentOfASingleContainer() {
        final ItemStack buckets = waterBuckets(3);

        assertEquals(new GenericStack(WATER, 1000), ContainerItemStrategies.getContainedStack(buckets));
        assertEquals(new GenericStack(WATER, 400), ContainerItemStrategies.getContainedStack(cells(WATER, 400, 5)));
        assertStack(waterBuckets(3), buckets);
    }

    @Test
    void emptyContainersAndOtherItemsHoldNothing() {
        assertNull(ContainerItemStrategies.getContainedStack(emptyBuckets(1)));
        assertNull(ContainerItemStrategies.getContainedStack(emptyCells(1)));
        assertNull(ContainerItemStrategies.getContainedStack(new ItemStack(Items.stick)));
    }

    @Test
    void filterRejectsContent() {
        assertNull(ContainerItemStrategies.getContainedStack(waterBuckets(1), key -> !key.equals(WATER)));
        assertEquals(
                new GenericStack(WATER, 1000),
                ContainerItemStrategies.getContainedStack(waterBuckets(1), key -> key.equals(WATER)));
    }

    @Test
    void fillsRegistryContainersOnlyAsAWhole() {
        assertResult(waterBuckets(1), 1000, ContainerItemStrategies.fill(emptyBuckets(1), WATER, 1000));
        assertResult(waterBuckets(1), 1000, ContainerItemStrategies.fill(emptyBuckets(1), WATER, Long.MAX_VALUE));
        assertResult(new ItemStack(Items.lava_bucket), 1000, ContainerItemStrategies.fill(emptyBuckets(1), LAVA, 1000));

        assertNone(ContainerItemStrategies.fill(emptyBuckets(1), WATER, 999));
        assertNone(ContainerItemStrategies.fill(waterBuckets(1), WATER, 1000));
        assertNone(ContainerItemStrategies.fill(waterBuckets(1), LAVA, 1000));
    }

    @Test
    void drainsRegistryContainersOnlyAsAWhole() {
        assertResult(emptyBuckets(1), 1000, ContainerItemStrategies.drain(waterBuckets(1), WATER, 1000));
        assertResult(emptyBuckets(1), 1000, ContainerItemStrategies.drain(waterBuckets(1), WATER, Long.MAX_VALUE));

        assertNone(ContainerItemStrategies.drain(waterBuckets(1), WATER, 999));
        assertNone(ContainerItemStrategies.drain(waterBuckets(1), LAVA, 1000));
        assertNone(ContainerItemStrategies.drain(emptyBuckets(1), WATER, 1000));
    }

    @Test
    void fillsFluidContainerItemsPartially() {
        assertResult(cells(WATER, 400, 1), 400, ContainerItemStrategies.fill(emptyCells(1), WATER, 400));
        assertResult(cells(WATER, 1000, 1), 1000, ContainerItemStrategies.fill(emptyCells(1), WATER, Long.MAX_VALUE));
        assertResult(cells(WATER, 1000, 1), 700, ContainerItemStrategies.fill(cells(WATER, 300, 1), WATER, 5000));

        assertNone(ContainerItemStrategies.fill(cells(WATER, 300, 1), LAVA, 500));
        assertNone(ContainerItemStrategies.fill(cells(WATER, 1000, 1), WATER, 500));
    }

    @Test
    void drainsFluidContainerItemsPartially() {
        assertResult(cells(WATER, 600, 1), 400, ContainerItemStrategies.drain(cells(WATER, 1000, 1), WATER, 400));
        assertResult(emptyCells(1), 300, ContainerItemStrategies.drain(cells(WATER, 300, 1), WATER, Long.MAX_VALUE));

        assertNone(ContainerItemStrategies.drain(cells(WATER, 300, 1), LAVA, 300));
        assertNone(ContainerItemStrategies.drain(emptyCells(1), WATER, 300));
    }

    @Test
    void operatesOnASingleContainerWithoutModifyingTheInput() {
        final ItemStack stack = cells(WATER, 300, 5);
        assertResult(cells(WATER, 1000, 1), 700, ContainerItemStrategies.fill(stack, WATER, 1000));
        assertResult(emptyCells(1), 300, ContainerItemStrategies.drain(stack, WATER, 1000));
        assertStack(cells(WATER, 300, 5), stack);

        // A single container is passed to the strategy as is.
        final ItemStack single = cells(WATER, 300, 1);
        ContainerItemStrategies.fill(single, WATER, 1000);
        ContainerItemStrategies.drain(single, WATER, 1000);
        assertStack(cells(WATER, 300, 1), single);
    }

    @Test
    void ignoresKeysWithoutAStrategyForTheContainer() {
        final AEItemKey stick = AEItemKey.of(Items.stick);

        assertNone(ContainerItemStrategies.fill(emptyBuckets(1), stick, 1));
        assertNone(ContainerItemStrategies.drain(waterBuckets(1), stick, 1));
        assertNone(ContainerItemStrategies.fill(new ItemStack(Items.stick), WATER, 1000));
        assertNone(ContainerItemStrategies.drain(new ItemStack(Items.stick), WATER, 1000));
    }

    @Test
    void rejectsKeyClassNotMatchingTheType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ContainerItemStrategies.register(AEKeyType.fluids(), AEItemKey.class, new ClaimingStrategy<>()));
    }

    @Test
    void doesNotReplaceARegisteredStrategy() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ContainerItemStrategies.register(AEKeyType.fluids(), AEFluidKey.class, new ClaimingStrategy<>()));

        assertFalse(ContainerItemStrategies.isContainer(new ItemStack(Items.stick)));
        assertResult(emptyBuckets(1), 1000, ContainerItemStrategies.drain(waterBuckets(1), WATER, 1000));
    }

    /**
     * Claims every item, so that it would be noticed if it was registered.
     */
    private static final class ClaimingStrategy<T extends AEKey> implements ContainerItemStrategy<T> {

        @Override
        public boolean isContainer(@Nullable ItemStack stack) {
            return true;
        }

        @Override
        public GenericStack getContainedStack(ItemStack container) {
            return new GenericStack(LAVA, 1);
        }

        @Override
        public ContainerItemResult fill(ItemStack container, T key, long amount) {
            return ContainerItemResult.of(container.copy(), amount);
        }

        @Override
        public ContainerItemResult drain(ItemStack container, T key, long amount) {
            return ContainerItemResult.of(container.copy(), amount);
        }
    }
}
