package appeng.helpers;

import java.nio.charset.StandardCharsets;

import net.minecraft.item.ItemStack;

import com.github.bsideup.jabel.Desugar;

import appeng.api.util.DimensionalCoord;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

@Desugar
public record CraftingProviderLocation(DimensionalCoord position, String rawName, String suffix, ItemStack displayItem,
        boolean customName) {

    private static final int MAX_UTF8_BYTES = (1 << 14) - 1;

    public void writeToPacket(ByteBuf buffer) {
        position.writeToPacket(buffer);
        writeOptionalString(buffer, rawName);
        writeOptionalString(buffer, suffix);
        ByteBufUtils.writeItemStack(buffer, displayItem);
        buffer.writeBoolean(customName);
    }

    private static void writeOptionalString(ByteBuf buffer, String value) {
        ByteBufUtils.writeUTF8String(
                buffer,
                value != null && value.getBytes(StandardCharsets.UTF_8).length <= MAX_UTF8_BYTES ? value : "");
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
