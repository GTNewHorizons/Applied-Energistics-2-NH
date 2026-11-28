package appeng.test.behaviors;

import static appeng.test.behaviors.BehaviorAssertions.assertNone;
import static appeng.test.behaviors.BehaviorAssertions.assertResult;
import static appeng.test.behaviors.BehaviorAssertions.assertStack;
import static appeng.test.behaviors.BehaviorAssertions.assertStacks;
import static appeng.test.behaviors.TestContainers.LAVA;
import static appeng.test.behaviors.TestContainers.WATER;
import static appeng.test.behaviors.TestContainers.cells;
import static appeng.test.behaviors.TestContainers.emptyBuckets;
import static appeng.test.behaviors.TestContainers.emptyCells;
import static appeng.test.behaviors.TestContainers.waterBuckets;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import appeng.api.behaviors.ContainerTransfer;
import appeng.api.behaviors.ContainerTransferResult;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;

public class ContainerTransferFillFunctionalTest {

    private static final int ALL = Integer.MAX_VALUE;

    private final TestStorage storage = new TestStorage();
    private TestEnergySource energy = TestEnergySource.infinite();

    @Test
    void doesNothingWithoutFillableContainers() {
        this.storage.add(WATER, 10000);

        assertNone(this.fill(new TestSlot(null), WATER, ALL));
        assertNone(this.fill(new TestSlot(new ItemStack(Items.stick)), WATER, ALL));
        assertNone(this.fill(new TestSlot(waterBuckets(1)), WATER, ALL));
        assertNone(this.fill(new TestSlot(cells(WATER, 1000, 1)), WATER, ALL));
        assertNone(this.fill(new TestSlot(cells(LAVA, 300, 1)), WATER, ALL));
        assertNone(this.fill(new TestSlot(emptyBuckets(1)), WATER, 0));
        assertEquals(10000, this.storage.getAmount(WATER));
    }

    @Test
    void doesNothingForKeysWithoutContainers() {
        final AEItemKey stick = AEItemKey.of(Items.stick);
        this.storage.add(stick, 10);
        final TestSlot slot = new TestSlot(emptyBuckets(1));

        assertNone(this.fill(slot, stick, ALL));
        assertStack(emptyBuckets(1), slot.get());
        assertEquals(10, this.storage.getAmount(stick));
    }

    @Test
    void doesNothingIfTheStorageLacksTheResource() {
        this.storage.add(LAVA, 10000);
        final TestSlot slot = new TestSlot(emptyBuckets(1));

        assertNone(this.fill(slot, WATER, ALL));
        assertStack(emptyBuckets(1), slot.get());
        assertEquals(10000, this.storage.getAmount(LAVA));
    }

    @Test
    void putsTheFilledContainerBackIntoTheSlot() {
        this.storage.add(WATER, 5000);
        final TestSlot slot = new TestSlot(emptyBuckets(1));

        assertResult(1000, 1, this.fill(slot, WATER, ALL));
        assertStack(waterBuckets(1), slot.get());
        assertTrue(slot.getGiven().isEmpty());
        assertEquals(4000, this.storage.getAmount(WATER));
    }

    @Test
    void splitsFilledContainersByTheirMaxStackSize() {
        this.storage.add(WATER, 10000);
        final TestSlot slot = new TestSlot(emptyBuckets(3));

        assertResult(3000, 3, this.fill(slot, WATER, ALL));
        assertStack(waterBuckets(1), slot.get());
        assertStacks(asList(waterBuckets(1), waterBuckets(1)), slot.getGiven());
        assertEquals(7000, this.storage.getAmount(WATER));
    }

    @Test
    void fillsAsManyRegistryContainersAsTheStorageAllows() {
        this.storage.add(WATER, 2500);
        final TestSlot slot = new TestSlot(emptyBuckets(16));

        assertResult(2000, 2, this.fill(slot, WATER, ALL));
        assertStack(emptyBuckets(14), slot.get());
        assertStacks(asList(waterBuckets(1), waterBuckets(1)), slot.getGiven());
        assertEquals(500, this.storage.getAmount(WATER));
    }

    @Test
    void fillsOneContainerPartiallyWithTheRest() {
        this.storage.add(WATER, 2500);
        final TestSlot slot = new TestSlot(emptyCells(3));

        assertResult(2500, 3, this.fill(slot, WATER, ALL));
        assertStack(cells(WATER, 1000, 2), slot.get());
        assertStacks(singletonList(cells(WATER, 500, 1)), slot.getGiven());
        assertEquals(0, this.storage.getAmount(WATER));
    }

