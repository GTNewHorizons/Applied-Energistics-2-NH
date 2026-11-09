package appeng.api.behaviors;

import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.util.Platform;

/**
 * Moves resources between a stack of container items and a {@link MEStorage}, using {@link ContainerItemStrategies}.
 * <p>
 * The result for one container is applied to as many containers as the storage allows, plus at most one partially
 * processed container. If the storage moves less than simulated, the surplus is moved back.
 */
public final class ContainerTransfer {

    private ContainerTransfer() {}

    /**
     * Empties containers from {@code slot} into {@code storage}.
     *
     * @param filter        which contained resources may be inserted
     * @param maxContainers how many containers of the stack to process at most
     * @param requireAll    only transfer if all {@code maxContainers} (or the whole stack, if smaller) can be fully
     *                      emptied
     * @param mode          with {@link Actionable#SIMULATE}, nothing is changed and the possible result is returned
     */
    public static ContainerTransferResult drainInto(ItemSlotAccess slot, MEStorage storage, IEnergySource energy,
            BaseActionSource source, Predicate<? super AEKey> filter, int maxContainers, boolean requireAll,
            Actionable mode) {
        final ItemStack hand = slot.get();
        if (hand == null || hand.stackSize <= 0 || maxContainers <= 0) {
            return ContainerTransferResult.none();
        }

        final ItemStack one = Platform.copyStackWithSizeOne(hand);
        final GenericStack contained = ContainerItemStrategies.getContainedStack(one, filter);
        if (contained == null) {
            return ContainerTransferResult.none();
        }

        final AEKey what = contained.getKey();
        return transfer(
                slot,
                hand,
                one,
                what,
                contained.getAmount(),
                maxContainers,
                requireAll,
                mode,
                (container, amount) -> ContainerItemStrategies.drain(container, what, amount),
                (amount, actionable) -> StorageHelper.poweredInsert(energy, storage, what, amount, source, actionable),
                // The storage received more than the containers can give up; take it back.
                surplus -> surplus - storage.extract(what, surplus, Actionable.MODULATE, source));
    }

    /**
     * Fills containers from {@code slot} with {@code what} taken from {@code storage}.
     *
     * @param maxContainers how many containers of the stack to process at most
     * @param mode          with {@link Actionable#SIMULATE}, nothing is changed and the possible result is returned
     */
    public static ContainerTransferResult fillFrom(ItemSlotAccess slot, MEStorage storage, IEnergySource energy,
            BaseActionSource source, AEKey what, int maxContainers, Actionable mode) {
        final ItemStack hand = slot.get();
        if (hand == null || hand.stackSize <= 0 || maxContainers <= 0) {
            return ContainerTransferResult.none();
        }

        final ItemStack one = Platform.copyStackWithSizeOne(hand);
        if (!ContainerItemStrategies.isContainer(one, what.getType())) {
            return ContainerTransferResult.none();
        }

        return transfer(
                slot,
                hand,
                one,
                what,
                Long.MAX_VALUE,
                maxContainers,
                false,
                mode,
                (container, amount) -> ContainerItemStrategies.fill(container, what, amount),
                (amount, actionable) -> StorageHelper.poweredExtract(energy, storage, what, amount, source, actionable),
                surplus -> {
                    // The containers cannot take everything that was extracted; put the rest back.
                    final long reverted = storage.insert(what, surplus, Actionable.MODULATE, source);
                    slot.giveLeftover(what, surplus - reverted);
                    return 0;
                });
    }

    /**
     * @param request   the amount to request from a single container
     * @param operation fills or drains a container
     * @param storageOp moves the resource in the storage and returns how much was moved
     * @param revert    moves back a surplus that the storage moved but the containers cannot take, and returns how much
     *                  of it could not be moved back. That rest must be something the containers of the original plan
     *                  can give up, which only holds when draining.
     */
    private static ContainerTransferResult transfer(ItemSlotAccess slot, ItemStack hand, ItemStack one, AEKey what,
            long request, int maxContainers, boolean requireAll, Actionable mode, Operation operation,
            StorageOperation storageOp, SurplusHandler revert) {
        final ContainerItemResult perContainer = operation.apply(one, request);
        if (perContainer.isEmpty()) {
            return ContainerTransferResult.none();
        }

        final int count = Math.min(hand.stackSize, maxContainers);
        final long budget = storageOp.move(saturatedMultiply(perContainer.getAmount(), count), Actionable.SIMULATE);
        Plan plan = Plan.create(one, perContainer, count, budget, operation);
        if (plan == null || requireAll && plan.fullCount < count) {
            return ContainerTransferResult.none();
        }
        if (mode == Actionable.SIMULATE) {
            return plan.toResult();
        }

        final long planned = plan.getAmount();
        final long moved = storageOp.move(planned, Actionable.MODULATE);
        if (moved < planned) {
            final Plan fullPlan = plan;
            plan = Plan.create(one, perContainer, count, moved, operation);
            if (requireAll && plan != null && plan.fullCount < count) {
                // The storage changed since the simulation; move everything back instead of transferring a part.
                plan = null;
            }
            final long surplus = moved - (plan == null ? 0 : plan.getAmount());
            final long notReverted = surplus > 0 ? revert.handle(surplus) : 0;
            if (notReverted > 0) {
                // The rest is stuck in the storage, so keeping it in the containers would duplicate it. Process the
                // original plan instead and hand out what the storage did not receive.
                plan = fullPlan;
                slot.giveLeftover(what, planned - (moved - surplus + notReverted));
            } else if (plan == null) {
                return ContainerTransferResult.none();
            }
        }

        plan.apply(slot, hand);
        return plan.toResult();
    }

