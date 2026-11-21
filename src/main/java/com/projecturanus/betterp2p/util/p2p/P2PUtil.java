package com.projecturanus.betterp2p.util.p2p;

import com.projecturanus.betterp2p.BetterP2P;
import com.projecturanus.betterp2p.network.data.MemoryInfo;

import appeng.parts.p2p.PartP2PTunnel;

public final class P2PUtil {

    private P2PUtil() {}

    public static boolean hasChannel(PartP2PTunnel<?> tunnel) {
        return tunnel.isPowered() && tunnel.isActive();
    }

    public static int getTypeIndex(PartP2PTunnel<?> tunnel) {
        TunnelInfo info = BetterP2P.proxy.getP2PFromClass(tunnel.getClass());
        return info != null ? info.index : MemoryInfo.TUNNEL_ANY;
    }
}
