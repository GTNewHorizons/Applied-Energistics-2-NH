package appeng.helpers.transfer;

import java.util.List;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEStackType;
import appeng.tile.storage.TileDrive;

/**
 * An ME Drive is listed as one row, its cells are told apart by slot when an insert is simulated.
 */
public final class DriveInspector extends CellStorageInspector<TileDrive> {

    @Override
    @SuppressWarnings("rawtypes")
    protected List<IMEInventoryHandler> handlers(final TileDrive drive, final IAEStackType<?> type) {
        return drive.getCellArray(type);
    }

    @Override
    protected int priority(final TileDrive drive) {
        return drive.getPriority();
    }

    @Override
    protected BaseActionSource source(final TileDrive drive) {
        return new MachineSource(drive);
    }
}
