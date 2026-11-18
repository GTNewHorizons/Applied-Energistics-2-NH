package com.projecturanus.betterp2p;

import net.minecraftforge.common.config.Configuration;

import org.apache.logging.log4j.Logger;

import com.projecturanus.betterp2p.config.BetterP2PConfig;
import com.projecturanus.betterp2p.item.ItemAdvancedMemoryCard;
import com.projecturanus.betterp2p.network.ModNetwork;

import appeng.core.BuildTags;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Better P2P is created by LasmGratel. GlodBlock backported this to 1.7.10
 */
@Mod(modid = BetterP2P.MODID, version = BuildTags.VERSION, dependencies = "required-after:appliedenergistics2")
public class BetterP2P {

    public static final String MODID = "betterp2p";

    public static Logger logger;

    @SidedProxy(
            serverSide = "com.projecturanus.betterp2p.CommonProxy",
            clientSide = "com.projecturanus.betterp2p.ClientProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        ModNetwork.registerNetwork();
        BetterP2PConfig.loadConfig(new Configuration(event.getSuggestedConfigurationFile()));
        GameRegistry.registerItem(ItemAdvancedMemoryCard.INSTANCE, "advanced_memory_card", MODID);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }

    @Mod.EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        ModNetwork.stop();
    }
}
