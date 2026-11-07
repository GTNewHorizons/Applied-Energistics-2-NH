package appeng.crafting.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import org.apache.logging.log4j.Level;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.CraftingMode;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingCallback;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.AEStackTypeRegistry;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.core.AELog;
import appeng.core.localization.PlayerMessages;
import appeng.crafting.MECraftingInventory;
import appeng.crafting.v2.CraftingContext.RequestInProcessing;
import appeng.crafting.v2.CraftingRequest.SubstitutionMode;
import appeng.crafting.v2.resolvers.CraftingTask;
import appeng.crafting.v2.resolvers.ExtractItemResolver.ExtractItemTask;
import appeng.hooks.TickHandler;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.util.Platform;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * A new, self-contained implementation of the crafting calculator. Does an iterative search on the crafting recipe
 * tree.
 */
public class CraftingJobV2<StackType extends IAEStack<StackType>>
        implements ICraftingJob<StackType>, Future<ICraftingJob<StackType>>, ITreeSerializable {

    protected volatile long totalByteCost = -1; // -1 means it needs to be recalculated

    public CraftingContext getContext() {
        return context;
    }

    protected CraftingContext context;
    public CraftingRequest originalRequest;
    protected ICraftingCallback callback;
    protected String errorMessage = "";

    protected enum State {
        RUNNING,
        FINISHED,
        CANCELLED
    }

    protected State state = State.RUNNING;

    /**
     * Limits how often the plan gets re-resolved because items disappeared, in case the network keeps changing.
     */
    private static final int MAX_RECONCILE_ROUNDS_PLAYER = 5;
    private static final int MAX_RECONCILE_ROUNDS_MACHINE = 1;
    /** How many of the items that went missing are named in the chat message, the rest are shortened */
    private static final int MAX_REPORTED_MISSING_STACKS = 3;
    private int reconcileRounds = 0;

    public CraftingJobV2(final World world, final IGrid meGrid, final BaseActionSource actionSource,
            final StackType what, final ICraftingCallback callback) {
        this(world, meGrid, actionSource, what, CraftingMode.STANDARD, callback);
    }

    public CraftingJobV2(final World world, final IGrid meGrid, final BaseActionSource actionSource,
            final StackType what, final CraftingMode craftingMode, final ICraftingCallback callback) {
        this.context = new CraftingContext(world, meGrid, actionSource);
        this.callback = callback;
        this.originalRequest = new CraftingRequest(what, SubstitutionMode.PRECISE_FRESH, true, craftingMode);
        this.context.addRequest(this.originalRequest);
        this.context.itemModel.ignore(what);
    }

    public CraftingJobV2(CraftingTreeSerializer serializer, ITreeSerializable parent) throws IOException {
        this.totalByteCost = serializer.getBuffer().readLong();
        this.state = serializer.readEnum(State.class);
        this.errorMessage = ByteBufUtils.readUTF8String(serializer.getBuffer());
        this.originalRequest = new CraftingRequest(serializer, this);
    }

    public static CraftingJobV2 deserialize(World world, ByteBuf buffer) {
        if (buffer.readableBytes() < 1) {
            return null;
        }
        final CraftingTreeSerializer serializer = new CraftingTreeSerializer(world, buffer);
        try {
            serializer.initializeSerializer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final ITreeSerializable rawJob;
        try {
            rawJob = serializer.readSerializableAndQueueChildren(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!(rawJob instanceof CraftingJobV2 job)) {
            throw new UnsupportedOperationException("Invalid job type deserialized: " + rawJob.getClass());
        }
        while (serializer.hasWork()) {
            try {
                serializer.doWork();
            } catch (IndexOutOfBoundsException e) {
                // can not serialize any more items, cut off the tree
                AELog.warn(e, "Ran out of assigned space for crafting tree serialization");
                serializer.doBestEffortWork();
                break;
            }
        }
        return job;
    }

    @Override
    public CraftingMode getCraftingMode() {
        return this.originalRequest.craftingMode;
    }

    public ByteBuf serialize() {
        try {
            final CraftingTreeSerializer serializer = new CraftingTreeSerializer(context.world);
            try {
                serializer.writeSerializableAndQueueChildren(this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            while (serializer.hasWork()) {
                try {
                    serializer.doWork();
                } catch (IndexOutOfBoundsException e) {
                    // can not serialize any more items, cut off the tree
                    AELog.warn(e, "Ran out of assigned space for crafting tree serialization");
                    break;
                }
            }
            return serializer.finalizeSerializer().slice();
        } catch (Exception e) {
            AELog.error(e, "Could not serialize the crafting job");
            return Unpooled.buffer(0);
        }
    }

    @Override
    public boolean isSimulation() {
        return state == State.CANCELLED || context.wasSimulated;
    }

    @Override
    public long getByteTotal() {
        long byteCost = totalByteCost;
        if (byteCost < 0) {
            byteCost = 0;
            for (RequestInProcessing request : context.getLiveRequests()) {
                byteCost += request.request.getByteCost();
            }
            totalByteCost = byteCost;
        }
        return byteCost;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void populatePlan(IItemList<IAEStack<?>> plan) {
        for (CraftingTask task : context.getResolvedTasks()) {
            task.populatePlan(plan);
        }
    }

    @Override
    public List<? extends ITreeSerializable> serializeTree(CraftingTreeSerializer serializer) throws IOException {
        if (this.state == State.RUNNING) {
            throw new IllegalStateException("Can't serialize a running crafting simulation");
        }
        if (this.originalRequest == null) {
            throw new IllegalStateException("Can't serialize a null request");
        }
        serializer.getBuffer().writeLong(getByteTotal());
        serializer.writeEnum(state);
        ByteBufUtils.writeUTF8String(serializer.getBuffer(), errorMessage);
        return originalRequest.serializeTree(serializer);
    }

    @Override
    public ITreeSerializable getSerializationParent() {
        return originalRequest;
    }

    @Override
    public void loadChildren(List<ITreeSerializable> children) throws IOException {
        originalRequest.loadChildren(children);
    }

    @Override
    public StackType getOutput() {
        return (StackType) originalRequest.stack;
    }

    @Override
    public boolean simulateFor(int milli) {
        if (this.state != State.RUNNING) {
            return false;
        }
        final long startTime = System.currentTimeMillis();
        final long finishTime = startTime + milli;
        CraftingTask.State taskState = CraftingTask.State.NEEDS_MORE_WORK;
        try {
            do {
                taskState = context.doWork();
                totalByteCost = -1;
                // The plan was made against a snapshot of the AE system from when the job started, check it still
                // holds. A plan with missing items can't be started anyway, so there is nothing to keep up to date,
                // and neither is a job that got cancelled in the meantime.
                if (taskState == CraftingTask.State.SUCCESS && state == State.RUNNING
                        && !context.wasSimulated
                        && reconcile()) {
                    taskState = CraftingTask.State.NEEDS_MORE_WORK;
                }
            } while (taskState.needsMoreWork && System.currentTimeMillis() < finishTime && (state == State.RUNNING));
        } catch (Exception e) {
            AELog.error(e, "Error while simulating crafting for " + originalRequest);
            errorMessage = e.toString();
            this.state = State.CANCELLED;
            if (callback != null) {
                callback.calculationComplete(this);
            }
            return false;
        }

        if (!taskState.needsMoreWork) {
            getByteTotal();
            this.state = taskState == CraftingTask.State.FAILURE ? State.CANCELLED : State.FINISHED;
            if (AELog.isCraftingDebugLogEnabled()) {
                AELog.log(Level.INFO, "Crafting job for %s finished with resolved steps:", originalRequest.toString());
                AELog.logSimple(Level.INFO, context.toString());
            }
            if (callback != null) {
                callback.calculationComplete(this);
            }
        }

        return taskState.needsMoreWork;
    }

    /**
     * Checks a finished plan against the current contents of the AE system. If items it plans to extract have
     * disappeared since the calculation started (e.g. used by another craft), the missing amount gets resolved again as
     * part of this same job: crafted if possible, otherwise shown as missing.
     *
     * @return If the job went back to running, in which case it has to be scheduled again before it can be started
     */
    public boolean reconcileWithLiveStorage() {
        if (this.state != State.FINISHED || context.wasSimulated) {
            return false;
        }
        try {
            if (!reconcile()) {
                return false;
            }
        } catch (Exception e) {
            // Called from the confirm screen's start button, surface the problem there instead of throwing out of it
            AELog.error(e, "Error while re-planning crafting for " + originalRequest);
            errorMessage = e.toString();
            this.state = State.CANCELLED;
            return true;
        }
        this.state = State.RUNNING;
        return true;
    }

    /**
     * @return If any requests got reopened and need more work. Throws if a shrunk request could not be reopened, or if
     *         items are missing but the plan has already been re-resolved as often as allowed: the plan must never be
     *         started short.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private boolean reconcile() {
        if (getCraftingMode() == CraftingMode.IGNORE_MISSING) {
            // The crafting CPU waits for whatever is missing when such a job starts (see ExtractItemTask.startOnCpu),
            // re-planning would only turn the job into a simulation that can't be submitted at all
            return false;
        }
        final IStorageGrid sg = context.meGrid.getCache(IStorageGrid.class);
        if (sg == null) {
            return false;
        }

        final Set<CraftingTask> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        final List<ExtractItemTask<?>> extractTasks = new ArrayList<>();
        final IItemList<IAEStack<?>> planned = AEApi.instance().storage().createAEStackList();
        for (CraftingTask task : context.getResolvedTasks()) {
            if (task instanceof ExtractItemTask<?>extractTask && seen.add(task)) {
                extractTasks.add(extractTask);
                for (IAEStack<?> removed : extractTask.removedFromSystem) {
                    planned.add(removed.copy());
                }
            }
        }

        // Find out what is short before changing anything, so that a plan which can't be re-resolved anymore is left
        // intact for the error instead of being shrunk
        final IItemList<IAEStack<?>> shortfalls = AEApi.instance().storage().createAEStackList();
        for (IAEStack<?> plannedStack : planned) {
            final IMEMonitor monitor = sg.getMEMonitor(plannedStack.getStackType());
            final IAEStack<?> found = monitor == null ? null
                    : (IAEStack<?>) monitor.getStorageList().findPrecise(plannedStack);
            final long live = found == null ? 0 : Math.max(0, found.getStackSize());
            final long shortfall = plannedStack.getStackSize() - live;
            if (shortfall > 0) {
                shortfalls.add(plannedStack.copy().setStackSize(shortfall));
            }
        }
        if (shortfalls.isEmpty()) {
            return false;
        }
        if (reconcileRounds >= getMaxReconcileRounds()) {
            throw new IllegalStateException(
                    "Ingredients are still missing after re-planning " + reconcileRounds
                            + " time(s), the network keeps changing. Missing: "
                            + shortfalls);
        }

        // Insertion ordered, so the requests get reopened (and the new tasks queued) in the same order every time.
        // Requests don't override equals, so this still de-duplicates by identity.
        final Set<CraftingRequest> toReopen = new LinkedHashSet<>();
        /** How much of each stack the plan gave up on extracting */
        final IItemList<IAEStack<?>> shrunkByStack = AEApi.instance().storage().createAEStackList();
        for (IAEStack<?> shortStack : shortfalls) {
            long shortfall = shortStack.getStackSize();
            // Latest extractions first, those are the furthest from the root of the tree
            for (int i = extractTasks.size() - 1; i >= 0 && shortfall > 0; i--) {
                final ExtractItemTask<?> task = extractTasks.get(i);
                final long shrunk = task.shrinkSystemExtraction(shortStack, shortfall);
                if (shrunk > 0) {
                    shortfall -= shrunk;
                    shrunkByStack.add(shortStack.copy().setStackSize(shrunk));
                    toReopen.add(task.request);
                }
            }
        }
        if (toReopen.isEmpty()) {
            return false;
        }

        // Bring the whole snapshot up to date, not just the stacks the plan already used: re-resolving may need other
        // ingredients that have changed as well, and the plan shown to the player compares against the right amounts
        resyncSnapshot(sg, shrunkByStack);

        reconcileRounds++;
        notifyPlayerOfRetry(shrunkByStack);
        for (CraftingRequest request : toReopen) {
            // The extractions are already shrunk, so a request that can't be reopened would leave the plan short and
            // the job would stall on the CPU. Fail the job instead.
            if (!context.reopenRequest(request)) {
                throw new IllegalStateException(
                        "Could not re-resolve " + request + " after the items it planned to extract went missing");
            }
        }
        totalByteCost = -1;
        if (AELog.isCraftingDebugLogEnabled()) {
            AELog.log(
                    Level.INFO,
                    "Crafting job for %s re-resolving %d requests whose items disappeared",
                    originalRequest.toString(),
                    toReopen.size());
        }
        return true;
    }

    /**
     * Applies every change of the AE system since the snapshot was taken to the item model and the snapshot itself.
     * Only the differences are applied, so items the plan already extracted, returned or produced as leftovers stay
     * accounted for.
     *
     * @param shrunkByStack What the plan gave up on extracting because it is gone. The item model never held those, so
     *                      they must not be subtracted from it a second time.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void resyncSnapshot(IStorageGrid sg, IItemList<IAEStack<?>> shrunkByStack) {
        for (IAEStackType<?> type : AEStackTypeRegistry.getAllTypes()) {
            final IMEMonitor monitor = sg.getMEMonitor(type);
            if (monitor == null) {
                continue;
            }
            final IItemList<IAEStack<?>> live = monitor.getStorageList();
            for (IAEStack<?> liveStack : live) {
                applyLiveAmount(liveStack, Math.max(0, liveStack.getStackSize()), shrunkByStack);
            }
            // Stacks of the snapshot that the system doesn't hold at all anymore
            for (IAEStack<?> cached : (Iterable<IAEStack<?>>) context.availableCache
                    .getAvailableItems(type.createList())) {
                if (live.findPrecise((IAEStack) cached) == null) {
                    applyLiveAmount(cached, 0, shrunkByStack);
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void applyLiveAmount(IAEStack<?> stack, long liveAmount, IItemList<IAEStack<?>> shrunkByStack) {
        final IAEStack<?> cached = context.availableCache.findPrecise((IAEStack) stack);
        final long cachedAmount = cached == null ? 0 : cached.getStackSize();
        final IAEStack<?> shrunk = shrunkByStack.findPrecise((IAEStack) stack);
        final long delta = liveAmount - cachedAmount + (shrunk == null ? 0 : shrunk.getStackSize());
        // The item being crafted was taken out of the model by ignore() in the constructor, so the plan never extracts
        // the very thing it is making. Keep it out, only the snapshot learns the live amount.
        if (delta != 0 && !stack.equals(originalRequest.stack)) {
            final IAEStack<?> modelled = context.itemModel.findPrecise((IAEStack) stack);
            final long modelledAmount = modelled == null ? 0 : modelled.getStackSize();
            setAvailableAmount(context.itemModel, stack, Math.max(0, modelledAmount + delta));
        }
        if (liveAmount != cachedAmount) {
            setAvailableAmount(context.availableCache, stack, liveAmount);
        }
    }

    /**
     * Players get several attempts to keep up with a changing network, machines (e.g. level maintainers) only one, they
     * will simply request again later.
     */
    private int getMaxReconcileRounds() {
        return context.actionSource != null && context.actionSource.isPlayer() ? MAX_RECONCILE_ROUNDS_PLAYER
                : MAX_RECONCILE_ROUNDS_MACHINE;
    }

    /**
     * Tells the player who requested the job that it is being recalculated, and which ingredients caused it.
     */
    private void notifyPlayerOfRetry(IItemList<IAEStack<?>> wentMissing) {
        if (!(context.actionSource instanceof PlayerSource playerSource) || playerSource.player == null
                || wentMissing.isEmpty()) {
            return;
        }
        try {
            final IChatComponent missingList = new ChatComponentText("");
            int reported = 0;
            for (IAEStack<?> stack : wentMissing) {
                if (reported >= MAX_REPORTED_MISSING_STACKS) {
                    missingList.appendText(", ...");
                    break;
                }
                if (reported > 0) {
                    missingList.appendText(", ");
                }
                missingList.appendText(Platform.formatChatCount(stack.getStackSize()) + " * ");
                missingList.appendSibling(Platform.formatChatStackName(stack));
                reported++;
            }
            playerSource.player.addChatMessage(
                    new ChatComponentTranslation(
                            PlayerMessages.CraftingIngredientsChangedRetry.getUnlocalized(),
                            reconcileRounds,
                            getMaxReconcileRounds(),
                            missingList));
        } catch (Exception e) {
            AELog.error(e, "Could not notify player of crafting recalculation");
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void setAvailableAmount(MECraftingInventory inventory, IAEStack<?> stack, long amount) {
        final IAEStack<?> existing = inventory.findPrecise((IAEStack) stack);
        if (existing != null) {
            existing.setStackSize(amount);
        } else if (amount > 0) {
            inventory.injectItems(stack.copy().setStackSize(amount), Actionable.MODULATE);
        }
    }

    @Override
    public Future<ICraftingJob<StackType>> schedule() {
        TickHandler.INSTANCE.registerCraftingSimulation(this.context.world, this);
        return this;
    }

    @Override
    public boolean supportsCPUCluster(ICraftingCPU cluster) {
        return cluster instanceof CraftingCPUCluster;
    }

    @Override
    public void startCrafting(MECraftingInventory storage, ICraftingCPU rawCluster, BaseActionSource src) {
        if (this.state != State.FINISHED) {
            throw new IllegalStateException(
                    "Trying to start crafting a not fully calculated job for " + originalRequest.toString());
        }
        CraftingCPUCluster cluster = (CraftingCPUCluster) rawCluster;
        context.actionSource = src;
        List<CraftingTask> resolvedTasks = context.getResolvedTasks();
        for (CraftingTask task : resolvedTasks) {
            task.startOnCpu(context, cluster, storage);
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (this.state != State.RUNNING) {
            return false;
        } else {
            this.state = State.CANCELLED;
            return true;
        }
    }

    @Override
    public boolean isCancelled() {
        return state == State.CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state != State.RUNNING;
    }

    @Override
    public CraftingJobV2 get() throws InterruptedException, ExecutionException {
        this.simulateFor(Integer.MAX_VALUE);
        return this;
    }

    @Override
    public CraftingJobV2 get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        try {
            this.simulateFor((int) unit.convert(timeout, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
        return switch (this.state) {
            case RUNNING -> throw new TimeoutException();
            case CANCELLED -> throw new InterruptedException();
            case FINISHED -> this;
            default -> throw new IllegalStateException();
        };
    }

    public MECraftingInventory getStorageAtBeginning() {
        return getContext().availableCache;
    }

    @Override
    public boolean supportsOptimization() {
        return true;
    }
}
