package appeng.core.sync.packets;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.util.DimensionalCoord;
import appeng.client.render.highlighter.ChannelAlertHighlighter;
import appeng.core.localization.PlayerMessages;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** A bounded, server-to-client batch; only the player who placed the new device receives it. */
public class PacketChannelAlert extends AppEngPacket {

    public static final int MAX_TARGETS = 64;
    private static final int MAX_CHAT_MESSAGES = 8;
    private final List<Target> targets;
    private final int total;

    public PacketChannelAlert(ByteBuf stream) {
        total = stream.readInt();
        final int size = stream.readInt();
        if (size < 0 || size > MAX_TARGETS || total < size) {
            throw new IllegalArgumentException("Invalid channel alert size");
        }
        targets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            final DimensionalCoord location = DimensionalCoord.readFromPacket(stream);
            final int side = stream.readUnsignedByte();
            if (side > ForgeDirection.UNKNOWN.ordinal()) {
                throw new IllegalArgumentException("Invalid channel alert side");
            }
            targets.add(new Target(location, ForgeDirection.getOrientation(side), ByteBufUtils.readItemStack(stream)));
        }
    }

    public PacketChannelAlert(List<Target> targets) {
        total = targets.size();
        this.targets = new ArrayList<>(targets.subList(0, Math.min(total, MAX_TARGETS)));
        final ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(getPacketID());
        buffer.writeInt(total);
        buffer.writeInt(this.targets.size());
        for (Target target : this.targets) {
            target.location.writeToPacket(buffer);
            buffer.writeByte(target.side.ordinal());
            ByteBufUtils.writeItemStack(buffer, target.icon);
        }
        configureWrite(buffer);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void clientPacketData(INetworkInfo network, AppEngPacket packet, EntityPlayer player) {
        if (player == null) return;
        Minecraft.getMinecraft().func_152344_a(() -> showAlert(player));
    }

    @SideOnly(Side.CLIENT)
    private void showAlert(EntityPlayer player) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != player || mc.theWorld != player.worldObj) return;
        final int shown = Math.min(targets.size(), MAX_CHAT_MESSAGES);
        for (int i = 0; i < shown; i++) {
            final Target target = targets.get(i);
            final IChatComponent name = target.icon == null
                    ? new ChatComponentTranslation(PlayerMessages.ChannelDevice.getUnlocalized())
                    : target.icon.func_151000_E();
            if (target.side == ForgeDirection.UNKNOWN) {
                player.addChatMessage(
                        PlayerMessages.ChannelLost.toChat(
                                name,
                                target.location.x,
                                target.location.y,
                                target.location.z,
                                target.location.getDimension()));
            } else {
                player.addChatMessage(
                        PlayerMessages.ChannelLostPart.toChat(
                                name,
                                new ChatComponentTranslation(
                                        "chat.appliedenergistics2.ChannelSide." + target.side.name()),
                                target.location.x,
                                target.location.y,
                                target.location.z,
                                target.location.getDimension()));
            }
        }
        if (total > shown) {
            player.addChatMessage(PlayerMessages.ChannelLostMore.toChat(total - shown));
        }
        ChannelAlertHighlighter.highlight(targets);
    }

    public static final class Target {

        public final DimensionalCoord location;
        public final ForgeDirection side;
        public final ItemStack icon;

        public Target(DimensionalCoord location, ForgeDirection side, ItemStack icon) {
            this.location = location;
            this.side = side == null ? ForgeDirection.UNKNOWN : side;
            this.icon = icon;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Target)) return false;
            final Target other = (Target) obj;
            return location.equals(other.location) && side == other.side;
        }

        @Override
        public int hashCode() {
            return 31 * location.hashCode() + side.ordinal();
        }
    }
}
