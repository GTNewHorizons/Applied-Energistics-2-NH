package appeng.gametests.compatibility.appliedenergistics2_ae2fc;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.parts.PartFluidAnnihilationPlane;
import com.glodblock.github.common.parts.PartFluidExportBus;
import com.glodblock.github.common.parts.PartFluidStorageBus;
import com.glodblock.github.common.tile.TileFluidInterface;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.api.AEApi;
import appeng.api.config.TransferDirection;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.networking.IGridHost;
import appeng.api.parts.IPart;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.core.AppEng;
import appeng.tile.networking.TileCableBus;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

/**
 * TEMPORARY - delete together with {@code TempAddonTransferInspectors} before the pull request.
 */
@GameTestHolder(value = AppEng.MOD_ID, requiredMods = "ae2fc")
public class TempAddonTransferInspectorTests {

    private static final String TEMPLATE = "storage_bus";
    private static final String CABLE_LABEL = "storage_bus";
    private static final IAEStack<?> IRON = AEItemStack.create(new ItemStack(Items.iron_ingot));

    private static IAEStack<?> water() {
        return AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1));
    }

    private static IAEStack<?> lava() {
        return AEFluidStack.create(new FluidStack(FluidRegistry.LAVA, 1));
    }

    @SuppressWarnings("unchecked")
    private static TransferDescription inspect(final IGridHost host) {
        final ITransferInspector<IGridHost> inspector = (ITransferInspector<IGridHost>) AEApi.instance().registries()
                .transferInspectors().getInspector(host.getClass());
        return inspector.inspect(host);
    }

    @SuppressWarnings("unchecked")
    private static <T extends IPart> T place(final GameTestHelper helper, final ItemStack stack) {
        final TileCableBus host = helper.assertTileEntityPresent(TileCableBus.class, CABLE_LABEL);
        helper.assertNotNull(host.addPart(stack, ForgeDirection.UP, null), "Part should be placed");
        return (T) host.getPart(ForgeDirection.UP);
    }

    private static boolean moves(final TransferDescription description, final IAEStack<?> stack,
            final TransferDirection direction) {
        return description.matcher.canTransfer(stack, direction);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fluidBusesInheritTheBaseInspectors(GameTestHelper helper) {
        final PartFluidExportBus bus = place(helper, new ItemStack(ItemAndBlockHolder.FLUID_EXPORT_BUS));
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, water());

        final TransferDescription description = inspect(bus);

        helper.assertTrue(moves(description, water(), TransferDirection.EXPORT), "Configured fluid should match");
        helper.assertFalse(moves(description, lava(), TransferDirection.EXPORT), "Other fluids should not match");
        helper.assertFalse(moves(description, IRON, TransferDirection.EXPORT), "Items are a different type");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fluidStorageBusUsesItsOwnStackType(GameTestHelper helper) {
        final PartFluidStorageBus bus = place(helper, new ItemStack(ItemAndBlockHolder.FLUID_STORAGE_BUS));

        final TransferDescription description = inspect(bus);

        helper.assertEquals(FLUID_STACK_TYPE, bus.getStackType(), "Bus should handle fluids");
        helper.assertTrue(moves(description, water(), TransferDirection.EXPORT), "Unfiltered bus takes any fluid");
        helper.assertFalse(moves(description, IRON, TransferDirection.EXPORT), "Items are a different type");
        helper.succeed();
    }

    @GameTest(template = "interface_network", timeoutTicks = 20)
    public static void fluidInterfaceReportsItsFluidConfig(GameTestHelper helper) {
        helper.setBlock("adjacent_chest", ItemAndBlockHolder.INTERFACE);
        final TileFluidInterface tile = helper.assertTileEntityPresent(TileFluidInterface.class, "adjacent_chest");
        tile.setConfig(0, AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1000)));

        final TransferDescription description = inspect(tile);

        helper.assertTrue(moves(description, water(), TransferDirection.EXPORT), "Stocked fluid leaves the network");
        helper.assertFalse(moves(description, water(), TransferDirection.IMPORT), "Stocked fluid stays");
        helper.assertTrue(moves(description, lava(), TransferDirection.IMPORT), "Other fluids return to the network");
        helper.assertTrue(description.canExport, "Fluid config enables exporting");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fluidAnnihilationPlaneImportsFluids(GameTestHelper helper) {
        final PartFluidAnnihilationPlane plane = place(
                helper,
                new ItemStack(ItemAndBlockHolder.FLUID_ANNIHILATION_PLANE));

        final TransferDescription description = inspect(plane);

        helper.assertTrue(moves(description, water(), TransferDirection.IMPORT), "Any fluid can be drained");
        helper.assertFalse(moves(description, IRON, TransferDirection.IMPORT), "It does not produce items");
        helper.succeed();
    }
}
