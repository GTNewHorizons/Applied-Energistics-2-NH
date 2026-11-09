package appeng.api.behaviors;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

public final class ContainerItemResult {

    private static final ContainerItemResult NONE = new ContainerItemResult(null, 0);

    private final @Nullable ItemStack container;
    private final long amount;

    private ContainerItemResult(@Nullable ItemStack container, long amount) {
        this.container = container;
        this.amount = amount;
    }

    /**
     * @return a result for an operation that moved nothing and left the container unchanged.
     */
    public static ContainerItemResult none() {
        return NONE;
    }

    /**
     * @param container the container after the operation
     * @param amount    the amount moved, if not positive the result is {@link #none()}
     */
    public static ContainerItemResult of(@Nullable ItemStack container, long amount) {
        return container == null || amount <= 0 ? NONE : new ContainerItemResult(container, amount);
    }

    /**
     * @return the container after the operation, or null if nothing was moved.
     */
    public @Nullable ItemStack getContainer() {
        return this.container;
    }

    public long getAmount() {
        return this.amount;
    }

    public boolean isEmpty() {
        return this.amount <= 0;
    }
}
