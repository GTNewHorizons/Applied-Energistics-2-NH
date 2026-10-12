package appeng.me;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;

public class ChannelAlertTrackerTest {

    private final IGrid grid = (IGrid) Proxy.newProxyInstance(
            IGrid.class.getClassLoader(),
            new Class<?>[] { IGrid.class },
            (proxy, method, args) -> null);

    @Test
    public void recordsOnlyChannelConsumersWithPreviouslyAssignedChannels() {
        final Node working = new Node(true, true);
        final Node alreadyOffline = new Node(true, false);
        final Node cable = new Node(false, true);
        final Set<IGridNode> snapshot = ChannelAlertTracker
                .snapshot(Arrays.asList(working.node, alreadyOffline.node, cable.node));
        assertEquals(Collections.singleton(working.node), snapshot);
    }

    @Test
    public void reportsAnExistingDeviceThatLostItsChannelDueToCapacity() {
        final Node existing = new Node(true, true);
        final Set<IGridNode> snapshot = ChannelAlertTracker.snapshot(Collections.singleton(existing.node));
        existing.satisfied = false;
        assertEquals(
                Collections.singletonList(existing.node),
                ChannelAlertTracker.findLostChannels(snapshot, Collections.singleton(existing.node), grid));
    }

    @Test
    public void ignoresPowerControllerAndOtherNonCapacityFailures() {
        final Node existing = new Node(true, true);
        final Set<IGridNode> snapshot = ChannelAlertTracker.snapshot(Collections.singleton(existing.node));
        existing.satisfied = false;
        assertTrue(ChannelAlertTracker.findLostChannels(snapshot, Collections.emptySet(), grid).isEmpty());
    }

    @Test
    public void ignoresNewDevicesAlreadyOfflineDevicesAndRemovedNodes() {
        final Node existing = new Node(true, true);
        final Node alreadyOffline = new Node(true, false);
        final Set<IGridNode> snapshot = ChannelAlertTracker.snapshot(Arrays.asList(existing.node, alreadyOffline.node));
        final Node newDevice = new Node(true, false);
        existing.satisfied = false;
        existing.currentGrid = null;
        assertTrue(
                ChannelAlertTracker.findLostChannels(
                        snapshot,
                        Arrays.asList(existing.node, alreadyOffline.node, newDevice.node),
                        grid).isEmpty());
    }

    @Test
    public void ignoresRejectedMultiblockMembersThatUltimatelyReceivedAChannel() {
        final Node existing = new Node(true, true);
        final Set<IGridNode> snapshot = ChannelAlertTracker.snapshot(Collections.singleton(existing.node));
        assertTrue(
                ChannelAlertTracker.findLostChannels(snapshot, Collections.singleton(existing.node), grid).isEmpty());
    }

    @Test
    public void placementsExpireAndDoNotSurviveWorldTimeReset() {
        assertFalse(ChannelAlertTracker.expired(100, 100));
        assertFalse(ChannelAlertTracker.expired(100, 120));
        assertTrue(ChannelAlertTracker.expired(100, 121));
        assertTrue(ChannelAlertTracker.expired(100, 99));
    }

    private final class Node {

        private boolean satisfied;
        private IGrid currentGrid = grid;
        private final IGridNode node;

        private Node(boolean requiresChannel, boolean satisfied) {
            this.satisfied = satisfied;
            node = (IGridNode) Proxy.newProxyInstance(
                    IGridNode.class.getClassLoader(),
                    new Class<?>[] { IGridNode.class },
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "hasFlag":
                                return requiresChannel && args[0] == GridFlags.REQUIRE_CHANNEL;
                            case "meetsChannelRequirements":
                                return this.satisfied;
                            case "getGrid":
                                return currentGrid;
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "equals":
                                return proxy == args[0];
                            case "toString":
                                return "Channel test node";
                            default:
                                throw new AssertionError("Unexpected method: " + method.getName());
                        }
                    });
        }
    }
}
