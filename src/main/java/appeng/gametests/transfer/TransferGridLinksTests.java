package appeng.gametests.transfer;

import static appeng.gametests.AEGameTestHelpers.assertActive;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPart;
import appeng.api.util.AEColor;
import appeng.core.AppEng;
import appeng.me.GridAccessException;
import appeng.me.NetworkList;
import appeng.parts.misc.PartStorageBus;
import appeng.tile.misc.TileInterface;
import appeng.tile.networking.TileCableBus;
import appeng.tile.networking.TileController;

@GameTestHolder(AppEng.MOD_ID)
public class TransferGridLinksTests {

    private static final String TEMPLATE = "interface_smart_blocking_subnet";
    private static final String MAIN_CONTROLLER_LABEL = "main_controller";
    private static final String SUBNET_CONTROLLER_LABEL = "subnet_controller";

    /** The gap between the two rows of interfaces: the west block joins the main network, the east one the subnet. */
    private static final int MAIN_INTERFACE_X = 3;
    private static final int SUBNET_CABLE_X = 4;
    private static final int GAP_Y = 0;
    private static final int GAP_Z = 1;

    // The storage bus sits on the subnet and points at an interface of the main network.
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void reverseLinksAreOptInAndFindTheOtherGrid(GameTestHelper helper) {
        Setup setup = placeLink(helper);

        helper.startSequence()
                .thenWaitUntil("wait for both networks to activate", 160, () -> assertReady(helper, setup))
                .thenExecute("compare both traversal modes", () -> {
                    IGrid main = grid(setup.main);
                    IGrid subnet = grid(setup.subnet);
                    helper.assertFalse(main.equals(subnet), "The storage bus must not merge the networks");

                    NetworkList oldBehaviour = main.getAllRecursiveGridConnections(PartStorageBus.class, false);
                    helper.assertEquals(1, oldBehaviour.size(), "Without the flag only forward links count");

                    NetworkList extended = main.getAllRecursiveGridConnections(PartStorageBus.class, true);
                    helper.assertEquals(2, extended.size(), "With the flag the reverse link is followed");
                    helper.assertTrue(main.equals(extended.iterator().next()), "The starting grid comes first");
                    helper.assertTrue(extended.contains(subnet), "The subnet should be found from the main network");
                }).thenSucceed();
    }

    // Configuring an interface hides its network from the storage bus, which the old path cannot see through.
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void forwardLinksSurviveConfiguredInterfaces(GameTestHelper helper) {
        Setup setup = placeLink(helper);

        helper.startSequence()
                .thenWaitUntil("wait for both networks to activate", 160, () -> assertReady(helper, setup))
                .thenExecute("configure the interface and walk from the subnet", () -> {
                    IGrid main = grid(setup.main);
                    IGrid subnet = grid(setup.subnet);

                    helper.assertTrue(
                            subnet.getAllRecursiveGridConnections(PartStorageBus.class, false).contains(main),
                            "An unconfigured interface is visible to the old path");

                    setup.linkedInterface.getInterfaceDuality().getConfig()
                            .setInventorySlotContents(0, new ItemStack(Blocks.cobblestone, 1));

                    helper.assertFalse(
                            subnet.getAllRecursiveGridConnections(PartStorageBus.class, false).contains(main),
                            "A configured interface is invisible to the old path");
                    helper.assertTrue(
                            subnet.getAllRecursiveGridConnections(PartStorageBus.class, true).contains(main),
                            "The extended path finds it by adjacency");
                }).thenSucceed();
    }

    private static Setup placeLink(GameTestHelper helper) {
        Block interfaceBlock = AEApi.instance().definitions().blocks().iface().maybeBlock().get();
        Block cableBusBlock = AEApi.instance().definitions().blocks().multiPart().maybeBlock().get();

        helper.setBlock(MAIN_INTERFACE_X, GAP_Y, GAP_Z, interfaceBlock);
        helper.setBlock(SUBNET_CABLE_X, GAP_Y, GAP_Z, cableBusBlock);

        TileCableBus host = helper.assertTileEntityPresent(TileCableBus.class, SUBNET_CABLE_X, GAP_Y, GAP_Z);
        ItemStack cable = AEApi.instance().definitions().parts().cableGlass().stack(AEColor.Transparent, 1);
        ItemStack storageBus = AEApi.instance().definitions().parts().storageBus().maybeStack(1).get();
        helper.assertNotNull(host.addPart(storageBus, ForgeDirection.WEST, null), "Storage bus should be placed");
        helper.assertNotNull(host.addPart(cable, ForgeDirection.UNKNOWN, null), "Cable should be placed");

        return new Setup(
                helper.assertTileEntityPresent(TileController.class, MAIN_CONTROLLER_LABEL),
                helper.assertTileEntityPresent(TileController.class, SUBNET_CONTROLLER_LABEL),
                helper.assertTileEntityPresent(TileInterface.class, MAIN_INTERFACE_X, GAP_Y, GAP_Z),
                host.getPart(ForgeDirection.WEST));
    }

    private static void assertReady(GameTestHelper helper, Setup setup) {
        assertActive(helper, setup.main.getProxy(), "Main controller should become active");
        assertActive(helper, setup.subnet.getProxy(), "Subnet controller should become active");
        assertActive(helper, setup.linkedInterface.getProxy(), "Linked interface should become active");
        assertActive(helper, setup.bus, "Storage bus should become active");
    }

    private static IGrid grid(TileController controller) {
        try {
            return controller.getProxy().getGrid();
        } catch (GridAccessException e) {
            throw new AssertionError("Controller should have a grid", e);
        }
    }

    private static final class Setup {

        private final TileController main;
        private final TileController subnet;
        private final TileInterface linkedInterface;
        private final IPart bus;

        private Setup(TileController main, TileController subnet, TileInterface linkedInterface, IPart bus) {
            this.main = main;
            this.subnet = subnet;
            this.linkedInterface = linkedInterface;
            this.bus = bus;
        }
    }
}
