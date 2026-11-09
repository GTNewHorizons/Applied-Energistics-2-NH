package appeng.test.behaviors;

import static appeng.test.behaviors.TestContainers.LAVA;
import static appeng.test.behaviors.TestContainers.WATER;
import static appeng.test.behaviors.TestContainers.cells;
import static appeng.test.behaviors.TestContainers.waterBuckets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.ItemFluidContainer;

import org.junit.jupiter.api.Test;

import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.behaviors.DisplayItemConverters;
import appeng.api.stacks.GenericStack;
import appeng.util.Platform;
import codechicken.nei.item.ItemFluidDisplay;
import cpw.mods.fml.common.Loader;
import gregtech.api.util.GTUtility;

public class DisplayItemConvertersFunctionalTest {

    private static final long BEYOND_INT = 5_000_000_000L;

    @Test
    void ignoresOrdinaryItems() {
        assertNull(DisplayItemConverters.convert(null));
        assertNull(DisplayItemConverters.convert(new ItemStack(Items.stick)));
    }

    @Test
    void ignoresContainers() {
        assertNull(DisplayItemConverters.convert(waterBuckets(1)));
        assertNull(DisplayItemConverters.convert(cells(WATER, 500, 1)));
    }

    @Test
    void convertsNeiFluidDisplaysWithLongAmounts() {
        assumeTrue(Platform.isNEILoaded);
        final ItemStack display = ItemFluidDisplay.createStack(FluidRegistry.WATER, BEYOND_INT);

        assertEquals(new GenericStack(WATER, BEYOND_INT), DisplayItemConverters.convert(display));
    }

    @Test
    void convertsFluidPacketsWithLongAmounts() {
        final ItemStack packet = ItemFluidPacket.newDisplayStack(WATER.toStack());
        ItemFluidPacket.setFluidAmount(packet, BEYOND_INT);

        assertEquals(new GenericStack(WATER, BEYOND_INT), DisplayItemConverters.convert(packet));
    }

    @Test
    void convertsGregTechFluidDisplays() {
        assumeTrue(Platform.isNEILoaded && Loader.isModLoaded("gregtech"));
        final ItemStack display = GTUtility.getFluidDisplayStack(new FluidStack(FluidRegistry.LAVA, 144), true);

        assertEquals(new GenericStack(LAVA, 144), DisplayItemConverters.convert(display));
    }

    @Test
    void earlierConvertersWin() {
        final Item item = new Item();
        DisplayItemConverters.register(stack -> stack.getItem() == item ? new GenericStack(WATER, 1) : null);
        DisplayItemConverters.register(stack -> stack.getItem() == item ? new GenericStack(LAVA, 2) : null);

        assertEquals(new GenericStack(WATER, 1), DisplayItemConverters.convert(new ItemStack(item)));
    }

    @Test
    void containersNeverReachConverters() {
        final Item container = new ItemFluidContainer(0, 1000);
        DisplayItemConverters.register(stack -> stack.getItem() == container ? new GenericStack(LAVA, 1) : null);

        assertNull(DisplayItemConverters.convert(new ItemStack(container)));
    }
}
