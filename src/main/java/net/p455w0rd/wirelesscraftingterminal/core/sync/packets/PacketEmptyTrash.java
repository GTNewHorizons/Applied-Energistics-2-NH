package net.p455w0rd.wirelesscraftingterminal.core.sync.packets;

import net.minecraft.entity.player.EntityPlayer;
import net.p455w0rd.wirelesscraftingterminal.common.container.ContainerWirelessCraftingTerminal;
import net.p455w0rd.wirelesscraftingterminal.core.sync.WCTPacket;
import net.p455w0rd.wirelesscraftingterminal.core.sync.network.INetworkInfo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketEmptyTrash extends WCTPacket {

    public PacketEmptyTrash(final ByteBuf stream) {}

    // api
    public PacketEmptyTrash() {
        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());
        this.configureWrite(data);
    }

    @Override
    public void serverPacketData(final INetworkInfo manager, final WCTPacket packet, final EntityPlayer player) {
        if (player.openContainer instanceof ContainerWirelessCraftingTerminal container) {
            container.trashSlot.clearStack();
        }
    }

    @Override
    public void clientPacketData(final INetworkInfo network, final WCTPacket packet, final EntityPlayer player) {}
}
