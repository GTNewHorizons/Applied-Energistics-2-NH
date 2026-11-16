package appeng.helpers.transfer;

import java.util.ArrayList;
import java.util.List;

import appeng.api.config.TransferDirection;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.tile.inventory.IAEStackInventory;
import appeng.tile.misc.TileSuperMEReplenisher;

/**
 * The replenisher pulls exactly its configured stacks out of the network into its own cells.
 */
public final class SuperReplenisherInspector implements ITransferInspector<TileSuperMEReplenisher> {

    @Override
    public TransferDescription inspect(final TileSuperMEReplenisher replenisher) {
        final IAEStackInventory inventory = replenisher.getAEInventoryByName(StorageName.CONFIG);
        final TransferDescription.Builder builder = TransferDescription.builder().canExport(true);
        final List<IAEStack<?>> config = new ArrayList<>();

        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            final IAEStack<?> stack = inventory.getAEStackInSlot(slot);
            if (stack != null) {
                config.add(stack);
                builder.config(stack, false);
            }
        }

        return builder
                .matcher((stack, direction) -> direction == TransferDirection.EXPORT && isConfigured(config, stack))
                .build();
    }

    private static boolean isConfigured(final List<IAEStack<?>> config, final IAEStack<?> stack) {
        if (stack == null) {
            return false;
        }

        for (final IAEStack<?> configured : config) {
            if (configured.getStackType() == stack.getStackType() && configured.isSameType((Object) stack)) {
                return true;
            }
        }
        return false;
    }
}
