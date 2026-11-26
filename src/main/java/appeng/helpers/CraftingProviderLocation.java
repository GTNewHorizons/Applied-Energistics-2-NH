package appeng.helpers;

import net.minecraft.item.ItemStack;

import com.github.bsideup.jabel.Desugar;

import appeng.api.util.DimensionalCoord;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

@Desugar
public record CraftingProviderLocation(DimensionalCoord position, String rawName, String suffix, ItemStack displayItem,
        boolean customName) {

    public void writeToPacket(ByteBuf buffer) {
        position.writeToPacket(buffer);
        ByteBufUtils.writeUTF8String(buffer, rawName);
        ByteBufUtils.writeUTF8String(buffer, suffix);
        ByteBufUtils.writeItemStack(buffer, displayItem);
        buffer.writeBoolean(customName);
    }

    public static CraftingProviderLocation readFromPacket(ByteBuf buffer) {
        return new CraftingProviderLocation(
                DimensionalCoord.readFromPacket(buffer),
                ByteBufUtils.readUTF8String(buffer),
                ByteBufUtils.readUTF8String(buffer),
                ByteBufUtils.readItemStack(buffer),
                buffer.readBoolean());
    }
}
