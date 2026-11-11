package com.glodblock.github.common.item;

import static com.glodblock.github.loader.recipe.WirelessTerminalEnergyRecipe.getEnergyCard;
import static com.glodblock.github.loader.recipe.WirelessTerminalQuantumBridgeRecipe.getQuantumBridgeCard;
import static com.glodblock.github.util.Util.DimensionalCoordSide.hasEnergyCard;
import static com.glodblock.github.util.Util.hasInfinityBoosterCard;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.inventory.item.IItemInventory;
import com.glodblock.github.util.NameConst;
import com.glodblock.github.util.Util;

import appeng.api.AEApi;
import appeng.api.features.IUltraTerminalType;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.core.localization.PlayerMessages;
import appeng.items.tools.powered.ToolWirelessTerminal;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class ItemBaseWirelessTerminal extends ToolWirelessTerminal implements IItemInventory, IGuiItem {

    public static String infinityBoosterCard = "infinityBoosterCard";
    public static String infinityEnergyCard = "InfinityEnergyCard";

    @Override
    public ItemStack onItemRightClick(final ItemStack item, final World w, final EntityPlayer player) {
        if (player.isSneaking()) return removeQuantumBridgeCard(player, item); // todo: doesn't work in universal
                                                                               // terminal
        return super.onItemRightClick(item, w, player);
    }

    private ItemStack removeQuantumBridgeCard(final EntityPlayer player, ItemStack is) {
        if (hasInfinityBoosterCard(is)) {
            ItemStack quantumBridgeCardStack = getQuantumBridgeCard().copy();
            if (!player.inventory.addItemStackToInventory(quantumBridgeCardStack)) {
                player.entityDropItem(quantumBridgeCardStack, 0);
            }
            is.getTagCompound().setBoolean(infinityBoosterCard, false);
        }
        return is;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
            boolean displayMoreInfo) {
        super.addCheckedInformation(stack, player, lines, displayMoreInfo);
        if (GuiScreen.isCtrlKeyDown()) {
            lines.add(NameConst.i18n(NameConst.TT_WIRELESS_INSTALLED));
            if (hasInfinityBoosterCard(stack)) {
                lines.add("  " + EnumChatFormatting.GOLD + getQuantumBridgeCard().getDisplayName());
            }
            if (hasEnergyCard(stack)) {
                lines.add("  " + EnumChatFormatting.GOLD + getEnergyCard().getDisplayName());
            }
        } else {
            lines.add(NameConst.i18n(NameConst.TT_CTRL_FOR_MORE));
        }
    }

    @Override
    public boolean canHandle(final ItemStack is) {
        if (is == null) {
            return false;
        }
        return is.getItem() == this;
    }

    public ItemStack stack() {
        return new ItemStack(this, 1);
    }

    /**
     * @return the terminal type this item opens, or null if it is not registered.
     */
    @Nullable
    protected abstract IUltraTerminalType getTerminalType(ItemStack is);

    /**
     * @return the registered terminal type with the given id, or null if it is not registered.
     */
    @Nullable
    protected static IUltraTerminalType findTerminalType(final String id) {
        return AEApi.instance().registries().ultraTerminal().getById(id);
    }

    /**
     * @return true if this item may open the given terminal.
     */
    protected boolean canOpen(final ItemStack is, final IUltraTerminalType type) {
        return true;
    }

    /**
     * @param index registry index of the terminal to open, or {@link Integer#MIN_VALUE} for the selected terminal.
     */
    @Nullable
    protected IUltraTerminalType getGuiType(final ItemStack is, final int index) {
        final IUltraTerminalType indexed = AEApi.instance().registries().ultraTerminal().getByIndex(index);
        if (indexed != null && canOpen(is, indexed)) {
            return indexed;
        }
        return getTerminalType(is);
    }

    @Override
    public Object getInventory(final ItemStack stack, final World world, final int x, final int y, final int z,
            final EntityPlayer player) {
        return createItemInventory(stack, x, player, getTerminalType(stack));
    }

    /**
     * Builds the item inventory host of the given terminal, exceptions are reported to the player instead of crashing
     * the gui.
     */
    @Nullable
    protected static Object createItemInventory(final ItemStack stack, final int slotIndex, final EntityPlayer player,
            final @Nullable IUltraTerminalType type) {
        if (type == null) {
            return null;
        }
        try {
            return type.createItemInventory(stack, slotIndex, Util.getWirelessGrid(stack), player);
        } catch (final Exception e) {
            player.addChatMessage(PlayerMessages.OutOfRange.toChat());
            return null;
        }
    }

    @Override
    public void openGui(final ItemStack is, final World w, final EntityPlayer player, final Object mode) {
        final IUltraTerminalType type = getTerminalType(is);
        if (type != null) {
            type.openGui(player, is, player.inventory.currentItem);
        }
    }

    @Override
    public IGuiItemObject getGuiObject(final ItemStack is, final World world, final EntityPlayer p, final int x,
            final int y, final int z) {
        final IWirelessTermHandler wh = AEApi.instance().registries().wireless().getWirelessTerminalHandler(is);
        if (wh == null) {
            return null;
        }
        final IUltraTerminalType type = getGuiType(is, y);
        return type == null ? null : type.createGuiObject(wh, is, p, world, x, y, z);
    }

    @Override
    public boolean hasInfinityPower(ItemStack is) {
        NBTTagCompound data = is.getTagCompound();
        return data != null && data.hasKey(infinityEnergyCard) && data.getBoolean(infinityEnergyCard);
    }

    @Override
    public boolean hasInfinityRange(ItemStack is) {
        NBTTagCompound data = is.getTagCompound();
        return data != null && data.hasKey(infinityBoosterCard) && data.getBoolean(infinityBoosterCard);
    }
}
