package appeng.api.behaviors;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.util.FluidUtils;
import appeng.util.Platform;
import codechicken.nei.recipe.StackInfo;
import it.unimi.dsi.fastutil.objects.ObjectLongPair;

/**
 * Handles {@link IFluidContainerItem}s and containers registered in {@link FluidContainerRegistry}.
 */
final class FluidContainerItemStrategy implements ContainerItemStrategy<AEFluidKey> {

    @Override
    public boolean isContainer(@Nullable ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (stack.getItem() instanceof IFluidContainerItem) {
            // Some fluid display items are IFluidContainerItems, but only show a fluid.
            return !Platform.isNEILoaded || !StackInfo.isFluidDisplayItem(stack);
        }
        return FluidContainerRegistry.isContainer(stack);
    }

    @Override
    public @Nullable GenericStack getContainedStack(ItemStack container) {
        if (!this.isContainer(container)) {
            return null;
        }

        FluidStack fluidStack = container.getItem() instanceof IFluidContainerItem fluidContainer
                ? fluidContainer.getFluid(container)
                : FluidContainerRegistry.getFluidForFilledItem(container);
        AEFluidKey key = AEFluidKey.of(fluidStack);
        return key == null || fluidStack.amount <= 0 ? null : new GenericStack(key, fluidStack.amount);
    }

    @Override
    public ContainerItemResult fill(ItemStack container, AEFluidKey key, long amount) {
        ObjectLongPair<ItemStack> filled = FluidUtils.fillFluidContainer(container.copy(), key.toStack(amount));
        return ContainerItemResult.of(filled.left(), filled.rightLong());
    }

    @Override
    public ContainerItemResult drain(ItemStack container, AEFluidKey key, long amount) {
        ItemStack workingCopy = container.copy();
        if (workingCopy.getItem() instanceof IFluidContainerItem fluidContainer) {
            if (!key.matches(fluidContainer.getFluid(workingCopy))) {
                return ContainerItemResult.none();
            }

            FluidStack drained = fluidContainer.drain(workingCopy, Platform.longToInt(amount), true);
            return ContainerItemResult.of(workingCopy, drained == null ? 0 : drained.amount);
        }

        if (FluidContainerRegistry.isContainer(workingCopy)) {
            // Registry containers can only be emptied as a whole.
            FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(workingCopy);
            if (!key.matches(fluid) || fluid.amount > amount) {
                return ContainerItemResult.none();
            }

            return ContainerItemResult.of(FluidContainerRegistry.drainFluidContainer(workingCopy), fluid.amount);
        }

        return ContainerItemResult.none();
    }
}
