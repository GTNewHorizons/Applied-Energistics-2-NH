package appeng.core.sync.packets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import appeng.api.util.AEColor;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.network.NetworkHandler;
import appeng.helpers.ControllerPlacementChecker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketControllerPlacementQuery extends AppEngPacket {

    public static final byte KIND_CONTROLLER = 0;
    public static final byte KIND_CABLE = 1;

    private final int x;
    private final int y;
    private final int z;
    private final byte kind;
    private final int color;

    // automatic.
    public PacketControllerPlacementQuery(final ByteBuf stream) {
        this.x = stream.readInt();
        this.y = stream.readInt();
        this.z = stream.readInt();
        this.kind = stream.readByte();
        this.color = stream.readInt();
    }

    // api
    public PacketControllerPlacementQuery(final int x, final int y, final int z, final byte kind, final int color) {
        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());
        data.writeInt(this.x = x);
        data.writeInt(this.y = y);
        data.writeInt(this.z = z);
        data.writeByte(this.kind = kind);
        data.writeInt(this.color = color);

        this.configureWrite(data);
    }

    @Override
    public void serverPacketData(final INetworkInfo manager, final AppEngPacket packet, final EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP playerMP)) return;

        final double dx = player.posX - this.x;
        final double dy = player.posY - this.y;
        final double dz = player.posZ - this.z;
        if (dx * dx + dy * dy + dz * dz > 64 * 64) return;

        final World w = player.worldObj;
        if (!w.blockExists(this.x, this.y, this.z)) return;

        final boolean conflict;
        if (this.kind == KIND_CABLE) {
            final AEColor[] colors = AEColor.values();
            if (this.color < 0 || this.color >= colors.length) return;
            conflict = ControllerPlacementChecker.wouldCableConflict(w, this.x, this.y, this.z, colors[this.color]);
        } else {
            conflict = ControllerPlacementChecker.wouldControllerConflict(w, this.x, this.y, this.z);
        }
        NetworkHandler.instance
                .sendTo(new PacketControllerPlacementResult(this.x, this.y, this.z, this.kind, conflict), playerMP);
    }
}
