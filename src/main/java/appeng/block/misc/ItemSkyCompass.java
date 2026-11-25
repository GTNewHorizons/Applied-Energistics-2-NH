/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.block.misc;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import appeng.block.AEBaseItemBlock;

public class ItemSkyCompass extends AEBaseItemBlock {

    public ItemSkyCompass(final Block block) {
        super(block);
    }

    @Override
    public ItemStack onItemRightClick(final ItemStack stack, final World world, final EntityPlayer player) {
        if (player.isSneaking() && !world.isRemote) {
            toggleMode(stack, player);
        }
        return stack;
    }

    @Override
    public boolean onItemUseFirst(final ItemStack stack, final EntityPlayer player, final World world, final int x,
            final int y, final int z, final int side, final float hitX, final float hitY, final float hitZ) {
        if (!player.isSneaking() || world.isRemote) {
            return false;
        }
        toggleMode(stack, player);
        return true;
    }

    @Override
    public boolean func_150936_a(final World world, final int x, final int y, final int z, final int side,
            final EntityPlayer player, final ItemStack stack) {
        return player.isSneaking() || super.func_150936_a(world, x, y, z, side, player, stack);
    }

    @Override
    public boolean onItemUse(final ItemStack stack, final EntityPlayer player, final World world, final int x,
            final int y, final int z, final int side, final float hitX, final float hitY, final float hitZ) {
        return player.isSneaking() || super.onItemUse(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
    }

    private static void toggleMode(final ItemStack stack, final EntityPlayer player) {
        final int mode = stack.getItemDamage() == BlockSkyCompass.CHEST_MODE ? BlockSkyCompass.SKY_STONE_MODE
                : BlockSkyCompass.CHEST_MODE;
        stack.setItemDamage(mode);
        player.addChatMessage(
                new ChatComponentTranslation(
                        "gui.appliedenergistics2.CompassMode",
                        new ChatComponentTranslation(BlockSkyCompass.modeKey(mode))));
    }
}
