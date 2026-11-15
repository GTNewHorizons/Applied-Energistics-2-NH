package appeng.api.storage;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants.NBT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

/**
 * A fixed-size configuration inventory, either {@link KeyConfigInventory} or {@link StackConfigInventory}. Code that
 * owns a specific kind should use its own API rather than {@link #getStack(int)} and
 * {@link #setStack(int, GenericStack)}.
 */
@NullMarked
public abstract class ConfigInventory implements GenericStackSlots {

    private static final Logger LOG = LogManager.getLogger(ConfigInventory.class);

    private final @Nullable IConfigInventoryHost host;
    private final StorageName storageName;

    protected ConfigInventory(final @Nullable IConfigInventoryHost host, final StorageName storageName) {
        this.host = host;
        this.storageName = storageName;
    }

    public abstract @Nullable AEKey getKey(int slot);

    @Override
    public abstract @Nullable GenericStack getStack(int slot);

    @Override
    public abstract void setStack(int slot, @Nullable GenericStack stack);

    /**
     * @return the NBT of a non-empty slot
     */
    protected abstract NBTTagCompound writeSlotToNBT(int slot);

    /**
     * Sets a slot from its NBT while loading, without notifying the host.
     */
    protected abstract void readSlotFromNBT(int slot, NBTTagCompound tag);

    public boolean isEmpty() {
        for (int x = 0; x < this.getSizeInventory(); x++) {
            if (this.getKey(x) != null) {
                return false;
            }
        }
        return true;
    }

    public void writeToNBT(final ItemStack stack, final String name) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        this.writeToNBT(tag, name);
        if (tag.hasNoTags()) {
            stack.setTagCompound(null);
        }
    }

    public void writeToNBT(final NBTTagCompound data, final String name) {
        final NBTTagCompound c = new NBTTagCompound();
        this.writeToNBT(c);
        if (c.hasNoTags()) {
            data.removeTag(name);
        } else {
            data.setTag(name, c);
        }
    }

    private void writeToNBT(final NBTTagCompound target) {
        for (int x = 0; x < this.getSizeInventory(); x++) {
            if (this.getKey(x) == null) {
                continue;
            }
            try {
                target.setTag("#" + x, this.writeSlotToNBT(x));
            } catch (final Exception e) {
                LOG.debug("Failed to write config slot {}", x, e);
            }
        }
    }

    public void readFromNBT(final @Nullable NBTTagCompound data, final String name) {
        if (data != null && data.hasKey(name, NBT.TAG_COMPOUND)) {
            this.readFromNBT(data.getCompoundTag(name));
        }
    }

    private void readFromNBT(final NBTTagCompound target) {
        for (int x = 0; x < this.getSizeInventory(); x++) {
            try {
                final String key = "#" + x;
                if (target.hasKey(key, NBT.TAG_COMPOUND)) {
                    this.readSlotFromNBT(x, target.getCompoundTag(key));
                }
            } catch (final Exception e) {
                LOG.debug("Failed to read config slot {}", x, e);
            }
        }
    }

    /**
     * Called after a slot was changed, but not while loading from NBT.
     */
    protected void onChange(final int slot) {
        if (this.host != null) {
            this.host.onChangeConfigInventory(this, slot);
        }
    }

    public StorageName getStorageName() {
        return this.storageName;
    }
}
