package net.p455w0rd.wirelesscraftingterminal.common.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;
import net.p455w0rd.wirelesscraftingterminal.api.IWirelessCraftingTerminalItem;

import org.jspecify.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;

import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;

public class WCTInventoryCrafting extends AppEngInternalInventory {

    private static final String NBT_KEY = "CraftingMatrix";

    public WCTInventoryCrafting(final @Nullable IAEAppEngInventory host) {
        super(host, 9);
    }

    @Override
    public boolean isItemValidForSlot(final int slot, final ItemStack stack) {
        return !(stack.getItem() instanceof IWirelessCraftingTerminalItem);
    }

    /**
     * Reads the grid without firing inventory events, so that the NBT is not written back while it is being read.
     */
    public void readFromTerminal(final ItemStack terminal) {
        for (int i = 0; i < this.inv.length; i++) {
            this.inv[i] = null;
        }

        final NBTTagList tagList = ItemStackNBT.getTagList(terminal, NBT_KEY, NBT.TAG_COMPOUND);
        if (tagList == null) {
            return;
        }

        for (int i = 0; i < tagList.tagCount(); i++) {
            final NBTTagCompound tag = tagList.getCompoundTagAt(i);
            final int slot = tag.getByte("Slot");
            if (slot >= 0 && slot < this.inv.length) {
                this.inv[slot] = ItemStack.loadItemStackFromNBT(tag);
            }
        }
    }

    public void writeToTerminal(final ItemStack terminal) {
        final NBTTagList tagList = new NBTTagList();
        for (int i = 0; i < this.inv.length; i++) {
            final ItemStack stack = this.inv[i];
            if (stack == null) {
                continue;
            }
            if (stack.stackSize <= 0) {
                this.inv[i] = null;
                continue;
            }

            final NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("Slot", (byte) i);
            stack.writeToNBT(tag);
            tagList.appendTag(tag);
        }
        ItemStackNBT.setTagList(terminal, NBT_KEY, tagList);
    }
}
