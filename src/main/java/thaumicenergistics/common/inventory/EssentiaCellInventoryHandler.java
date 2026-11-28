package thaumicenergistics.common.inventory;

import static thaumicenergistics.common.storage.AEEssentiaStackType.ESSENTIA_STACK_TYPE;

import org.jspecify.annotations.NonNull;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.KeyConfigInventory;
import appeng.api.storage.data.IAEStackType;
import appeng.me.storage.CellInventoryHandler;
import appeng.util.partitionlist.IPartitionList;
import thaumicenergistics.common.storage.AEEssentiaKey;
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
    protected void setConfigPartitionList(boolean hasFuzzy, KeyConfigInventory config, FuzzyMode fzMode) {
        final IPartitionList.Builder builder = IPartitionList.builder();
        for (int x = 0; x < config.getSizeInventory(); x++) {
            if (config.getKey(x) instanceof AEEssentiaKey key) {
                builder.add(key);
            }
        }
        this.setPartitionList(builder.build());
    }
}
