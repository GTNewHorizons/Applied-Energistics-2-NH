package appeng.util.item;

import java.util.Objects;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class FluidKey {

    final Fluid fluid;
    final @Nullable NBTTagCompound tag;

    FluidKey(final FluidStack stack) {
        this.fluid = Objects.requireNonNull(stack.getFluid());
        this.tag = stack.tag == null ? null : (NBTTagCompound) stack.tag.copy();
    }

    public static FluidKey of(final FluidStack stack) {
        return new FluidKey(stack);
    }

    public FluidStack toStack(final int amount) {
        final FluidStack stack = new FluidStack(this.fluid, amount);
        stack.tag = this.tag == null ? null : (NBTTagCompound) this.tag.copy();
        return stack;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        return o instanceof FluidKey k && k.fluid == this.fluid && Objects.equals(k.tag, this.tag);
    }

    @Override
    public int hashCode() {
        return 31 * this.fluid.hashCode() + Objects.hashCode(this.tag);
    }
}
