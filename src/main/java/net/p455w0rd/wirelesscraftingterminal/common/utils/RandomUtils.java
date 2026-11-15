package net.p455w0rd.wirelesscraftingterminal.common.utils;

import org.jspecify.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.p455w0rd.wirelesscraftingterminal.api.IWirelessCraftingTerminalItem;
import net.p455w0rd.wirelesscraftingterminal.items.ItemMagnet;

public class RandomUtils {

	@SideOnly(Side.CLIENT)
	public static String color(String color) {
		switch (color) {
		case "white":
			return EnumChatFormatting.WHITE.toString();
		case "black":
			return EnumChatFormatting.BLACK.toString();
		case "green":
			return EnumChatFormatting.GREEN.toString();
		case "red":
			return EnumChatFormatting.RED.toString();
		case "yellow":
			return EnumChatFormatting.YELLOW.toString();
		case "aqua":
			return EnumChatFormatting.AQUA.toString();
		case "blue":
			return EnumChatFormatting.BLUE.toString();
		case "italics":
			return EnumChatFormatting.ITALIC.toString();
		case "bold":
			return EnumChatFormatting.BOLD.toString();
		default:
		case "gray":
			return EnumChatFormatting.GRAY.toString();
		}
	}

	public static @Nullable ItemStack getWirelessTerm(InventoryPlayer playerInv) {
		final IntObjectPair<ItemStack> wirelessTerm = getWirelessTermWithSlot(playerInv);
		return wirelessTerm == null ? null : wirelessTerm.second();
	}

	/**
	 * Finds the wireless crafting terminal of the player. A held terminal takes precedence over the other inventory
	 * slots.
	 *
	 * @return the inventory slot index and the terminal, or null if the player has no terminal
	 */
	public static @Nullable IntObjectPair<ItemStack> getWirelessTermWithSlot(InventoryPlayer playerInv) {
		final ItemStack heldItem = playerInv.player != null ? playerInv.player.getHeldItem() : null;
		if (heldItem != null && heldItem.getItem() instanceof IWirelessCraftingTerminalItem) {
			return new IntObjectImmutablePair<>(playerInv.currentItem, heldItem);
		}
		final int invSize = playerInv.getSizeInventory();
		for (int i = 0; i < invSize; ++i) {
			final ItemStack item = playerInv.getStackInSlot(i);
			if (item != null && item.getItem() instanceof IWirelessCraftingTerminalItem) {
				return new IntObjectImmutablePair<>(i, item);
			}
		}
		return null;
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
	
	public static boolean readBoolean(ItemStack is, String key) {
		if (is == null) {
			return false;
		}
		NBTTagCompound tag = getTag(is);
		return (tag.hasKey(key) ? tag.getBoolean(key) : false);
	}
	
	public static int readInt(ItemStack is, String key) {
		if (is == null) {
			return 0;
		}
		NBTTagCompound tag = getTag(is);
		return (tag.hasKey(key) ? tag.getInteger(key) : -1);
	}
	
	public static void writeInt(ItemStack is, String key, int value) {
		if (is == null) {
			return;
		}
		NBTTagCompound tag = getTag(is);
		tag.setInteger(key, value);
	}
	
	public static void writeBoolean(ItemStack is, String key, boolean value) {
		if (is == null) {
			return;
		}
		NBTTagCompound tag = getTag(is);
		tag.setBoolean(key, value);
	}
	
	public static void delKey(ItemStack is, String key) {
		if (is == null) {
			return;
		}
		NBTTagCompound tag = getTag(is);
		tag.removeTag(key);
	}
	
	public static NBTTagCompound getTag(ItemStack is) {
		if (!is.hasTagCompound()) {
			is.setTagCompound(new NBTTagCompound());
		}
		return is.getTagCompound();
	}
	
	public static ItemStack readStack(NBTTagCompound nbtTC, String key) {
		return (nbtTC.hasKey(key) ? ItemStack.loadItemStackFromNBT(nbtTC) : null);
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
