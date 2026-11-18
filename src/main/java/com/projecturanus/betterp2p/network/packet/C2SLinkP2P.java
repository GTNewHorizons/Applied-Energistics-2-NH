package com.projecturanus.betterp2p.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.network.ModNetwork;
import com.projecturanus.betterp2p.network.data.GridServerCache;
import com.projecturanus.betterp2p.network.data.P2PLocation;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SLinkP2P implements IMessage {

    private @Nullable P2PLocation input;
    private @Nullable P2PLocation output;

    public C2SLinkP2P() {}

    public C2SLinkP2P(P2PLocation input, P2PLocation output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        input = P2PLocation.read(buf);
        output = P2PLocation.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        input.write(buf);
        output.write(buf);
    }

    public static class Handler implements IMessageHandler<C2SLinkP2P, IMessage> {

        @Override
        public @Nullable IMessage onMessage(C2SLinkP2P message, MessageContext ctx) {
            if (message.input == null || message.output == null) {
                return null;
            }
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            GridServerCache cache = ModNetwork.getGridCache(player);
            if (cache != null && cache.linkP2P(message.input, message.output)) {
                ModNetwork.requestP2PUpdate(player);
            }
            return null;
        }
    }
}
