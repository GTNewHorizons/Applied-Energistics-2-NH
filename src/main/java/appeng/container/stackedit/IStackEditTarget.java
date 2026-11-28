package appeng.container.stackedit;

import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;

import appeng.api.storage.data.IAEStack;
import appeng.tile.inventory.IAEStackInventory;

public interface IStackEditTarget {

    @Nullable
    IAEStack<?> getStack();

    void setStack(IAEStack<?> stack);

    static @Nullable IStackEditTarget of(final IAEStackInventory inventory, final int slot) {
        return of(inventory, slot, UnaryOperator.identity());
    }

    /**
     * @return {@code null} if {@code slot} is out of range
     */
    static @Nullable IStackEditTarget of(final IAEStackInventory inventory, final int slot,
            final UnaryOperator<IAEStack<?>> normalizer) {
        if (slot < 0 || slot >= inventory.getSizeInventory()) {
            return null;
        }

        return new IStackEditTarget() {

            @Override
            public @Nullable IAEStack<?> getStack() {
                return inventory.getAEStackInSlot(slot);
            }

            @Override
            public void setStack(final IAEStack<?> stack) {
                inventory.putAEStackInSlot(slot, normalizer.apply(stack));
            }
        };
    }
}
