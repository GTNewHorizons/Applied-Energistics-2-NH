package appeng.gametests.transfer;

import static appeng.gametests.transfer.TransferTestHelpers.TEMPLATE;
import static appeng.gametests.transfer.TransferTestHelpers.damagedPickaxe;
import static appeng.gametests.transfer.TransferTestHelpers.exports;
import static appeng.gametests.transfer.TransferTestHelpers.gold;
import static appeng.gametests.transfer.TransferTestHelpers.imports;
import static appeng.gametests.transfer.TransferTestHelpers.install;
import static appeng.gametests.transfer.TransferTestHelpers.iron;
import static appeng.gametests.transfer.TransferTestHelpers.pickaxe;
import static appeng.gametests.transfer.TransferTestHelpers.place;
import static appeng.gametests.transfer.TransferTestHelpers.water;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.definitions.IMaterials;
import appeng.api.definitions.IParts;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.storage.StorageName;
import appeng.core.AppEng;
import appeng.helpers.transfer.ExportBusInspector;
import appeng.helpers.transfer.ImportBusInspector;
import appeng.parts.automation.PartExportBus;
import appeng.parts.automation.PartImportBus;

@GameTestHolder(AppEng.MOD_ID)
public class BusInspectorTests {

    private static final ImportBusInspector IMPORT = new ImportBusInspector();
    private static final ExportBusInspector EXPORT = new ExportBusInspector();

    private static IParts parts() {
        return AEApi.instance().definitions().parts();
    }

    private static IMaterials cards() {
        return AEApi.instance().definitions().materials();
    }

    private static PartImportBus importBus(GameTestHelper helper) {
        return place(helper, parts().importBus(), ForgeDirection.UP);
    }

    private static PartExportBus exportBus(GameTestHelper helper) {
        return place(helper, parts().exportBus(), ForgeDirection.UP);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void emptyImportBusTakesEverythingAndNeverExports(GameTestHelper helper) {
        TransferDescription description = IMPORT.inspect(importBus(helper));

        helper.assertTrue(description.canImport, "Import bus should import");
        helper.assertFalse(description.canExport, "Import bus should not export");
        helper.assertTrue(imports(description, iron()), "Empty import bus should import anything");
        helper.assertFalse(exports(description, iron()), "Import bus should never export");
        helper.assertFalse(imports(description, water()), "Item bus should ignore fluids");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void emptyExportBusMovesNothing(GameTestHelper helper) {
        TransferDescription description = EXPORT.inspect(exportBus(helper));

        helper.assertTrue(description.canExport, "Export bus should export");
        helper.assertFalse(exports(description, iron()), "Empty export bus should export nothing");
        helper.assertFalse(imports(description, iron()), "Export bus should never import");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void configuredBusesMatchOnlyTheirFilter(GameTestHelper helper) {
        PartImportBus bus = importBus(helper);
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, iron());

        TransferDescription description = IMPORT.inspect(bus);

        helper.assertTrue(imports(description, iron()), "Configured stack should match");
        helper.assertFalse(imports(description, gold()), "Other stacks should not match");
        helper.assertEquals(1, description.config.size(), "The config slot should be reported");
        helper.assertFalse(description.config.get(0).fuzzy, "No fuzzy card is installed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void slotsBeyondTheCapacityAreIgnored(GameTestHelper helper) {
        PartExportBus bus = exportBus(helper);
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(5, iron());

        helper.assertFalse(exports(EXPORT.inspect(bus), iron()), "Slot 5 needs capacity cards");

        install(bus.getInventoryByName("upgrades"), cards().cardCapacity(), cards().cardCapacity());
        TransferDescription description = EXPORT.inspect(bus);

        helper.assertTrue(exports(description, iron()), "Two capacity cards should open slot 5");
        helper.assertEquals(1, description.upgrades.size(), "Identical cards should merge");
        helper.assertEquals(2, description.upgrades.get(0).stackSize, "Merged cards should be counted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void importBusIgnoresConfigOutsideItsSlots(GameTestHelper helper) {
        PartImportBus bus = importBus(helper);
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(5, iron());

        helper.assertTrue(
                imports(IMPORT.inspect(bus), gold()),
                "Config in an unusable slot leaves the bus unconfigured");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fuzzyCardUsesFuzzyMode(GameTestHelper helper) {
        PartExportBus bus = exportBus(helper);
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, pickaxe());
        helper.assertFalse(exports(EXPORT.inspect(bus), damagedPickaxe()), "Precise match should see damage");

        install(bus.getInventoryByName("upgrades"), cards().cardFuzzy());
        bus.getConfigManager().putSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        TransferDescription description = EXPORT.inspect(bus);

        helper.assertTrue(exports(description, damagedPickaxe()), "Fuzzy match should ignore damage");
        helper.assertTrue(description.config.get(0).fuzzy, "Config entry should be marked fuzzy");
        helper.assertTrue(hasSetting(description, Settings.FUZZY_MODE), "Fuzzy mode should be reported");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void oreFilterReplacesConfig(GameTestHelper helper) {
        PartExportBus bus = exportBus(helper);
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, gold());
        install(bus.getInventoryByName("upgrades"), cards().cardOreFilter());
        bus.setFilter("ingotIron");

        TransferDescription description = EXPORT.inspect(bus);

        helper.assertTrue(exports(description, iron()), "Ore filter should match iron");
        helper.assertFalse(exports(description, gold()), "Config is ignored with an ore filter");
        helper.assertEquals("ingotIron", description.oreFilter, "Ore filter should be reported");
        helper.assertTrue(description.config.isEmpty(), "Unused config should not be reported");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void importBusOreFilterEdgeCases(GameTestHelper helper) {
        PartImportBus bus = importBus(helper);
        install(bus.getInventoryByName("upgrades"), cards().cardOreFilter());

        bus.setFilter("");
        helper.assertTrue(imports(IMPORT.inspect(bus), gold()), "An empty ore filter imports everything");

        bus.setFilter("[");
        helper.assertFalse(imports(IMPORT.inspect(bus), gold()), "An invalid ore filter imports nothing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void craftingCardIsReported(GameTestHelper helper) {
        PartExportBus bus = exportBus(helper);
        helper.assertFalse(EXPORT.inspect(bus).hasCraftingCard, "No crafting card yet");

        install(bus.getInventoryByName("upgrades"), cards().cardCrafting());
        TransferDescription description = EXPORT.inspect(bus);

        helper.assertTrue(description.hasCraftingCard, "Crafting card should be reported");
        helper.assertTrue(hasSetting(description, Settings.CRAFT_ONLY), "Craft-only mode should be reported");
        helper.assertFalse(IMPORT.inspect(importBus(helper)).hasCraftingCard, "Import buses take no crafting card");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void redstoneModeOnlyShowsWithItsCard(GameTestHelper helper) {
        PartImportBus bus = importBus(helper);
        helper.assertFalse(hasSetting(IMPORT.inspect(bus), Settings.REDSTONE_CONTROLLED), "No redstone card yet");

        install(bus.getInventoryByName("upgrades"), cards().cardRedstone());

        helper.assertTrue(hasSetting(IMPORT.inspect(bus), Settings.REDSTONE_CONTROLLED), "Card makes it relevant");
        helper.succeed();
    }

    private static boolean hasSetting(TransferDescription description, Settings setting) {
        return description.settings.stream().anyMatch(entry -> entry.setting == setting);
    }
}
