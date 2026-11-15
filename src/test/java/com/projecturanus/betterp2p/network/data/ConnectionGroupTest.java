package com.projecturanus.betterp2p.network.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

public class ConnectionGroupTest {

    @Test
    public void noSelectionHasNoConnections() {
        assertTrue(ConnectionGroup.select(Collections.singletonList(tunnel(10, 0, false, true, 0)), null).isEmpty());
    }

    @Test
    public void zeroFrequencyDoesNotConnectUnboundTunnels() {
        P2PInfo selected = tunnel(0, 0, false, true, 0);
        assertTrue(ConnectionGroup.select(Arrays.asList(selected, tunnel(0, 1, true, true, 0)), selected).isEmpty());
    }

    @Test
    public void includesInputAndAllOutputsFromEitherSelection() {
        P2PInfo input = tunnel(10, 0, false, true, 0);
        P2PInfo first = tunnel(10, 1, true, true, 0);
        P2PInfo second = tunnel(10, 2, true, true, 0);
        java.util.List<P2PInfo> group = Arrays.asList(input, first, second);
        assertEquals(group, ConnectionGroup.select(group, input));
        assertEquals(group, ConnectionGroup.select(group, first));
    }

    @Test
    public void excludesOtherFrequenciesAndTunnelTypes() {
        P2PInfo input = tunnel(10, 0, false, true, 0);
        P2PInfo wrongFrequency = tunnel(11, 1, true, true, 0);
        P2PInfo wrongType = new P2PInfo(10, 2, 64, 0, 0, ForgeDirection.NORTH, "", true, true, -1, 2);
        assertEquals(
                Collections.singletonList(input),
                ConnectionGroup.select(Arrays.asList(input, wrongFrequency, wrongType), input));
    }

    @Test
    public void keepsOfflineAndCrossDimensionEndpoints() {
        P2PInfo input = tunnel(10, 0, false, true, 0);
        P2PInfo offline = tunnel(10, 1, true, false, 0);
        P2PInfo otherDimension = tunnel(10, 2, true, true, -1);
        java.util.List<P2PInfo> group = Arrays.asList(input, offline, otherDimension);
        assertEquals(group, ConnectionGroup.select(group, input));
    }

    @Test
    public void distinguishesPartsOnDifferentFacesOfTheSameBlock() {
        P2PInfo input = tunnel(10, 0, false, true, 0);
        P2PInfo output = new P2PInfo(10, 0, 64, 0, 0, ForgeDirection.SOUTH, "", true, true, -1, 1);
        java.util.List<P2PInfo> group = Arrays.asList(input, output);
        assertEquals(2, ConnectionGroup.select(group, input).size());
    }

    @Test
    public void doesNotMutateServerSnapshot() {
        P2PInfo input = tunnel(10, 0, false, true, 0);
        java.util.List<P2PInfo> snapshot = Collections.singletonList(input);
        ConnectionGroup.select(snapshot, input).clear();
        assertEquals(1, snapshot.size());
    }

    private static P2PInfo tunnel(long frequency, int x, boolean output, boolean hasChannel, int dimension) {
        return new P2PInfo(frequency, x, 64, 0, dimension, ForgeDirection.NORTH, "", output, hasChannel, -1, 1);
    }
}
