package appeng.gametests.transfer;

import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.google.common.base.Optional;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;

import appeng.api.config.TransferDirection;
import appeng.api.definitions.IItemDefinition;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.parts.IPart;
import appeng.api.storage.data.IAEStack;
import appeng.tile.networking.TileCableBus;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

final class TransferTestHelpers {

    static final String TEMPLATE = "storage_bus";
    private static final String CABLE_LABEL = "storage_bus";

    private TransferTestHelpers() {}

    /**
     * Places a part on a free side of the template's cable, the part does not need to be powered to be inspected.
     */
    @SuppressWarnings("unchecked")
    static <T extends IPart> T place(final GameTestHelper helper, final IItemDefinition definition,
            final ForgeDirection side) {
        final TileCableBus host = helper.assertTileEntityPresent(TileCableBus.class, CABLE_LABEL);
        host.removePart(side, false);
        helper.assertNotNull(host.addPart(stack(definition), side, null), "Part should be placed");
        return (T) host.getPart(side);
    }

    static ItemStack stack(final IItemDefinition definition) {
        final Optional<ItemStack> stack = definition.maybeStack(1);
        return stack.get();
    }

    static void install(final IInventory upgrades, final IItemDefinition... cards) {
        for (int slot = 0; slot < cards.length; slot++) {
            upgrades.setInventorySlotContents(slot, stack(cards[slot]));
        }
    }

    static IAEStack<?> iron() {
        return AEItemStack.create(new ItemStack(Items.iron_ingot));
    }

    static IAEStack<?> gold() {
        return AEItemStack.create(new ItemStack(Items.gold_ingot));
    }

    static IAEStack<?> pickaxe() {
        return AEItemStack.create(new ItemStack(Items.iron_pickaxe));
    }

    static IAEStack<?> damagedPickaxe() {
        return AEItemStack.create(new ItemStack(Items.iron_pickaxe, 1, 100));
    }

    static IAEStack<?> water() {
        return AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1));
    }

    static boolean imports(final TransferDescription description, final IAEStack<?> stack) {
        return description.matcher.canTransfer(stack, TransferDirection.IMPORT);
    }

    static boolean exports(final TransferDescription description, final IAEStack<?> stack) {
        return description.matcher.canTransfer(stack, TransferDirection.EXPORT);
    }
}
