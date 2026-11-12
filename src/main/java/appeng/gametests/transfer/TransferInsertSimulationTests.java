package appeng.gametests.transfer;

import static appeng.gametests.AEGameTestHelpers.assertActive;
import static appeng.gametests.AEGameTestHelpers.cell1k;
import static appeng.gametests.AEGameTestHelpers.insertItems;
import static appeng.gametests.AEGameTestHelpers.itemStack;
import static appeng.gametests.transfer.TransferTestHelpers.install;
import static appeng.gametests.transfer.TransferTestHelpers.place;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.InventoryHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.api.AEApi;
import appeng.api.config.Settings;
import appeng.api.config.TransferDirection;
import appeng.api.config.YesNo;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.StorageName;
import appeng.container.implementations.ContainerTransferTerminal;
import appeng.core.AppEng;
import appeng.helpers.transfer.TransferInsertSimulation;
import appeng.helpers.transfer.TransferScanner;
import appeng.me.GridAccessException;
import appeng.parts.misc.PartStorageBus;
import appeng.parts.reporting.PartTransferTerminal;
import appeng.tile.crafting.TileCraftingStorageTile;
import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileController;
import appeng.util.Platform;

@GameTestHolder(AppEng.MOD_ID)
public class TransferInsertSimulationTests {

    private static final String TEMPLATE = TransferTestHelpers.TEMPLATE;
    private static final String CHEST_LABEL = "external_chest";
    private static final String DRIVE_LABEL = "drive";
    private static final BaseActionSource SOURCE = new BaseActionSource();

    private static IGrid grid(final GameTestHelper helper) {
        try {
            return helper.assertTileEntityPresent(TileController.class, "controller").getProxy().getGrid();
        } catch (final GridAccessException e) {
            throw new AssertionError("Controller should have a grid", e);
        }
    }

    private static TransferInsertSimulation.Result simulate(final GameTestHelper helper, final Block block,
            final long amount) {
        final List<IGrid> grids = Collections.singletonList(grid(helper));
        final List<TransferComponentInfo> rows = TransferScanner
                .scan(grids, AEApi.instance().registries().transferInspectors());
        return TransferInsertSimulation.run(grids, rows, itemStack(block, amount), SOURCE);
    }

    private static TransferComponentInfo busRow(final GameTestHelper helper,
            final TransferInsertSimulation.Result result) {
        helper.assertTrue(result.simulated(), "The network should be asked");
        for (final TransferComponentInfo row : result.rows) {
            if (row.side == (byte) ForgeDirection.EAST.ordinal()) {
                return row;
            }
        }
        throw new AssertionError("The storage bus should be listed");
    }

    private static TransferComponentInfo driveRow(final GameTestHelper helper,
            final TransferInsertSimulation.Result result) {
        for (final TransferComponentInfo row : result.rows) {
            if (row.side == (byte) ForgeDirection.UNKNOWN.ordinal()) {
                return row;
            }
        }
        throw new AssertionError("The drive should be listed");
    }

    private static PartStorageBus bus(final GameTestHelper helper) {
        return place(helper, AEApi.instance().definitions().parts().storageBus(), ForgeDirection.EAST);
    }

