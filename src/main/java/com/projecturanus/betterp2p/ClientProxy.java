package com.projecturanus.betterp2p;

import java.util.function.Function;

import net.minecraft.item.ItemStack;

import com.projecturanus.betterp2p.client.render.RenderHandler;
import com.projecturanus.betterp2p.util.p2p.ClientTunnelInfo;
import com.projecturanus.betterp2p.util.p2p.TunnelInfo;

import appeng.parts.p2p.PartP2PTunnel;

public class ClientProxy extends CommonProxy {

    @Override
    public void postInit() {
        RenderHandler.register();
        super.postInit();
    }

    @Override
    protected TunnelInfo createTunnelInfo(int type, ItemStack stack, Class<? extends PartP2PTunnel<?>> clazz,
            Function<ItemStack, ? extends PartP2PTunnel<?>> factory) {
        return new ClientTunnelInfo(type, stack, clazz, () -> factory.apply(stack).getTypeTexture());
    }
}
