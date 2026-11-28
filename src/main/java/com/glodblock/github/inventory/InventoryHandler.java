package com.glodblock.github.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.inventory.gui.GuiType;
import com.glodblock.github.util.BlockPos;

import appeng.client.gui.AEBaseGui;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class InventoryHandler implements IGuiHandler {

    private static final int GUI_ID_TYPE_SHIFT = 4;
    private static final int GUI_ID_SUB_GUI = 1 << 3;

    public static void openGui(EntityPlayer player, World world, BlockPos pos, ForgeDirection face, Object guiType) {
        openGui(player, world, pos, face, guiType, false);
    }

    /**
     * @param subGui whether the GUI is opened by moving between a primary GUI and its sub-GUIs, see
     *               {@link AEBaseGui#isSubGui()}
     */
    public static void openGui(EntityPlayer player, World world, BlockPos pos, ForgeDirection face, Object guiType,
            boolean subGui) {
        if (Platform.isClient()) {
            return;
        }

        if (guiType instanceof GuiType gt) {
            player.openGui(
                    FluidCraft.INSTANCE,
                    gt.ordinal() << GUI_ID_TYPE_SHIFT | (subGui ? GUI_ID_SUB_GUI : 0) | face.ordinal(),
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ());
        } else if (guiType instanceof GuiBridge gb) {
            Platform.openGUI(
                    player,
                    world.getTileEntity(pos.getX(), pos.getY(), pos.getZ()),
                    face,
                    gb,
                    Integer.MIN_VALUE,
                    subGui);
        }
    }

    @Nullable
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        int faceOrd = id & 0x7;
        if (faceOrd > ForgeDirection.values().length) {
            return null;
        }
        ForgeDirection face = ForgeDirection.getOrientation(faceOrd);
        GuiType type = GuiType.getByOrdinal(id >>> GUI_ID_TYPE_SHIFT);
        return type != null ? type.guiFactory.createServerGui(player, world, x, y, z, face) : null;
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        int faceOrd = id & 0x7;
        if (faceOrd > ForgeDirection.values().length) {
            return null;
        }
        ForgeDirection face = ForgeDirection.getOrientation(faceOrd);
        GuiType type = GuiType.getByOrdinal(id >>> GUI_ID_TYPE_SHIFT);
        if (type == null) {
            return null;
        }

        final Object gui = type.guiFactory.createClientGui(player, world, x, y, z, face);
        if ((id & GUI_ID_SUB_GUI) != 0 && gui instanceof AEBaseGui aeGui) {
            aeGui.setSubGui(true);
        }
        return gui;
    }
}
