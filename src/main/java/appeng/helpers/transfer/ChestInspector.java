package appeng.helpers.transfer;

import java.util.Collections;
import java.util.List;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEStackType;
import appeng.tile.storage.TileChest;

/**
 * An ME Chest holds one cell.
 */
public final class ChestInspector extends CellStorageInspector<TileChest> {

    @Override
    @SuppressWarnings("rawtypes")
    protected List<IMEInventoryHandler> handlers(final TileChest chest, final IAEStackType<?> type) {
        return chest.getCellArray(type);
    }

    /**
     * The chest wraps its cell in a monitor that hides it, so the partitions come from a handler made for reading.
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected List<IMEInventoryHandler> partitionHandlers(final TileChest chest, final IAEStackType<?> type) {
        try {
            final IMEInventoryHandler cell = chest.getInternalHandler(type);
            return cell == null ? Collections.emptyList() : Collections.singletonList(cell);
        } catch (final RuntimeException e) {
            // An empty chest has no cell to read.
            return Collections.emptyList();
        }
    }

    @Override
    protected int priority(final TileChest chest) {
        return chest.getPriority();
    }

    @Override
    protected BaseActionSource source(final TileChest chest) {
        return new MachineSource(chest);
    }
}
