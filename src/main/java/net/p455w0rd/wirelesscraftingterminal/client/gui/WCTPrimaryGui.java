package net.p455w0rd.wirelesscraftingterminal.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.p455w0rd.wirelesscraftingterminal.common.WCTGuiHandler;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import appeng.container.PrimaryGui;

public class WCTPrimaryGui extends PrimaryGui {

    public WCTPrimaryGui(Object gui, ItemStack guiIcon, TileEntity te, ForgeDirection side) {
        super(gui, guiIcon, te, side);
    }

    @Override
    public void open(EntityPlayer player) {
        WCTGuiHandler.launchGui(
                (int) this.gui,
                player,
                player.worldObj,
                (int) this.gui == Reference.GUI_ULTRA_WCT ? this.slotIndex : (int) player.posX,
                (int) player.posY,
                (int) player.posZ);
    }
}
