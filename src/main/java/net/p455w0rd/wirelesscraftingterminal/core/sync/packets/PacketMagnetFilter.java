package net.p455w0rd.wirelesscraftingterminal.core.sync.packets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.p455w0rd.wirelesscraftingterminal.common.utils.RandomUtils;
import net.p455w0rd.wirelesscraftingterminal.core.sync.WCTPacket;
import net.p455w0rd.wirelesscraftingterminal.core.sync.network.INetworkInfo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketMagnetFilter extends WCTPacket {

    // 0 = initialize
    // 1 = whitelist/blacklist
    // 2 = ignore NBT
    // 3 = ignore meta
    // 4 = use oredict
    int whichMode;
    boolean modeValue;

    public PacketMagnetFilter(final ByteBuf stream) {
        this.whichMode = stream.readInt();
        this.modeValue = stream.readBoolean();
    }

    public PacketMagnetFilter(final int mode, final boolean modeVal) {
        this.modeValue = modeVal;
        this.whichMode = mode;
        final ByteBuf data = Unpooled.buffer();
        data.writeInt(this.getPacketID());
        data.writeInt(this.whichMode);
        data.writeBoolean(this.modeValue);
        this.configureWrite(data);
    }

    @Override
    public void serverPacketData(final INetworkInfo manager, final WCTPacket packet, final EntityPlayer player) {
        final ItemStack magnetItem = RandomUtils.getMagnet(player.inventory);

        if (magnetItem != null) {
            if (!magnetItem.hasTagCompound()) {
                magnetItem.setTagCompound(new NBTTagCompound());
            }
            final NBTTagCompound tag = magnetItem.getTagCompound();
            if (this.whichMode == 0) {
                tag.setBoolean("Initialized", this.modeValue);
            }
            final String key = switch (this.whichMode) {
                case 1 -> "Whitelisting";
                case 2 -> "IgnoreNBT";
                case 3 -> "IgnoreMeta";
                case 4 -> "UseOreDict";
                default -> null;
            };
            if (key == null) {
                return;
            }
            tag.setBoolean(key, this.modeValue);
        }
    }
}
