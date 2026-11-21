package com.projecturanus.betterp2p.util.p2p;

import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import appeng.parts.p2p.PartP2PTunnel;

public class ClientTunnelInfo extends TunnelInfo {

    // Textures are not loaded until after postInit, so the icon has to be resolved lazily.
    private final Supplier<IIcon> icon;

    public ClientTunnelInfo(int index, ItemStack stack, Class<? extends PartP2PTunnel<?>> clazz, Supplier<IIcon> icon) {
        super(index, stack, clazz);
        this.icon = icon;
    }

    public IIcon getIcon() {
        return icon.get();
    }
}
