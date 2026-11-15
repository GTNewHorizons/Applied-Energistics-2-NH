package appeng.crafting.target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.crafting.IPatternTarget;
import appeng.api.crafting.IPatternTargetMatcher;
import appeng.api.crafting.PatternMatch;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IInterfaceViewable;
import appeng.core.AELog;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.me.cache.CraftingGridCache;
import appeng.parts.misc.PartPatternRepeater;

/**
 * Finds the pattern providers whose machine can run a pattern.
 */
public final class PatternTargetQuery {

    public static final class Entry {

        public final IInterfaceViewable provider;
        public final PatternMatch match;
        public final int freeSlots;

        private Entry(IInterfaceViewable provider, PatternMatch match, int freeSlots) {
            this.provider = provider;
            this.match = match;
            this.freeSlots = freeSlots;
        }
    }

    public static final class Result {

        public final List<Entry> entries;
        public final int distinctKeys;
        public final long nanos;

        private Result(List<Entry> entries, int distinctKeys, long nanos) {
            this.entries = entries;
            this.distinctKeys = distinctKeys;
            this.nanos = nanos;
        }
    }

    private PatternTargetQuery() {}

    /**
     * Matches the pattern against every provider reachable from the grid, including grids behind Pattern Repeaters;
     * call it on the server thread.
     */
    public static Result run(IGrid grid, ICraftingPatternDetails pattern) {
        final long start = System.nanoTime();

        final Set<IAEStackType<?>> neededTypes = new HashSet<>();
        for (IAEStack<?> input : pattern.getCondensedAEInputs()) {
            if (input != null) {
                neededTypes.add(input.getStackType());
            }
        }

        final List<IInterfaceViewable> providers = new ArrayList<>();
        final List<Object> keys = new ArrayList<>();
        final Map<Class<?>, Set<Object>> keysByClass = new HashMap<>();
        final Set<IInterfaceViewable> visited = new HashSet<>();
        final Set<Class<? extends IInterfaceViewable>> supported = AEApi.instance().registries().interfaceTerminal()
                .getSupportedClasses();

        for (IGrid currentGrid : collectReachableGrids(grid)) {
            for (Class<? extends IInterfaceViewable> c : supported) {
                for (IGridNode node : currentGrid.getMachines(c)) {
                    final IInterfaceViewable machine = (IInterfaceViewable) node.getMachine();
                    if (!node.isActive() || !visited.add(machine) || !machine.shouldDisplay()) {
                        continue;
                    }

                    providers.add(machine);
                    if (!supportsAll(machine.getSupportedStackTypes(), neededTypes)) {
                        keys.add(StackTypeMismatch.INSTANCE);
                        continue;
                    }

                    final IPatternTarget target = machine.getPatternTarget();
                    final Object key = target == null ? null : target.getTargetKey();
                    keys.add(key);
                    if (key != null) {
                        keysByClass.computeIfAbsent(key.getClass(), k -> new HashSet<>()).add(key);
                    }
                }
            }
        }

        final Map<Object, PatternMatch> results = new HashMap<>();
        int distinctKeys = 0;
        for (Map.Entry<Class<?>, Set<Object>> e : keysByClass.entrySet()) {
            distinctKeys += e.getValue().size();
            results.putAll(matchKeys(e.getKey(), pattern, e.getValue()));
        }

        final List<Entry> entries = new ArrayList<>(providers.size());
        for (int i = 0; i < providers.size(); i++) {
            final IInterfaceViewable provider = providers.get(i);
            final Object key = keys.get(i);
            final PatternMatch match;
            if (key == StackTypeMismatch.INSTANCE) {
                match = PatternMatch.NO_MATCH;
            } else if (key == null) {
                match = PatternMatch.UNKNOWN;
            } else {
                match = results.getOrDefault(key, PatternMatch.UNKNOWN);
            }
            entries.add(new Entry(provider, match, countFreeSlots(provider)));
        }

        return new Result(entries, distinctKeys, System.nanoTime() - start);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, PatternMatch> matchKeys(Class<?> keyClass, ICraftingPatternDetails pattern,
            Set<Object> keys) {
        final IPatternTargetMatcher<Object> matcher = (IPatternTargetMatcher<Object>) (IPatternTargetMatcher<?>) AEApi
                .instance().registries().patternTargets().getMatcher(keyClass);
        if (matcher == null) {
            return Collections.emptyMap();
        }

        // A broken matcher from another mod must not take the query down with it.
        try {
            final Map<Object, PatternMatch> results = matcher.match(pattern, Collections.unmodifiableSet(keys));
            return results == null ? Collections.emptyMap() : results;
        } catch (RuntimeException e) {
            AELog.error(e, "Pattern target matcher for " + keyClass.getName() + " failed");
            return Collections.emptyMap();
        }
    }

    private static boolean supportsAll(IAEStackType<?>[] supportedTypes, Set<IAEStackType<?>> neededTypes) {
        for (IAEStackType<?> needed : neededTypes) {
            boolean found = false;
            for (IAEStackType<?> type : supportedTypes) {
                if (type == needed) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static int countFreeSlots(IInterfaceViewable provider) {
        final IInventory patterns = provider.getPatterns();
        if (patterns == null) {
            return 0;
        }

        final int slots = Math.min(provider.numSlots(), patterns.getSizeInventory());
        int free = 0;
        for (int i = 0; i < slots; i++) {
            if (patterns.getStackInSlot(i) == null) {
                free++;
            }
        }
        return free;
    }

    /**
     * Puts one pattern into the first free slot of a provider reachable from the grid; returns false if the provider is
     * gone, full or the player lacks BUILD on its grid.
     */
    public static boolean insertPattern(IInterfaceViewable provider, IGrid grid, EntityPlayer player,
            ItemStack pattern) {
        final IInventory patterns = provider.getPatterns();
        if (!canModify(provider, grid, player) || patterns == null) {
            return false;
        }
        final int slots = Math.min(provider.numSlots(), patterns.getSizeInventory());
        for (int i = 0; i < slots; i++) {
            if (patterns.getStackInSlot(i) == null && patterns.isItemValidForSlot(i, pattern)) {
                final ItemStack single = pattern.copy();
                single.stackSize = 1;
                patterns.setInventorySlotContents(i, single);
                return true;
            }
        }
        return false;
    }

    /**
     * Lists the patterns on the grid and on grids behind Pattern Repeaters that make the same primary output.
     */
    public static List<Conflict> findConflicts(IGrid grid, ICraftingPatternDetails pattern) {
        final IAEStack<?> output = primaryOutput(pattern);
        if (output == null) {
            return Collections.emptyList();
        }

        final List<Conflict> conflicts = new ArrayList<>();
        for (IGrid currentGrid : collectReachableGrids(grid)) {
            if (!(currentGrid.getCache(ICraftingGrid.class) instanceof CraftingGridCache craftingGrid)) {
                continue;
            }
            final List<ICraftingPatternDetails> existing = craftingGrid.getCraftingMultiPatterns().get(output);
            if (existing == null) {
                continue;
            }
            for (ICraftingPatternDetails details : existing) {
                // A Pattern Repeater re-offers patterns of another grid; that grid is walked too, so skipping the
                // repeater here keeps each pattern once.
                for (ICraftingMedium medium : craftingGrid.getMediums(details)) {
                    final Object host = medium instanceof DualityInterface duality ? duality.getHost() : medium;
                    final IInterfaceViewable provider = host instanceof IInterfaceViewable viewable ? viewable : null;
                    if (provider != null) {
                        addConflict(conflicts, provider, details, sameRecipe(details, pattern));
                    }
                }
            }
        }
        return conflicts;
    }

    /**
     * Takes the conflicting pattern out of its provider; returns null if it moved, or the player lacks BUILD.
     */
    public static ItemStack extractPattern(Conflict conflict, IGrid grid, EntityPlayer player) {
        final IInventory patterns = conflict.provider.getPatterns();
        if (!canModify(conflict.provider, grid, player) || patterns == null
                || conflict.slot >= patterns.getSizeInventory()
                || !ItemStack.areItemStacksEqual(patterns.getStackInSlot(conflict.slot), conflict.pattern)) {
            return null;
        }
        final ItemStack removed = patterns.getStackInSlot(conflict.slot);
        patterns.setInventorySlotContents(conflict.slot, null);
        return removed;
    }

    private static void addConflict(List<Conflict> conflicts, IInterfaceViewable provider,
            ICraftingPatternDetails details, boolean sameRecipe) {
        final IInventory patterns = provider.getPatterns();
        if (patterns == null) {
            return;
        }
        final int slots = Math.min(provider.numSlots(), patterns.getSizeInventory());
        for (int i = 0; i < slots; i++) {
            final ItemStack stack = patterns.getStackInSlot(i);
            if (stack != null && ItemStack.areItemStacksEqual(stack, details.getPattern())) {
                for (Conflict known : conflicts) {
                    if (known.provider == provider && known.slot == i) {
                        return;
                    }
                }
                conflicts.add(new Conflict(provider, i, stack.copy(), sameRecipe));
                return;
            }
        }
    }

    private static IAEStack<?> primaryOutput(ICraftingPatternDetails pattern) {
        for (IAEStack<?> output : pattern.getCondensedAEOutputs()) {
            if (output != null) {
                return output;
            }
        }
        return null;
    }

    private static boolean sameRecipe(ICraftingPatternDetails a, ICraftingPatternDetails b) {
        return a.isCraftable() == b.isCraftable() && sameStacks(a.getCondensedAEInputs(), b.getCondensedAEInputs())
                && sameStacks(a.getCondensedAEOutputs(), b.getCondensedAEOutputs());
    }

    // Condensed arrays have no fixed order, so each stack is matched against any unused one of the other side.
    private static boolean sameStacks(IAEStack<?>[] a, IAEStack<?>[] b) {
        if (a.length != b.length) {
            return false;
        }
        final boolean[] used = new boolean[b.length];
        for (IAEStack<?> stack : a) {
            boolean found = false;
            for (int i = 0; i < b.length; i++) {
                if (!used[i] && stack != null && stack.equals(b[i]) && stack.getStackSize() == b[i].getStackSize()) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the free upgrade slot of an ME interface that takes this Pattern Capacity Card, or -1.
     */
    public static int findCapacityCardSlot(IInterfaceViewable provider, ItemStack card) {
        if (!(provider instanceof IInterfaceHost host)) {
            return -1;
        }
        final IInventory upgrades = host.getInterfaceDuality().getUpgrades();
        for (int i = 0; i < upgrades.getSizeInventory(); i++) {
            if (upgrades.getStackInSlot(i) == null && upgrades.isItemValidForSlot(i, card)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Puts one Pattern Capacity Card into the given upgrade slot found by {@link #findCapacityCardSlot}.
     */
    public static void installCapacityCard(IInterfaceViewable provider, int slot, ItemStack card) {
        final ItemStack single = card.copy();
        single.stackSize = 1;
        ((IInterfaceHost) provider).getInterfaceDuality().getUpgrades().setInventorySlotContents(slot, single);
    }

    /**
     * Returns whether the provider is still in place, reachable from the grid and open to the player's BUILD.
     */
    public static boolean canModify(IInterfaceViewable provider, IGrid grid, EntityPlayer player) {
        // The provider was picked when the list was built, so it may have been broken or unlinked since.
        final TileEntity tile = provider.getTileEntity();
        if (tile == null || tile.isInvalid()
                || tile.getWorldObj().getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) != tile) {
            return false;
        }
        final IGridNode node = provider.getGridNode(ForgeDirection.UNKNOWN);
        if (node == null || !node.isActive() || !collectReachableGrids(grid).contains(node.getGrid())) {
            return false;
        }
        final ISecurityGrid security = node.getGrid().getCache(ISecurityGrid.class);
        return security.hasPermission(player, SecurityPermissions.BUILD);
    }

    // Same walk as ContainerInterfaceTerminal#collectReachableGrids.
    private static Set<IGrid> collectReachableGrids(IGrid grid) {
        final Set<IGrid> grids = new HashSet<>();
        grids.add(grid);
        collectRepeaterGrids(grid, grids);
        return grids;
    }

    private static void collectRepeaterGrids(IGrid grid, Set<IGrid> grids) {
        for (IGridNode node : grid.getMachines(PartPatternRepeater.class)) {
            final PartPatternRepeater rep = (PartPatternRepeater) node.getMachine();
            if (!rep.isProvider() || rep.getPair() == null || !node.isActive() || rep.getPair().isProvider()) continue;
            final IGridNode n = rep.getPair().getGridNode();
            if (n == null || !n.isActive()) continue;
            final IGrid currentGrid = n.getGrid();
            if (grids.add(currentGrid)) {
                collectRepeaterGrids(currentGrid, grids);
            }
        }
    }

    private enum StackTypeMismatch {
        INSTANCE
    }

    public static final class Conflict {

        public final IInterfaceViewable provider;
        public final int slot;
        public final ItemStack pattern;
        public final boolean sameRecipe;

        private Conflict(IInterfaceViewable provider, int slot, ItemStack pattern, boolean sameRecipe) {
            this.provider = provider;
            this.slot = slot;
            this.pattern = pattern;
            this.sameRecipe = sameRecipe;
        }
    }
}
