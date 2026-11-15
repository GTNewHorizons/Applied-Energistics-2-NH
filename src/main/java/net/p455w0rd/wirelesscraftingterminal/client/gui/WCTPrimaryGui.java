package net.p455w0rd.wirelesscraftingterminal.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.p455w0rd.wirelesscraftingterminal.common.WCTGuiHandler;

import org.jspecify.annotations.Nullable;

import appeng.container.PrimaryGui;

public class WCTPrimaryGui extends PrimaryGui {

    public WCTPrimaryGui(final int gui, final @Nullable ItemStack guiIcon, final @Nullable TileEntity te,
            final @Nullable ForgeDirection side) {
        super(gui, guiIcon, te, side);
    }

    @Override
    public void open(final EntityPlayer player) {
        WCTGuiHandler.launchGui(
                (int) this.gui,
                player,
                player.worldObj,
                (int) player.posX,
                (int) player.posY,
                (int) player.posZ);
    }
}
