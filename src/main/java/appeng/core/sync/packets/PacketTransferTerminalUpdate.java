package appeng.core.sync.packets;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import appeng.api.features.transfer.TransferComponentInfo;
import appeng.client.gui.implementations.GuiTransferTerminal;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Sends the Transfer Terminal's rows to the client, split over several packets on large networks.
 */
public class PacketTransferTerminalUpdate extends AppEngPacket {

    private final boolean first;
    private final boolean last;
    private final List<TransferComponentInfo> rows;

    public PacketTransferTerminalUpdate(final ByteBuf stream) {
        this.first = stream.readBoolean();
        this.last = stream.readBoolean();
        final int count = stream.readInt();
        this.rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            this.rows.add(TransferComponentInfo.read(stream));
        }
    }

    /**
     * @param first whether this packet starts a new list
     * @param last  whether this packet completes the list
     * @param rows  rows already written with {@link TransferComponentInfo#write}
     */
    public PacketTransferTerminalUpdate(final boolean first, final boolean last, final List<byte[]> rows) {
        this.first = first;
        this.last = last;
        this.rows = new ArrayList<>();

        final ByteBuf data = Unpooled.buffer();
        data.writeInt(this.getPacketID());
        data.writeBoolean(first);
        data.writeBoolean(last);
        data.writeInt(rows.size());
        for (final byte[] row : rows) {
            data.writeBytes(row);
        }
        this.configureWrite(data);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void clientPacketData(final INetworkInfo network, final AppEngPacket packet, final EntityPlayer player) {
        final GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof GuiTransferTerminal gui) {
            gui.receiveRows(this.rows, this.first, this.last);
        }
    }
}
