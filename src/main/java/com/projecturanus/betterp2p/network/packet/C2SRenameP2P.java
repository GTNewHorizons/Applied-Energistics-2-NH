package com.projecturanus.betterp2p.network.packet;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.network.ModNetwork;
import com.projecturanus.betterp2p.network.data.GridServerCache;
import com.projecturanus.betterp2p.network.data.P2PLocation;

import appeng.api.networking.IGridHost;
import appeng.api.parts.IPartHost;
import appeng.me.GridAccessException;
import appeng.parts.p2p.PartP2PTunnel;
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
            World world = DimensionManager.getWorld(loc.dim);
            if (cache == null || world == null) {
                return null;
            }
            TileEntity te = world.getTileEntity(loc.x, loc.y, loc.z);
            if (!(te instanceof IGridHost gridHost) || gridHost.getGridNode(loc.facing) == null
                    || !(te instanceof IPartHost partHost)
                    || !(partHost.getPart(loc.facing) instanceof PartP2PTunnel<?>tunnel)) {
                return null;
            }

            tunnel.setCustomName(message.name);
            PartP2PTunnel<?> input = tunnel.isOutput() ? tunnel.getInput() : tunnel;
            if (input == null) {
                input = tunnel;
            }
            try {
                for (PartP2PTunnel<?> output : input.getOutputs()) {
                    cache.markDirty(P2PLocation.of(output), output);
                }
            } catch (GridAccessException ignored) {}
            cache.markDirty(P2PLocation.of(input), input);

            ModNetwork.requestP2PUpdate(player);
            return null;
        }
    }
}
