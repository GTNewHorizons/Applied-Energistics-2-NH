package appeng.api.features;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/**
 * Opens the gui of a {@link IUltraTerminalType}. Called on the server side only, implementations have to open the gui
 * on both sides (usually through {@link EntityPlayer#openGui}).
 */
public interface IUltraTerminalGuiOpener {

    void open(EntityPlayer player, ItemStack terminal, int slotIndex, int terminalIndex);
}
