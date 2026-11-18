package com.projecturanus.betterp2p.network;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public final class ServerPlayerDisconnectHandler {

    private ServerPlayerDisconnectHandler() {}

    @SubscribeEvent
    public void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ModNetwork.removeConnection(event.player);
    }

    public static void register() {
        ServerPlayerDisconnectHandler handler = new ServerPlayerDisconnectHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }
}
