package appeng.api.storage;

import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.util.GenericStackNBT;

/**
 * A configuration inventory that holds resources together with amounts, e.g. for pattern encoding or stocking targets.
 */
@NullMarked
public class StackConfigInventory extends ConfigInventory {

    private final @Nullable GenericStack[] stacks;

    public StackConfigInventory(final @Nullable IConfigInventoryHost host, final int size,
            final StorageName storageName) {
        super(host, storageName);
        this.stacks = new GenericStack[size];
    }

    public StackConfigInventory(final @Nullable IConfigInventoryHost host, final int size) {
        this(host, size, StorageName.NONE);
    }

    @Override
    public int getSizeInventory() {
        return this.stacks.length;
    }

    @Override
    public @Nullable GenericStack getStack(final int slot) {
        return this.stacks[slot];
    }

    @Override
    public @Nullable AEKey getKey(final int slot) {
        final GenericStack stack = this.getStack(slot);
        return stack == null ? null : stack.getKey();
    }

    public long getAmount(final int slot) {
        final GenericStack stack = this.getStack(slot);
        return stack == null ? 0 : stack.getAmount();
    }

    @Override
    public void setStack(final int slot, final @Nullable GenericStack stack) {
        this.stacks[slot] = stack;
        this.onChange(slot);
    }

    @Override
    protected NBTTagCompound writeSlotToNBT(final int slot) {
        return Objects.requireNonNull(this.getStack(slot)).toNBT();
    }

    @Override
    protected void readSlotFromNBT(final int slot, final NBTTagCompound tag) {
        this.stacks[slot] = GenericStackNBT.read(tag);
    }
}
