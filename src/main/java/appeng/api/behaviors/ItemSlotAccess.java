package appeng.api.behaviors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.util.Platform;

/**
 * Where a container item that is being filled or drained lives, and where the results go.
 */
public abstract class ItemSlotAccess {

    protected final EntityPlayer player;

    protected ItemSlotAccess(EntityPlayer player) {
        this.player = player;
    }

    /**
     * The item carried by the mouse cursor in an open GUI.
     *
     * @param onChanged called after the carried item changed, e.g. to sync it to the client
     */
    public static ItemSlotAccess carried(EntityPlayer player, Runnable onChanged) {
        return new ItemSlotAccess(player) {

            @Override
            public @Nullable ItemStack get() {
                return this.player.inventory.getItemStack();
            }

            @Override
            public void set(@Nullable ItemStack stack) {
                this.player.inventory.setItemStack(stack);
                onChanged.run();
            }
        };
    }

    /**
     * The item held in the player's hand.
     */
    public static ItemSlotAccess equipped(EntityPlayer player) {
        final int slot = player.inventory.currentItem;
        return new ItemSlotAccess(player) {

            @Override
            public @Nullable ItemStack get() {
                return this.player.inventory.getStackInSlot(slot);
            }

            @Override
            public void set(@Nullable ItemStack stack) {
                this.player.inventory.setInventorySlotContents(slot, stack);
            }
        };
    }

    /**
     * A slot of an open container.
     */
    public static ItemSlotAccess slot(EntityPlayer player, Slot slot) {
        return new ItemSlotAccess(player) {

            @Override
            public @Nullable ItemStack get() {
                return slot.getStack();
            }

            @Override
            public void set(@Nullable ItemStack stack) {
                slot.putStack(stack);
            }
        };
    }

    public abstract @Nullable ItemStack get();

    /**
     * Replaces the item. {@code null} or an empty stack clears the slot.
     */
    public abstract void set(@Nullable ItemStack stack);

    /**
     * Gives an item that does not go back into this slot to the player, dropping it if the inventory is full.
     */
    public void giveOrDrop(ItemStack stack) {
        Platform.addToPlayerInvOrDrop(this.player, stack);
    }

    /**
     * Gives an amount of a resource that could neither be put into a container nor back into storage to the player,
     * wrapped in an item.
     */
    public void giveLeftover(AEKey what, long amount) {
        if (amount > 0) {
            Platform.handleLeftover(this.player, what, amount);
        }
    }
}
