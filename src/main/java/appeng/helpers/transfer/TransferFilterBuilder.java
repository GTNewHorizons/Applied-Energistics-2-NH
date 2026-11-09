package appeng.helpers.transfer;

import static appeng.util.Platform.isAE2FCLoaded;
import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;
import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.prioitylist.FuzzyPriorityList;
import appeng.util.prioitylist.IPartitionList;
import appeng.util.prioitylist.OreFilteredList;
import appeng.util.prioitylist.PrecisePriorityList;

/**
 * Turns a component's config slots and cards into the same partition lists AE uses when it moves stacks.
 */
public final class TransferFilterBuilder {

    /** What a component does when nothing is configured. */
    public enum EmptyPolicy {
        MATCH_ALL,
        MATCH_NONE
    }

    private TransferFilterBuilder() {}

    /**
     * @param type            stack type the component handles, other types never match
     * @param config          configured stacks in usable slots
     * @param fuzzy           whether a fuzzy card applies
     * @param fuzzyMode       mode used when fuzzy
     * @param oreFilter       whether an oredict card is installed, config is then ignored
     * @param oreFilterString the oredict card's filter
     * @param inverter        whether an inverter card turns the list into a blacklist
     * @param emptyPolicy     result when there is no usable list
     */
    public static Filter build(final IAEStackType<?> type, final List<IAEStack<?>> config, final boolean fuzzy,
            final FuzzyMode fuzzyMode, final boolean oreFilter, final String oreFilterString, final boolean inverter,
            final EmptyPolicy emptyPolicy) {
        return new Filter(
                type,
                createList(type, config, fuzzy, fuzzyMode, oreFilter, oreFilterString),
                inverter,
                emptyPolicy);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static IPartitionList createList(final IAEStackType<?> type, final List<IAEStack<?>> config,
            final boolean fuzzy, final FuzzyMode fuzzyMode, final boolean oreFilter, final String oreFilterString) {
        if (oreFilter && type == ITEM_STACK_TYPE) {
            return new OreFilteredList(oreFilterString);
        }

        final IItemList list = type.createList();
        for (final IAEStack<?> stack : config) {
            if (stack.getStackType() == type) {
                list.add(stack);
            }
        }

        return fuzzy && type == ITEM_STACK_TYPE ? new FuzzyPriorityList<>(list, fuzzyMode)
                : new PrecisePriorityList<>(list);
    }

    /**
     * @return the configured stacks in the first {@code usableSlots} slots, without empty slots
     */
    public static List<IAEStack<?>> activeConfig(final IAEStackInventory inventory, final int usableSlots,
            final IAEStackType<?> type) {
        final List<IAEStack<?>> stacks = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSizeInventory() && slot < usableSlots; slot++) {
            IAEStack<?> stack = inventory.getAEStackInSlot(slot);
            if (type == FLUID_STACK_TYPE && isAE2FCLoaded
                    && stack instanceof IAEItemStack itemStack
                    && itemStack.getItem() instanceof ItemFluidPacket) {
                stack = ItemFluidPacket.getFluidAEStack(itemStack);
            }
            if (stack != null) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    /**
     * @return one stack per kind of installed card, its size being how many of that card are installed
     */
    public static List<ItemStack> installedUpgrades(final IInventory upgrades) {
        final List<ItemStack> merged = new ArrayList<>();
        if (upgrades == null) {
            return merged;
        }

        for (int slot = 0; slot < upgrades.getSizeInventory(); slot++) {
            final ItemStack card = upgrades.getStackInSlot(slot);
            if (card != null) {
                addMerged(merged, card);
            }
        }
        return merged;
    }

    private static void addMerged(final List<ItemStack> merged, final ItemStack card) {
        for (final ItemStack existing : merged) {
            if (ItemStack.areItemStackTagsEqual(existing, card) && existing.isItemEqual(card)) {
                existing.stackSize += card.stackSize;
                return;
            }
        }
        merged.add(card.copy());
    }

    /**
     * @return usable config slots of an import or export bus
     */
    public static int usableBusSlots(final int capacityCards) {
        return Math.min(1 + capacityCards * 4, 9);
    }

    /**
     * @return usable config slots of a storage bus or formation plane
     */
    public static int usableWideSlots(final int capacityCards) {
        return 18 + capacityCards * 9;
    }

    public static final class Filter {

        private final IAEStackType<?> type;
        @SuppressWarnings("rawtypes")
        private final IPartitionList list;
        private final boolean inverter;
        private final EmptyPolicy emptyPolicy;

        @SuppressWarnings("rawtypes")
        private Filter(final IAEStackType<?> type, final IPartitionList list, final boolean inverter,
                final EmptyPolicy emptyPolicy) {
            this.type = type;
            this.list = list;
            this.inverter = inverter;
            this.emptyPolicy = emptyPolicy;
        }

        /**
         * @return true if the component would let this stack through
         */
        @SuppressWarnings("unchecked")
        public boolean test(final IAEStack<?> stack) {
            if (!this.isSameType(stack)) {
                return false;
            }
            if (this.list.isEmpty()) {
                return this.emptyPolicy == EmptyPolicy.MATCH_ALL;
            }
            return this.list.isListed(stack) != this.inverter;
        }

        /**
         * @return true if a whitelist names this stack, as opposed to the stack merely not being refused
         */
        @SuppressWarnings("unchecked")
        public boolean isListed(final IAEStack<?> stack) {
            return this.isSameType(stack) && !this.inverter && !this.list.isEmpty() && this.list.isListed(stack);
        }

        private boolean isSameType(final IAEStack<?> stack) {
            return stack != null && stack.getStackType() == this.type;
        }
    }
}
