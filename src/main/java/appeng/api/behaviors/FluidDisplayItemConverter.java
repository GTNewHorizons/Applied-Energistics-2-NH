package appeng.api.behaviors;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.util.Platform;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.StackInfo;

/**
 * Converts fluid display items known to NEI (e.g. GT fluid displays) into fluids. Only registered if NEI is loaded.
 */
final class FluidDisplayItemConverter implements DisplayItemConverter {

    @Override
    public @Nullable GenericStack convert(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidDisplay display) {
            // StackInfo would cut the amount of NEI's own display item to an int.
            AEFluidKey key = AEFluidKey.of(display.getFluid(stack));
            return key == null ? null : new GenericStack(key, display.getAmountLong(stack));
        }
        if (stack.getItem() instanceof ItemFluidPacket) {
            // Left to FluidPacketDisplayItemConverter, since StackInfo would also cut the amount to an int.
            return null;
        }

        FluidStack fluidStack = StackInfo.getFluid(Platform.copyStackWithSizeOne(stack));
        AEFluidKey key = AEFluidKey.of(fluidStack);
        return key == null ? null : new GenericStack(key, fluidStack.amount);
    }
}
