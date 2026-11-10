package appeng.api.storage;

import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.util.GenericStackNBT;

/**
 * A configuration inventory that only holds resources without amounts, as used by filters.
 * <p/>
 * {@link #getStack(int)} reports an amount of 1 and {@link #setStack(int, GenericStack)} drops the amount.
 */
@NullMarked
public class KeyConfigInventory extends ConfigInventory {

    private final @Nullable AEKey[] keys;

    public KeyConfigInventory(final @Nullable IConfigInventoryHost host, final int size,
            final StorageName storageName) {
        super(host, storageName);
        this.keys = new AEKey[size];
    }

    public KeyConfigInventory(final @Nullable IConfigInventoryHost host, final int size) {
        this(host, size, StorageName.NONE);
    }

    @Override
    public int getSizeInventory() {
        return this.keys.length;
    }

    @Override
    public @Nullable AEKey getKey(final int slot) {
        return this.keys[slot];
    }

    public void setKey(final int slot, final @Nullable AEKey key) {
        this.keys[slot] = key;
        this.onChange(slot);
    }

    @Override
    public final @Nullable GenericStack getStack(final int slot) {
        final AEKey key = this.getKey(slot);
        return key == null ? null : new GenericStack(key, 1);
    }

    @Override
    public final void setStack(final int slot, final @Nullable GenericStack stack) {
        this.setKey(slot, stack == null ? null : stack.getKey());
    }

    @Override
    protected NBTTagCompound writeSlotToNBT(final int slot) {
        return Objects.requireNonNull(this.getKey(slot)).toNBT();
    }

    @Override
    protected void readSlotFromNBT(final int slot, final NBTTagCompound tag) {
        final GenericStack stack = GenericStackNBT.read(tag);
        this.keys[slot] = stack == null ? null : stack.getKey();
    }
}
