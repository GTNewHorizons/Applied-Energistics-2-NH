package appeng.test.behaviors;

import static appeng.test.behaviors.BehaviorAssertions.assertNone;
import static appeng.test.behaviors.BehaviorAssertions.assertResult;
import static appeng.test.behaviors.BehaviorAssertions.assertStack;
import static appeng.test.behaviors.BehaviorAssertions.assertStacks;
import static appeng.test.behaviors.TestContainers.emptyBuckets;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.ContainerTransfer;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.util.Platform;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.common.integration.tc.EssentiaItemContainerHelper;
import thaumicenergistics.common.storage.AEEssentiaKey;
import thaumicenergistics.common.storage.EssentiaKeyType;

/**
 * Phials hold 8 essentia and are only filled or emptied as a whole, jars hold up to 64 and can hold any amount.
 */
public class EssentiaContainerItemStrategyFunctionalTest {

    private static final EssentiaItemContainerHelper HELPER = EssentiaItemContainerHelper.INSTANCE;
    private static final AEEssentiaKey AQUA = AEEssentiaKey.of(Aspect.WATER);
    private static final AEEssentiaKey IGNIS = AEEssentiaKey.of(Aspect.FIRE);

    @Test
    void recognizesPhialsAndJars() {
        assertTrue(ContainerItemStrategies.getSupportedKeyTypes().contains(EssentiaKeyType.INSTANCE));

        for (ItemStack container : new ItemStack[] { emptyPhial(), filledPhial(), emptyJar(), jar(30),
                labeledJar(0) }) {
            assertTrue(ContainerItemStrategies.isContainer(container, EssentiaKeyType.INSTANCE));
            assertFalse(ContainerItemStrategies.isContainer(container, AEKeyType.fluids()));
        }
        assertFalse(ContainerItemStrategies.isContainer(new ItemStack(Items.stick), EssentiaKeyType.INSTANCE));
        assertFalse(ContainerItemStrategies.isContainer(emptyBuckets(1), EssentiaKeyType.INSTANCE));
    }

    @Test
    void getsContainedEssentia() {
        assertEquals(new GenericStack(AQUA, 8), ContainerItemStrategies.getContainedStack(filledPhial()));
        assertEquals(new GenericStack(AQUA, 30), ContainerItemStrategies.getContainedStack(jar(30)));
        assertEquals(new GenericStack(AQUA, 30), ContainerItemStrategies.getContainedStack(labeledJar(30)));

        assertNull(ContainerItemStrategies.getContainedStack(emptyPhial()));
        assertNull(ContainerItemStrategies.getContainedStack(emptyJar()));
        assertNull(ContainerItemStrategies.getContainedStack(labeledJar(0)));
    }

    @Test
    void fillsPhialsOnlyAsAWhole() {
        assertResult(filledPhial(), 8, ContainerItemStrategies.fill(emptyPhial(), AQUA, 8));
        assertResult(filledPhial(), 8, ContainerItemStrategies.fill(emptyPhial(), AQUA, Long.MAX_VALUE));

        assertNone(ContainerItemStrategies.fill(emptyPhial(), AQUA, 7));
        assertNone(ContainerItemStrategies.fill(filledPhial(), AQUA, 8));
    }

    @Test
    void drainsPhialsOnlyAsAWhole() {
        assertResult(emptyPhial(), 8, ContainerItemStrategies.drain(filledPhial(), AQUA, 8));
        assertResult(emptyPhial(), 8, ContainerItemStrategies.drain(filledPhial(), AQUA, 100));

        assertNone(ContainerItemStrategies.drain(filledPhial(), AQUA, 7));
        assertNone(ContainerItemStrategies.drain(filledPhial(), IGNIS, 8));
        assertNone(ContainerItemStrategies.drain(emptyPhial(), AQUA, 8));
    }

