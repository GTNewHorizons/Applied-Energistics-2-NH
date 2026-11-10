package appeng.api.stacks;

import java.io.IOException;
import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.Nullable;

import appeng.api.storage.data.IAEStack;
import io.netty.buffer.ByteBuf;

/**
 * Represents some amount of some generic resource that AE can store or handle in crafting.
 */
public final class GenericStack {

    private static final String NBT_AMOUNT = "Cnt";

    private final AEKey key;
    private final long amount;

    public GenericStack(AEKey key, long amount) {
        this.key = Objects.requireNonNull(key, "key");
        this.amount = amount;
    }

    /**
     * Converts from an {@link IAEStack} for code that does not use keys yet. Goes away together with {@link IAEStack}.
     */
    @Nullable
    public static GenericStack fromAEStack(@Nullable IAEStack<?> stack) {
        return stack == null ? null : new GenericStack(stack.getKey(), stack.getStackSize());
    }

    public AEKey getKey() {
        return this.key;
    }

    public long getAmount() {
        return this.amount;
    }

    /**
     * Converts to an {@link IAEStack} for code that does not use keys yet. Goes away together with {@link IAEStack}.
     */
    public IAEStack<?> toAEStack() {
        return this.key.toAEStack(this.amount);
    }

    /**
     * Null-tolerant variant of {@link #toAEStack()}. Goes away together with {@link IAEStack}.
     */
    @Nullable
    public static IAEStack<?> toAEStackOrNull(@Nullable GenericStack stack) {
        return stack == null ? null : stack.toAEStack();
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = this.key.toNBT();
        tag.setLong(NBT_AMOUNT, this.amount);
        return tag;
    }

    @Nullable
    public static GenericStack fromNBT(@Nullable NBTTagCompound tag) {
        if (tag == null) {
            return null;
        }
        AEKey key = AEKey.fromNBT(tag);
        return key == null ? null : new GenericStack(key, tag.getLong(NBT_AMOUNT));
    }

    public static void writeBuffer(ByteBuf output, @Nullable GenericStack stack) throws IOException {
        output.writeBoolean(stack != null);
        if (stack != null) {
            stack.key.writeToPacket(output);
            output.writeLong(stack.amount);
        }
    }

    @Nullable
    public static GenericStack readBuffer(ByteBuf input) throws IOException {
        if (!input.readBoolean()) {
            return null;
        }
        final AEKey key = AEKey.readKey(input);
        final long amount = input.readLong();
        return key == null ? null : new GenericStack(key, amount);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof GenericStack other && this.amount == other.amount && this.key.equals(other.key);
    }

    @Override
    public int hashCode() {
        return 31 * this.key.hashCode() + Long.hashCode(this.amount);
    }

    @Override
    public String toString() {
        return this.amount + "x" + this.key;
    }
}
