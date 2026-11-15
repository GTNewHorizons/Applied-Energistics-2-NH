package appeng.core.sync.packets;

import net.minecraft.entity.player.EntityPlayer;

import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketOpenInterface extends AppEngPacket {

    private final long id;

    public PacketOpenInterface(final ByteBuf stream) {
        this.id = stream.readLong();
    }

    public PacketOpenInterface(final long id) {
        this.id = id;
        final ByteBuf data = Unpooled.buffer();
        data.writeInt(this.getPacketID());
        data.writeLong(id);
        this.configureWrite(data);
    }

    @Override
    public void serverPacketData(final INetworkInfo manager, final AppEngPacket packet, final EntityPlayer player) {
        if (player.openContainer instanceof ContainerInterfaceTerminal terminal) {
            terminal.openInterface(player, this.id);
        }
    }
}
