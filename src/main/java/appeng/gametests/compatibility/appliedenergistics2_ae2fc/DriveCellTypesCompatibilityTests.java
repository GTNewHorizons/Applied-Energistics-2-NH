package appeng.gametests.compatibility.appliedenergistics2_ae2fc;

import static appeng.gametests.AEGameTestHelpers.assertActive;
import static appeng.gametests.AEGameTestHelpers.cell1k;
import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;
import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.Arrays;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.core.AppEng;
import appeng.helpers.transfer.DriveInspector;
import appeng.tile.storage.TileDrive;

@GameTestHolder(value = AppEng.MOD_ID, requiredMods = "ae2fc")
public class DriveCellTypesCompatibilityTests {

    @GameTest(template = "drive_cells", timeoutTicks = 100)
    public static void aDriveWithItemAndFluidCellsStoresBoth(final GameTestHelper helper) {
        final TileDrive drive = helper.assertTileEntityPresent(TileDrive.class, "drive");
        helper.assertNotNull(ItemAndBlockHolder.CELL1K, "AE2FC 1k fluid cell should be available");
        // The fluid cell comes first, the types are still listed in the storage terminals' order.
        helper.setSlot("drive", 0, ItemAndBlockHolder.CELL1K.stack());
        helper.setSlot("drive", 1, cell1k());

        helper.startSequence().thenWaitUntil("wait for the drive to see its cells", 80, () -> {
            assertActive(helper, drive.getProxy(), "Drive should become active");
            helper.assertEquals(
                    Arrays.asList(ITEM_STACK_TYPE, FLUID_STACK_TYPE),
                    new DriveInspector().inspect(drive).storedTypes,
                    "The drive stores items and fluids");
        }).thenSucceed();
    }
}
