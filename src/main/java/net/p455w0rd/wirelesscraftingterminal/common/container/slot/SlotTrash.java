package net.p455w0rd.wirelesscraftingterminal.common.container.slot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.p455w0rd.wirelesscraftingterminal.common.utils.RandomUtils;

public class SlotTrash extends appeng.container.slot.AppEngSlot {

    private final EntityPlayer entityPlayer;
    private final ItemStack terminal;
    private ItemStack itemStack;
    private boolean shouldDeleteMouseStack;
    private String name = "TrashSlot";

    public SlotTrash(IInventory inv, int x, int y, EntityPlayer player) {
        this(inv, x, y, player, RandomUtils.getWirelessTerm(player.inventory));
    }

    public SlotTrash(IInventory inv, int x, int y, EntityPlayer player, ItemStack terminal) {
        super(null, 0, x, y);
        entityPlayer = player;
        this.terminal = terminal;
        readNBT();
    }

    @Override
    public ItemStack getStack() {
        return itemStack;
    }

    @Override
    public ItemStack getDisplayStack() {
        return this.getStack();
    }

    @Override
    public boolean getHasStack() {
        return itemStack != null;
    }

    @Override
    public void putStack(ItemStack itemStack) {
        shouldDeleteMouseStack = (this.itemStack != null);
        this.itemStack = itemStack;
        writeNBT();
    }

    @Override
    public int getSlotStackLimit() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void onSlotChanged() {
        if (itemStack != null && shouldDeleteMouseStack) {
            entityPlayer.inventory.setItemStack(null);
        }
    }

    @Override
    public boolean isItemValid(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return true;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        if (itemStack != null) {
            ItemStack returnStack;
            if (itemStack.stackSize <= amount) {
                returnStack = itemStack;
                itemStack = null;
                shouldDeleteMouseStack = true;
                writeNBT();
                return returnStack;
            } else {
                returnStack = itemStack.splitStack(amount);
                shouldDeleteMouseStack = false;
                if (itemStack.stackSize == 0) {
                    itemStack = null;
                    shouldDeleteMouseStack = true;
                }
                writeNBT();
                return returnStack;
            }
        }
        return null;
    }

    public void readNBT() {
        if (this.terminal == null) {
            return;
        }
        NBTTagCompound nbtTagCompound = RandomUtils.getTag(this.terminal);
        NBTTagList tagList = nbtTagCompound.getTagList(this.name, 10);
        NBTTagCompound tagCompound = tagList.getCompoundTagAt(0);
        if (tagCompound != null) {
            itemStack = ItemStack.loadItemStackFromNBT(tagCompound);
        }
    }

    public void writeNBT() {
        if (this.terminal == null) {
            return;
        }
        NBTTagCompound nbtTagCompound = RandomUtils.getTag(this.terminal);
        NBTTagList tagList = new NBTTagList();
        if (itemStack != null) {
            NBTTagCompound tagCompound = new NBTTagCompound();
            itemStack.writeToNBT(tagCompound);
            tagList.appendTag(tagCompound);
        }
        nbtTagCompound.setTag(this.name, tagList);
    }

    public void clearStack() {
        itemStack = null;
        writeNBT();
    }
}