    @Test
    void fillsJarsPartially() {
        assertResult(jar(30), 30, ContainerItemStrategies.fill(emptyJar(), AQUA, 30));
        assertResult(jar(64), 64, ContainerItemStrategies.fill(emptyJar(), AQUA, 100));
        assertResult(jar(64), 34, ContainerItemStrategies.fill(jar(30), AQUA, 100));

        assertNone(ContainerItemStrategies.fill(jar(64), AQUA, 10));
        // Jars do not mix aspects.
        assertNone(ContainerItemStrategies.fill(jar(30), IGNIS, 10));
    }

    @Test
    void drainsJarsPartially() {
        assertResult(jar(20), 10, ContainerItemStrategies.drain(jar(30), AQUA, 10));
        assertResult(emptyJar(), 30, ContainerItemStrategies.drain(jar(30), AQUA, 100));

        assertNone(ContainerItemStrategies.drain(jar(30), IGNIS, 10));
        assertNone(ContainerItemStrategies.drain(emptyJar(), AQUA, 10));
    }

    @Test
    void labeledJarsKeepTheirLabelAndOnlyAcceptTheirAspect() {
        assertResult(labeledJar(20), 20, ContainerItemStrategies.fill(labeledJar(0), AQUA, 20));
        assertResult(labeledJar(0), 30, ContainerItemStrategies.drain(labeledJar(30), AQUA, 30));

        assertNone(ContainerItemStrategies.fill(labeledJar(0), IGNIS, 10));
        assertNone(ContainerItemStrategies.fill(labeledJar(30), IGNIS, 10));
        assertNone(ContainerItemStrategies.drain(labeledJar(0), AQUA, 10));
    }

    @Test
    void doesNotModifyTheInput() {
        final ItemStack jar = jar(30);
        ContainerItemStrategies.fill(jar, AQUA, 10);
        ContainerItemStrategies.drain(jar, AQUA, 10);
        assertStack(jar(30), jar);

        final ItemStack phial = emptyPhial();
        ContainerItemStrategies.fill(phial, AQUA, 8);
        assertStack(emptyPhial(), phial);
    }

    @Test
    void drainsOnlyWholePhialsIntoStorage() {
        final TestStorage storage = new TestStorage();
        storage.setCapacity(50);
        final TestSlot slot = new TestSlot(Platform.copyStackWithSize(filledPhial(), 10));

        assertResult(
                48,
                6,
                ContainerTransfer.drainInto(
                        slot,
                        storage,
                        TestEnergySource.infinite(),
                        new BaseActionSource(),
                        key -> true,
                        Integer.MAX_VALUE,
                        false,
                        Actionable.MODULATE));
        assertStack(Platform.copyStackWithSize(filledPhial(), 4), slot.get());
        assertStacks(singletonList(Platform.copyStackWithSize(emptyPhial(), 6)), slot.getGiven());
        assertEquals(48, storage.getAmount(AQUA));
    }

    @Test
    void fillsOnlyWholePhialsFromStorage() {
        final TestStorage storage = new TestStorage();
        storage.add(AQUA, 20);
        final TestSlot slot = new TestSlot(Platform.copyStackWithSize(emptyPhial(), 5));

        assertResult(
                16,
                2,
                ContainerTransfer.fillFrom(
                        slot,
                        storage,
                        TestEnergySource.infinite(),
                        new BaseActionSource(),
                        AQUA,
                        Integer.MAX_VALUE,
                        Actionable.MODULATE));
        assertStack(Platform.copyStackWithSize(emptyPhial(), 3), slot.get());
        assertStacks(singletonList(Platform.copyStackWithSize(filledPhial(), 2)), slot.getGiven());
        assertEquals(4, storage.getAmount(AQUA));
    }

    private static ItemStack emptyPhial() {
        return HELPER.createEmptyPhial();
    }

    private static ItemStack filledPhial() {
        return HELPER.createFilledPhial(Aspect.WATER);
    }

    private static ItemStack emptyJar() {
        return HELPER.createEmptyJar(0);
    }

    private static ItemStack jar(int amount) {
        return HELPER.createFilledJar(Aspect.WATER, amount, 0, false);
    }

    private static ItemStack labeledJar(int amount) {
        return HELPER.createFilledJar(Aspect.WATER, amount, 0, true);
    }
}
