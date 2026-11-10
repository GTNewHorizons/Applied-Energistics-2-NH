package appeng.util;

import static appeng.util.Platform.readStackNBT;

import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.data.IAEStack;

@NullMarked
public final class GenericStackNBT {

    private GenericStackNBT() {}

    /**
     * Reads a stack written by {@link GenericStack#toNBT()} or {@link AEKey#toNBT()}, or by {@link IAEStack} in one of
     * its older formats. The fallback to the older formats goes away together with {@link IAEStack}.
     */
    public static @Nullable GenericStack read(final NBTTagCompound tag) {
        if (!tag.getString(AEKey.TYPE_FIELD).isEmpty()) {
            return GenericStack.fromNBT(tag);
        }

        // Older formats have no stack type, a numeric one, or are plain item stacks without "Cnt"
        final IAEStack<?> stack = readStackNBT(tag, false);
        if (stack != null && stack.getStackSize() == 0) stack.setStackSize(tag.getInteger("Count"));
        return GenericStack.fromAEStack(stack);
    }
}
