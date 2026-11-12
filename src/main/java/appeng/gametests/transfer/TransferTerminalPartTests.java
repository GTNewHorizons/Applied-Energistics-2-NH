package appeng.gametests.transfer;

import static appeng.gametests.AEGameTestHelpers.assertActive;
import static appeng.gametests.transfer.TransferTestHelpers.TEMPLATE;
import static appeng.gametests.transfer.TransferTestHelpers.iron;
import static appeng.gametests.transfer.TransferTestHelpers.place;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;

import java.util.ArrayList;
import java.util.List;

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
import appeng.helpers.transfer.TransferRowWindow;
import appeng.parts.automation.PartExportBus;
import appeng.parts.reporting.PartTransferTerminal;
import appeng.tile.networking.TileCableBus;

@GameTestHolder(AppEng.MOD_ID)
public class TransferTerminalPartTests {

    private static PartTransferTerminal terminal(GameTestHelper helper) {
        return place(helper, AEApi.instance().definitions().parts().transferTerminal(), ForgeDirection.UP);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void terminalCanBePlacedOnACable(GameTestHelper helper) {
        helper.assertNotNull(terminal(helper), "Terminal part should be placed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void containerScansTheNetworkWhenOpened(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal to join the network",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("open the container and tick it", () -> {
                    FakePlayer player = helper.spawnFakePlayer("transfer_terminal");
                    ContainerTransferTerminal container = new ContainerTransferTerminal(player.inventory, terminal);

                    container.detectAndSendChanges();
                    container.detectAndSendChanges();

                    helper.assertNull(container.getQuery(), "A new terminal has nothing searched");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void filtersAndWindowChangesApplyWithoutError(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal to join the network",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("change every filter and the window", () -> {
                    FakePlayer player = helper.spawnFakePlayer("transfer_terminal");
                    ContainerTransferTerminal container = new ContainerTransferTerminal(player.inventory, terminal);
                    container.detectAndSendChanges();

                    container.directionSync.set(TransferDirection.EXPORT);
                    container.craftingOnlySync.set(YesNo.YES);
                    container.hideUnfilteredSync.set(YesNo.YES);
                    container.networkOnlySync.set(YesNo.YES);
                    container.searchTextSync.set("iron");
                    container.windowStartSync.setLocalValue(1000);
                    container.windowSizeSync.setLocalValue(TransferRowWindow.ALL);
                    container.detectAndSendChanges();

                    helper.assertNull(container.getQuery(), "The filters do not touch the query");
                }).thenSucceed();
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
        helper.succeed();
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
        helper.assertEquals(
                terminal.getChangeCount(),
                terminal.getChangeCount(),
                "Every open screen has to see the change, so reading it must not reset it");
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
