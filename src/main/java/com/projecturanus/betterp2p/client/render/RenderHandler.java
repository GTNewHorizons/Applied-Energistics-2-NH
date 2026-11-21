package com.projecturanus.betterp2p.client.render;

import java.util.Collections;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import com.projecturanus.betterp2p.client.ClientCache;
import com.projecturanus.betterp2p.item.ItemAdvancedMemoryCard;
import com.projecturanus.betterp2p.network.data.P2PLocation;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class RenderHandler {

    @SubscribeEvent
    public void renderOverlays(RenderWorldLastEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || heldItem.getItem() != ItemAdvancedMemoryCard.INSTANCE) {
            return;
        }
        P2PLocation selected = ClientCache.getSelectedPosition();
        if (selected != null) {
            OutlineRenderer
                    .renderOutlinesWithFacing(event, player, Collections.singletonList(selected), 0x45, 0xDA, 0x75);
        }
        if (!ClientCache.getPositions().isEmpty()) {
            OutlineRenderer.renderOutlinesWithFacing(event, player, ClientCache.getPositions(), 0x66, 0xCC, 0xFF);
        }
    }

    public static void register() {
        RenderHandler handler = new RenderHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }
}
