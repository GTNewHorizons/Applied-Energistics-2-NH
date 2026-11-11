package appeng.test.me.storage;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.DimensionManager;

import org.junit.jupiter.api.Test;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.me.cache.SecurityCache;
import appeng.me.storage.InjectTrace;
import appeng.me.storage.NetworkInventoryHandler;
import appeng.test.DummySaveHandler;
import appeng.test.mockme.MockAESystem;
import appeng.util.item.AEItemStack;

public class NetworkInventoryHandlerTraceFunctionalTest {

    private static World world;
    private static final BaseActionSource SOURCE = new BaseActionSource();

    private static World world() {
        if (world != null) {
            return world;
        }
        if (!DimensionManager.isDimensionRegistered(256)) {
            DimensionManager.registerProviderType(256, WorldProviderSurface.class, false);
            DimensionManager.registerDimension(256, 256);
        }
        world = new WorldServer(
                MinecraftServer.getServer(),
                new DummySaveHandler(),
                "DummyTestWorld",
                256,
                new WorldSettings(256, GameType.SURVIVAL, false, false, WorldType.DEFAULT),
                MinecraftServer.getServer().theProfiler) {

            @Override
            public File getChunkSaveLocation() {
                return new File("dummy-ignoreme");
            }
        };
        return world;
    }

    private static NetworkInventoryHandler<IAEItemStack> network(final MockAESystem system) {
        return new NetworkInventoryHandler<>(
                ITEM_STACK_TYPE,
                (SecurityCache) system.grid.getCache(ISecurityGrid.class));
    }

    private static IAEItemStack iron(final long amount) {
        final IAEItemStack stack = AEItemStack.create(new ItemStack(Items.iron_ingot));
        stack.setStackSize(amount);
        return stack;
    }

    private static IAEItemStack simulate(final NetworkInventoryHandler<IAEItemStack> network, final InjectTrace trace,
            final long amount) {
        return InjectTrace.run(trace, () -> network.injectItems(iron(amount), Actionable.SIMULATE, SOURCE));
    }

    private static long acceptedBy(final InjectTrace trace, final IMEInventoryHandler<?> handler) {
        long total = 0;
        for (final InjectTrace.Entry entry : trace.getEntries()) {
            if (entry.handler == handler) {
                total += entry.getAccepted();
            }
        }
        return total;
    }

    @Test
    public void overflowIsRecordedInPriorityOrder() {
        final NetworkInventoryHandler<IAEItemStack> network = network(new MockAESystem(world()));
        final Storage high = new Storage(20).priority(10);
        final Storage low = new Storage(100);
        network.addNewStorage(low);
        network.addNewStorage(high);

        final InjectTrace trace = new InjectTrace();
        final IAEItemStack leftover = simulate(network, trace, 64);

        assertNull(leftover);
        final List<InjectTrace.Entry> entries = trace.getEntries();
        assertEquals(2, entries.size());
        assertSame(high, entries.get(0).handler);
        assertEquals(20, entries.get(0).getAccepted());
        assertSame(low, entries.get(1).handler);
        assertEquals(44, entries.get(1).getAccepted());
        assertTrue(entries.get(0).order < entries.get(1).order);
        assertEquals(1, entries.get(0).depth);
        assertEquals(0, high.stored, "a simulation must not store anything");
    }

    @Test
    public void leftoverMatchesWhatNoHandlerTook() {
        final NetworkInventoryHandler<IAEItemStack> network = network(new MockAESystem(world()));
        final Storage small = new Storage(10);
        network.addNewStorage(small);

        final InjectTrace trace = new InjectTrace();
        final IAEItemStack leftover = simulate(network, trace, 64);

        assertNotNull(leftover);
        assertEquals(54, leftover.getStackSize());
        assertEquals(10, acceptedBy(trace, small));
    }

