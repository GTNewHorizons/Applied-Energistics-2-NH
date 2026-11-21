package com.projecturanus.betterp2p.network.packet;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.network.ModNetwork;
import com.projecturanus.betterp2p.network.data.MemoryInfo;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SRefreshP2PList implements IMessage {

    private int type = MemoryInfo.TUNNEL_ANY;

    public C2SRefreshP2PList() {}

    public C2SRefreshP2PList(int type) {
        this.type = type;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
    }

    public static class Handler implements IMessageHandler<C2SRefreshP2PList, IMessage> {

        @Override
        public @Nullable IMessage onMessage(C2SRefreshP2PList message, MessageContext ctx) {
            ModNetwork.requestP2PList(ctx.getServerHandler().playerEntity, message.type);
            return null;
        }
    }
}
