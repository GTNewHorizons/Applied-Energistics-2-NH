package com.glodblock.github.common.block;

import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.glodblock.github.common.item.FCBaseItemBlock;
import com.glodblock.github.common.tabs.FluidCraftingTabs;
import com.glodblock.github.common.tile.TileFluidPacketDecoder;
import com.glodblock.github.inventory.InventoryHandler;
import com.glodblock.github.inventory.gui.GuiType;
import com.glodblock.github.util.BlockPos;
import com.glodblock.github.util.NameConst;
import com.glodblock.github.util.RenderUtil;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockFluidPacketDecoder extends FCBaseBlock {

    public BlockFluidPacketDecoder() {
        super(Material.iron, NameConst.BLOCK_FLUID_PACKET_DECODER);
        setFullBlock(true);
        setOpaque(true);
        setTileEntity(TileFluidPacketDecoder.class);
    }

    @Override
    public boolean onActivated(World world, int x, int y, int z, EntityPlayer player, int facing, float hitX,
            float hitY, float hitZ) {
        if (player.isSneaking()) {
            return false;
        }
        TileFluidPacketDecoder tile = getTileEntity(world, x, y, z);
        if (tile != null) {
            if (!world.isRemote) {
                InventoryHandler.openGui(
                        player,
                        world,
                        new BlockPos(x, y, z),
                        ForgeDirection.getOrientation(facing),
                        GuiType.FLUID_PACKET_DECODER);
            }
            return true;
        }
        return false;
    }

    @Override
    public BlockFluidPacketDecoder register() {
        GameRegistry.registerBlock(this, FCBaseItemBlock.class, NameConst.BLOCK_FLUID_PACKET_DECODER);
        GameRegistry.registerTileEntity(TileFluidPacketDecoder.class, NameConst.BLOCK_FLUID_PACKET_DECODER);
        setCreativeTab(FluidCraftingTabs.INSTANCE);
        return this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(final ItemStack itemStack, final EntityPlayer player, final List<String> toolTip,
            final boolean advancedToolTips) {
        if (isShiftKeyDown()) {
            toolTip.addAll(
                    RenderUtil.listFormattedStringToWidth(NameConst.i18n(NameConst.TT_FLUID_PACKET_DECODER_DESC)));
        } else {
            toolTip.add(NameConst.i18n(NameConst.TT_SHIFT_FOR_MORE));
        }
    }
}
