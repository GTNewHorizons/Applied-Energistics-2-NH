package net.p455w0rd.wirelesscraftingterminal.common.utils;

import org.jspecify.annotations.Nullable;

import appeng.util.PlayerInventoryUtil;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.p455w0rd.wirelesscraftingterminal.api.IWirelessCraftingTerminalItem;
import net.p455w0rd.wirelesscraftingterminal.items.ItemMagnet;

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

	public static boolean isWirelessCraftingTerminal(@Nullable ItemStack is) {
		return is != null && is.getItem() instanceof IWirelessCraftingTerminalItem;
	}

	public static ItemStack getMagnet(InventoryPlayer playerInv) {
		// Is player holding a Magnet Card?
		if (playerInv.player != null && playerInv.player.getHeldItem() != null && playerInv.player.getHeldItem().getItem() instanceof ItemMagnet) {
			return playerInv.player.getHeldItem();
		}
		// if not true, try to return first magnet card from first
		// wireless term that has a MagnetCard installed
		ItemStack wirelessTerm = getWirelessTerm(playerInv);
		if (wirelessTerm != null && wirelessTerm.getItem() instanceof IWirelessCraftingTerminalItem) {
			NBTTagCompound nbtTC = wirelessTerm.getTagCompound();
			if (nbtTC.hasKey("MagnetSlot")) {
				NBTTagList magnetSlot = nbtTC.getTagList("MagnetSlot", 10);
				ItemStack magnetItem = ItemStack.loadItemStackFromNBT(magnetSlot.getCompoundTagAt(0));
				if (magnetItem != null && magnetItem.getItem() instanceof ItemMagnet) {
					return magnetItem;
				}
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
	
	public static boolean isMagnetInstalled(InventoryPlayer ip) {
		NBTTagCompound magnetNBTForm = RandomUtils.getWirelessTerm(ip).getTagCompound().getTagList("MagnetSlot", 10).getCompoundTagAt(0);
		if (magnetNBTForm != null) {
			ItemStack magnetItem = ItemStack.loadItemStackFromNBT(magnetNBTForm);
			if (magnetItem != null && magnetItem.getItem() instanceof ItemMagnet) {
				return true;
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
