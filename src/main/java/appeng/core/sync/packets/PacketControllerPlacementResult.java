package appeng.core.sync.packets;

import net.minecraft.entity.player.EntityPlayer;

import appeng.client.render.previewBlocks.ControllerPlacementPreview;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketControllerPlacementResult extends AppEngPacket {

    private final int x;
    private final int y;
    private final int z;
    private final byte kind;
    private final boolean conflict;

    // automatic.
    public PacketControllerPlacementResult(final ByteBuf stream) {
        this.x = stream.readInt();
        this.y = stream.readInt();
        this.z = stream.readInt();
        this.kind = stream.readByte();
        this.conflict = stream.readBoolean();
    }

    // api
    public PacketControllerPlacementResult(final int x, final int y, final int z, final byte kind,
            final boolean conflict) {
        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());
        data.writeInt(this.x = x);
        data.writeInt(this.y = y);
        data.writeInt(this.z = z);
        data.writeByte(this.kind = kind);
        data.writeBoolean(this.conflict = conflict);

        this.configureWrite(data);
    }

    @Override
    public void clientPacketData(final INetworkInfo network, final AppEngPacket packet, final EntityPlayer player) {
        ControllerPlacementPreview.getInstance().setResult(this.x, this.y, this.z, this.kind, this.conflict);
    }
}
