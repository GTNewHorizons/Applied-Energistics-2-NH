package com.projecturanus.betterp2p.network.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.util.p2p.P2PUtil;

import appeng.api.networking.IGridNode;
import appeng.api.util.DimensionalCoord;
import appeng.me.GridNode;
import appeng.parts.p2p.PartP2PTunnel;
import io.netty.buffer.ByteBuf;

public final class P2PInfo {

    public final long frequency;
    public final int posX;
    public final int posY;
    public final int posZ;
    public final int dim;
    public final ForgeDirection facing;
    public final String name;
    public final boolean output;
    public final boolean hasChannel;
    /** Number of used channels for ME P2P tunnels, -1 for other types. */
    public final int channels;
    public final int type;

    public P2PInfo(long frequency, int posX, int posY, int posZ, int dim, ForgeDirection facing, String name,
            boolean output, boolean hasChannel, int channels, int type) {
        this.frequency = frequency;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.dim = dim;
        this.facing = facing;
        this.name = name;
        this.output = output;
        this.hasChannel = hasChannel;
        this.channels = channels;
        this.type = type;
    }

    public static P2PInfo of(PartP2PTunnel<?> tunnel) {
        DimensionalCoord location = tunnel.getLocation();
        IGridNode node = tunnel.getExternalFacingNode();
        return new P2PInfo(
                tunnel.getFrequency(),
                location.x,
                location.y,
                location.z,
                location.getDimension(),
                tunnel.getSide(),
                tunnel.getCustomName(),
                tunnel.isOutput(),
                P2PUtil.hasChannel(tunnel),
                node instanceof GridNode gridNode ? gridNode.usedChannels() : -1,
                P2PUtil.getTypeIndex(tunnel));
    }

    public static @Nullable P2PInfo read(ByteBuf buf) {
        try {
            long freq = buf.readLong();
            int posX = buf.readInt();
            int posY = buf.readInt();
            int posZ = buf.readInt();
            int dim = buf.readInt();
            ForgeDirection facing = ForgeDirection.values()[buf.readInt()];
            int nameLength = buf.readShort();
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < nameLength; i++) {
                name.append(buf.readChar());
            }
            boolean output = buf.readBoolean();
            boolean hasChannel = buf.readBoolean();
            int channels = buf.readByte();
            int type = buf.readByte();
            return new P2PInfo(
                    freq,
                    posX,
                    posY,
                    posZ,
                    dim,
                    facing,
                    name.toString(),
                    output,
                    hasChannel,
                    channels,
                    type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void write(ByteBuf buf) {
        buf.writeLong(frequency);
        buf.writeInt(posX);
        buf.writeInt(posY);
        buf.writeInt(posZ);
        buf.writeInt(dim);
        buf.writeInt(facing.ordinal());
        buf.writeShort(name.length());
        for (int i = 0; i < name.length(); i++) {
            buf.writeChar(name.charAt(i));
        }
        buf.writeBoolean(output);
        buf.writeBoolean(hasChannel);
        buf.writeByte(channels);
        buf.writeByte(type);
    }

    public static List<P2PInfo> readList(ByteBuf buf) {
        int length = buf.readInt();
        List<P2PInfo> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            P2PInfo info = read(buf);
            if (info != null) {
                list.add(info);
            }
        }
        return list;
    }

    public static void writeList(ByteBuf buf, List<P2PInfo> infos) {
        buf.writeInt(infos.size());
        for (P2PInfo info : infos) {
            info.write(buf);
        }
    }

    @Override
    public int hashCode() {
        return Long.hashCode(HashHelper.hashP2P(posX, posY, posZ, facing.ordinal(), dim));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof P2PInfo other)) return false;
        return posX == other.posX && posY == other.posY
                && posZ == other.posZ
                && facing == other.facing
                && dim == other.dim;
    }
}
