package com.projecturanus.betterp2p.network.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.jspecify.annotations.Nullable;

import appeng.api.util.DimensionalCoord;
import appeng.parts.p2p.PartP2PTunnel;
import io.netty.buffer.ByteBuf;

public final class P2PLocation {

    public final int x;
    public final int y;
    public final int z;
    public final ForgeDirection facing;
    public final int dim;

    public P2PLocation(int x, int y, int z, ForgeDirection facing, int dim) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.facing = facing;
        this.dim = dim;
    }

    public static P2PLocation of(PartP2PTunnel<?> tunnel) {
        DimensionalCoord location = tunnel.getLocation();
        return new P2PLocation(location.x, location.y, location.z, tunnel.getSide(), location.getDimension());
    }

    public void write(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeByte(facing.ordinal());
        buf.writeInt(dim);
    }

    public static @Nullable P2PLocation read(ByteBuf buf) {
        try {
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            ForgeDirection facing = ForgeDirection.values()[buf.readByte()];
            int dim = buf.readInt();
            return new P2PLocation(x, y, z, facing, dim);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static NBTTagCompound writeToNBT(@Nullable P2PLocation loc) {
        NBTTagCompound nbt = new NBTTagCompound();
        if (loc != null) {
            nbt.setInteger("x", loc.x);
            nbt.setInteger("y", loc.y);
            nbt.setInteger("z", loc.z);
            nbt.setByte("f", (byte) loc.facing.ordinal());
            nbt.setInteger("d", loc.dim);
        }
        return nbt;
    }

    public static @Nullable P2PLocation readFromNBT(NBTTagCompound tag) {
        try {
            return new P2PLocation(
                    tag.getInteger("x"),
                    tag.getInteger("y"),
                    tag.getInteger("z"),
                    ForgeDirection.values()[tag.getByte("f")],
                    tag.getInteger("d"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int hashCode() {
        return Long.hashCode(HashHelper.hashP2P(x, y, z, facing.ordinal(), dim));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof P2PLocation other)) return false;
        return x == other.x && y == other.y && z == other.z && facing == other.facing && dim == other.dim;
    }

    @Override
    public String toString() {
        return "P2PLocation(x=" + x + ", y=" + y + ", z=" + z + ", facing=" + facing + ", dim=" + dim + ")";
    }
}
