package appeng.core.sync.packets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import appeng.api.util.DimensionalCoord;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketChannelAlertTest {

    @Test
    public void readsLocationSideAndNullItemWithoutLeavingBytes() {
        final ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeInt(1);
            buffer.writeInt(1);
            new DimensionalCoord(123, 64, -321, 0).writeToPacket(buffer);
            buffer.writeByte(ForgeDirection.NORTH.ordinal());
            buffer.writeShort(-1); // ByteBufUtils' null ItemStack encoding.
            new PacketChannelAlert(buffer);
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnboundedTargetCount() {
        readInvalidHeader(PacketChannelAlert.MAX_TARGETS + 1, PacketChannelAlert.MAX_TARGETS + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeTargetCount() {
        readInvalidHeader(0, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInconsistentTotal() {
        readInvalidHeader(1, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidSide() {
        final ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeInt(1);
            buffer.writeInt(1);
            new DimensionalCoord(0, 64, 0, 0).writeToPacket(buffer);
            buffer.writeByte(7);
            new PacketChannelAlert(buffer);
        } finally {
            buffer.release();
        }
    }

    @Test
    public void differentPartsAtTheSameBlockAreDistinctTargets() {
        final PacketChannelAlert.Target north = new PacketChannelAlert.Target(
                new DimensionalCoord(1, 64, 2, 0),
                ForgeDirection.NORTH,
                null);
        final PacketChannelAlert.Target same = new PacketChannelAlert.Target(
                new DimensionalCoord(1, 64, 2, 0),
                ForgeDirection.NORTH,
                null);
        final PacketChannelAlert.Target south = new PacketChannelAlert.Target(
                new DimensionalCoord(1, 64, 2, 0),
                ForgeDirection.SOUTH,
                null);
        assertEquals(north, same);
        assertEquals(north.hashCode(), same.hashCode());
        assertNotEquals(north, south);
    }

    private void readInvalidHeader(int total, int count) {
        final ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeInt(total);
            buffer.writeInt(count);
            new PacketChannelAlert(buffer);
        } finally {
            buffer.release();
        }
    }
}
