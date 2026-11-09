package appeng.gametests.transfer;

import static appeng.gametests.transfer.TransferTestHelpers.TEMPLATE;
import static appeng.gametests.transfer.TransferTestHelpers.place;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.api.AEApi;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.core.AppEng;
import appeng.parts.reporting.PartTransferTerminal;

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

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void storageChangesRequestARescan(GameTestHelper helper) {
        PartTransferTerminal terminal = terminal(helper);
        terminal.needsUpdate();

        terminal.onCellArrayChanged(new MENetworkCellArrayUpdate());

        helper.assertTrue(terminal.needsUpdate(), "A storage change should request a rescan");
        helper.assertFalse(terminal.needsUpdate(), "Reading the flag should reset it");
        helper.succeed();
    }
}
