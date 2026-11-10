package appeng.container.sync.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import appeng.api.stacks.GenericStack;
import appeng.api.storage.GenericStackSlots;
import appeng.container.sync.AbstractSyncHandler;
import appeng.container.sync.SyncDirection;
import appeng.container.sync.SyncEndpoint;
import appeng.container.sync.SyncManager;
import appeng.container.sync.SyncMode;
import appeng.container.sync.deltas.GenericStackSlotDelta;
import appeng.util.Platform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class GenericStackSlotsSyncHandler<T extends GenericStackSlots> extends AbstractSyncHandler {

    private static final int DEFAULT_SERVER_DIFF_CHECK_INTERVAL = 1;
    private static final int DEFAULT_CLIENT_DIFF_CHECK_INTERVAL = 0;

    @FunctionalInterface
    public interface InventoryChangeListener<T extends GenericStackSlots> {

        void onChange(@NonNull T inventory);
    }

    private final @NonNull T inventory;
    private final List<GenericStackSlotDelta> pendingDeltas = new ArrayList<>();
    private @Nullable GenericStack[] lastSentSnapshot;
    private int diffCheckInterval;
    private int diffCheckTicks;
    private @Nullable InventoryChangeListener<T> clientChangeListener;
    private @Nullable InventoryChangeListener<T> serverChangeListener;

    public GenericStackSlotsSyncHandler(final SyncManager manager, final String key, final String fullKey,
            final SyncDirection direction, final @NonNull T inventory) {
        super(manager, key, fullKey, direction);
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.lastSentSnapshot = this.copyInventory();
        this.diffCheckInterval = Platform.isClient() ? DEFAULT_CLIENT_DIFF_CHECK_INTERVAL
                : DEFAULT_SERVER_DIFF_CHECK_INTERVAL;
        this.dirty = false;
    }

    public @NonNull T get() {
        return this.inventory;
    }

    public void markDirty() {
        this.clearQueuedDeltas();
        this.markDirtyInternal();
    }

    public void applyAndQueueDelta(final GenericStackSlotDelta delta) {
        this.applyDelta(delta);
        this.pendingDeltas.add(delta);
        this.markDirtyInternal();
    }

    /**
     * Sets how often the handler performs a periodic full inventory diff without a dirty mark. Dirty marks, queued
     * deltas, and forced full resyncs are still sent immediately. Use 0 to disable periodic checks.
     */
    public @NonNull GenericStackSlotsSyncHandler<T> setDiffCheckInterval(final int ticks,
            final @NonNull SyncDirection applicableDirection) {
        if (!applicableDirection.canSendFrom(this.getLocalEndpoint())) {
            return this;
        }

        final int interval = Math.max(0, ticks);
        this.diffCheckInterval = interval;
        this.diffCheckTicks = this.initialDiffCheckTicks(interval);
        return this;
    }

    public @NonNull GenericStackSlotsSyncHandler<T> onClientChange(final InventoryChangeListener<T> listener) {
        this.clientChangeListener = listener;
        return this;
    }

    public @NonNull GenericStackSlotsSyncHandler<T> onServerChange(final InventoryChangeListener<T> listener) {
        this.serverChangeListener = listener;
        return this;
    }

    @Override
    protected SyncMode writeUpdate(final SyncEndpoint localEndpoint, final boolean initialSync, final ByteBuf buf)
            throws IOException {
        final boolean forceFullResync = this.shouldForceFullResync();
        if (!this.canSendFrom(localEndpoint)) {
            return null;
        }
        if (initialSync) {
            if (!this.sendsInitialStateFrom(localEndpoint)) {
                return null;
            }
        } else if (!this.dirty && !forceFullResync) {
            if (!this.shouldRunDiffCheck()) {
                return null;
            }
        }

        List<GenericStackSlotDelta> detectedDeltas = null;
        if (!initialSync && !forceFullResync && this.pendingDeltas.isEmpty()) {
            detectedDeltas = this.diff();
            if (detectedDeltas == null || detectedDeltas.isEmpty()) {
                this.clearQueuedDeltas();
                this.dirty = false;
                return null;
            }
        }

        SyncMode mode = SyncMode.FULL;
        ByteBuf payload = buf;

        if (!initialSync && !forceFullResync) {
            final ByteBuf fullPayload = Unpooled.buffer();
            this.writeFull(fullPayload);

            final ByteBuf deltaPayload = this.writeDeltaPayload(detectedDeltas);
            if (deltaPayload != null && deltaPayload.readableBytes() < fullPayload.readableBytes()) {
                mode = SyncMode.DELTA;
                payload = deltaPayload;
            } else {
                payload = fullPayload;
            }
        } else {
            this.writeFull(payload);
        }

        if (payload != buf) {
            buf.writeBytes(payload, payload.readerIndex(), payload.readableBytes());
        }

        this.lastSentSnapshot = this.copyInventory();
        this.clearQueuedDeltas();
        this.dirty = false;
        this.clearFullResyncRequest();
        return mode;
    }

    @Override
    protected void readUpdate(final SyncEndpoint remoteEndpoint, final SyncMode mode, final ByteBuf buf)
            throws IOException {
        if (mode == SyncMode.FULL) {
            this.readFull(buf);
        } else if (mode == SyncMode.DELTA) {
            try {
                this.applyDeltaPayload(buf);
            } catch (final IOException e) {
                if (this.getDirection() == SyncDirection.CLIENT_TO_SERVER || remoteEndpoint == SyncEndpoint.SERVER) {
                    this.requestRemoteFullResync();
                } else {
                    this.requestFullResync();
                }
                throw e;
            }
        } else {
            throw new IOException(
                    "GenericStackSlotsSyncHandler for " + this.fullKey + " does not support " + mode + ".");
        }

        this.lastSentSnapshot = this.copyInventory();
        this.clearQueuedDeltas();
        this.dirty = false;

        if (remoteEndpoint == SyncEndpoint.SERVER) {
            if (this.clientChangeListener != null) {
                this.clientChangeListener.onChange(this.inventory);
            }
        } else if (this.serverChangeListener != null) {
            this.serverChangeListener.onChange(this.inventory);
        }
    }

    @Override
    protected String getTypeKey() {
        return GenericStackSlots.class.getName();
    }

    private void writeFull(final ByteBuf buf) throws IOException {
        for (int slot = 0; slot < this.inventory.getSizeInventory(); slot++) {
            GenericStack.writeBuffer(buf, this.inventory.getStack(slot));
        }
    }

    private void readFull(final ByteBuf buf) throws IOException {
        for (int slot = 0; slot < this.inventory.getSizeInventory(); slot++) {
            this.inventory.setStack(slot, GenericStack.readBuffer(buf));
        }
    }

    private @Nullable ByteBuf writeDeltaPayload(final @Nullable List<GenericStackSlotDelta> detectedDeltas)
            throws IOException {
        final List<GenericStackSlotDelta> deltas = this.pendingDeltas.isEmpty() ? detectedDeltas : this.pendingDeltas;
        if (deltas == null || deltas.isEmpty()) {
            return null;
        }

        final ByteBuf deltaPayload = Unpooled.buffer();
        deltaPayload.writeInt(deltas.size());
        for (final GenericStackSlotDelta delta : deltas) {
            deltaPayload.writeInt(delta.getSlotIndex());
            GenericStack.writeBuffer(deltaPayload, delta.getStack());
        }
        return deltaPayload;
    }

    private @Nullable List<GenericStackSlotDelta> diff() {
        List<GenericStackSlotDelta> deltas = null;
        for (int slot = 0; slot < this.inventory.getSizeInventory(); slot++) {
            final GenericStack previousStack = this.lastSentSnapshot[slot];
            final GenericStack currentStack = this.inventory.getStack(slot);
            if (!Objects.equals(previousStack, currentStack)) {
                if (deltas == null) {
                    deltas = new ArrayList<>();
                }
                deltas.add(new GenericStackSlotDelta(slot, currentStack));
            }
        }

        return deltas;
    }

    private void clearQueuedDeltas() {
        this.pendingDeltas.clear();
    }

    private boolean shouldRunDiffCheck() {
        final int interval = this.diffCheckInterval;
        if (interval <= 0) {
            return false;
        }
        if (interval <= 1) {
            return true;
        }

        this.diffCheckTicks = Math.min(this.diffCheckTicks, interval - 1);
        if (this.diffCheckTicks > 0) {
            this.diffCheckTicks--;
            return false;
        }

        this.diffCheckTicks = interval - 1;
        return true;
    }

    private SyncEndpoint getLocalEndpoint() {
        return Platform.isClient() ? SyncEndpoint.CLIENT : SyncEndpoint.SERVER;
    }

    private int initialDiffCheckTicks(final int interval) {
        if (interval <= 1) {
            return 0;
        }

        return Math.floorMod(this.fullKey.hashCode(), interval);
    }

    private void applyDeltaPayload(final ByteBuf buf) throws IOException {
        final int deltaCount = buf.readInt();
        if (deltaCount <= 0) {
            throw new IOException("Invalid delta count " + deltaCount + " for " + this.fullKey + ".");
        }

        for (int i = 0; i < deltaCount; i++) {
            final int slotIndex = buf.readInt();
            if (slotIndex < 0 || slotIndex >= this.inventory.getSizeInventory()) {
                throw new IOException("Invalid slot index: " + slotIndex);
            }
            this.inventory.setStack(slotIndex, GenericStack.readBuffer(buf));
        }
    }

    private void applyDelta(final GenericStackSlotDelta delta) {
        final int slotIndex = delta.getSlotIndex();
        if (slotIndex < 0 || slotIndex >= this.inventory.getSizeInventory()) {
            throw new IllegalArgumentException("Invalid slot index: " + slotIndex);
        }

        this.inventory.setStack(slotIndex, delta.getStack());
    }

    private @Nullable GenericStack[] copyInventory() {
        final @Nullable GenericStack[] copy = new GenericStack[this.inventory.getSizeInventory()];
        for (int slot = 0; slot < copy.length; slot++) {
            copy[slot] = this.inventory.getStack(slot);
        }

        return copy;
    }
}
