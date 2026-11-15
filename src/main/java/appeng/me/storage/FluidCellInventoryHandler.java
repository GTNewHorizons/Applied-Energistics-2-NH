package appeng.me.storage;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;

import com.glodblock.github.util.Util;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.KeyConfigInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.util.partitionlist.IPartitionList;

public class FluidCellInventoryHandler extends CellInventoryHandler<IAEFluidStack> {

    public FluidCellInventoryHandler(IMEInventory<IAEFluidStack> c) {
        super(c, FLUID_STACK_TYPE);
    }

    @Override
    protected void setOreFilterPartitionList(String filter) {
        // no ore dict
    }

    @Override
    protected void setConfigPartitionList(boolean hasFuzzy, KeyConfigInventory config, FuzzyMode fzMode) {
        final IPartitionList.Builder builder = IPartitionList.builder();
        for (int x = 0; x < config.getSizeInventory(); x++) {
            final AEKey key = config.getKey(x);
            final AEFluidKey fluid = key instanceof AEFluidKey fluidKey ? fluidKey
                    : key instanceof AEItemKey itemKey ? AEFluidKey.of(Util.getFluidFromItem(itemKey.toStack())) : null;
            if (fluid != null) {
                builder.add(fluid);

                // convert partition on cells in network
                if (key instanceof AEItemKey) {
                    config.setKey(x, fluid);
                }
            }
        }
        this.setPartitionList(builder.build());
    }

    @Override
    public StorageChannel getStorageChannel() {
        return StorageChannel.FLUIDS;
    }
}
