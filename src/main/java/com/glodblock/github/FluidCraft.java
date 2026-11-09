package com.glodblock.github;

import net.minecraft.util.ResourceLocation;

import com.glodblock.github.common.Config;
import com.glodblock.github.crossmod.opencomputers.OCDriverInit;
import com.glodblock.github.crossmod.waila.WailaInit;
import com.glodblock.github.inventory.InventoryHandler;
import com.glodblock.github.loader.ChannelLoader;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.glodblock.github.loader.RecipeLoader;
import com.glodblock.github.loader.UltraTerminalLoader;
import com.glodblock.github.proxy.CommonProxy;
import com.glodblock.github.util.FluidCellHandler;
import com.glodblock.github.util.ModAndClassUtil;

import appeng.api.AEApi;
import appeng.api.features.IUltraTerminalRegistry;
import appeng.core.AELog;
import appeng.core.BuildTags;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;

@Mod(
        modid = FluidCraft.MODID,
        version = FluidCraft.VERSION,
        name = FluidCraft.MODNAME,
        dependencies = "required-after:appliedenergistics2;after:waila;after:thaumicenergistics;after:ae2wct")
public class FluidCraft {

    public static final String MODID = "ae2fc";
    public static final String VERSION = BuildTags.VERSION;
    public static final String MODNAME = "AE2 Fluid Crafting";

    @Mod.Instance(MODID)
    public static FluidCraft INSTANCE;

    @SidedProxy(
            clientSide = "com.glodblock.github.proxy.ClientProxy",
            serverSide = "com.glodblock.github.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public static void preInit(FMLPreInitializationEvent event) {
        Config.run();
        ChannelLoader.INSTANCE.run();
        proxy.preInit(event);
        UltraTerminalLoader.INSTANCE.run();
    }

    @Mod.EventHandler
    public static void init(FMLInitializationEvent event) {
        if (ModAndClassUtil.OC) {
            OCDriverInit.run();
        }
        if (ModAndClassUtil.WAILA) {
            WailaInit.run();
        }
        proxy.init(event);
    }

    @Mod.EventHandler
    public static void postInit(FMLPostInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(FluidCraft.INSTANCE, new InventoryHandler());
        AEApi.instance().registries().cell().addCellHandler(new FluidCellHandler());
        ItemAndBlockHolder.loadSetting();

        if (!Config.removeRecipe) {
            RecipeLoader.INSTANCE.run();
        }
        if (!ModAndClassUtil.NEW_HORIZONS_CORE_MOD) {
            RecipeLoader.addTerminalCards();
            RecipeLoader.addInfiniteWaterCell();
        }
        // the registration order is the index used by the gui opening channel, log it to diagnose mismatches
        final IUltraTerminalRegistry terminalRegistry = AEApi.instance().registries().ultraTerminal();
        terminalRegistry.closeRegistration();
        AELog.info("[UltraTerminal] Terminal order: %s", terminalRegistry.getTypes());
        RecipeLoader.runTerminalRecipe();
        RecipeLoader.addDisassembleRecipe();

        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        proxy.onLoadComplete(event);
    }

    public static ResourceLocation resource(String path) {
        return new ResourceLocation(MODID, path);
    }
}
