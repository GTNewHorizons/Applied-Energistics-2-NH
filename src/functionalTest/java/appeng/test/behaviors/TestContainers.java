package appeng.test.behaviors;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.ItemFluidContainer;

import appeng.api.stacks.AEFluidKey;

final class TestContainers {

    /** Holds up to one bucket and, unlike registry containers such as buckets, can be partially filled or drained. */
    static final ItemFluidContainer CELL = new ItemFluidContainer(0, 1000);

    static final AEFluidKey WATER = AEFluidKey.of(FluidRegistry.WATER);
    static final AEFluidKey LAVA = AEFluidKey.of(FluidRegistry.LAVA);

    private TestContainers() {}

    static ItemStack emptyCells(int count) {
        return new ItemStack(CELL, count);
    }

    static ItemStack cells(AEFluidKey fluid, int amount, int count) {
        final ItemStack stack = new ItemStack(CELL, count);
        CELL.fill(stack, fluid.toStack(amount), true);
        return stack;
    }

    static ItemStack emptyBuckets(int count) {
        return new ItemStack(Items.bucket, count);
    }

    /**
     * Water buckets do not stack, but stacks of them can still be handed over and have to be handled.
     */
    static ItemStack waterBuckets(int count) {
        return new ItemStack(Items.water_bucket, count);
    }
}
