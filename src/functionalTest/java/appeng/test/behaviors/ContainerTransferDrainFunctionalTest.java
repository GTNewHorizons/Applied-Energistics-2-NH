package appeng.test.behaviors;

import static appeng.test.behaviors.BehaviorAssertions.assertNone;
import static appeng.test.behaviors.BehaviorAssertions.assertResult;
import static appeng.test.behaviors.BehaviorAssertions.assertStack;
import static appeng.test.behaviors.BehaviorAssertions.assertStacks;
import static appeng.test.behaviors.TestContainers.WATER;
import static appeng.test.behaviors.TestContainers.cells;
import static appeng.test.behaviors.TestContainers.emptyBuckets;
import static appeng.test.behaviors.TestContainers.emptyCells;
import static appeng.test.behaviors.TestContainers.waterBuckets;
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

public class ContainerTransferDrainFunctionalTest {

    private static final int ALL = Integer.MAX_VALUE;

    private final TestStorage storage = new TestStorage();
    private TestEnergySource energy = TestEnergySource.infinite();

    @Test
    void doesNothingWithoutFilledContainers() {
        assertNone(this.drain(new TestSlot(null), ALL));
        assertNone(this.drain(new TestSlot(new ItemStack(Items.stick)), ALL));
        assertNone(this.drain(new TestSlot(emptyBuckets(1)), ALL));
        assertNone(this.drain(new TestSlot(emptyCells(1)), ALL));

        final TestSlot slot = new TestSlot(waterBuckets(1));
        assertNone(this.drain(slot, 0));
        assertStack(waterBuckets(1), slot.get());
        assertEquals(0, this.storage.getAmount(WATER));
    }

    @Test
    void keepsContentRejectedByTheFilter() {
        final TestSlot slot = new TestSlot(waterBuckets(1));

        assertNone(
                ContainerTransfer.drainInto(
                        slot,
                        this.storage,
                        this.energy,
                        new BaseActionSource(),
                        key -> !key.equals(WATER),
                        ALL,
                        false,
                        Actionable.MODULATE));
        assertStack(waterBuckets(1), slot.get());
        assertEquals(0, this.storage.getAmount(WATER));
    }

    @Test
    void putsTheDrainedContainerBackIntoTheSlot() {
        final TestSlot slot = new TestSlot(waterBuckets(1));

        assertResult(1000, 1, this.drain(slot, ALL));
        assertStack(emptyBuckets(1), slot.get());
        assertTrue(slot.getGiven().isEmpty());
        assertEquals(1000, this.storage.getAmount(WATER));
    }

    @Test
    void drainsTheWholeStack() {
        final TestSlot slot = new TestSlot(cells(WATER, 1000, 5));

        assertResult(5000, 5, this.drain(slot, ALL));
        assertStack(emptyCells(5), slot.get());
        assertTrue(slot.getGiven().isEmpty());
        assertEquals(5000, this.storage.getAmount(WATER));
    }

    @Test
    void keepsUnprocessedContainersInTheSlot() {
        final TestSlot slot = new TestSlot(cells(WATER, 1000, 5));

        assertResult(2000, 2, this.drain(slot, 2));
        assertStack(cells(WATER, 1000, 3), slot.get());
        assertStacks(singletonList(emptyCells(2)), slot.getGiven());
        assertEquals(2000, this.storage.getAmount(WATER));
    }

    @Test
    void drainsOneContainerPartiallyIfTheStorageIsNearlyFull() {
        this.storage.setCapacity(2500);
        final TestSlot slot = new TestSlot(cells(WATER, 1000, 3));

        assertResult(2500, 3, this.drain(slot, ALL));
        assertStack(emptyCells(2), slot.get());
        assertStacks(singletonList(cells(WATER, 500, 1)), slot.getGiven());
        assertEquals(2500, this.storage.getAmount(WATER));
    }

    @Test
    void drainsOnlyWholeRegistryContainers() {
        this.storage.setCapacity(2500);
        final TestSlot slot = new TestSlot(waterBuckets(3));

        assertResult(2000, 2, this.drain(slot, ALL));
        assertStack(waterBuckets(1), slot.get());
        assertStacks(singletonList(emptyBuckets(2)), slot.getGiven());
        assertEquals(2000, this.storage.getAmount(WATER));
    }