    @Test
    void topsUpPartiallyFilledContainers() {
        this.storage.add(WATER, 5000);
        final TestSlot slot = new TestSlot(cells(WATER, 300, 2));

        assertResult(1400, 2, this.fill(slot, WATER, ALL));
        assertStack(cells(WATER, 1000, 2), slot.get());
        assertEquals(3600, this.storage.getAmount(WATER));
    }

    @Test
    void keepsUnprocessedContainersInTheSlot() {
        this.storage.add(WATER, 5000);
        final TestSlot slot = new TestSlot(emptyCells(5));

        assertResult(1000, 1, this.fill(slot, WATER, 1));
        assertStack(emptyCells(4), slot.get());
        assertStacks(singletonList(cells(WATER, 1000, 1)), slot.getGiven());
        assertEquals(4000, this.storage.getAmount(WATER));
    }

    @Test
    void simulationChangesNothing() {
        this.storage.add(WATER, 2500);
        final TestSlot slot = new TestSlot(emptyCells(3));

        assertResult(2500, 3, this.fill(slot, WATER, ALL, Actionable.SIMULATE));
        assertStack(emptyCells(3), slot.get());
        assertTrue(slot.getGiven().isEmpty());
        assertEquals(2500, this.storage.getAmount(WATER));
        assertEquals(0, this.energy.getExtracted());
    }

    @Test
    void isLimitedByPower() {
        this.energy = new TestEnergySource(1);
        this.storage.add(WATER, 10000);
        final TestSlot slot = new TestSlot(emptyBuckets(3));

        assertResult(1000, 1, this.fill(slot, WATER, ALL));
        assertStack(emptyBuckets(2), slot.get());
        assertStacks(singletonList(waterBuckets(1)), slot.getGiven());
        assertEquals(9000, this.storage.getAmount(WATER));
        assertEquals(1, this.energy.getExtracted());
    }

    @Test
    void putsBackWhatTheContainersCannotTake() {
        // Of the 2500 extracted, 500 cannot go into a bucket, as buckets are only filled as a whole.
        this.storage.add(WATER, 10000);
        this.storage.setMaxExtractPerModulate(2500);
        final TestSlot slot = new TestSlot(emptyBuckets(3));

        assertResult(2000, 2, this.fill(slot, WATER, ALL));
        assertStack(emptyBuckets(1), slot.get());
        assertStacks(asList(waterBuckets(1), waterBuckets(1)), slot.getGiven());
        assertEquals(8000, this.storage.getAmount(WATER));
        assertEquals(0, slot.getLeftover(WATER));
    }

    @Test
    void handsOutWhatCannotBePutBack() {
        this.storage.add(WATER, 10000);
        this.storage.setMaxExtractPerModulate(2500);
        this.storage.setInsertable(false);
        final TestSlot slot = new TestSlot(emptyBuckets(3));

        assertResult(2000, 2, this.fill(slot, WATER, ALL));
        assertStack(emptyBuckets(1), slot.get());
        assertEquals(7500, this.storage.getAmount(WATER));
        assertEquals(500, slot.getLeftover(WATER));
    }

    @Test
    void handsOutTheExtractedAmountIfNoContainerCanBeFilled() {
        this.storage.add(WATER, 10000);
        this.storage.setMaxExtractPerModulate(500);
        this.storage.setInsertable(false);
        final TestSlot slot = new TestSlot(emptyBuckets(3));

        assertNone(this.fill(slot, WATER, ALL));
        assertStack(emptyBuckets(3), slot.get());
        assertTrue(slot.getGiven().isEmpty());
        assertEquals(9500, this.storage.getAmount(WATER));
        assertEquals(500, slot.getLeftover(WATER));
    }

    private ContainerTransferResult fill(TestSlot slot, AEKey what, int maxContainers) {
        return this.fill(slot, what, maxContainers, Actionable.MODULATE);
    }

    private ContainerTransferResult fill(TestSlot slot, AEKey what, int maxContainers, Actionable mode) {
        return ContainerTransfer
                .fillFrom(slot, this.storage, this.energy, new BaseActionSource(), what, maxContainers, mode);
    }
}
