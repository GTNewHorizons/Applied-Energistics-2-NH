package com.glodblock.github.common.item;

import java.util.EnumSet;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.common.tabs.FluidCraftingTabs;
import com.glodblock.github.loader.IRegister;
import com.glodblock.github.util.NameConst;

import appeng.api.AEApi;
import appeng.core.features.AEFeature;
import cpw.mods.fml.common.registry.GameRegistry;

public class ItemWirelessLevelTerminal extends ItemBaseWirelessTerminal
        implements IRegister<ItemWirelessLevelTerminal> {

    public ItemWirelessLevelTerminal() {
        AEApi.instance().registries().wireless().registerWirelessHandler(this);
        this.setFeature(EnumSet.of(AEFeature.WirelessAccessTerminal, AEFeature.PoweredTools));
        setUnlocalizedName(NameConst.ITEM_WIRELESS_LEVEL_TERMINAL);
        setTextureName(FluidCraft.resource(NameConst.ITEM_WIRELESS_LEVEL_TERMINAL).toString());
    }

    @Override
    public ItemWirelessLevelTerminal register() {
        GameRegistry.registerItem(this, NameConst.ITEM_WIRELESS_LEVEL_TERMINAL, FluidCraft.MODID);
        setCreativeTab(FluidCraftingTabs.INSTANCE);
        return this;
    }
}
