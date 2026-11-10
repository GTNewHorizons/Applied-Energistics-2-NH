package appeng.gametests.transfer;

import static appeng.gametests.AEGameTestHelpers.assertActive;
import static appeng.gametests.AEGameTestHelpers.cell1k;
import static appeng.gametests.AEGameTestHelpers.insertItems;
import static appeng.gametests.AEGameTestHelpers.itemStack;
import static appeng.gametests.transfer.TransferTestHelpers.exports;
import static appeng.gametests.transfer.TransferTestHelpers.imports;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.InventoryHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.api.AEApi;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.core.AppEng;
import appeng.helpers.transfer.ChestInspector;
import appeng.helpers.transfer.DriveInspector;
import appeng.tile.storage.TileChest;
import appeng.tile.storage.TileDrive;

@GameTestHolder(AppEng.MOD_ID)
public class CellStorageInspectorTests {

    private static final String TEMPLATE = "drive_cells";
    private static final DriveInspector DRIVE = new DriveInspector();
    private static final ChestInspector CHEST = new ChestInspector();

    private static TileDrive drive(final GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileDrive.class, "drive");
    }

    private static TileChest chest(final GameTestHelper helper) {
        return helper.assertTileEntityPresent(TileChest.class, "me_chest");
    }

    private static ItemStack partitionedCell(final Block partition, final boolean sticky) {
        final ItemStack cell = cell1k();
        final ICellWorkbenchItem item = (ICellWorkbenchItem) cell.getItem();
        item.getConfigAEInventory(cell).putAEStackInSlot(0, itemStack(partition, 1));
        if (sticky) {
            InventoryHelper.setSlot(
                    item.getUpgradesInventory(cell),
                    0,
                    AEApi.instance().definitions().materials().cardSticky().maybeStack(1).get());
        }
        return cell;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aPartitionedCellDecidesWhatTheDriveTakes(final GameTestHelper helper) {
        final TileDrive drive = drive(helper);
        drive.setPriority(7);
        helper.setSlot("drive", 0, partitionedCell(Blocks.cobblestone, false));

        helper.startSequence().thenWaitUntil("wait for the drive to see its cell", 80, () -> {
            assertActive(helper, drive.getProxy(), "Drive should become active");
            helper.assertTrue(exports(DRIVE.inspect(drive), itemStack(Blocks.cobblestone, 1)), "Cobblestone fits");
        }).thenExecute("ask the inspector", () -> {
            final TransferDescription description = DRIVE.inspect(drive);
            helper.assertTrue(description.storageHandler, "A drive is storage");
            helper.assertEquals(7, description.priority, "The drive's priority is shown");
            helper.assertFalse(exports(description, itemStack(Blocks.dirt, 1)), "The partition keeps dirt out");
            helper.assertTrue(
                    description.matcher.isExplicitlyListed(itemStack(Blocks.cobblestone, 1)),
                    "The partition lists cobblestone");
            helper.assertEquals(1, description.config.size(), "The partition is shown");
            helper.assertFalse(description.sticky, "No cell has a sticky card");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aDriveGivesUpWhatItHolds(final GameTestHelper helper) {
        final TileDrive drive = drive(helper);
        final ItemStack cell = cell1k();
        insertItems(helper, cell, Blocks.cobblestone, 10);
        helper.setSlot("drive", 0, cell);

        helper.startSequence().thenWaitUntil("wait for the drive to see its cell", 80, () -> {
            assertActive(helper, drive.getProxy(), "Drive should become active");
            helper.assertTrue(imports(DRIVE.inspect(drive), itemStack(Blocks.cobblestone, 1)), "It holds cobblestone");
            helper.assertFalse(imports(DRIVE.inspect(drive), itemStack(Blocks.dirt, 1)), "It holds no dirt");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void anEmptyDriveTakesNothing(final GameTestHelper helper) {
        final TileDrive drive = drive(helper);

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the drive",
                        80,
                        () -> assertActive(helper, drive.getProxy(), "Drive should become active"))
                .thenExecute("ask the inspector", () -> {
                    final TransferDescription description = DRIVE.inspect(drive);
                    helper.assertFalse(exports(description, itemStack(Blocks.dirt, 1)), "There is no cell");
                    helper.assertTrue(description.config.isEmpty(), "There is nothing to show");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aStickyCellClaimsItsPartition(final GameTestHelper helper) {
        final TileChest chest = chest(helper);
        chest.setInventorySlotContents(1, partitionedCell(Blocks.cobblestone, true));

        helper.startSequence().thenWaitUntil("wait for the chest to see its cell", 80, () -> {
            assertActive(helper, chest.getProxy(), "Chest should become active");
            helper.assertTrue(CHEST.inspect(chest).sticky, "The cell has a sticky card");
        }).thenExecute("ask the inspector", () -> {
            final TransferDescription description = CHEST.inspect(chest);
            helper.assertTrue(
                    description.matcher.capturesAsSticky(itemStack(Blocks.cobblestone, 1)),
                    "Sticky storage claims its partition");
            helper.assertFalse(
                    description.matcher.capturesAsSticky(itemStack(Blocks.dirt, 1)),
                    "The partition keeps dirt out");
            helper.assertEquals(1, description.config.size(), "The chest's partition is shown");
        }).thenSucceed();
    }
}
