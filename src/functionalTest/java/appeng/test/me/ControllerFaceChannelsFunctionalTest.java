package appeng.test.me;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.me.ChannelAlertTracker;
import appeng.me.GridConnection;
import appeng.me.GridNode;
import appeng.me.pathfinding.PathingCalculation;
import appeng.test.mockme.MockGridBlock;
import appeng.test.mockme.MockGridMachine;
import appeng.tile.networking.TileController;

public class ControllerFaceChannelsFunctionalTest {

    private final List<GridNode> nodes = new ArrayList<>();

    @AfterEach
    public void destroyGrid() {
        for (final GridNode node : this.nodes) {
            node.destroy();
        }
    }

    @Test
    public void connectionsOnTheSameControllerFaceShareItsChannelCapacity() throws Exception {
        final GridNode controller = this.node(new TileController(), GridFlags.CANNOT_CARRY, GridFlags.DENSE_CAPACITY);

        for (int i = 0; i < 33; i++) {
            new GridConnection(
                    controller,
                    this.node(new MockGridMachine(), GridFlags.REQUIRE_CHANNEL),
                    ForgeDirection.NORTH);
        }
        new GridConnection(
                controller,
                this.node(new MockGridMachine(), GridFlags.REQUIRE_CHANNEL),
                ForgeDirection.SOUTH);

        final PathingCalculation calculation = new PathingCalculation(controller.getGrid());
        calculation.compute();

        assertEquals(33, calculation.getChannelsInUse());
        assertEquals(1, calculation.getCapacityRejectedNodes().size());
    }

    @Test
    public void ninthDeviceCanDisplaceAnExistingDeviceOnAnEightChannelBranch() throws Exception {
        final GridNode controller = this.node(new TileController(), GridFlags.CANNOT_CARRY, GridFlags.DENSE_CAPACITY);
        final GridNode cable = this.node(new MockGridMachine(), GridFlags.PREFERRED);
        final GridNode downstreamCable = this.node(new MockGridMachine(), GridFlags.PREFERRED);
        new GridConnection(controller, cable, ForgeDirection.NORTH);
        new GridConnection(cable, downstreamCable, ForgeDirection.NORTH);
        final List<IGridNode> devices = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            final GridNode device = this.node(new MockGridMachine(), GridFlags.REQUIRE_CHANNEL);
            devices.add(device);
            new GridConnection(downstreamCable, device, ForgeDirection.NORTH);
        }
        new PathingCalculation(controller.getGrid()).compute();
        for (GridNode node : nodes) node.finalizeChannels();
        final Set<IGridNode> before = ChannelAlertTracker.snapshot(devices);
        assertEquals(8, before.size());

        final GridNode newDevice = this.node(new MockGridMachine(), GridFlags.REQUIRE_CHANNEL);
        new GridConnection(cable, newDevice, ForgeDirection.SOUTH);
        final PathingCalculation calculation = new PathingCalculation(controller.getGrid());
        calculation.compute();
        for (GridNode node : nodes) node.finalizeChannels();
        assertEquals(8, calculation.getChannelsInUse());
        assertTrue(newDevice.meetsChannelRequirements());
        final List<IGridNode> lost = ChannelAlertTracker
                .findLostChannels(before, calculation.getCapacityRejectedNodes(), controller.getGrid());
        assertEquals(1, lost.size());
        assertTrue(devices.contains(lost.get(0)));
    }

    @Test
    public void compressedChannelRestrictionIsNotReportedAsCapacityExhaustion() throws Exception {
        final GridNode controller = this.node(new TileController(), GridFlags.CANNOT_CARRY, GridFlags.DENSE_CAPACITY);
        final GridNode cable = this.node(new MockGridMachine(), GridFlags.PREFERRED, GridFlags.CANNOT_CARRY_COMPRESSED);
        final GridNode device = this
                .node(new MockGridMachine(), GridFlags.REQUIRE_CHANNEL, GridFlags.COMPRESSED_CHANNEL);
        new GridConnection(controller, cable, ForgeDirection.NORTH);
        new GridConnection(cable, device, ForgeDirection.NORTH);
        final PathingCalculation calculation = new PathingCalculation(controller.getGrid());
        calculation.compute();
        assertEquals(0, calculation.getChannelsInUse());
        assertTrue(calculation.getCapacityRejectedNodes().isEmpty());
    }

    private GridNode node(final IGridHost machine, final GridFlags... flags) {
        final EnumSet<GridFlags> nodeFlags = flags.length == 0 ? EnumSet.noneOf(GridFlags.class)
                : EnumSet.copyOf(Arrays.asList(flags));
        final GridNode node = new GridNode(new MockGridBlock() {

            @Override
            public IGridHost getMachine() {
                return machine;
            }

            @Override
            public EnumSet<GridFlags> getFlags() {
                return nodeFlags;
            }

            @Override
            public boolean hasFlag(final GridFlags flag) {
                return nodeFlags.contains(flag);
            }
        });
        node.updateState();
        this.nodes.add(node);
        return node;
    }
}
