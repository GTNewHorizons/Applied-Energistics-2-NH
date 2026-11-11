package appeng.me.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEStack;
import appeng.me.cluster.implementations.CraftingCPUCluster;

/**
 * Records where a simulated insert into networked storage would put a stack.
 * <p>
 * While {@link #run} is active on a thread, every {@code Actionable.SIMULATE} injection routed by a
 * {@link NetworkInventoryHandler} on that thread reports each handler it hands the stack to, including the handlers of
 * subnetworks reached through storage buses. Real insertions are never recorded.
 */
public final class InjectTrace {

    private static final ThreadLocal<InjectTrace> CURRENT = new ThreadLocal<>();

    /** One hand-over of the stack from a network to one of its storage handlers. */
    public static final class Entry {

        public final IMEInventoryHandler<?> handler;

        /** Network that routed the stack, null if it could not name itself. */
        @Nullable
        public final IGrid grid;

        /** 1 for the network the insert started on, one more for each subnetwork it passed through. */
        public final int depth;

        /** Position of this hand-over among all recorded events, in the order they happened. */
        public final int order;

        /** Whether a sticky handler claimed the stack here, which shuts out every non-sticky handler. */
        public final boolean stickyClaim;

        private long accepted;

        /** Whether a later hand-over of the same stack recorded this again, see {@link #discard}. */
        private boolean discarded;

        private Entry(final IMEInventoryHandler<?> handler, @Nullable final IGrid grid, final int depth,
                final int order, final boolean stickyClaim) {
            this.handler = handler;
            this.grid = grid;
            this.depth = depth;
            this.order = order;
            this.stickyClaim = stickyClaim;
        }

        /** Amount the handler took, including anything it passed on to a subnetwork. */
        public long getAccepted() {
            return this.accepted;
        }
    }

    /** Amount a crafting CPU that waits for the stack would take. */
    public static final class CpuEntry {

        public final CraftingCPUCluster cpu;
        @Nullable
        public final IGrid grid;
        public final int order;
        public final long accepted;

        private boolean discarded;

        private CpuEntry(final CraftingCPUCluster cpu, @Nullable final IGrid grid, final int order,
                final long accepted) {
            this.cpu = cpu;
            this.grid = grid;
            this.order = order;
            this.accepted = accepted;
        }
    }

    /** A point in the record, taken with {@link #mark}. */
    static final class Mark {

        private final int entries;
        private final int cpus;

        private Mark(final int entries, final int cpus) {
            this.entries = entries;
            this.cpus = cpus;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final List<CpuEntry> cpus = new ArrayList<>();
    private final Set<IGrid> denied = Collections.newSetFromMap(new IdentityHashMap<>());
    private int nextOrder;

    /**
     * Runs {@code body} with simulated injections on this thread recorded into {@code trace}.
     */
    public static <R> R run(final InjectTrace trace, final Supplier<R> body) {
        final InjectTrace previous = CURRENT.get();
        CURRENT.set(trace);
        try {
            return body.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /**
     * @return the trace to record into, never one for a real insertion
     */
    @Nullable
    public static InjectTrace current(final Actionable type) {
        return type == Actionable.SIMULATE ? CURRENT.get() : null;
    }

    Entry begin(final IMEInventoryHandler<?> handler, @Nullable final IGrid grid, final int depth,
            final boolean stickyClaim) {
        final Entry entry = new Entry(handler, grid, depth, this.nextOrder++, stickyClaim);
        this.entries.add(entry);
        return entry;
    }

    /**
     * @param before size of the stack handed to the handler
     * @param after  what the handler gave back, null if it took everything
     */
    static void finish(@Nullable final Entry entry, final long before, @Nullable final IAEStack<?> after) {
        if (entry != null) {
            entry.accepted = Math.max(0, before - sizeOf(after));
        }
    }

    Mark mark() {
        return new Mark(this.entries.size(), this.cpus.size());
    }

    /**
     * Drops everything recorded between two marks. Pass 2 of a simulation hands a handler the stack again together with
     * what it took in pass 1, so a subnetwork behind that handler records its storage a second time with the combined
     * amount, and what it recorded in pass 1 has to go.
     */
    void discard(final Mark from, final Mark to) {
        for (int index = from.entries; index < to.entries; index++) {
            this.entries.get(index).discarded = true;
        }
        for (int index = from.cpus; index < to.cpus; index++) {
            this.cpus.get(index).discarded = true;
        }
    }

    void denied(@Nullable final IGrid grid) {
        if (grid != null) {
            this.denied.add(grid);
        }
    }

    /**
     * Records the part of a stack a crafting CPU would take, nothing is recorded if it takes none.
     */
    public void cpu(final CraftingCPUCluster cpu, @Nullable final IGrid grid, final long before,
            @Nullable final IAEStack<?> after) {
        final long accepted = before - sizeOf(after);
        if (accepted > 0) {
            this.cpus.add(new CpuEntry(cpu, grid, this.nextOrder++, accepted));
        }
    }

    public List<Entry> getEntries() {
        final List<Entry> result = new ArrayList<>(this.entries.size());
        for (final Entry entry : this.entries) {
            if (!entry.discarded) {
                result.add(entry);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<CpuEntry> getCpus() {
        final List<CpuEntry> result = new ArrayList<>(this.cpus.size());
        for (final CpuEntry cpu : this.cpus) {
            if (!cpu.discarded) {
                result.add(cpu);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * @return whether a network refused the insert because the source lacked permission
     */
    public boolean wasDenied(final IGrid grid) {
        return this.denied.contains(grid);
    }

    public boolean anyDenied() {
        return !this.denied.isEmpty();
    }

    private static long sizeOf(@Nullable final IAEStack<?> stack) {
        return stack == null ? 0 : stack.getStackSize();
    }
}