    private static long saturatedMultiply(long a, long b) {
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            return Long.MAX_VALUE;
        }
    }

    @FunctionalInterface
    private interface Operation {

        ContainerItemResult apply(ItemStack container, long amount);
    }

    @FunctionalInterface
    private interface StorageOperation {

        long move(long amount, Actionable mode);
    }

    @FunctionalInterface
    private interface SurplusHandler {

        /**
         * @return the amount that could not be moved back
         */
        long handle(long surplus);
    }

    /**
     * {@code fullCount} containers become {@code fullResult}, and optionally one more becomes {@code partialResult}.
     */
    private static final class Plan {

        private final ItemStack fullResult;
        private final long perContainer;
        private final int fullCount;
        private final @Nullable ItemStack partialResult;
        private final long partialAmount;

        private Plan(ItemStack fullResult, long perContainer, int fullCount, @Nullable ItemStack partialResult,
                long partialAmount) {
            this.fullResult = fullResult;
            this.perContainer = perContainer;
            this.fullCount = fullCount;
            this.partialResult = partialResult;
            this.partialAmount = partialAmount;
        }

        /**
         * @param perContainer the result of the operation on a single container without an amount limit
         * @param budget       how much the storage can move
         * @return the plan, or null if nothing can be moved
         */
        static @Nullable Plan create(ItemStack one, ContainerItemResult perContainer, int count, long budget,
                Operation operation) {
            final ItemStack fullResult = perContainer.getContainer();
            if (fullResult == null || budget <= 0) {
                return null;
            }

            final long amount = perContainer.getAmount();
            final int fullCount = (int) Math.min(count, budget / amount);
            final long remainder = budget - fullCount * amount;

            ItemStack partialResult = null;
            long partialAmount = 0;
            if (fullCount < count && remainder > 0) {
                final ContainerItemResult partial = operation.apply(one, remainder);
                if (!partial.isEmpty()) {
                    partialResult = partial.getContainer();
                    partialAmount = partial.getAmount();
                }
            }

            if (fullCount == 0 && partialResult == null) {
                return null;
            }
            return new Plan(fullResult, amount, fullCount, partialResult, partialAmount);
        }

        long getAmount() {
            return this.perContainer * this.fullCount + this.partialAmount;
        }

        int getContainers() {
            return this.fullCount + (this.partialResult == null ? 0 : 1);
        }

        ContainerTransferResult toResult() {
            return ContainerTransferResult.of(this.getAmount(), this.getContainers());
        }

        /**
         * Removes the processed containers from the slot and hands out the results. If the slot becomes empty, the
         * first result goes back into it.
         */
        void apply(ItemSlotAccess slot, ItemStack hand) {
            final int remaining = hand.stackSize - this.getContainers();
            boolean slotFree = remaining <= 0;
            if (!slotFree) {
                slot.set(Platform.copyStackWithSize(hand, remaining));
            }

            int left = this.fullCount;
            final int maxStackSize = Math.max(1, this.fullResult.getMaxStackSize());
            while (left > 0) {
                final int size = Math.min(left, maxStackSize);
                left -= size;
                slotFree = place(slot, Platform.copyStackWithSize(this.fullResult, size), slotFree);
            }
            if (this.partialResult != null) {
                slotFree = place(slot, this.partialResult.copy(), slotFree);
            }
            if (slotFree) {
                slot.set(null);
            }
        }

        /**
         * Puts {@code output} into the slot if it is free, otherwise gives it to the player.
         *
         * @return whether the slot is still free
         */
        private static boolean place(ItemSlotAccess slot, ItemStack output, boolean slotFree) {
            if (slotFree) {
                slot.set(output);
            } else {
                slot.giveOrDrop(output);
            }
            return false;
        }
    }
}
