package appeng.test.helpers.transfer;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;
import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEStack;
import appeng.helpers.transfer.TransferFilterBuilder;
import appeng.helpers.transfer.TransferFilterBuilder.EmptyPolicy;
import appeng.helpers.transfer.TransferFilterBuilder.Filter;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

public class TransferFilterBuilderFunctionalTest {

    private static IAEStack<?> iron;
    private static IAEStack<?> gold;
    private static IAEStack<?> water;
    private static IAEStack<?> pickaxe;
    private static IAEStack<?> damagedPickaxe;

    @BeforeAll
    public static void createStacks() {
        OreDictionary.registerOre("ingotIron", new ItemStack(Items.iron_ingot));
        iron = AEItemStack.create(new ItemStack(Items.iron_ingot));
        gold = AEItemStack.create(new ItemStack(Items.gold_ingot));
        water = AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1));
        pickaxe = AEItemStack.create(new ItemStack(Items.iron_pickaxe));
        damagedPickaxe = AEItemStack.create(new ItemStack(Items.iron_pickaxe, 1, 100));
    }

    private static Filter precise(final List<IAEStack<?>> config, final boolean inverter, final EmptyPolicy policy) {
        return TransferFilterBuilder
                .build(ITEM_STACK_TYPE, config, false, FuzzyMode.IGNORE_ALL, false, "", inverter, policy);
    }

    @Test
    public void preciseListMatchesOnlyConfiguredStacks() {
        final Filter filter = precise(Collections.singletonList(iron), false, EmptyPolicy.MATCH_ALL);

        assertTrue(filter.test(iron));
        assertFalse(filter.test(gold));
    }

    @Test
    public void preciseListDistinguishesDamage() {
        final Filter filter = precise(Collections.singletonList(pickaxe), false, EmptyPolicy.MATCH_NONE);

        assertFalse(filter.test(damagedPickaxe));
    }

    @Test
    public void fuzzyListIgnoresDamage() {
        final Filter filter = TransferFilterBuilder.build(
                ITEM_STACK_TYPE,
                Collections.singletonList(pickaxe),
                true,
                FuzzyMode.IGNORE_ALL,
                false,
                "",
                false,
                EmptyPolicy.MATCH_NONE);

        assertTrue(filter.test(damagedPickaxe));
        assertFalse(filter.test(iron));
    }

    @Test
    public void inverterTurnsTheListIntoABlacklist() {
        final Filter filter = precise(Collections.singletonList(iron), true, EmptyPolicy.MATCH_ALL);

        assertFalse(filter.test(iron));
        assertTrue(filter.test(gold));
    }

    @Test
    public void emptyPolicyDecidesWhenNothingIsConfigured() {
        assertTrue(precise(Collections.emptyList(), false, EmptyPolicy.MATCH_ALL).test(iron));
        assertFalse(precise(Collections.emptyList(), false, EmptyPolicy.MATCH_NONE).test(iron));
        assertTrue(precise(Collections.emptyList(), true, EmptyPolicy.MATCH_ALL).test(iron));
    }

    @Test
    public void oreFilterReplacesConfig() {
        final Filter filter = TransferFilterBuilder.build(
                ITEM_STACK_TYPE,
                Collections.singletonList(gold),
                false,
                FuzzyMode.IGNORE_ALL,
                true,
                "ingotIron",
                false,
                EmptyPolicy.MATCH_NONE);

        assertTrue(filter.test(iron));
        assertFalse(filter.test(gold));
    }

    @Test
    public void emptyOreFilterFallsBackToThePolicy() {
        final Filter all = TransferFilterBuilder.build(
                ITEM_STACK_TYPE,
                Collections.emptyList(),
                false,
                FuzzyMode.IGNORE_ALL,
                true,
                "",
                false,
                EmptyPolicy.MATCH_ALL);

        assertTrue(all.test(iron));
    }

    @Test
    public void otherStackTypesNeverMatch() {
        assertFalse(precise(Collections.emptyList(), false, EmptyPolicy.MATCH_ALL).test(water));
        assertFalse(precise(Collections.singletonList(iron), true, EmptyPolicy.MATCH_ALL).test(water));

        final Filter fluids = TransferFilterBuilder.build(
                FLUID_STACK_TYPE,
                Collections.singletonList(water),
                false,
                FuzzyMode.IGNORE_ALL,
                false,
                "",
                false,
                EmptyPolicy.MATCH_NONE);
        assertTrue(fluids.test(water));
        assertFalse(fluids.test(iron));
    }

    @Test
    public void onlyWhitelistsCountAsListed() {
        assertTrue(precise(Collections.singletonList(iron), false, EmptyPolicy.MATCH_ALL).isListed(iron));
        assertFalse(precise(Collections.emptyList(), false, EmptyPolicy.MATCH_ALL).isListed(iron));
        assertFalse(precise(Collections.singletonList(gold), true, EmptyPolicy.MATCH_ALL).isListed(iron));
    }

    @Test
    public void activeConfigSkipsEmptyAndUnusableSlots() {
        final IAEStackInventory inventory = new IAEStackInventory(null, 9);
        inventory.putAEStackInSlot(0, iron);
        inventory.putAEStackInSlot(4, gold);

        assertEquals(
                Collections.singletonList(iron),
                TransferFilterBuilder.activeConfig(inventory, 1, ITEM_STACK_TYPE));
        assertEquals(Arrays.asList(iron, gold), TransferFilterBuilder.activeConfig(inventory, 9, ITEM_STACK_TYPE));
    }

    @Test
    public void usableSlotCountsFollowCapacityCards() {
        assertEquals(1, TransferFilterBuilder.usableBusSlots(0));
        assertEquals(9, TransferFilterBuilder.usableBusSlots(2));
        assertEquals(9, TransferFilterBuilder.usableBusSlots(5));
        assertEquals(18, TransferFilterBuilder.usableWideSlots(0));
        assertEquals(63, TransferFilterBuilder.usableWideSlots(5));
    }

    @Test
    public void installedUpgradesMergeIdenticalCards() {
        final InventoryBasic upgrades = new InventoryBasic("test", false, 4);
        upgrades.setInventorySlotContents(0, new ItemStack(Items.diamond, 1));
        upgrades.setInventorySlotContents(2, new ItemStack(Items.diamond, 1));
        upgrades.setInventorySlotContents(3, new ItemStack(Items.emerald, 1));

        final List<ItemStack> merged = TransferFilterBuilder.installedUpgrades(upgrades);

        assertEquals(2, merged.size());
        assertEquals(2, merged.get(0).stackSize);
        assertEquals(1, merged.get(1).stackSize);
    }
}
