package com.projecturanus.betterp2p.util.p2p;

import net.minecraft.item.ItemStack;

import appeng.parts.p2p.PartP2PTunnel;

public class TunnelInfo {

    public final int index;
    public final ItemStack stack;
    public final Class<? extends PartP2PTunnel<?>> clazz;
    public final String dispName;

    public TunnelInfo(int index, ItemStack stack, Class<? extends PartP2PTunnel<?>> clazz) {
        this.index = index;
        this.stack = stack;
        this.clazz = clazz;
        String[] parts = stack.getItem().getItemStackDisplayName(stack).split(" - ", -1);
        this.dispName = parts.length > 1 ? parts[1] : "§c<Unknown P2P Type>";
    }
}
