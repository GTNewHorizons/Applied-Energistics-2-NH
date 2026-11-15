package com.projecturanus.betterp2p.network.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Selects a configured connection group from one server-supplied grid snapshot. */
public final class ConnectionGroup {

    private ConnectionGroup() {}

    public static List<P2PInfo> select(Collection<P2PInfo> infos, P2PInfo selected) {
        final List<P2PInfo> targets = new ArrayList<>();
        if (selected == null || selected.getFrequency() == 0) return targets;
        for (P2PInfo info : infos) {
            if (info.getFrequency() == selected.getFrequency() && info.getType() == selected.getType()) {
                targets.add(info);
            }
        }
        return targets;
    }
}
