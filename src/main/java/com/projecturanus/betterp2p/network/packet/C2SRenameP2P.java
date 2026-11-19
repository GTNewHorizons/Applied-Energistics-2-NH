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

public class C2SRenameP2P implements IMessage {

    private @Nullable P2PLocation p2p;
    private String name = "";

    public C2SRenameP2P() {}

    public C2SRenameP2P(P2PLocation p2p, String name) {
        this.p2p = p2p;
        this.name = name;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        p2p = P2PLocation.read(buf);
        int length = buf.readInt();
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            nameBuilder.append(buf.readChar());
        }
        name = nameBuilder.toString();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        p2p.write(buf);
        buf.writeInt(name.length());
        for (int i = 0; i < name.length(); i++) {
            buf.writeChar(name.charAt(i));
        }
    }

    public static class Handler implements IMessageHandler<C2SRenameP2P, IMessage> {

        @Override
        public @Nullable IMessage onMessage(C2SRenameP2P message, MessageContext ctx) {
            P2PLocation loc = message.p2p;
            if (loc == null) {
                return null;
            }
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            GridServerCache cache = ModNetwork.getGridCache(player);
            if (cache != null && cache.renameP2P(loc, message.name)) {
                ModNetwork.requestP2PUpdate(player);
            }
            return null;
        }
    }
}
