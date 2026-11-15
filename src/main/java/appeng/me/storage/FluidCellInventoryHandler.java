package appeng.me.storage;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;

import com.glodblock.github.util.Util;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.item.AEFluidStack;
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
    protected void setConfigPartitionList(boolean hasFuzzy, IAEStackInventory config, FuzzyMode fzMode) {
        final IPartitionList.Builder builder = IPartitionList.builder();
        for (int x = 0; x < config.getSizeInventory(); x++) {
            final IAEStack<?> aes = config.getAEStackInSlot(x);
            final IAEFluidStack fluid = aes instanceof IAEFluidStack afs ? afs
                    : aes instanceof IAEItemStack ais ? AEFluidStack.create(Util.getFluidFromItem(ais.getItemStack()))
                            : null;
            if (fluid != null) {
                builder.add(fluid.getKey());

                // convert partition on cells in network
                if (aes instanceof IAEItemStack) {
                    fluid.setStackSize(1);
                    config.putAEStackInSlot(x, fluid);
                    config.markDirty();
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
