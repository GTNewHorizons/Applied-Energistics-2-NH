package appeng.api.behaviors;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;

/**
 * Converts fluid packets, which {@link AEFluidKey#wrapForDisplayOrFilter} creates without NEI, into fluids.
 */
final class FluidPacketDisplayItemConverter implements DisplayItemConverter {

    @Override
    public @Nullable GenericStack convert(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemFluidPacket) || !stack.hasTagCompound()) {
            return null;
        }
        // ItemFluidPacket.getFluidStack drops packets without amount, but keys wrapped with amount 0 must convert back.
        FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(stack.getTagCompound().getCompoundTag("FluidStack"));
        AEFluidKey key = AEFluidKey.of(fluidStack);
        return key == null ? null : new GenericStack(key, ItemFluidPacket.getFluidAmount(stack));
    }
}
