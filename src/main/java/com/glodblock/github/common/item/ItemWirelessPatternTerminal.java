package com.glodblock.github.common.item;

import java.util.EnumSet;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.common.tabs.FluidCraftingTabs;
import com.glodblock.github.common.terminal.TerminalIds;
import com.glodblock.github.loader.IRegister;
import com.glodblock.github.util.NameConst;

import appeng.api.AEApi;
import appeng.api.features.IUltraTerminalType;
import appeng.core.features.AEFeature;
import cpw.mods.fml.common.registry.GameRegistry;

public class ItemWirelessPatternTerminal extends ItemBaseWirelessTerminal
        implements IRegister<ItemWirelessPatternTerminal> {

    public ItemWirelessPatternTerminal() {
        AEApi.instance().registries().wireless().registerWirelessHandler(this);
        this.setFeature(EnumSet.of(AEFeature.WirelessAccessTerminal, AEFeature.PoweredTools));
        setUnlocalizedName(NameConst.ITEM_WIRELESS_FLUID_PATTERN_TERMINAL);
        setTextureName(FluidCraft.resource(NameConst.ITEM_WIRELESS_FLUID_PATTERN_TERMINAL).toString());
    }

    @Override
    public ItemWirelessPatternTerminal register() {
        GameRegistry.registerItem(this, NameConst.ITEM_WIRELESS_FLUID_PATTERN_TERMINAL, FluidCraft.MODID);
        setCreativeTab(FluidCraftingTabs.INSTANCE);
        return this;
    }

    @Nullable
    @Override
    protected IUltraTerminalType getTerminalType(ItemStack is) {
        return findTerminalType(TerminalIds.AE2FC_PATTERN);
    }
}
