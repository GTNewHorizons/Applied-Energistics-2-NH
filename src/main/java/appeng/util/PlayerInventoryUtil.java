package appeng.util;

import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S2FPacketSetSlot;

import org.jspecify.annotations.Nullable;

import appeng.api.AEApi;
import baubles.api.BaublesApi;
import cpw.mods.fml.common.Optional;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;

/**
 * A collection of utility functions for manipulating player inventories.
 */
public class PlayerInventoryUtil {

    /**
     * Sets the specified inventory slot as the player's active (held) slot. If the target slot is in the hotbar (slots
     * 0-8), that slot is set as the active slot. If the target slot is outside the hotbar, it swaps the contents of
     * that slot with the current active slot.
     *
     * @param player The player whose active slot should be changed.
     * @param slot   The inventory slot index to make active (0-8 for hotbar, 9+ for main inventory).
     */
    public static void setSlotAsActiveSlot(EntityPlayerMP player, int slot) {
        if (slot >= 0 && slot <= 8) {
            player.inventory.currentItem = slot;
            player.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(slot));
        } else {
            swapInventorySlots(player, player.inventory.currentItem, slot);
        }
        player.inventory.markDirty();
    }

    /**
     * Moves an ItemStack from a source slot to a destination slot in the player's inventory. If the destination slot is
     * occupied, the items are swapped.
     *
     * @param player The player whose inventory is to be modified.
     * @param slot1  The index of the first ItemStack to move.
     * @param slot2  The index of the second ItemStack to move.
     */
    public static void swapInventorySlots(EntityPlayerMP player, int slot1, int slot2) {
        if (slot1 == slot2) {
            return;
        }

        // Get the stacks from both slots
        ItemStack sourceStack = player.inventory.getStackInSlot(slot1);
        ItemStack destinationStack = player.inventory.getStackInSlot(slot2);

        // Copy the item stacks to prevents any potential shared reference issue.
        if (sourceStack != null) {
            sourceStack = sourceStack.copy();
        }
        if (destinationStack != null) {
            destinationStack = destinationStack.copy();
        }

        // Set the destination slot with the source stack (even if it's null)
        player.inventory.setInventorySlotContents(slot2, sourceStack);

        // Set the source slot with the original destination stack
        player.inventory.setInventorySlotContents(slot1, destinationStack);

        // Mark the inventory as dirty to ensure changes are saved and synced
        player.inventory.markDirty();
        player.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(-2, slot2, sourceStack));
        player.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(-2, slot1, destinationStack));
    }

    /**
     * Consolidate ItemStacks from a source slot to a destination slot in the player's inventory.
     *
     * @param player          The player whose inventory is to be modified.
     * @param sourceSlot      The index of the slot to move the item from.
     * @param destinationSlot The index of the slot to move the item to.
     * @return the consolidated item stack
     */
    public static ItemStack consolidateItemStacks(EntityPlayerMP player, int sourceSlot, int destinationSlot) {
        ItemStack sourceStack = player.inventory.getStackInSlot(sourceSlot);
        ItemStack destinationStack = player.inventory.getStackInSlot(destinationSlot);

        // We can only consolidate if both the source and destination slots have item stacks.
        if (sourceStack == null || destinationStack == null) {
            return destinationStack;
        }

        if (!sourceStack.isItemEqual(destinationStack)
                || !ItemStack.areItemStackTagsEqual(sourceStack, destinationStack)) {
            return destinationStack;
        }

        // Copy the item stacks to prevents any potential shared reference issue.
        sourceStack = sourceStack.copy();
        destinationStack = destinationStack.copy();

        int missingQuantity = destinationStack.getMaxStackSize() - destinationStack.stackSize;
        if (missingQuantity >= sourceStack.stackSize) {
            destinationStack.stackSize = destinationStack.stackSize + sourceStack.stackSize;
            sourceStack = null;
        } else {
            sourceStack.stackSize -= missingQuantity;
            destinationStack.stackSize += missingQuantity;
        }

        // Update the inventory stacks
        player.inventory.setInventorySlotContents(destinationSlot, destinationStack);
        player.inventory.setInventorySlotContents(sourceSlot, sourceStack);

        // Mark the inventory as dirty to ensure changes are saved and synced
        player.inventory.markDirty();
        player.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(-2, destinationSlot, destinationStack));
        player.playerNetServerHandler.sendPacket(new S2FPacketSetSlot(-2, sourceSlot, sourceStack));

        return player.inventory.getStackInSlot(destinationSlot);
    }

    /**
     * Finds the first wireless terminal in the player's inventory.
     *
     * @param player the player to check
     * @return the wireless terminal ItemStack, or null if not found
     */
    public static @Nullable ItemStack getFirstWirelessTerminal(EntityPlayer player) {
        final IntObjectPair<ItemStack> terminal = findWirelessTerminal(player);
        return terminal != null ? terminal.second() : null;
    }

    /**
     * Finds the first wireless terminal in the player's inventory.
     *
     * @param player the player to check
     * @return the slot index and the wireless terminal ItemStack, or null if not found
     */
    public static @Nullable IntObjectPair<ItemStack> findWirelessTerminal(EntityPlayer player) {
        return findWirelessTerminal(player, AEApi.instance().registries().wireless()::isWirelessTerminal);
    }

    /**
     * Finds the first terminal accepted by the filter. The bauble slots are checked before the hotbar and the main
     * inventory (slots 0-35).
     *
     * @param player the player to check
     * @param filter selects the terminals to find
     * @return the slot index and the terminal ItemStack, or null if not found. The slot index can be passed to
     *         {@link Platform#getItemFromPlayerInventoryBySlotIndex}. A terminal in the bauble slots uses
     *         {@link Platform#baublesSlotsOffset} plus its bauble slot index.
     */
    public static @Nullable IntObjectPair<ItemStack> findWirelessTerminal(EntityPlayer player,
            Predicate<ItemStack> filter) {
        if (Platform.isBaublesLoaded) {
            final IntObjectPair<ItemStack> terminal = findWirelessTerminalInBaubles(player, filter);
            if (terminal != null) {
                return terminal;
            }
        }

        final ItemStack[] mainInventory = player.inventory.mainInventory;
        for (int i = 0; i < mainInventory.length; i++) {
            final ItemStack stack = mainInventory[i];
            if (stack != null && filter.test(stack)) {
                return new IntObjectImmutablePair<>(i, stack);
            }
        }

        return null;
    }

    /**
     * Finds the first empty hotbar slot.
     *
     * @param player the player whose inventory to check
     * @return the slot number of the first empty hotbar slot, otherwise -1
     */
    public static int getFirstEmptyHotbarSlot(EntityPlayer player) {
        for (int i = 0; i <= 8; ++i) {
            if (player.inventory.mainInventory[i] == null) {
                return i;
            }
        }
        return -1;
    }

    @Optional.Method(modid = "Baubles|Expanded")
    private static @Nullable IntObjectPair<ItemStack> findWirelessTerminalInBaubles(EntityPlayer player,
            Predicate<ItemStack> filter) {
        final IInventory baubles = BaublesApi.getBaubles(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSizeInventory(); i++) {
                final ItemStack stack = baubles.getStackInSlot(i);
                if (stack != null && filter.test(stack)) {
                    return new IntObjectImmutablePair<>(Platform.baublesSlotsOffset + i, stack);
                }
            }
        }
        return null;
    }
}
