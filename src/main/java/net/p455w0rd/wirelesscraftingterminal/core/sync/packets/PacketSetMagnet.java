package net.p455w0rd.wirelesscraftingterminal.core.sync.packets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.p455w0rd.wirelesscraftingterminal.common.utils.RandomUtils;
import net.p455w0rd.wirelesscraftingterminal.core.sync.WCTPacket;
import net.p455w0rd.wirelesscraftingterminal.core.sync.network.INetworkInfo;
import net.p455w0rd.wirelesscraftingterminal.handlers.LocaleHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntObjectPair;

public class PacketSetMagnet extends WCTPacket {

    int magnetDamage;

    public PacketSetMagnet(final ByteBuf stream) {
        this.magnetDamage = stream.readInt();
    }

    // api
    public PacketSetMagnet(int itemDamage) {
        this.magnetDamage = itemDamage;
        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());
        data.writeInt(this.magnetDamage);
        this.configureWrite(data);
    }

    @Override
    public void serverPacketData(final INetworkInfo manager, final WCTPacket packet, final EntityPlayer player) {
        // One lookup only: the card installed in the terminal is a deserialized copy, so the mode has to be
        // written back through the terminal instead of through the returned stack.
        final IntObjectPair<ItemStack> terminal = RandomUtils.getWirelessTermWithMagnet(player.inventory);
        if (terminal == null) {
            return;
        }
        final ItemStack wirelessTerm = terminal.second();
        final ItemStack magnetItem = RandomUtils.getInstalledMagnet(wirelessTerm);
        if (magnetItem != null) {
            final int mode = magnetItem.getItemDamage();
            final int next = mode >= 2 ? 0 : mode + 1;
            if (!RandomUtils.setInstalledMagnetMode(wirelessTerm, next)) {
                return;
            }
            final LocaleHandler key = switch (next) {
                case 0 -> LocaleHandler.MagnetMode1;
                case 1 -> LocaleHandler.MagnetMode2;
                default -> LocaleHandler.MagnetMode3;
            };
            player.addChatMessage(new ChatComponentTranslation(key.getUnlocalized()));
        }
    }

    @Override
    public void clientPacketData(final INetworkInfo network, final WCTPacket packet, final EntityPlayer player) {

    }
}
