package com.glodblock.github.common.item;

import java.util.EnumSet;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.common.tabs.FluidCraftingTabs;
import com.glodblock.github.loader.IRegister;
import com.glodblock.github.util.NameConst;

import appeng.api.AEApi;
import appeng.core.features.AEFeature;
import cpw.mods.fml.common.registry.GameRegistry;

public class ItemWirelessInterfaceTerminal extends ItemBaseWirelessTerminal
        implements IRegister<ItemWirelessInterfaceTerminal> {

    public ItemWirelessInterfaceTerminal() {
        AEApi.instance().registries().wireless().registerWirelessHandler(this);
        this.setFeature(EnumSet.of(AEFeature.WirelessAccessTerminal, AEFeature.PoweredTools));
        setUnlocalizedName(NameConst.ITEM_WIRELESS_INTERFACE_TERMINAL);
        setTextureName(FluidCraft.resource(NameConst.ITEM_WIRELESS_INTERFACE_TERMINAL).toString());
    }

    @Override
    public ItemWirelessInterfaceTerminal register() {
        GameRegistry.registerItem(this, NameConst.ITEM_WIRELESS_INTERFACE_TERMINAL, FluidCraft.MODID);
        setCreativeTab(FluidCraftingTabs.INSTANCE);
        return this;
    }
}