    private static void fillChestExceptOne(final GameTestHelper helper, final int cobblestoneInLastSlot) {
        final TileEntityChest chest = helper.assertTileEntityPresent(TileEntityChest.class, CHEST_LABEL);
        for (int slot = 0; slot < chest.getSizeInventory() - 1; slot++) {
            InventoryHelper.setSlot(chest, slot, new ItemStack(Blocks.dirt, 64));
        }
        if (cobblestoneInLastSlot > 0) {
            InventoryHelper.setSlot(
                    chest,
                    chest.getSizeInventory() - 1,
                    new ItemStack(Blocks.cobblestone, cobblestoneInLastSlot));
        } else {
            InventoryHelper.setSlot(chest, chest.getSizeInventory() - 1, new ItemStack(Blocks.dirt, 64));
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anEmptyChestTakesTheWholeStack(final GameTestHelper helper) {
        final PartStorageBus bus = bus(helper);

        helper.startSequence().thenWaitUntil("wait for the bus to take the stack", 80, () -> {
            assertActive(helper, bus, "Storage bus should become active");
            final TransferInsertSimulation.Result result = simulate(helper, Blocks.cobblestone, 64);
            final TransferComponentInfo row = busRow(helper, result);
            helper.assertEquals(64L, row.simulatedAmount, "The chest has room for everything");
            helper.assertEquals(0L, result.leftover, "Nothing should be left over");
            helper.assertFalse(row.stickyClaimed, "The bus is not sticky");
        }).thenExecute("nothing was stored", () -> {
            final TileEntityChest chest = helper.assertTileEntityPresent(TileEntityChest.class, CHEST_LABEL);
            helper.assertEquals(
                    0L,
                    (long) InventoryHelper.count(chest, new ItemStack(Blocks.cobblestone)),
                    "A simulation must not move items");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void whatTheChestCannotHoldGoesElsewhere(final GameTestHelper helper) {
        final PartStorageBus bus = bus(helper);
        fillChestExceptOne(helper, 40);
        helper.setSlot(DRIVE_LABEL, 0, cell1k());

        helper.startSequence().thenWaitUntil("wait for the drive to take the rest", 80, () -> {
            assertActive(helper, bus, "Storage bus should become active");
            final TransferInsertSimulation.Result result = simulate(helper, Blocks.cobblestone, 64);
            helper.assertEquals(24L, busRow(helper, result).simulatedAmount, "Only the last slot has room");
            helper.assertEquals(0L, result.leftover, "The cell in the drive takes the rest");
            final TransferComponentInfo drive = driveRow(helper, result);
            helper.assertEquals(40L, drive.simulatedAmount, "The drive takes the rest");
            helper.assertEquals(1, drive.simulatedSlots.size(), "One cell takes it");
            helper.assertEquals(0, drive.simulatedSlots.get(0).slot, "The cell is in the first slot");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void leftoverIsWhatFitsNowhere(final GameTestHelper helper) {
        final PartStorageBus bus = bus(helper);
        fillChestExceptOne(helper, 40);

        helper.startSequence().thenWaitUntil("wait for the bus to find its chest", 80, () -> {
            assertActive(helper, bus, "Storage bus should become active");
            final TransferInsertSimulation.Result result = simulate(helper, Blocks.cobblestone, 64);
            helper.assertEquals(24L, busRow(helper, result).simulatedAmount, "Only the last slot has room");
            helper.assertEquals(40L, result.leftover, "There is no other storage");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aFullStickyBusShutsEveryOtherStorageOut(final GameTestHelper helper) {
        final PartStorageBus bus = bus(helper);
        bus.getAEInventoryByName(StorageName.CONFIG).putAEStackInSlot(0, itemStack(Blocks.cobblestone, 1));
        install(bus.getInventoryByName("upgrades"), AEApi.instance().definitions().materials().cardSticky());
        fillChestExceptOne(helper, 0);
        helper.setSlot(DRIVE_LABEL, 0, cell1k());

        helper.startSequence().thenWaitUntil("wait for the sticky bus to claim the stack", 80, () -> {
            assertActive(helper, bus, "Storage bus should become active");
            helper.assertTrue(bus.getInternalHandler().getSticky(), "Handler should be sticky");
            final TransferInsertSimulation.Result result = simulate(helper, Blocks.cobblestone, 64);
            final TransferComponentInfo row = busRow(helper, result);
            helper.assertTrue(row.stickyClaimed, "The sticky bus claims whitelisted stacks");
            helper.assertEquals(0L, row.simulatedAmount, "Its chest is full");
            helper.assertEquals(64L, result.leftover, "The drive never gets a chance");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nonStorageRowsAreLeftAlone(final GameTestHelper helper) {
        final PartStorageBus bus = bus(helper);
        place(helper, AEApi.instance().definitions().parts().exportBus(), ForgeDirection.UP);

        helper.startSequence().thenWaitUntil("wait for the simulation", 80, () -> {
            assertActive(helper, bus, "Storage bus should become active");
            final TransferInsertSimulation.Result result = simulate(helper, Blocks.cobblestone, 64);
            boolean sawExportBus = false;
            for (final TransferComponentInfo row : result.rows) {
                if (row.side == (byte) ForgeDirection.UP.ordinal()) {
                    sawExportBus = true;
                    helper.assertEquals(
                            TransferComponentInfo.NOT_SIMULATED,
                            row.simulatedAmount,
                            "Export buses are not part of storage routing");
                }
            }
            helper.assertTrue(sawExportBus, "The export bus should be listed");
        }).thenSucceed();
    }

    // A CPU that waits for an ingredient takes it ahead of all storage, and gets a row of its own.
    @GameTest(template = "crafting_cpu", timeoutTicks = 520)
    public static void aCpuWaitingForAnIngredientGetsARow(final GameTestHelper helper) {
        final TileController controller = helper.assertTileEntityPresent(TileController.class, "controller");
        final TileCraftingStorageTile cpuStorage = helper
                .assertTileEntityPresent(TileCraftingStorageTile.class, "cpu_storage");
        final TileInterface blockInterface = helper.assertTileEntityPresent(TileInterface.class, "interface");
        final ItemStack cell = cell1k();
        insertItems(helper, cell, Blocks.cobblestone, 1);
        helper.setSlot(DRIVE_LABEL, 0, cell);

        helper.startSequence().thenWaitUntil("wait for the crafting network", 100, () -> {
            assertActive(helper, controller.getProxy(), "Controller should become active");
            assertActive(helper, blockInterface.getProxy(), "Interface should become active");
            helper.assertTrue(cpuStorage.isFormed(), "The CPU should form");
        }).thenExecute("turn the assembler into a chest and install a two step chain", () -> {
            helper.setBlock("assembler", Blocks.chest);
            InventoryHelper.setSlot(
                    blockInterface.getInterfaceDuality().getPatterns(),
                    0,
                    processingPattern(Blocks.cobblestone, Blocks.stone));
            InventoryHelper.setSlot(
                    blockInterface.getInterfaceDuality().getPatterns(),
                    1,
                    processingPattern(Blocks.stone, Blocks.stonebrick));
        }).thenWaitUntil("wait for the chain to be advertised", 80, () -> {
            helper.assertTrue(
                    craftingGrid(controller)
                            .getCraftingFor(itemStack(Blocks.stonebrick, 1), null, -1, controller.getWorldObj()).size()
                            > 0,
                    "Stone bricks should be craftable");
        }).thenExecute("request one stone brick", () -> submitCraft(helper, controller, Blocks.stonebrick))
                .thenWaitUntil("wait for the CPU to wait for stone", 220, () -> {
                    helper.assertInventoryCount("assembler", new ItemStack(Blocks.cobblestone), 1);
                    final TransferInsertSimulation.Result result = simulate(helper, Blocks.stone, 3);
                    helper.assertEquals(1, result.cpuRows.size(), "The waiting CPU should be listed");
                    final TransferComponentInfo cpu = result.cpuRows.get(0);
                    helper.assertEquals(1L, cpu.simulatedAmount, "It waits for one stone");
                    helper.assertEquals(0, cpu.networkTier, "It is on the terminal's network");
                    helper.assertEquals(0L, result.leftover, "The empty cell takes the rest");
                    helper.assertEquals(0, simulate(helper, Blocks.dirt, 3).cpuRows.size(), "No CPU waits for dirt");
                }).thenSucceed();
    }

    private static PartTransferTerminal terminal(final GameTestHelper helper) {
        return place(helper, AEApi.instance().definitions().parts().transferTerminal(), ForgeDirection.UP);
    }

    private static ContainerTransferTerminal open(final GameTestHelper helper, final PartTransferTerminal terminal) {
        final FakePlayer player = helper.spawnFakePlayer("transfer_terminal");
        return new ContainerTransferTerminal(player.inventory, terminal);
    }

    private static TransferComponentInfo firstRow(final GameTestHelper helper,
            final ContainerTransferTerminal container) {
        final List<TransferComponentInfo> rows = container.getFilteredRows();
        helper.assertFalse(rows.isEmpty(), "The terminal should list the storage bus");
        return rows.get(0);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void theTerminalRefreshesTheSimulationWhileOpen(final GameTestHelper helper) {
        final PartStorageBus bus = bus(helper);
        final PartTransferTerminal terminal = terminal(helper);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_SIMULATE, YesNo.YES);
        terminal.getQueryInventory().putAEStackInSlot(0, itemStack(Blocks.cobblestone, 64));
        final ContainerTransferTerminal[] container = new ContainerTransferTerminal[1];

        helper.startSequence().thenWaitUntil("wait for the network", 80, () -> {
            assertActive(helper, terminal, "Terminal should become active");
            assertActive(helper, bus, "Storage bus should become active");
            helper.assertNotNull(bus.getInternalHandler(), "Storage bus should find its chest");
        }).thenExecute("open the terminal", () -> {
            container[0] = open(helper, terminal);
            container[0].detectAndSendChanges();

            helper.assertTrue(container[0].simulatingSync.get(), "The rows should carry a simulation");
            helper.assertEquals(64L, firstRow(helper, container[0]).simulatedAmount, "The chest takes it all");
            helper.assertEquals(0L, container[0].leftoverSync.get(), "Nothing is left over");
            helper.assertEquals(0, container[0].stickyCountSync.get(), "The separator is not needed");
        }).thenExecute("fill the chest and wait a second", () -> {
            fillChestExceptOne(helper, 0);
            for (int tick = 0; tick < 20; tick++) {
                container[0].detectAndSendChanges();
            }

            helper.assertEquals(0L, firstRow(helper, container[0]).simulatedAmount, "The chest is full now");
            helper.assertEquals(64L, container[0].leftoverSync.get(), "Nothing else can take it");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void simulatingLocksTheDirectionToExportUntilTurnedOff(final GameTestHelper helper) {
        final PartTransferTerminal terminal = terminal(helper);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_DIRECTION, TransferDirection.IMPORT);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_SIMULATE, YesNo.YES);
        terminal.getQueryInventory().putAEStackInSlot(0, itemStack(Blocks.cobblestone, 1));

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the terminal",
                        60,
                        () -> assertActive(helper, terminal, "Terminal should become active"))
                .thenExecute("toggle simulating", () -> {
                    final ContainerTransferTerminal container = open(helper, terminal);
                    container.detectAndSendChanges();
                    helper.assertEquals(TransferDirection.EXPORT, container.getMode(), "Simulating forces export");
                    helper.assertEquals(
                            TransferDirection.IMPORT,
                            terminal.getConfigManager().getSetting(Settings.TRANSFER_DIRECTION),
                            "The saved direction is kept");

                    container.simulateSync.set(YesNo.NO);
                    container.detectAndSendChanges();
                    helper.assertEquals(TransferDirection.IMPORT, container.getMode(), "The direction comes back");
                    helper.assertFalse(container.simulatingSync.get(), "No simulation without the setting");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void simulatingIgnoresEveryOtherFilter(final GameTestHelper helper) {
        final PartStorageBus bus = bus(helper);
        final PartTransferTerminal terminal = terminal(helper);
        // Each of these would hide the storage bus, which has no crafting card and no filter.
        terminal.getConfigManager().putSetting(Settings.TRANSFER_CRAFTING_CARD_ONLY, YesNo.YES);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_HIDE_UNFILTERED, YesNo.YES);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_NETWORK_ONLY, YesNo.YES);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_SIMULATE, YesNo.YES);
        terminal.getQueryInventory().putAEStackInSlot(0, itemStack(Blocks.cobblestone, 64));

        helper.startSequence().thenWaitUntil("wait for the network", 80, () -> {
            assertActive(helper, terminal, "Terminal should become active");
            assertActive(helper, bus, "Storage bus should become active");
            helper.assertNotNull(bus.getInternalHandler(), "Storage bus should find its chest");
        }).thenExecute("toggle simulating with every filter on", () -> {
            final ContainerTransferTerminal container = open(helper, terminal);
            container.searchTextSync.set("no config is named this");
            container.detectAndSendChanges();
            helper.assertEquals(
                    64L,
                    firstRow(helper, container).simulatedAmount,
                    "The storage bus is listed while simulating");

            container.simulateSync.set(YesNo.NO);
            container.detectAndSendChanges();
            helper.assertTrue(container.getFilteredRows().isEmpty(), "The saved filters apply again");
            helper.assertEquals(
                    YesNo.YES,
                    terminal.getConfigManager().getSetting(Settings.TRANSFER_HIDE_UNFILTERED),
                    "Simulating leaves the saved filters alone");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void theAmountDialogChangesWhatIsSimulated(final GameTestHelper helper) {
        final PartStorageBus bus = bus(helper);
        final PartTransferTerminal terminal = terminal(helper);
        terminal.getConfigManager().putSetting(Settings.TRANSFER_SIMULATE, YesNo.YES);
        terminal.getQueryInventory().putAEStackInSlot(0, itemStack(Blocks.cobblestone, 1));

        helper.startSequence().thenWaitUntil("wait for the network", 80, () -> {
            assertActive(helper, terminal, "Terminal should become active");
            helper.assertNotNull(bus.getInternalHandler(), "Storage bus should find its chest");
        }).thenExecute("set a new amount", () -> {
            final ContainerTransferTerminal container = open(helper, terminal);
            container.detectAndSendChanges();
            helper.assertEquals(1L, firstRow(helper, container).simulatedAmount, "One is searched at first");

            container.updateVirtualSlot(StorageName.CONFIG, 0, itemStack(Blocks.cobblestone, 300));
            container.detectAndSendChanges();
            helper.assertEquals(300L, container.getQuery().getStackSize(), "The query takes the new amount");
            helper.assertEquals(300L, firstRow(helper, container).simulatedAmount, "The new amount is simulated");
        }).thenSucceed();
    }

    private static ICraftingGrid craftingGrid(final TileController controller) {
        try {
            return controller.getProxy().getCrafting();
        } catch (final GridAccessException e) {
            throw new AssertionError("Network crafting cache should be accessible", e);
        }
    }

    private static void submitCraft(final GameTestHelper helper, final TileController controller, final Block output) {
        try {
            final ICraftingGrid crafting = craftingGrid(controller);
            final Future<ICraftingJob> future = crafting.beginCraftingJob(
                    controller.getWorldObj(),
                    controller.getProxy().getGrid(),
                    SOURCE,
                    itemStack(output, 1),
                    null);
            final ICraftingJob job = future.get(5_000, TimeUnit.MILLISECONDS);
            helper.assertFalse(job.isSimulation(), "The job should be executable");
            helper.assertNotNull(crafting.submitJob(job, null, null, false, SOURCE), "The job should start");
        } catch (final Exception e) {
            throw new AssertionError("The job should calculate and start", e);
        }
    }

    private static ItemStack processingPattern(final Block input, final Block output) {
        final ItemStack pattern = AEApi.instance().definitions().items().encodedPattern().maybeStack(1).get();
        final NBTTagCompound tags = new NBTTagCompound();
        final NBTTagList inputs = new NBTTagList();
        final NBTTagList outputs = new NBTTagList();
        tags.setBoolean("crafting", false);
        tags.setBoolean("substitute", false);
        tags.setBoolean("beSubstitute", false);
        inputs.appendTag(itemTag(input));
        outputs.appendTag(itemTag(output));
        tags.setTag("in", inputs);
        tags.setTag("out", outputs);
        pattern.setTagCompound(tags);
        return pattern;
    }

    private static NBTTagCompound itemTag(final Block block) {
        final NBTTagCompound tag = new NBTTagCompound();
        Platform.writeItemStackToNBT(new ItemStack(block, 1), tag);
        return tag;
    }
}
