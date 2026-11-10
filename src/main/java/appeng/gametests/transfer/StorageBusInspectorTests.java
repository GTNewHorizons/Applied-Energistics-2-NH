package appeng.gametests.transfer;

import static appeng.gametests.transfer.TransferTestHelpers.TEMPLATE;
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
import appeng.api.config.AccessRestriction;
import appeng.api.config.ExtractionMode;
import appeng.api.config.IncludeExclude;
import appeng.api.config.Settings;
import appeng.api.definitions.IMaterials;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.core.AppEng;
import appeng.helpers.transfer.StorageBusInspector;
import appeng.me.storage.MEInventoryHandler;
import appeng.parts.misc.PartStorageBus;

@GameTestHolder(AppEng.MOD_ID)
public class StorageBusInspectorTests {

    private static final StorageBusInspector INSPECTOR = new StorageBusInspector();
    private static final String EXTERNAL_CHEST_LABEL = "external_chest";

    private static IMaterials cards() {
        return AEApi.instance().definitions().materials();
    }

    /** Replaces the template's storage bus, which faces the chest, with a fresh one. */
    private static PartStorageBus bus(GameTestHelper helper) {
        return place(helper, AEApi.instance().definitions().parts().storageBus(), ForgeDirection.EAST);
    }

    private static void access(PartStorageBus bus, AccessRestriction access, ExtractionMode extraction) {
        bus.getConfigManager().putSetting(Settings.ACCESS, access);
        bus.getConfigManager().putSetting(Settings.EXTRACTION_MODE, extraction);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void unfilteredBusMovesEverythingBothWays(GameTestHelper helper) {
        TransferDescription description = INSPECTOR.inspect(bus(helper));

        helper.assertTrue(description.canImport, "Default access reads");
        helper.assertTrue(description.canExport, "Default access writes");
        helper.assertTrue(imports(description, iron()), "An unfiltered bus can import anything");
        helper.assertTrue(exports(description, iron()), "An unfiltered bus can export anything");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void accessLimitsDirections(GameTestHelper helper) {
        PartStorageBus bus = bus(helper);

        access(bus, AccessRestriction.READ, ExtractionMode.STRICT);
        TransferDescription read = INSPECTOR.inspect(bus);
        helper.assertTrue(imports(read, iron()), "Read access imports");
        helper.assertFalse(exports(read, iron()), "Read access does not export");

        access(bus, AccessRestriction.WRITE, ExtractionMode.STRICT);
        TransferDescription write = INSPECTOR.inspect(bus);
        helper.assertFalse(imports(write, iron()), "Write access does not import");
        helper.assertTrue(exports(write, iron()), "Write access exports");

        access(bus, AccessRestriction.NO_ACCESS, ExtractionMode.STRICT);
        TransferDescription none = INSPECTOR.inspect(bus);
        helper.assertFalse(imports(none, iron()), "No access does not import");
        helper.assertFalse(exports(none, iron()), "No access does not export");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void looseExtractionImportsPastTheFilter(GameTestHelper helper) {
        PartStorageBus bus = bus(helper);
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, iron());

        access(bus, AccessRestriction.READ_WRITE, ExtractionMode.LOOSE);
        TransferDescription loose = INSPECTOR.inspect(bus);
        helper.assertTrue(imports(loose, gold()), "Loose extraction ignores the filter");
        helper.assertFalse(exports(loose, gold()), "Insertion still follows the filter");

        access(bus, AccessRestriction.READ_WRITE, ExtractionMode.STRICT);
        helper.assertFalse(imports(INSPECTOR.inspect(bus), gold()), "Strict extraction follows the filter");

        access(bus, AccessRestriction.READ, ExtractionMode.LOOSE);
        helper.assertFalse(imports(INSPECTOR.inspect(bus), gold()), "Read access always follows the filter");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void inverterBlacklistsTheConfig(GameTestHelper helper) {
        PartStorageBus bus = bus(helper);
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, iron());
        install(bus.getInventoryByName("upgrades"), cards().cardInverter());

        TransferDescription description = INSPECTOR.inspect(bus);

        helper.assertFalse(exports(description, iron()), "Listed stack is blocked");
        helper.assertTrue(exports(description, gold()), "Other stacks pass");
        helper.assertFalse(description.matcher.isExplicitlyListed(iron()), "A blacklist never lists");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void reportsPriorityStickyAndListing(GameTestHelper helper) {
        PartStorageBus bus = bus(helper);
        bus.setPriority(42);
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, iron());
        install(bus.getInventoryByName("upgrades"), cards().cardSticky());

        TransferDescription description = INSPECTOR.inspect(bus);

        helper.assertTrue(description.storageHandler, "Storage buses follow AE's routing");
        helper.assertEquals(42, description.priority, "Priority should be reported");
        helper.assertTrue(description.sticky, "Sticky card should be reported");
        helper.assertTrue(description.matcher.isExplicitlyListed(iron()), "Whitelisted stack is listed");
        helper.assertFalse(description.matcher.isExplicitlyListed(gold()), "Other stacks are not listed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void stickyCardIsNotDrawnAsASetting(GameTestHelper helper) {
        PartStorageBus bus = bus(helper);
        install(bus.getInventoryByName("upgrades"), cards().cardSticky());

        for (TransferDescription.SettingEntry entry : INSPECTOR.inspect(bus).settings) {
            helper.assertFalse(
                    entry.setting == Settings.STICKY_MODE,
                    "Sticky mode has no button appearance, the card already shows the sticky state");
        }
        helper.succeed();
    }

    /**
     * Sets up a sticky bus facing the template's chest and checks, once the live handler has picked the settings up,
     * which stacks the bus claims ahead of all other storage.
     */
    private static void assertStickyClaims(GameTestHelper helper, AccessRestriction access, boolean whitelistIron,
            boolean ironStored, boolean expectIron) {
        PartStorageBus bus = bus(helper);
        access(bus, access, ExtractionMode.LOOSE);
        if (whitelistIron) {
            bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, iron());
        }
        if (ironStored) {
            helper.setSlot(EXTERNAL_CHEST_LABEL, 0, new ItemStack(Items.iron_ingot));
        }
        install(bus.getInventoryByName("upgrades"), cards().cardSticky());

        helper.startSequence().thenWaitUntil("wait for the handler to pick up the sticky card", 60, () -> {
            MEInventoryHandler<?> handler = bus.getInternalHandler();
            helper.assertNotNull(handler, "Storage bus should have a handler");
            helper.assertTrue(handler.getSticky(), "Handler should become sticky");
        }).thenExecute("ask the inspector", () -> {
            TransferDescription description = INSPECTOR.inspect(bus);
            helper.assertEquals(expectIron, description.matcher.capturesAsSticky(iron()), "Sticky claim on iron");
            helper.assertFalse(description.matcher.capturesAsSticky(gold()), "Gold is neither listed nor stored");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void stickyBusClaimsWhitelistedStacks(GameTestHelper helper) {
        assertStickyClaims(helper, AccessRestriction.WRITE, true, false, true);
    }

    // AE only checks what a sticky bus holds by extracting from it, which an insert only bus never allows.
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void insertOnlyStickyBusDoesNotClaimStoredStacks(GameTestHelper helper) {
        assertStickyClaims(helper, AccessRestriction.WRITE, false, true, false);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void readWriteStickyBusClaimsStoredStacks(GameTestHelper helper) {
        assertStickyClaims(helper, AccessRestriction.READ_WRITE, false, true, true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void unfilteredEmptyStickyBusClaimsNothing(GameTestHelper helper) {
        assertStickyClaims(helper, AccessRestriction.READ_WRITE, false, false, false);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void busWithoutStickyCardClaimsNothing(GameTestHelper helper) {
        PartStorageBus bus = bus(helper);
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, iron());

        helper.assertFalse(
                INSPECTOR.inspect(bus).matcher.capturesAsSticky(iron()),
                "Only sticky storage claims stacks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void itemBusNeverMatchesFluids(GameTestHelper helper) {
        TransferDescription description = INSPECTOR.inspect(bus(helper));

        helper.assertFalse(imports(description, water()), "Fluids are a different stack type");
        helper.assertFalse(exports(description, water()), "Fluids are a different stack type");
        helper.succeed();
    }

    // The inspector replicates the handler's rules, so compare them against the real thing.
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void agreesWithTheRealStorageHandler(GameTestHelper helper) {
        PartStorageBus bus = bus(helper);
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, iron());

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the bus to find its inventory",
                        60,
                        () -> { helper.assertNotNull(bus.getInternalHandler(), "Storage bus should have a handler"); })
                .thenExecute("compare whitelist and blacklist", () -> {
                    MEInventoryHandler handler = bus.getInternalHandler();
                    for (IAEStack stack : new IAEStack[] { iron(), gold() }) {
                        helper.assertEquals(
                                handler.canAccept(stack),
                                exports(INSPECTOR.inspect(bus), stack),
                                "Whitelist should agree for " + stack);
                    }

                    install(bus.getInventoryByName("upgrades"), cards().cardInverter());
                }).thenWaitUntil("wait for the handler to pick up the inverter", 40, () -> {
                    helper.assertEquals(
                            IncludeExclude.BLACKLIST,
                            bus.getInternalHandler().getWhitelist(),
                            "Handler should become a blacklist");
                }).thenExecute("compare the blacklist", () -> {
                    MEInventoryHandler inverted = bus.getInternalHandler();
                    for (IAEStack stack : new IAEStack[] { iron(), gold() }) {
                        helper.assertEquals(
                                inverted.canAccept(stack),
                                exports(INSPECTOR.inspect(bus), stack),
                                "Blacklist should agree for " + stack);
                    }
                }).thenSucceed();
    }
}
