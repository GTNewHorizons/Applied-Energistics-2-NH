package com.projecturanus.betterp2p.client.gui.widget;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.util.p2p.ClientTunnelInfo;

public interface ITypeReceiver {

    /**
     * @param type the selected type, or {@code null} for any type
     */
    void accept(@Nullable ClientTunnelInfo type);

    int x();

    int y();
}
