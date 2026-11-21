package com.projecturanus.betterp2p.config;

import net.minecraftforge.common.config.Configuration;

public final class BetterP2PConfig {

    public static boolean autoRefresh = true;

    private BetterP2PConfig() {}

    public static void loadConfig(Configuration config) {
        config.load();
        autoRefresh = config.getBoolean("autoRefresh", "client", true, "Auto refresh the P2P status");
    }
}
