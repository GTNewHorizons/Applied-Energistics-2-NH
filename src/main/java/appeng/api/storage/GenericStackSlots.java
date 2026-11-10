package appeng.api.storage;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import appeng.api.stacks.GenericStack;

/**
 * Slot-indexed access to {@link GenericStack}s.
 */
@NullMarked
public interface GenericStackSlots {

    int getSizeInventory();

    @Nullable
    GenericStack getStack(int slot);

    void setStack(int slot, @Nullable GenericStack stack);
}
