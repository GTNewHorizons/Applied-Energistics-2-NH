package appeng.gametests.transfer;

import static appeng.gametests.AEGameTestHelpers.assertActive;
import static appeng.gametests.AEGameTestHelpers.cell1k;
import static appeng.gametests.transfer.TransferTestHelpers.TEMPLATE;
import static appeng.gametests.transfer.TransferTestHelpers.iron;
import static appeng.gametests.transfer.TransferTestHelpers.place;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.api.AEApi;
import appeng.api.config.Settings;
import appeng.api.config.TransferDirection;
import appeng.api.config.YesNo;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerTransferTerminal;
import appeng.core.AppEng;
import appeng.helpers.transfer.TransferTypeFilter;
import appeng.parts.automation.PartExportBus;
import appeng.parts.reporting.PartTransferTerminal;
import appeng.tile.networking.TileCableBus;

@GameTestHolder(AppEng.MOD_ID)
public class TransferTerminalPartTests {

    private static PartTransferTerminal terminal(GameTestHelper helper) {
        return place(helper, AEApi.instance().definitions().parts().transferTerminal(), ForgeDirection.UP);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void terminalIsCraftedOnACraftingTable(GameTestHelper helper) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);
        grid.setInventorySlotContents(
                0,
                AEApi.instance().definitions().parts().interfaceTerminal().maybeStack(1).get());
        grid.setInventorySlotContents(4, AEApi.instance().definitions().parts().storageBus().maybeStack(1).get());
        grid.setInventorySlotContents(
                8,
                AEApi.instance().definitions().materials().logicProcessor().maybeStack(1).get());

        ItemStack crafted = CraftingManager.getInstance().findMatchingRecipe(grid, helper.getWorld());

