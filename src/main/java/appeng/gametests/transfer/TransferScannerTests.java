package appeng.gametests.transfer;

import static appeng.gametests.AEGameTestHelpers.assertActive;
import static appeng.gametests.transfer.TransferTestHelpers.TEMPLATE;
import static appeng.gametests.transfer.TransferTestHelpers.place;

import java.util.Collections;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.api.AEApi;
import appeng.api.features.transfer.ITransferInspectorRegistry;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPart;
import appeng.core.AppEng;
import appeng.core.features.registries.TransferInspectorRegistry;
import appeng.helpers.transfer.TransferScanner;
import appeng.me.GridAccessException;
import appeng.parts.automation.PartExportBus;
import appeng.parts.automation.PartImportBus;
import appeng.parts.misc.PartStorageBus;
import appeng.tile.networking.TileController;

@GameTestHolder(AppEng.MOD_ID)
public class TransferScannerTests {

    private static final String CONTROLLER_LABEL = "controller";

    private static IGrid grid(GameTestHelper helper) {
        try {
            return helper.assertTileEntityPresent(TileController.class, CONTROLLER_LABEL).getProxy().getGrid();
        } catch (GridAccessException e) {
            throw new AssertionError("Controller should have a grid", e);
        }
    }

    private static IPart placeImportBus(GameTestHelper helper) {
        return place(helper, AEApi.instance().definitions().parts().importBus(), ForgeDirection.UP);
    }

    private static List<TransferComponentInfo> scan(GameTestHelper helper, ITransferInspectorRegistry registry) {
        return TransferScanner.scan(Collections.singletonList(grid(helper)), registry);
    }

    private static ITransferInspectorRegistry coreRegistry() {
        return AEApi.instance().registries().transferInspectors();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void findsEveryComponentOnTheNetwork(GameTestHelper helper) {
        IPart importBus = placeImportBus(helper);

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the bus to join the network",
                        60,
                        () -> assertActive(helper, importBus, "Import bus should become active"))
                .thenExecute("scan the network", () -> {
                    List<TransferComponentInfo> rows = scan(helper, coreRegistry());

                    helper.assertEquals(2, rows.size(), "The storage bus and import bus should be found");
                    for (TransferComponentInfo row : rows) {
                        helper.assertEquals(0, row.networkTier, "Rows on the first grid have tier 0");
                        helper.assertNotNull(row.selfRep, "Rows should have an icon");
                    }
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void reportsTheComponentsLocationAndSide(GameTestHelper helper) {
        IPart importBus = placeImportBus(helper);

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the bus to join the network",
                        60,
                        () -> assertActive(helper, importBus, "Import bus should become active"))
                .thenExecute("scan the network", () -> {
                    TransferComponentInfo row = onlyImportBusRow(helper);

                    helper.assertEquals(helper.absolute("storage_bus").x(), row.x, "X should be the cable's");
                    helper.assertEquals(helper.absolute("storage_bus").y(), row.y, "Y should be the cable's");
                    helper.assertEquals(helper.absolute("storage_bus").z(), row.z, "Z should be the cable's");
                    helper.assertEquals(
                            (byte) ForgeDirection.UP.ordinal(),
                            row.side,
                            "The side tells two parts on one block apart");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aBrokenInspectorOnlyLosesItsOwnRow(GameTestHelper helper) {
        IPart importBus = placeImportBus(helper);

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the bus to join the network",
                        60,
                        () -> assertActive(helper, importBus, "Import bus should become active"))
                .thenExecute("scan with a throwing inspector", () -> {
                    TransferInspectorRegistry registry = new TransferInspectorRegistry();
                    registry.register(PartImportBus.class, host -> { throw new IllegalStateException("broken"); });
                    registry.register(PartStorageBus.class, host -> TransferDescription.builder().build());

                    helper.assertEquals(1, scan(helper, registry).size(), "The storage bus should remain");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void aNullDescriptionHidesTheComponent(GameTestHelper helper) {
        IPart importBus = placeImportBus(helper);

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the bus to join the network",
                        60,
                        () -> assertActive(helper, importBus, "Import bus should become active"))
                .thenExecute("scan with an inspector that skips", () -> {
                    TransferInspectorRegistry registry = new TransferInspectorRegistry();
                    registry.register(PartImportBus.class, host -> null);

                    helper.assertEquals(0, scan(helper, registry).size(), "The skipped component is not listed");
                }).thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void fingerprintNoticesNewComponents(GameTestHelper helper) {
        IPart importBus = placeImportBus(helper);
        long[] before = new long[1];

        helper.startSequence()
                .thenWaitUntil(
                        "wait for the bus to join the network",
                        60,
                        () -> assertActive(helper, importBus, "Import bus should become active"))
                .thenExecute("add a component", () -> {
                    before[0] = TransferScanner.fingerprint(Collections.singletonList(grid(helper)));
                    PartExportBus exportBus = place(
                            helper,
                            AEApi.instance().definitions().parts().exportBus(),
                            ForgeDirection.DOWN);
                    helper.assertNotNull(exportBus, "Export bus should be placed");
                }).thenWaitUntil("wait for the fingerprint to change", 60, () -> {
                    long after = TransferScanner.fingerprint(Collections.singletonList(grid(helper)));
                    helper.assertTrue(after != before[0], "The new bus should change the fingerprint");
                }).thenSucceed();
    }

    private static TransferComponentInfo onlyImportBusRow(GameTestHelper helper) {
        TransferInspectorRegistry registry = new TransferInspectorRegistry();
        registry.register(PartImportBus.class, host -> TransferDescription.builder().build());
        List<TransferComponentInfo> rows = scan(helper, registry);
        helper.assertEquals(1, rows.size(), "Only the import bus has an inspector");
        return rows.get(0);
    }
}
