package com.projecturanus.betterp2p.network;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class ServerEventHandler {

    private ServerEventHandler() {}

    @SubscribeEvent
    public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ModNetwork.removeConnection(event.player);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ModNetwork.flushPendingLists();
        }
    }

    public static void register() {
        FMLCommonHandler.instance().bus().register(new ServerEventHandler());
    }
}