        helper.assertNotNull(crafted, "Interface Terminal, Storage Bus and Logic Processor should craft something");
        helper.assertTrue(
                ItemStack.areItemStacksEqual(
                        AEApi.instance().definitions().parts().transferTerminal().maybeStack(1).get(),
                        crafted),
                "They should craft one Transfer Terminal");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aMissingSearchTextFromTheClientSearchesNothing(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal to join the network",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("receive no search text and filter the rows", () -> {
                    ContainerTransferTerminal container = new ContainerTransferTerminal(
                            helper.spawnFakePlayer("transfer_terminal").inventory,
                            terminal);
                    container.detectAndSendChanges();
                    int rows = container.getFilteredRows().size();
                    helper.assertTrue(rows > 0, "The template has components to list");

                    // A client can send the search text as absent, which reads back as null.
                    container.searchTextSync.set(null);
                    terminal.getConfigManager().putSetting(Settings.TRANSFER_DIRECTION, TransferDirection.IMPORT);
                    container.detectAndSendChanges();
                    terminal.getConfigManager().putSetting(Settings.TRANSFER_DIRECTION, TransferDirection.BOTH);
                    container.detectAndSendChanges();

                    helper.assertEquals(rows, container.getFilteredRows().size(), "No search text keeps every row");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void savedSearchTextIsCapped(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < PartTransferTerminal.MAX_SEARCH_LENGTH * 4; i++) {
            text.append('a');
        }

        terminal.setSearchText(text.toString());

        helper.assertEquals(
                PartTransferTerminal.MAX_SEARCH_LENGTH,
                terminal.getSearchText().length(),
                "A search text sent by the client is cut to the longest the terminal keeps");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void filtersSurviveSavingAndLoading(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_DIRECTION, TransferDirection.EXPORT);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_CRAFTING_CARD_ONLY, YesNo.YES);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_HIDE_UNFILTERED, YesNo.YES);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_NETWORK_ONLY, YesNo.YES);
        terminal.getQueryInventory().putAEStackInSlot(0, iron());
        terminal.setSearchText("ingot");
        terminal.setTypeShown("appliedenergistics2:tile.BlockDrive@0", true);
        terminal.setTypeShown("appliedenergistics2:item.ItemMultiPart@220", false);

        NBTTagCompound data = new NBTTagCompound();
        terminal.writeToNBT(data);
        PartTransferTerminal loaded = new PartTransferTerminal(
                AEApi.instance().definitions().parts().transferTerminal().maybeStack(1).get());
        loaded.readFromNBT(data);

        helper.assertEquals(
                TransferDirection.EXPORT,
                loaded.getConfigManager().getSetting(Settings.TRANSFER_DIRECTION),
                "Direction should be kept");
        helper.assertEquals(
                YesNo.YES,
                loaded.getConfigManager().getSetting(Settings.TRANSFER_CRAFTING_CARD_ONLY),
                "Crafting card filter should be kept");
        helper.assertEquals(
                YesNo.YES,
                loaded.getConfigManager().getSetting(Settings.TRANSFER_HIDE_UNFILTERED),
                "Hide unfiltered should be kept");
        helper.assertEquals(
                YesNo.YES,
                loaded.getConfigManager().getSetting(Settings.TRANSFER_NETWORK_ONLY),
                "Network only should be kept");
        helper.assertTrue(
                iron().isSameType((Object) loaded.getQueryInventory().getAEStackInSlot(0)),
                "Searched stack should be kept");
        helper.assertEquals("ingot", loaded.getSearchText(), "Search text should be kept");
        helper.assertEquals(
                Boolean.TRUE,
                loaded.getTypeOverrides().get("appliedenergistics2:tile.BlockDrive@0"),
                "A kind turned on should be kept");
        helper.assertEquals(
                Boolean.FALSE,
                loaded.getTypeOverrides().get("appliedenergistics2:item.ItemMultiPart@220"),
                "A kind turned off should be kept");
        helper.assertEquals(2, loaded.getTypeOverrides().size(), "Nothing else is turned on or off");
        helper.succeed();
    }

    /**
     * @return the drop-down entry of the only kind of component that starts hidden, the drive
     */
    private static TransferTypeFilter.Entry hiddenKind(GameTestHelper helper, ContainerTransferTerminal container) {
        TransferTypeFilter.Entry hidden = null;
        for (TransferTypeFilter.Entry entry : container.typesSync.get()) {
            if (!entry.shown) {
                helper.assertNull(hidden, "Only the drive starts hidden");
                hidden = entry;
            }
        }
        helper.assertNotNull(hidden, "The drive should be in the drop-down");
        return hidden;
    }

    private static boolean listsKind(ContainerTransferTerminal container, String key) {
        for (TransferComponentInfo row : container.getFilteredRows()) {
            if (key.equals(TransferTypeFilter.keyOf(row.selfRep))) {
                return true;
            }
        }
        return false;
    }

    private static String kindOfRowOnSide(GameTestHelper helper, ContainerTransferTerminal container,
            ForgeDirection side) {
        for (TransferComponentInfo row : container.getFilteredRows()) {
            if (row.side == (byte) side.ordinal()) {
                return TransferTypeFilter.keyOf(row.selfRep);
            }
        }
        throw new AssertionError("No row for the part on side " + side);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void drivesAreHiddenUntilTheirKindIsTurnedOn(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal to join the network",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("turn kinds of component on and off", () -> {
                    ContainerTransferTerminal container = new ContainerTransferTerminal(
                            helper.spawnFakePlayer("transfer_terminal").inventory,
                            terminal);
                    container.detectAndSendChanges();
                    String drive = hiddenKind(helper, container).key;
                    String bus = kindOfRowOnSide(helper, container, ForgeDirection.EAST);
                    helper.assertFalse(listsKind(container, drive), "Drives start hidden");

                    terminal.setTypeShown(drive, true);
                    container.detectAndSendChanges();
                    helper.assertTrue(listsKind(container, drive), "A drive is listed once turned on");

                    terminal.setTypeShown(bus, false);
                    container.detectAndSendChanges();
                    helper.assertFalse(listsKind(container, bus), "A kind turned off is not listed");
                    helper.assertTrue(listsKind(container, drive), "Other kinds stay listed");

                    terminal.resetTypeOverrides();
                    container.detectAndSendChanges();
                    helper.assertFalse(listsKind(container, drive), "Resetting hides drives again");
                    helper.assertTrue(listsKind(container, bus), "Resetting shows every other kind again");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void simulatingListsEveryKindOfComponent(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);
        helper.setSlot("drive", 0, cell1k());

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal to join the network",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("simulate with the storage bus turned off", () -> {
                    ContainerTransferTerminal container = new ContainerTransferTerminal(
                            helper.spawnFakePlayer("transfer_terminal").inventory,
                            terminal);
                    container.detectAndSendChanges();
                    String drive = hiddenKind(helper, container).key;
                    String bus = kindOfRowOnSide(helper, container, ForgeDirection.EAST);
                    terminal.setTypeShown(bus, false);

                    terminal.getQueryInventory().putAEStackInSlot(0, iron());
                    terminal.getConfigManager().putSetting(Settings.TRANSFER_SIMULATE, YesNo.YES);
                    container.detectAndSendChanges();

                    helper.assertTrue(container.simulatingSync.get(), "The insert should be simulated");
                    helper.assertTrue(listsKind(container, drive), "The hidden drive is listed while simulating");
                    helper.assertTrue(listsKind(container, bus), "The turned off bus is listed while simulating");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void containerStartsFromTheSavedFilters(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_DIRECTION, TransferDirection.IMPORT);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_NETWORK_ONLY, YesNo.YES);
        terminal.getQueryInventory().putAEStackInSlot(0, iron());
        terminal.setSearchText("ingot");

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal to join the network",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("open the container", () -> {
                    FakePlayer player = helper.spawnFakePlayer("transfer_terminal");
                    ContainerTransferTerminal container = new ContainerTransferTerminal(player.inventory, terminal);
                    container.detectAndSendChanges();

                    helper.assertEquals(TransferDirection.IMPORT, container.getMode(), "Direction is restored");
                    helper.assertTrue(
                            ContainerTransferTerminal.isYes(container.networkOnlySync),
                            "Network only is restored");
                    helper.assertNotNull(container.getQuery(), "Searched stack is restored");
                    helper.assertEquals("ingot", container.savedSearchSync.get(), "Search text is restored");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void storageChangesRequestARescan(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);
        long before = terminal.getChangeCount();

        terminal.onCellArrayChanged(new MENetworkCellArrayUpdate());

        helper.assertTrue(terminal.getChangeCount() > before, "A storage change should request a rescan");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void networkChangesRescanAtMostEveryFewTicks(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);
        ContainerTransferTerminal[] container = new ContainerTransferTerminal[1];
        int[] rowsBefore = new int[1];

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal to join the network",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("open the terminal", () -> {
                    container[0] = new ContainerTransferTerminal(
                            helper.spawnFakePlayer("transfer_terminal").inventory,
                            terminal);
                    container[0].detectAndSendChanges();
                    rowsBefore[0] = container[0].getFilteredRows().size();
                    PartExportBus bus = place(helper, AEApi.instance().definitions().parts().exportBus(), NORTH);
                    bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, iron());
                })
                .thenWaitUntil(
                        "wait for the export bus to join the network",
                        60,
                        () -> assertActive(
                                helper,
                                (PartExportBus) helper
                                        .assertTileEntityPresent(TileCableBus.class, TransferTestHelpers.CABLE_LABEL)
                                        .getPart(NORTH),
                                "Export bus should become active"))
                .thenExecute("report the change right after a scan", () -> {
                    container[0].detectAndSendChanges();
                    terminal.onCellArrayChanged(new MENetworkCellArrayUpdate());
                    container[0].detectAndSendChanges();
                    helper.assertEquals(
                            rowsBefore[0],
                            container[0].getFilteredRows().size(),
                            "A scan right after the last one waits");

                    for (int tick = 0; tick < 10; tick++) {
                        container[0].detectAndSendChanges();
                    }
                    helper.assertEquals(
                            rowsBefore[0] + 1,
                            container[0].getFilteredRows().size(),
                            "The waiting scan lists the export bus");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void togglingNetworkOnlyRescansAtMostEveryFewTicks(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);
        ContainerTransferTerminal[] container = new ContainerTransferTerminal[1];
        int[] rowsBefore = new int[1];

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal to join the network",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("open the terminal, then add a bus it has not seen", () -> {
                    container[0] = new ContainerTransferTerminal(
                            helper.spawnFakePlayer("transfer_terminal").inventory,
                            terminal);
                    container[0].detectAndSendChanges();
                    rowsBefore[0] = container[0].getFilteredRows().size();
                    PartExportBus bus = place(helper, AEApi.instance().definitions().parts().exportBus(), NORTH);
                    bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, iron());
                })
                .thenWaitUntil(
                        "wait for the export bus to join the network",
                        60,
                        () -> assertActive(
                                helper,
                                (PartExportBus) helper
                                        .assertTileEntityPresent(TileCableBus.class, TransferTestHelpers.CABLE_LABEL)
                                        .getPart(NORTH),
                                "Export bus should become active"))
                .thenExecute("toggle network only on every tick", () -> {
                    for (int tick = 0; tick < 5; tick++) {
                        // What another screen on the terminal changes reaches this one through the terminal.
                        terminal.getConfigManager()
                                .putSetting(Settings.TRANSFER_NETWORK_ONLY, tick % 2 == 0 ? YesNo.YES : YesNo.NO);
                        container[0].detectAndSendChanges();
                    }
                    helper.assertEquals(
                            rowsBefore[0],
                            container[0].getFilteredRows().size(),
                            "Toggling right after a scan does not scan again");

                    for (int tick = 0; tick < 10; tick++) {
                        container[0].detectAndSendChanges();
                    }
                    helper.assertEquals(
                            rowsBefore[0] + 1,
                            container[0].getFilteredRows().size(),
                            "The waiting scan lists the export bus");
                }).thenSucceed();
    }

    /**
     * @return the configured stacks of the row for the part on the given side of the template's cable
     */
    private static List<IAEStack<?>> configOfRowOnSide(GameTestHelper helper, ContainerTransferTerminal container,
            ForgeDirection side) {
        for (TransferComponentInfo row : container.getFilteredRows()) {
            if (row.side == (byte) side.ordinal()) {
                List<IAEStack<?>> stacks = new ArrayList<>();
                for (TransferDescription.ConfigEntry entry : row.description.config) {
                    stacks.add(entry.stack);
                }
                return stacks;
            }
        }
        throw new AssertionError("No row for the part on side " + side);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void filterEditsShowUpWithoutANetworkEvent(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);
        PartExportBus bus = place(helper, AEApi.instance().definitions().parts().exportBus(), NORTH);
        ContainerTransferTerminal[] container = new ContainerTransferTerminal[1];

        helper.startSequence().thenWaitUntil("wait for the terminal and the bus to join the network", 60, () -> {
            assertActive(helper, terminal, "Terminal should become active");
            assertActive(helper, bus, "Export bus should become active");
        }).thenExecute("edit the bus filter while the terminal is open", () -> {
            container[0] = new ContainerTransferTerminal(
                    helper.spawnFakePlayer("transfer_terminal").inventory,
                    terminal);
            container[0].detectAndSendChanges();
            helper.assertTrue(
                    configOfRowOnSide(helper, container[0], NORTH).isEmpty(),
                    "The bus starts without a filter");

            // Editing a filter posts no network event and adds no machine.
            bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, iron());
            for (int tick = 0; tick < 50; tick++) {
                container[0].detectAndSendChanges();
            }
            helper.assertTrue(
                    configOfRowOnSide(helper, container[0], NORTH).isEmpty(),
                    "Rows are not rebuilt every tick");

            for (int tick = 0; tick < 60; tick++) {
                container[0].detectAndSendChanges();
            }
            List<IAEStack<?>> config = configOfRowOnSide(helper, container[0], NORTH);
            helper.assertEquals(1, config.size(), "The periodic rescan picks up the new filter");
            helper.assertTrue(iron().isSameType((Object) config.get(0)), "The filter names iron");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void goingOfflineClearsTheSimulationNotes(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_SIMULATE, YesNo.YES);
        IAEStack<?> lotsOfIron = iron();
        lotsOfIron.setStackSize(1_000_000);
        terminal.getQueryInventory().putAEStackInSlot(0, lotsOfIron);
        ContainerTransferTerminal[] container = new ContainerTransferTerminal[1];

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal to join the network",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("simulate an insert that does not fit", () -> {
                    container[0] = new ContainerTransferTerminal(
                            helper.spawnFakePlayer("transfer_terminal").inventory,
                            terminal);
                    container[0].detectAndSendChanges();
                    helper.assertTrue(container[0].simulatingSync.get(), "The insert should be simulated");
                    helper.assertTrue(container[0].leftoverSync.get() > 0, "A million iron cannot all fit");
                    helper.destroyBlock("controller");
                })
                .thenWaitUntil(
                        "wait for the terminal to lose power",
                        60,
                        () -> helper.assertFalse(terminal.getProxy().isActive(), "Terminal should go offline"))
                .thenExecute("tick the screen while offline", () -> {
                    container[0].detectAndSendChanges();
                    helper.assertFalse(container[0].simulatingSync.get(), "No simulation is shown while offline");
                    helper.assertEquals(0L, container[0].leftoverSync.get(), "The leftover note is cleared");
                    helper.assertFalse(container[0].deniedSync.get(), "The denied note is cleared");
                    helper.assertEquals(0, container[0].stickyCountSync.get(), "The sticky separator is cleared");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void simulatingNeedsASearchedStack(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_DIRECTION, TransferDirection.IMPORT);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_SIMULATE, YesNo.YES);
        terminal.getQueryInventory().putAEStackInSlot(0, iron());

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal to join the network",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("empty the search slot while simulating", () -> {
                    ContainerTransferTerminal container = new ContainerTransferTerminal(
                            helper.spawnFakePlayer("transfer_terminal").inventory,
                            terminal);
                    container.detectAndSendChanges();
                    helper.assertTrue(container.simulatingSync.get(), "A searched stack is simulated");
                    helper.assertEquals(TransferDirection.EXPORT, container.getMode(), "Simulating forces export");

                    terminal.getQueryInventory().putAEStackInSlot(0, null);
                    container.detectAndSendChanges();

                    helper.assertEquals(YesNo.NO, container.simulateSync.get(), "Simulate turns off without a stack");
                    helper.assertEquals(
                            YesNo.NO,
                            terminal.getConfigManager().getSetting(Settings.TRANSFER_SIMULATE),
                            "The terminal remembers it is off");
                    helper.assertFalse(container.simulatingSync.get(), "Nothing is simulated");
                    helper.assertEquals(
                            TransferDirection.IMPORT,
                            container.getMode(),
                            "The saved direction applies again");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aSecondViewerFollowsWhatTheFirstChanges(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal to join the network",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("open the terminal twice", () -> {
                    ContainerTransferTerminal first = new ContainerTransferTerminal(
                            helper.spawnFakePlayer("transfer_terminal").inventory,
                            terminal);
                    ContainerTransferTerminal second = new ContainerTransferTerminal(
                            helper.spawnFakePlayer("transfer_terminal_2").inventory,
                            terminal);
                    first.detectAndSendChanges();
                    second.detectAndSendChanges();
                    helper.assertFalse(second.simulatingSync.get(), "Nothing is searched yet");

                    // What the first viewer's client sends ends up in the terminal both screens share.
                    terminal.getConfigManager().putSetting(Settings.TRANSFER_SIMULATE, YesNo.YES);
                    terminal.getQueryInventory().putAEStackInSlot(0, iron());
                    first.detectAndSendChanges();
                    second.detectAndSendChanges();

                    helper.assertEquals(YesNo.YES, second.simulateSync.get(), "The setting reaches the second screen");
                    helper.assertTrue(second.simulatingSync.get(), "The second screen simulates the new search");
                }).thenSucceed();
    }
}
