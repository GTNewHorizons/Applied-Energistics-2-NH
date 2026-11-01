package appeng.util.inv;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class WrapperP2PInventory implements IInventory, IInventoryWrapper {

    private final IInventory inv;
    private final boolean canInsert;
    private final boolean canExtract;

    public WrapperP2PInventory(IInventory inv, boolean canInsert, boolean canExtract) {
        this.inv = inv;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    public boolean canInsert() {
        return canInsert;
    }

    public boolean canExtract() {
        return canExtract;
    }

    @Override
    public boolean canRemoveItemFromSlot(int slot, ItemStack is) {
        if (!canExtract || is == null) {
            return false;
        }
        return !(inv instanceof IInventoryWrapper wrapper) || wrapper.canRemoveItemFromSlot(slot, is);
    }

    @Override
    public int getSizeInventory() {
        return inv.getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(int slotIn) {
        return inv.getStackInSlot(slotIn);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return canExtract ? inv.decrStackSize(index, count) : null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return canExtract ? inv.getStackInSlotOnClosing(index) : null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        inv.setInventorySlotContents(index, stack);
    }

    @Override
    public String getInventoryName() {
        return inv.getInventoryName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return inv.hasCustomInventoryName();
    }

    @Override
    public int getInventoryStackLimit() {
        return inv.getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        inv.markDirty();
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return inv.isUseableByPlayer(player);
    }

    @Override
    public void openInventory() {
        inv.openInventory();
    }

    @Override
    public void closeInventory() {
        inv.closeInventory();
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return canInsert && inv.isItemValidForSlot(index, stack);
    }
}