    @Test
    void requireAllRefusesIncompleteTransfers() {
        this.storage.setCapacity(2500);
        final TestSlot slot = new TestSlot(cells(WATER, 1000, 3));

        assertNone(this.drain(slot, ALL, true, Actionable.MODULATE));
        assertStack(cells(WATER, 1000, 3), slot.get());
        assertEquals(0, this.storage.getAmount(WATER));

        // Only the requested containers have to be emptied completely.
        assertResult(2000, 2, this.drain(slot, 2, true, Actionable.MODULATE));
        assertStack(cells(WATER, 1000, 1), slot.get());
        assertStacks(singletonList(emptyCells(2)), slot.getGiven());
        assertEquals(2000, this.storage.getAmount(WATER));
    }

    @Test
    void simulationChangesNothing() {
        this.storage.setCapacity(2500);
        final TestSlot slot = new TestSlot(cells(WATER, 1000, 3));

        assertResult(2500, 3, this.drain(slot, ALL, false, Actionable.SIMULATE));
        assertStack(cells(WATER, 1000, 3), slot.get());
        assertTrue(slot.getGiven().isEmpty());
        assertEquals(0, this.storage.getAmount(WATER));
        assertEquals(0, this.energy.getExtracted());
    }

    @Test
    void isLimitedByPower() {
        this.energy = new TestEnergySource(2);
        final TestSlot slot = new TestSlot(waterBuckets(3));

        assertResult(2000, 2, this.drain(slot, ALL));
        assertStack(waterBuckets(1), slot.get());
        assertStacks(singletonList(emptyBuckets(2)), slot.getGiven());
        assertEquals(2000, this.storage.getAmount(WATER));
        assertEquals(2, this.energy.getExtracted());
    }

    @Test
    void replansIfTheStorageAcceptsLessThanSimulated() {
        this.storage.setMaxInsertPerModulate(2500);
        final TestSlot slot = new TestSlot(cells(WATER, 1000, 3));

        assertResult(2500, 3, this.drain(slot, ALL));
        assertStack(emptyCells(2), slot.get());
        assertStacks(singletonList(cells(WATER, 500, 1)), slot.getGiven());
        assertEquals(2500, this.storage.getAmount(WATER));
    }

    @Test
    void takesBackWhatTheContainersCannotGiveUp() {
        // Of the 2500 accepted, 500 cannot come from a bucket, as buckets are only emptied as a whole.
        this.storage.setMaxInsertPerModulate(2500);
        final TestSlot slot = new TestSlot(waterBuckets(3));

        assertResult(2000, 2, this.drain(slot, ALL));
        assertStack(waterBuckets(1), slot.get());
        assertStacks(singletonList(emptyBuckets(2)), slot.getGiven());
        assertEquals(2000, this.storage.getAmount(WATER));
        assertEquals(0, slot.getLeftover(WATER));
    }

    @Test
    void handsOutWhatTheStorageMissedIfTheSurplusIsStuck() {
        this.storage.setMaxInsertPerModulate(2500);
        this.storage.setExtractable(false);
        final TestSlot slot = new TestSlot(waterBuckets(3));

        assertResult(3000, 3, this.drain(slot, ALL));
        assertStack(emptyBuckets(3), slot.get());
        assertTrue(slot.getGiven().isEmpty());
        assertEquals(2500, this.storage.getAmount(WATER));
        assertEquals(500, slot.getLeftover(WATER));
    }

    @Test
    void handsOutWhatTheStorageMissedIfTheSurplusIsPartlyStuck() {
        this.storage.setMaxInsertPerModulate(2500);
        this.storage.setMaxExtractPerModulate(200);
        final TestSlot slot = new TestSlot(waterBuckets(3));

        assertResult(3000, 3, this.drain(slot, ALL));
        assertStack(emptyBuckets(3), slot.get());
        assertEquals(2300, this.storage.getAmount(WATER));
        assertEquals(700, slot.getLeftover(WATER));
    }

    @Test
    void doesNothingIfTheStorageAcceptsNothing() {
        this.storage.setMaxInsertPerModulate(0);
        final TestSlot slot = new TestSlot(waterBuckets(3));

        assertNone(this.drain(slot, ALL));
        assertStack(waterBuckets(3), slot.get());
        assertTrue(slot.getGiven().isEmpty());
        assertEquals(0, this.storage.getAmount(WATER));
        assertEquals(0, slot.getLeftover(WATER));
    }

    private ContainerTransferResult drain(TestSlot slot, int maxContainers) {
        return this.drain(slot, maxContainers, false, Actionable.MODULATE);
    }

    private ContainerTransferResult drain(TestSlot slot, int maxContainers, boolean requireAll, Actionable mode) {
        return ContainerTransfer.drainInto(
                slot,
                this.storage,
                this.energy,
                new BaseActionSource(),
                key -> true,
                maxContainers,
                requireAll,
                mode);
    }
}
