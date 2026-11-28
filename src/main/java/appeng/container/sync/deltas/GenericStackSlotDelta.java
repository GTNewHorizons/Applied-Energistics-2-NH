package appeng.container.sync.deltas;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import appeng.api.stacks.GenericStack;

@NullMarked
public final class GenericStackSlotDelta {

    private final int slotIndex;
    private final @Nullable GenericStack stack;

    public GenericStackSlotDelta(final int slotIndex, final @Nullable GenericStack stack) {
        this.slotIndex = slotIndex;
        this.stack = stack;
    }

    public int getSlotIndex() {
        return this.slotIndex;
    }

    public @Nullable GenericStack getStack() {
        return this.stack;
    }
}