    @Test
    public void passTwoOnlyCountsWhatPassOneDidNotTake() {
        final NetworkInventoryHandler<IAEItemStack> network = network(new MockAESystem(world()));
        // Already holds iron, so pass 1 visits it, and as it is not whitelisted pass 2 visits it again.
        final Storage holding = new Storage(15).holding(5);
        final Storage empty = new Storage(100);
        network.addNewStorage(holding);
        network.addNewStorage(empty);

        final InjectTrace trace = new InjectTrace();
        final IAEItemStack leftover = simulate(network, trace, 64);

        assertNull(leftover);
        assertEquals(10, acceptedBy(trace, holding), "the same space must not be counted twice");
        assertEquals(54, acceptedBy(trace, empty));
    }

    @Test
    public void stickyClaimIsRecordedEvenWhenNothingFits() {
        final NetworkInventoryHandler<IAEItemStack> network = network(new MockAESystem(world()));
        final Storage sticky = new Storage(0).whitelisted().sticky();
        final Storage other = new Storage(100);
        network.addNewStorage(sticky);
        network.addNewStorage(other);

        final InjectTrace trace = new InjectTrace();
        final IAEItemStack leftover = simulate(network, trace, 64);

        assertNotNull(leftover);
        assertEquals(64, leftover.getStackSize());
        assertEquals(1, trace.getEntries().size(), "a sticky claim shuts every other handler out");
        final InjectTrace.Entry entry = trace.getEntries().get(0);
        assertSame(sticky, entry.handler);
        assertTrue(entry.stickyClaim);
        assertEquals(0, entry.getAccepted());
    }

    @Test
    public void subnetworkHandlersAreRecordedInsideTheOuterOne() {
        final MockAESystem outerSystem = new MockAESystem(world());
        final MockAESystem innerSystem = new MockAESystem(world());
        final NetworkInventoryHandler<IAEItemStack> outer = network(outerSystem);
        final NetworkInventoryHandler<IAEItemStack> inner = network(innerSystem);
        final Storage first = new Storage(30);
        final Storage second = new Storage(30).priority(-1);
        inner.addNewStorage(first);
        inner.addNewStorage(second);
        outer.addNewStorage(inner);

        final InjectTrace trace = new InjectTrace();
        final IAEItemStack leftover = simulate(outer, trace, 64);

        assertNotNull(leftover);
        assertEquals(4, leftover.getStackSize());
        assertEquals(60, acceptedBy(trace, inner));
        assertEquals(30, acceptedBy(trace, first));
        assertEquals(30, acceptedBy(trace, second));

        final InjectTrace.Entry outerEntry = trace.getEntries().get(0);
        assertSame(inner, outerEntry.handler);
        assertSame(outerSystem.grid, outerEntry.grid);
        assertEquals(1, outerEntry.depth);
        for (final InjectTrace.Entry entry : trace.getEntries().subList(1, trace.getEntries().size())) {
            assertSame(innerSystem.grid, entry.grid);
            assertEquals(2, entry.depth);
            assertTrue(entry.order > outerEntry.order);
        }
    }

    @Test
    public void subnetworkVisitedByBothPassesIsCountedOnce() {
        final NetworkInventoryHandler<IAEItemStack> outer = network(new MockAESystem(world()));
        final NetworkInventoryHandler<IAEItemStack> inner = network(new MockAESystem(world()));
        // Already holds iron, so the inner network can give some up and pass 1 of the outer network visits it.
        // Pass 2 then hands it the whole stack again, and the inner network routes it a second time.
        final Storage storage = new Storage(15).holding(5);
        inner.addNewStorage(storage);
        outer.addNewStorage(inner);

        final InjectTrace trace = new InjectTrace();
        final IAEItemStack leftover = simulate(outer, trace, 64);

        assertNotNull(leftover);
        assertEquals(54, leftover.getStackSize());
        assertEquals(10, acceptedBy(trace, inner));
        assertEquals(10, acceptedBy(trace, storage), "the subnetwork's storage must not be counted for each pass");
    }

