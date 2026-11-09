package appeng.test.helpers.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;

import appeng.api.config.TransferDirection;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.helpers.transfer.SuperReplenisherInspector;
import appeng.tile.inventory.IAEStackInventory;
import appeng.tile.misc.TileSuperMEReplenisher;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

public class SuperReplenisherInspectorFunctionalTest {

    private static final IAEStack<?> IRON = AEItemStack.create(new ItemStack(Items.iron_ingot));
    private static final IAEStack<?> GOLD = AEItemStack.create(new ItemStack(Items.gold_ingot));
    private static final IAEStack<?> WATER = AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1));

    /** The tile refuses config changes without a grid, so load the slots the way a saved world does. */
    private static void configure(final TileSuperMEReplenisher tile, final IAEStack<?>... stacks) {
        final IAEStackInventory saved = new IAEStackInventory(null, 27);
        for (int slot = 0; slot < stacks.length; slot++) {
            saved.putAEStackInSlot(slot, stacks[slot]);
        }

        final NBTTagCompound data = new NBTTagCompound();
        saved.writeToNBT(data, "config");
        tile.getAEInventoryByName(StorageName.CONFIG).readFromNBT(data, "config");
    }

    private static boolean exports(final TransferDescription description, final IAEStack<?> stack) {
        return description.matcher.canTransfer(stack, TransferDirection.EXPORT);
    }

    @Test
    public void emptyReplenisherPullsNothing() {
        final TransferDescription description = new SuperReplenisherInspector().inspect(new TileSuperMEReplenisher());

        assertFalse(exports(description, IRON));
        assertTrue(description.config.isEmpty());
    }

    @Test
    public void pullsOnlyItsConfiguredStacksOfAnyType() {
        final TileSuperMEReplenisher tile = new TileSuperMEReplenisher();
        configure(tile, IRON, WATER);

        final TransferDescription description = new SuperReplenisherInspector().inspect(tile);

        assertTrue(exports(description, IRON));
        assertTrue(exports(description, WATER));
        assertFalse(exports(description, GOLD));
        assertEquals(2, description.config.size());
    }

    @Test
    public void neverImports() {
        final TileSuperMEReplenisher tile = new TileSuperMEReplenisher();
        configure(tile, IRON, WATER);

        final TransferDescription description = new SuperReplenisherInspector().inspect(tile);

        assertFalse(description.matcher.canTransfer(IRON, TransferDirection.IMPORT));
        assertFalse(description.canImport);
    }
}
