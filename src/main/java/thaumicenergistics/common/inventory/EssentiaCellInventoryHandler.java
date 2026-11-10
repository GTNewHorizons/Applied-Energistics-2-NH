package thaumicenergistics.common.inventory;

import static thaumicenergistics.common.storage.AEEssentiaStackType.ESSENTIA_STACK_TYPE;

import org.jspecify.annotations.NonNull;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.data.IAEStackType;
import appeng.me.storage.CellInventoryHandler;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.partitionlist.IPartitionList;
import thaumicenergistics.common.storage.AEEssentiaStack;

public class EssentiaCellInventoryHandler extends CellInventoryHandler<AEEssentiaStack> {

    public EssentiaCellInventoryHandler(IMEInventory<AEEssentiaStack> c) {
        super(c, ESSENTIA_STACK_TYPE);
    }

    @Override
    public TYPE getCellType() {
        return TYPE.ESSENTIA;
    }

    @NonNull
    @Override
    public IAEStackType<?> getStackType() {
        return ESSENTIA_STACK_TYPE;
    }

    @Override
    protected void setConfigPartitionList(boolean hasFuzzy, IAEStackInventory config, FuzzyMode fzMode) {
        final IPartitionList.Builder builder = IPartitionList.builder();
        for (int x = 0; x < config.getSizeInventory(); x++) {
            if (config.getAEStackInSlot(x) instanceof AEEssentiaStack essentiaStack) {
                builder.add(essentiaStack.getKey());
            }
        }
        this.setPartitionList(builder.build());
    }
}
