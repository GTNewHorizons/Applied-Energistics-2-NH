package appeng.me.storage;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.KeyConfigInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
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
    protected void setConfigPartitionList(boolean hasFuzzy, KeyConfigInventory config, FuzzyMode fzMode) {
        final IPartitionList.Builder builder = IPartitionList.builder();
        for (int x = 0; x < config.getSizeInventory(); x++) {
            if (config.getKey(x) instanceof AEItemKey key) {
                builder.add(key);
            }
        }
        this.setPartitionList(builder.fuzzyMode(hasFuzzy ? fzMode : null).build());
    }

    @Override
    public StorageChannel getStorageChannel() {
        return StorageChannel.ITEMS;
    }
}
