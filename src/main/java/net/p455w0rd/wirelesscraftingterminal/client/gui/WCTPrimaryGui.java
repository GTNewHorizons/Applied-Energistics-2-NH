package net.p455w0rd.wirelesscraftingterminal.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.p455w0rd.wirelesscraftingterminal.common.WCTGuiHandler;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import org.jspecify.annotations.Nullable;

import appeng.container.PrimaryGui;

public class WCTPrimaryGui extends PrimaryGui {

    public WCTPrimaryGui(final @Nullable ItemStack guiIcon, final int slotIndex) {
        super(Reference.GUI_WCT, guiIcon, null, ForgeDirection.UNKNOWN);
        this.setSlotIndex(slotIndex);
    }

    @Override
    public void open(final EntityPlayer player) {
        WCTGuiHandler.openTerminalGui(player, this.slotIndex);
    }
}
