package appeng.gametests.transfer;

import static appeng.gametests.transfer.TransferTestHelpers.damagedPickaxe;
import static appeng.gametests.transfer.TransferTestHelpers.exports;
import static appeng.gametests.transfer.TransferTestHelpers.gold;
import static appeng.gametests.transfer.TransferTestHelpers.imports;
import static appeng.gametests.transfer.TransferTestHelpers.install;
import static appeng.gametests.transfer.TransferTestHelpers.iron;
import static appeng.gametests.transfer.TransferTestHelpers.place;
import static appeng.gametests.transfer.TransferTestHelpers.water;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.definitions.IMaterials;
import appeng.api.features.transfer.TransferDescription;
import appeng.core.AppEng;
import appeng.helpers.DualityInterface;
import appeng.helpers.transfer.InterfaceInspector;
import appeng.parts.misc.PartInterface;
import appeng.parts.p2p.PartP2PInterface;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.tile.misc.TileInterface;

@GameTestHolder(AppEng.MOD_ID)
public class InterfaceInspectorTests {

    private static final String TEMPLATE = "interface_network";
    private static final String BLOCK_INTERFACE_LABEL = "block_interface";
    private static final InterfaceInspector INSPECTOR = new InterfaceInspector();

    private static IMaterials cards() {
        return AEApi.instance().definitions().materials();
    }

    private static TileInterface blockInterface(GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileInterface.class, BLOCK_INTERFACE_LABEL);
    }

    private static void configure(DualityInterface duality, int slot, ItemStack stack) {
        AppEngInternalAEInventory config = duality.getConfig();
        config.setInventorySlotContents(slot, stack);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void emptyInterfaceIsNotListed(GameTestHelper helper) {
        helper.assertNull(INSPECTOR.inspect(blockInterface(helper)), "Without stocking it only works through patterns");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void onlyStockedStacksMoveAndOnlyOutOfTheNetwork(GameTestHelper helper) {
        TileInterface tile = blockInterface(helper);
        configure(tile.getInterfaceDuality(), 0, new ItemStack(Items.iron_ingot));

        TransferDescription description = INSPECTOR.inspect(tile);

        helper.assertTrue(exports(description, iron()), "Stocked stack leaves the network");
        helper.assertFalse(imports(description, iron()), "Stocked stack stays in the interface");
        helper.assertFalse(exports(description, gold()), "Other stacks are not stocked");
        helper.assertFalse(imports(description, gold()), "Interfaces are listed as export only");
        helper.assertFalse(exports(description, water()), "Item interfaces ignore fluids");
        helper.assertTrue(description.canExport, "Config enables exporting");
        helper.assertFalse(description.canImport, "Interfaces are listed as export only");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fuzzyCardsFillFromTheLastSlot(GameTestHelper helper) {
        helper.assertFalse(InterfaceInspector.isFuzzySlot(1, 5), "One card stops before slot 5");
        helper.assertTrue(InterfaceInspector.isFuzzySlot(1, 6), "One card covers slots 6 to 8");
        helper.assertFalse(InterfaceInspector.isFuzzySlot(2, 2), "Two cards stop before slot 2");
        helper.assertTrue(InterfaceInspector.isFuzzySlot(2, 3), "Two cards cover slots 3 to 8");
        helper.assertTrue(InterfaceInspector.isFuzzySlot(3, 0), "Three cards cover every slot");
        helper.assertFalse(InterfaceInspector.isFuzzySlot(0, 8), "No card covers nothing");

        TileInterface tile = blockInterface(helper);
        DualityInterface duality = tile.getInterfaceDuality();
        configure(duality, 0, new ItemStack(Items.iron_pickaxe));
        helper.assertFalse(exports(INSPECTOR.inspect(tile), damagedPickaxe()), "Slot 0 is precise without cards");

        install(duality.getInventoryByName("upgrades"), cards().cardFuzzy(), cards().cardFuzzy(), cards().cardFuzzy());
        duality.getConfigManager().putSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        TransferDescription description = INSPECTOR.inspect(tile);

        helper.assertTrue(exports(description, damagedPickaxe()), "Three cards make slot 0 fuzzy");
        helper.assertTrue(description.config.get(0).fuzzy, "The fuzzy slot should be marked");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void reportsCraftingCardPriorityAndIgnoresStorageRouting(GameTestHelper helper) {
        TileInterface tile = blockInterface(helper);
        DualityInterface duality = tile.getInterfaceDuality();
        configure(duality, 0, new ItemStack(Items.iron_ingot));
        duality.setPriority(7);
        helper.assertFalse(INSPECTOR.inspect(tile).hasCraftingCard, "No crafting card yet");

        install(duality.getInventoryByName("upgrades"), cards().cardCrafting());
        TransferDescription description = INSPECTOR.inspect(tile);

        helper.assertTrue(description.hasCraftingCard, "Crafting card should be reported");
        helper.assertEquals(7, description.priority, "Pattern priority should be reported");
        helper.assertFalse(description.storageHandler, "Interface priority does not route storage");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void partInterfaceIsInspected(GameTestHelper helper) {
        PartInterface part = placeOnInterfaceCable(helper, AEApi.instance().definitions().parts().iface());

        configure(part.getInterfaceDuality(), 0, new ItemStack(Items.iron_ingot));

        helper.assertTrue(exports(INSPECTOR.inspect(part), iron()), "Part interfaces work like blocks");
        helper.succeed();
    }

    @GameTest(template = "storage_bus", timeoutTicks = 20)
    public static void onlyTheInputSideOfATunnelIsListed(GameTestHelper helper) {
        PartP2PInterface tunnel = place(
                helper,
                AEApi.instance().definitions().parts().p2PTunnelMEInterface(),
                ForgeDirection.UP);
        configure(tunnel.getInterfaceDuality(), 0, new ItemStack(Items.iron_ingot));

        helper.assertNotNull(INSPECTOR.inspect(tunnel), "The input side should be listed");

        tunnel.output = true;

        helper.assertNull(INSPECTOR.inspect(tunnel), "The output side mirrors the input");
        helper.succeed();
    }

    private static PartInterface placeOnInterfaceCable(GameTestHelper helper,
            appeng.api.definitions.IItemDefinition definition) {
        appeng.tile.networking.TileCableBus host = helper
                .assertTileEntityPresent(appeng.tile.networking.TileCableBus.class, "part_interface_host");
        if (host.getPart(ForgeDirection.UP) == null) {
            helper.assertNotNull(
                    host.addPart(TransferTestHelpers.stack(definition), ForgeDirection.UP, null),
                    "Interface part should be placed");
        }
        return (PartInterface) host.getPart(ForgeDirection.UP);
    }
}
