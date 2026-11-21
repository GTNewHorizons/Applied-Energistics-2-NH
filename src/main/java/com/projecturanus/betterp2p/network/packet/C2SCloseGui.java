package com.projecturanus.betterp2p.network.packet;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.network.ModNetwork;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SCloseGui implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<C2SCloseGui, IMessage> {

        @Override
        public @Nullable IMessage onMessage(C2SCloseGui message, MessageContext ctx) {
            ModNetwork.removeConnection(ctx.getServerHandler().playerEntity);
            return null;
        }
    }
}