    @Test
    public void aSimulationThatThrowsLeavesTheNetworkUsable() {
        final NetworkInventoryHandler<IAEItemStack> network = network(new MockAESystem(world()));
        final Storage storage = new Storage(100).failing();
        network.addNewStorage(storage);

        assertThrows(
                IllegalStateException.class,
                () -> NetworkInventoryHandler.guardSimulation(() -> simulate(network, new InjectTrace(), 64)));

        storage.failing = false;
        assertNull(
                simulate(network, new InjectTrace(), 64),
                "the network must not still count itself as busy with the simulation that threw");
    }

    @Test
    public void realInsertionsAreNotRecorded() {
        final NetworkInventoryHandler<IAEItemStack> network = network(new MockAESystem(world()));
        final Storage storage = new Storage(100);
        network.addNewStorage(storage);

        final InjectTrace trace = new InjectTrace();
        InjectTrace.run(trace, () -> network.injectItems(iron(64), Actionable.MODULATE, SOURCE));

        assertTrue(trace.getEntries().isEmpty());
        assertEquals(64, storage.stored);
    }

    @Test
    public void traceEndsWithRun() {
        final InjectTrace trace = new InjectTrace();
        InjectTrace.run(trace, () -> {
            assertSame(trace, InjectTrace.current(Actionable.SIMULATE));
            assertNull(InjectTrace.current(Actionable.MODULATE));
            return null;
        });
        assertNull(InjectTrace.current(Actionable.SIMULATE));
    }

    /** Storage for iron with a fixed capacity. */
    private static final class Storage implements IMEInventoryHandler<IAEItemStack> {

        private final long capacity;
        private long stored;
        private int priority;
        private boolean whitelisted;
        private boolean sticky;
        private boolean failing;

        private Storage(final long capacity) {
            this.capacity = capacity;
        }

        private Storage priority(final int value) {
            this.priority = value;
            return this;
        }

        private Storage holding(final long amount) {
            this.stored = amount;
            return this;
        }

        private Storage whitelisted() {
            this.whitelisted = true;
            return this;
        }

        private Storage sticky() {
            this.sticky = true;
            return this;
        }

        /** Throws on every insert, like broken storage from another mod. */
        private Storage failing() {
            this.failing = true;
            return this;
        }

        @Override
        public IAEItemStack injectItems(final IAEItemStack input, final Actionable type, final BaseActionSource src) {
            if (this.failing) {
                throw new IllegalStateException("broken storage");
            }
            final long accepted = Math.min(Math.max(0, this.capacity - this.stored), input.getStackSize());
            if (type == Actionable.MODULATE) {
                this.stored += accepted;
            }
            if (accepted == input.getStackSize()) {
                return null;
            }
            final IAEItemStack leftover = input.copy();
            leftover.setStackSize(input.getStackSize() - accepted);
            return leftover;
        }

        @Override
        public IAEItemStack extractItems(final IAEItemStack request, final Actionable mode,
                final BaseActionSource src) {
            if (this.stored <= 0) {
                return null;
            }
            final IAEItemStack out = request.copy();
            out.setStackSize(Math.min(this.stored, request.getStackSize()));
            return out;
        }

        @Override
        public IItemList<IAEItemStack> getAvailableItems(final IItemList<IAEItemStack> out, final int iteration) {
            return out;
        }

        @Override
        public IAEStackType<?> getStackType() {
            return ITEM_STACK_TYPE;
        }

        @Override
        public AccessRestriction getAccess() {
            return AccessRestriction.READ_WRITE;
        }

        @Override
        public boolean isPrioritized(final IAEItemStack input) {
            return this.whitelisted;
        }

        @Override
        public boolean canAccept(final IAEItemStack input) {
            return true;
        }

        @Override
        public int getPriority() {
            return this.priority;
        }

        @Override
        public int getSlot() {
            return 0;
        }

        @Override
        public boolean validForPass(final int i) {
            return true;
        }

        @Override
        public boolean getSticky() {
            return this.sticky;
        }
    }
}
