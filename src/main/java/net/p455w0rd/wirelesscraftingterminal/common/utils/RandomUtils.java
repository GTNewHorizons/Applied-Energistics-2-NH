package net.p455w0rd.wirelesscraftingterminal.common.utils;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.p455w0rd.wirelesscraftingterminal.api.IWirelessCraftingTerminalItem;
import net.p455w0rd.wirelesscraftingterminal.items.ItemMagnet;

import org.jspecify.annotations.Nullable;

import appeng.util.PlayerInventoryUtil;
import it.unimi.dsi.fastutil.ints.IntObjectPair;

public class RandomUtils {

    public static @Nullable ItemStack getWirelessTerm(InventoryPlayer playerInv) {
        final IntObjectPair<ItemStack> wirelessTerm = getWirelessTermWithSlot(playerInv);
        return wirelessTerm == null ? null : wirelessTerm.second();
    }

    /**
     * Finds the wireless crafting terminal of the player in the same order as AE2 wireless terminals.
     *
     * @return the inventory slot index and the terminal, or null if the player has no terminal
     */
    public static @Nullable IntObjectPair<ItemStack> getWirelessTermWithSlot(InventoryPlayer playerInv) {
        return PlayerInventoryUtil.findWirelessTerminal(playerInv.player, RandomUtils::isWirelessCraftingTerminal);
    }

    public static @Nullable IntObjectPair<ItemStack> getWirelessTermWithMagnet(InventoryPlayer playerInv) {
        return PlayerInventoryUtil.findWirelessTerminal(playerInv.player, RandomUtils::hasInstalledMagnet);
    }

    public static boolean isWirelessCraftingTerminal(@Nullable ItemStack is) {
        return is != null && is.getItem() instanceof IWirelessCraftingTerminalItem terminal
                && terminal.isWirelessCraftingEnabled(is);
    }

    /**
     * @return the magnet card installed in the given terminal, or null if there is none
     */
    public static @Nullable ItemStack getInstalledMagnet(@Nullable ItemStack terminal) {
        if (!RandomUtils.isWirelessCraftingTerminal(terminal) || !terminal.hasTagCompound()) {
            return null;
        }
        final NBTTagList tagList = terminal.getTagCompound().getTagList("MagnetSlot", 10);
        final ItemStack magnetItem = ItemStack.loadItemStackFromNBT(tagList.getCompoundTagAt(0));
        return magnetItem != null && magnetItem.getItem() instanceof ItemMagnet ? magnetItem : null;
    }

    /**
     * @return true if the given terminal has a magnet card installed
     */
    public static boolean hasInstalledMagnet(@Nullable ItemStack terminal) {
        return getInstalledMagnet(terminal) != null;
    }

    /**
     * Writes the mode (item damage) to the magnet card installed in the given terminal.
     *
     * @return true if a magnet card is installed and has been updated
     */
    public static boolean setInstalledMagnetMode(@Nullable ItemStack terminal, final int mode) {
        if (!hasInstalledMagnet(terminal)) {
            return false;
        }
        terminal.getTagCompound().getTagList("MagnetSlot", 10).getCompoundTagAt(0).setShort("Damage", (short) mode);
        return true;
    }

    public static @Nullable ItemStack getMagnet(@Nullable InventoryPlayer playerInv) {
        // Is player holding a Magnet Card?
        if (playerInv == null || playerInv.player == null) {
            return null;
        }
        final ItemStack held = playerInv.player.getHeldItem();
        if (held != null && held.getItem() instanceof ItemMagnet) {
            return held;
        }
        // if not true, try to return first magnet card from first
        // wireless term that has a MagnetCard installed
        final IntObjectPair<ItemStack> terminal = getWirelessTermWithMagnet(playerInv);
        if (terminal != null) {
            final ItemStack magnetItem = getInstalledMagnet(terminal.second());
            if (magnetItem != null) {
                return magnetItem;
            }
        }
        // No wireless crafting terminal with Magnet Card installed,
        // is there a Magnet Card in the player's inventory?
        int invSize = playerInv.getSizeInventory();
        ItemStack magnetItem = null;
        if (invSize <= 0) {
            return null;
        }
        for (int i = 0; i < invSize; ++i) {
            ItemStack item = playerInv.getStackInSlot(i);
            if (item == null) {
                continue;
            }
            if (item.getItem() instanceof ItemMagnet) {
                magnetItem = item;
                break;
            }
        }
        return magnetItem;
    }

    public static boolean isMagnetInitialized(ItemStack magnetItem) {
        if (magnetItem != null && magnetItem.getItem() instanceof ItemMagnet) {
            if (magnetItem.hasTagCompound()) {
                if (magnetItem.getTagCompound().hasKey("Initialized")) {
                    return magnetItem.getTagCompound().getBoolean("Initialized");
                }
            }
        }
        return false;
    }

    public static void removeTimerTags(ItemStack is) {
        if (is == null || is.getTagCompound() == null) {
            return;
        }
        if (is.getTagCompound().hasKey("WCTReset")) {
            is.setTagCompound(null);
        }
        if (is.getTagCompound() != null) {
            if (is.getTagCompound().hasKey("WCTPickupTimer")) {
                is.getTagCompound().removeTag("WCTPickupTimer");
            }
        }
    }

}
