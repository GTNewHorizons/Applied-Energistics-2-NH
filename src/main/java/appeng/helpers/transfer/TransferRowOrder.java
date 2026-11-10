package appeng.helpers.transfer;

import java.util.Comparator;

import appeng.api.config.TransferDirection;
import appeng.api.features.transfer.TransferComponentInfo;

/**
 * Orders Transfer Terminal rows by how likely each component is to have moved a stack.
 * <p>
 * Mirrors {@code NetworkInventoryHandler}: stacks entering storage first go to the sticky handlers that claim them,
 * which then shut every other handler out, then by descending priority with whitelisted handlers ahead of unfiltered
 * ones. Stacks leaving storage are drained from the lowest priority first. Handlers that tie on all of that are chosen
 * in an unspecified order by AE.
 * <p>
 * Sticky handlers only claim a stack they whitelist or already hold, a sticky card alone changes nothing. Only the
 * export order takes them into account, the other directions sort by priority alone.
 */
public final class TransferRowOrder {

    private static final Comparator<TransferComponentInfo> BY_NAME_THEN_LOCATION = Comparator
            .comparing(TransferComponentInfo::getSortName).thenComparingInt(row -> row.dim)
            .thenComparingInt(row -> row.x).thenComparingInt(row -> row.y).thenComparingInt(row -> row.z)
            .thenComparingInt(row -> row.side);

    private TransferRowOrder() {}

    /**
     * @param hasQuery  whether rows were filtered by a stack
     * @param direction direction of that query
     */
    public static Comparator<TransferComponentInfo> comparator(final boolean hasQuery,
            final TransferDirection direction) {
        Comparator<TransferComponentInfo> order = Comparator.comparingInt(row -> row.networkTier);

        if (hasQuery) {
            order = order.thenComparing(row -> !row.description.storageHandler);
        }

        if (hasQuery && direction == TransferDirection.EXPORT) {
            order = order.thenComparing(row -> !row.stickyCapture);
            order = order.thenComparing(Comparator.comparingInt(TransferRowOrder::storagePriority).reversed());
            order = order.thenComparing(row -> !isListed(row));
        } else if (hasQuery && direction == TransferDirection.IMPORT) {
            order = order.thenComparingInt(TransferRowOrder::storagePriority);
        } else {
            order = order.thenComparing(Comparator.comparingInt(TransferRowOrder::storagePriority).reversed());
        }

        return order.thenComparing(BY_NAME_THEN_LOCATION);
    }

    /**
     * Only storage handlers take part in AE's priority routing, the priority of an interface orders crafting patterns.
     */
    private static int storagePriority(final TransferComponentInfo row) {
        return row.description.storageHandler ? row.description.priority : 0;
    }

    private static boolean isListed(final TransferComponentInfo row) {
        return row.description.storageHandler && row.explicitlyListed;
    }
}
