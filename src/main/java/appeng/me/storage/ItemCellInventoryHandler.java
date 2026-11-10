package appeng.me.storage;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.partitionlist.IPartitionList;
import appeng.util.partitionlist.OreFilterPartitionList;

public class ItemCellInventoryHandler extends CellInventoryHandler<IAEItemStack> {

    public ItemCellInventoryHandler(IMEInventory<IAEItemStack> c) {
        super(c, ITEM_STACK_TYPE);
    }

    @Override
    protected void setOreFilterPartitionList(String filter) {
        this.setPartitionList(new OreFilterPartitionList(filter));
    }

    @Override
    protected void setConfigPartitionList(boolean hasFuzzy, IAEStackInventory config, FuzzyMode fzMode) {
        final IPartitionList.Builder builder = IPartitionList.builder();
        for (int x = 0; x < config.getSizeInventory(); x++) {
            if (config.getAEStackInSlot(x) instanceof IAEItemStack ais) {
                builder.add(ais.getKey());
            }
        }
        this.setPartitionList(builder.fuzzyMode(hasFuzzy ? fzMode : null).build());
    }

    @Override
    public StorageChannel getStorageChannel() {
        return StorageChannel.ITEMS;
    }
}
