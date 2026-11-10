package appeng.helpers.transfer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.config.Actionable;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.DimensionalCoord;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.storage.InjectTrace;

/**
 * Asks a network where it would put a stack right now, by running a simulated insert and recording which storage
 * handlers it reaches. Server only.
 */
public final class TransferInsertSimulation {

    private TransferInsertSimulation() {}

    /** What a simulated insert found. */
    public static final class Result {

        static final Result NONE = new Result(null, Collections.emptyList(), 0, false);

        /**
         * The given rows in their given order. Storage rows carry how much they would receive, other rows are
         * unchanged. Null if nothing was simulated.
         */
        public final List<TransferComponentInfo> rows;

        /** One row for each crafting CPU that would take part of the stack. */
        public final List<TransferComponentInfo> cpuRows;

        /** How much of the stack would fit nowhere. */
        public final long leftover;

        /** Whether a network refused the insert for lack of permission. */
        public final boolean denied;

        private Result(final List<TransferComponentInfo> rows, final List<TransferComponentInfo> cpuRows,
                final long leftover, final boolean denied) {
            this.rows = rows;
            this.cpuRows = cpuRows;
            this.leftover = leftover;
            this.denied = denied;
        }

        public boolean simulated() {
            return this.rows != null;
        }
    }

    /**
     * @param grids networks the rows were scanned on, the first is the one the stack is inserted into and the position
     *              of each is the tier of its rows
     * @param rows  rows to mark, storage rows are matched through {@link TransferDescription#liveHandlers}
     * @param query stack to insert, including its amount
     * @param src   who inserts it, which decides what security allows
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Result run(final List<? extends IGrid> grids, final List<TransferComponentInfo> rows,
            final IAEStack<?> query, final BaseActionSource src) {
        if (grids.isEmpty() || query == null || query.getStackSize() <= 0) {
            return Result.NONE;
        }

        final IStorageGrid storage = grids.get(0).getCache(IStorageGrid.class);
        final IMEMonitor monitor = storage == null ? null : storage.getMEMonitor(query.getStackType());
        if (monitor == null) {
            return Result.NONE;
        }

        final Map<IMEInventoryHandler<?>, Integer> rowByHandler = mapHandlers(rows, query.getStackType());

        final InjectTrace trace = new InjectTrace();
        final IAEStack<?> leftover;
        try {
            leftover = InjectTrace
                    .run(trace, () -> (IAEStack<?>) monitor.injectItems(query.copy(), Actionable.SIMULATE, src));
        } catch (final RuntimeException e) {
            // Storage from another mod may throw while simulating, which must not take the terminal down.
            return Result.NONE;
        }

        final long[] amounts = new long[rows.size()];
        final int[] orders = new int[rows.size()];
        final boolean[] sticky = new boolean[rows.size()];
        final List<Map<Integer, Long>> slots = new ArrayList<>(Collections.nCopies(rows.size(), null));
        Arrays.fill(orders, Integer.MAX_VALUE);

        for (final InjectTrace.Entry entry : trace.getEntries()) {
            final Integer index = rowByHandler.get(entry.handler);
            if (index == null) {
                continue;
            }
            amounts[index] += entry.getAccepted();
            orders[index] = Math.min(orders[index], entry.order);
            sticky[index] |= entry.stickyClaim;
            addSlot(rows.get(index).description, entry, slots, index);
        }

        final List<TransferComponentInfo> marked = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            final TransferComponentInfo row = rows.get(index);
            if (row.description.liveHandlers == null) {
                marked.add(row);
                continue;
            }
            marked.add(
                    row.withSimulation(amounts[index], orders[index], sticky[index], toSlotAmounts(slots.get(index))));
        }

        return new Result(
                marked,
                cpuRows(grids, trace.getCpus()),
                leftover == null ? 0 : leftover.getStackSize(),
                trace.anyDenied());
    }

    private static Map<IMEInventoryHandler<?>, Integer> mapHandlers(final List<TransferComponentInfo> rows,
            final IAEStackType<?> type) {
        final Map<IMEInventoryHandler<?>, Integer> rowByHandler = new IdentityHashMap<>();
        for (int index = 0; index < rows.size(); index++) {
            final TransferDescription description = rows.get(index).description;
            if (description.liveHandlers == null) {
                continue;
            }
            try {
                for (final IMEInventoryHandler<?> handler : description.liveHandlers.apply(type)) {
                    rowByHandler.putIfAbsent(handler, index);
                }
            } catch (final RuntimeException e) {
                // A component from another mod failing to list its handlers only loses its own row.
            }
        }
        return rowByHandler;
    }

    private static void addSlot(final TransferDescription description, final InjectTrace.Entry entry,
            final List<Map<Integer, Long>> slots, final int index) {
        if (description.slotOf == null || entry.getAccepted() <= 0) {
            return;
        }
        final int slot = description.slotOf.applyAsInt(entry.handler);
        if (slot < 0) {
            return;
        }
        Map<Integer, Long> bySlot = slots.get(index);
        if (bySlot == null) {
            bySlot = new LinkedHashMap<>();
            slots.set(index, bySlot);
        }
        bySlot.merge(slot, entry.getAccepted(), Long::sum);
    }

    private static List<TransferComponentInfo.SlotAmount> toSlotAmounts(final Map<Integer, Long> bySlot) {
        if (bySlot == null) {
            return Collections.emptyList();
        }
        final List<TransferComponentInfo.SlotAmount> result = new ArrayList<>(bySlot.size());
        for (final Map.Entry<Integer, Long> entry : bySlot.entrySet()) {
            result.add(new TransferComponentInfo.SlotAmount(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * CPUs are not machines of their own, so their rows only exist while an insert is simulated, and only for CPUs that
     * would take part of the stack.
     */
    private static List<TransferComponentInfo> cpuRows(final List<? extends IGrid> grids,
            final List<InjectTrace.CpuEntry> entries) {
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        final Map<CraftingCPUCluster, TransferComponentInfo> rows = new LinkedHashMap<>();
        for (final InjectTrace.CpuEntry entry : entries) {
            final int tier = indexOf(grids, entry.grid);
            final TransferComponentInfo existing = rows.get(entry.cpu);
            if (existing != null) {
                rows.put(
                        entry.cpu,
                        existing.withSimulation(
                                existing.simulatedAmount + entry.accepted,
                                existing.simulationOrder,
                                false,
                                Collections.emptyList()));
                continue;
            }
            final TransferComponentInfo row = cpuRow(entry.cpu, tier);
            if (tier >= 0 && row != null) {
                rows.put(entry.cpu, row.withSimulation(entry.accepted, entry.order, false, Collections.emptyList()));
            }
        }
        return new ArrayList<>(rows.values());
    }

    private static TransferComponentInfo cpuRow(final CraftingCPUCluster cpu, final int tier) {
        final Iterator<IGridHost> tiles = cpu.getTiles();
        if (!tiles.hasNext()) {
            return null;
        }
        final IGridNode node = tiles.next().getGridNode(ForgeDirection.UNKNOWN);
        if (node == null) {
            return null;
        }
        final IGridBlock block = node.getGridBlock();
        final DimensionalCoord location = block.getLocation();
        final ItemStack icon = block.getMachineRepresentation();
        final String name = cpu.getName();
        return new TransferComponentInfo(
                icon,
                name == null ? "" : name,
                null,
                location.x,
                location.y,
                location.z,
                location.getDimension(),
                (byte) ForgeDirection.UNKNOWN.ordinal(),
                tier,
                TransferDescription.builder().canExport(true).build());
    }

    private static int indexOf(final List<? extends IGrid> grids, final IGrid grid) {
        for (int index = 0; index < grids.size(); index++) {
            if (grids.get(index) == grid) {
                return index;
            }
        }
        return -1;
    }
}
