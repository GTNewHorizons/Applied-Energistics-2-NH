package com.projecturanus.betterp2p.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.BetterP2P;
import com.projecturanus.betterp2p.network.ModNetwork;
import com.projecturanus.betterp2p.network.data.GridServerCache;
import com.projecturanus.betterp2p.network.data.MemoryInfo;
import com.projecturanus.betterp2p.network.data.P2PLocation;
import com.projecturanus.betterp2p.util.p2p.TunnelInfo;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2STypeChange implements IMessage {

    private int newType = MemoryInfo.TUNNEL_ANY;
    private @Nullable P2PLocation p2p;

    public C2STypeChange() {}

    public C2STypeChange(int newType, P2PLocation p2p) {
        this.newType = newType;
        this.p2p = p2p;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        newType = buf.readByte();
        p2p = P2PLocation.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(newType);
        p2p.write(buf);
    }

    public static class Handler implements IMessageHandler<C2STypeChange, IMessage> {

        @Override
        public @Nullable IMessage onMessage(C2STypeChange message, MessageContext ctx) {
            if (message.p2p == null) {
                return null;
            }
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            GridServerCache cache = ModNetwork.getGridCache(player);
            TunnelInfo type = BetterP2P.proxy.getP2PFromIndex(message.newType);
            if (cache != null && type != null && cache.changeAllP2Ps(message.p2p, type)) {
                ModNetwork.requestP2PList(player, type.index);
            }
            return null;
        }
    }
}
