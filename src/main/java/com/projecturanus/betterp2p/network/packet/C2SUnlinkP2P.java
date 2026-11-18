package com.projecturanus.betterp2p.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.network.ModNetwork;
import com.projecturanus.betterp2p.network.data.GridServerCache;
import com.projecturanus.betterp2p.network.data.MemoryInfo;
import com.projecturanus.betterp2p.network.data.P2PLocation;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SUnlinkP2P implements IMessage {

    private @Nullable P2PLocation p2p;
    private int type = MemoryInfo.TUNNEL_ANY;

    public C2SUnlinkP2P() {}

    public C2SUnlinkP2P(P2PLocation p2p, int type) {
        this.p2p = p2p;
        this.type = type;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        p2p = P2PLocation.read(buf);
        type = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        p2p.write(buf);
        buf.writeByte(type);
    }

    public static class Handler implements IMessageHandler<C2SUnlinkP2P, IMessage> {

        @Override
        public @Nullable IMessage onMessage(C2SUnlinkP2P message, MessageContext ctx) {
            if (message.p2p == null) {
                return null;
            }
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            GridServerCache cache = ModNetwork.getGridCache(player);
            if (cache == null) {
                return null;
            }
            cache.unlinkP2P(message.p2p);
            ModNetwork.requestP2PUpdate(player);
            return null;
        }
    }
}
