package appeng.api.behaviors;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

/**
 * Strategy to interact with the non-item keys held by container items, for example the fluid contained in a bucket.
 * <p>
 * {@link ContainerItemStrategies} passes containers as stacks of size 1. They are never modified; {@link #fill} and
 * {@link #drain} return the changed container as a new stack. Results must be deterministic.
 */
public interface ContainerItemStrategy<T extends AEKey> {

    /**
     * @return whether this strategy handles {@code stack}, regardless of whether it is currently empty.
     */
    boolean isContainer(@Nullable ItemStack stack);

    /**
     * @return the key and amount held by {@code container}, or null if it is empty or not handled by this strategy.
     */
    @Nullable
    GenericStack getContainedStack(ItemStack container);

    /**
     * Inserts up to {@code amount} of {@code key} into the container.
     */
    ContainerItemResult fill(ItemStack container, T key, long amount);

    /**
     * Extracts up to {@code amount} of {@code key} from the container.
     */
    ContainerItemResult drain(ItemStack container, T key, long amount);
}
