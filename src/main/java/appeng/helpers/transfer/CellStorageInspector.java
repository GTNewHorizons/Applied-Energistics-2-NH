package appeng.helpers.transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import appeng.api.config.Actionable;
import appeng.api.config.IncludeExclude;
import appeng.api.config.TransferDirection;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.features.transfer.TransferMatcher;
import appeng.api.networking.IGridHost;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.me.storage.MEInventoryHandler;
import appeng.util.prioitylist.IPartitionList;
import appeng.util.prioitylist.OreFilteredList;

/**
 * Describes a block that holds storage cells, such as an ME Drive or ME Chest, by asking its live cell handlers.
 * <p>
 * Cells carry their own filters and cards, so unlike buses there are no settings to copy. The config strip lists the
 * partitions of all cells, and the matcher asks the same handlers AE routes stacks into.
 */
abstract class CellStorageInspector<T extends IGridHost> implements ITransferInspector<T> {

    /** Cells can list up to 63 partitions each, more than a row has room for anyway. */
    private static final int MAX_CONFIG = 63;

    @SuppressWarnings("rawtypes")
    protected abstract List<IMEInventoryHandler> handlers(T host, IAEStackType<?> type);

    /**
     * @return the cell handlers whose partitions are shown, may differ from {@link #handlers} when those are wrapped in
     *         a way that hides the cell
     */
    @SuppressWarnings("rawtypes")
    protected List<IMEInventoryHandler> partitionHandlers(final T host, final IAEStackType<?> type) {
        return this.handlers(host, type);
    }

    protected abstract int priority(T host);

    protected abstract BaseActionSource source(T host);

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public TransferDescription inspect(final T host) {
        boolean sticky = false;
        final TransferDescription.Builder builder = TransferDescription.builder().storageHandler(true)
                .priority(this.priority(host)).canImport(true).canExport(true);

        int configCount = 0;
        final Set<String> oreFilters = new LinkedHashSet<>();
        for (final IAEStackType<?> type : AEStackTypeRegistry.getAllTypes()) {
            for (final IMEInventoryHandler handler : this.handlers(host, type)) {
                sticky |= handler.getSticky();
            }
            for (final IMEInventoryHandler handler : this.partitionHandlers(host, type)) {
                final ICellInventoryHandler<?> cell = cellOf(handler);
                if (!(cell instanceof MEInventoryHandler<?>partitioned)
                        || cell.getIncludeExcludeMode() == IncludeExclude.BLACKLIST) {
                    continue;
                }
                final IPartitionList<?> partition = partitioned.getPartitionList();
                if (partition instanceof OreFilteredList) {
                    // An ore filter has no stacks to list, only the expression the cell was given.
                    final String filter = cell.getCellInv() == null ? null : cell.getCellInv().getOreFilter();
                    if (filter != null && !filter.isEmpty()) {
                        oreFilters.add(filter);
                    }
                    continue;
                }
                final Iterable<?> stacks = partition == null ? null : partition.getItems();
                if (stacks == null) {
                    continue;
                }
                for (final Object stack : stacks) {
                    if (configCount++ < MAX_CONFIG) {
                        builder.config((IAEStack<?>) stack, cell.isFuzzy());
                    }
                }
            }
        }
        if (!oreFilters.isEmpty()) {
            // A cell takes a stack when any of the filters matches, which is what | means in a filter.
            builder.oreFilter(String.join("|", oreFilters));
        }

        return builder.sticky(sticky).matcher(new CellMatcher(type -> this.handlers(host, type), this.source(host)))
                .build();
    }

    /**
     * @return the cell's own handler, found by unwrapping the handlers that blocks put around it
     */
    static ICellInventoryHandler<?> cellOf(final IMEInventoryHandler<?> handler) {
        IMEInventory<?> current = handler;
        for (int depth = 0; depth < 4 && current != null; depth++) {
            if (current instanceof ICellInventoryHandler<?>cell) {
                return cell;
            }
            current = current instanceof MEInventoryHandler<?>wrapper ? wrapper.getInternal()
                    : current instanceof IMEInventoryHandler<?>inner ? inner.getInternal() : null;
        }
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static final class CellMatcher implements TransferMatcher {

        private final Function<IAEStackType<?>, List<IMEInventoryHandler>> handlers;
        private final BaseActionSource source;

        private CellMatcher(final Function<IAEStackType<?>, List<IMEInventoryHandler>> handlers,
                final BaseActionSource source) {
            this.handlers = handlers;
            this.source = source;
        }

        private List<IMEInventoryHandler> handlersFor(final IAEStack<?> stack) {
            try {
                return stack == null ? Collections.emptyList()
                        : new ArrayList<>(this.handlers.apply(stack.getStackType()));
            } catch (final RuntimeException e) {
                // A cell from another mod must not take the terminal down.
                return Collections.emptyList();
            }
        }

        /**
         * Export means a cell would accept the stack, import means one holds it already and could give it up.
         */
        @Override
        public boolean canTransfer(final IAEStack<?> stack, final TransferDirection direction) {
            for (final IMEInventoryHandler handler : this.handlersFor(stack)) {
                try {
                    if (direction != TransferDirection.IMPORT && handler.canAccept(stack)) {
                        return true;
                    }
                    if (direction != TransferDirection.EXPORT && handler.getAvailableItem(stack) != null) {
                        return true;
                    }
                } catch (final RuntimeException e) {
                    // Skip a cell that fails to answer.
                }
            }
            return false;
        }

        @Override
        public boolean isExplicitlyListed(final IAEStack<?> stack) {
            for (final IMEInventoryHandler handler : this.handlersFor(stack)) {
                try {
                    if (handler.isPrioritized(stack)) {
                        return true;
                    }
                } catch (final RuntimeException e) {
                    // Skip a cell that fails to answer.
                }
            }
            return false;
        }

        /**
         * Asks the same question as the sticky pass of {@code NetworkInventoryHandler.injectItems}, see
         * {@code StorageBusInspector}.
         */
        @Override
        public boolean capturesAsSticky(final IAEStack<?> stack) {
            for (final IMEInventoryHandler handler : this.handlersFor(stack)) {
                try {
                    if (!handler.getSticky()) {
                        continue;
                    }
                    final IAEStack probe = stack.copy();
                    probe.setStackSize(1);
                    if (handler.canAccept(probe) && (handler.isPrioritized(probe)
                            || handler.extractItems(probe, Actionable.SIMULATE, this.source) != null)) {
                        return true;
                    }
                } catch (final RuntimeException e) {
                    // Skip a cell that fails to answer.
                }
            }
            return false;
        }
    }
}
