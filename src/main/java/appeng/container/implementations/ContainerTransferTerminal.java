package appeng.container.implementations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.AEApi;
import appeng.api.config.TransferDirection;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.parts.ITransferTerminal;
import appeng.api.storage.data.IAEStack;
import appeng.container.AEBaseContainer;
import appeng.container.sync.SyncRegistrar;
import appeng.container.sync.handlers.AEStackInventorySyncHandler;
import appeng.container.sync.handlers.IntSyncHandler;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketTransferTerminalUpdate;
import appeng.helpers.transfer.TransferPacketChunks;
import appeng.helpers.transfer.TransferResultFilter;
import appeng.helpers.transfer.TransferScanner;
import appeng.parts.misc.PartStorageBus;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.Platform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ContainerTransferTerminal extends AEBaseContainer {

    private static final int FINGERPRINT_INTERVAL = 20;
    private static final int MAX_PACKET_BYTES = 512 * 1024;

    private final ITransferTerminal anchor;
    private final IAEStackInventory query = new IAEStackInventory(null, 1);
    public final AEStackInventorySyncHandler querySync;
    public final IntSyncHandler modeSync;

    private List<TransferComponentInfo> scanned = Collections.emptyList();
    private List<IGrid> grids = Collections.emptyList();
    private long fingerprint;
    private boolean hasScanned;
    private boolean queryChanged;
    private boolean wasOffline;
    private int ticksSinceFingerprint;
    private List<byte[]> lastSent;

    public ContainerTransferTerminal(final InventoryPlayer ip, final ITransferTerminal anchor) {
        super(ip, anchor);
        this.anchor = anchor;

        final SyncRegistrar sync = this.syncRegistrar();
        this.querySync = sync.aeStackInventory("query", this.query).onServerChange(i -> this.queryChanged = true);
        this.modeSync = sync.intC2S("mode").onServerChange((oldValue, newValue) -> this.queryChanged = true);
        this.modeSync.setLocalValue(TransferDirection.BOTH.ordinal());

        this.bindPlayerInventory(ip, 0, -31);
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isClient()) {
            return;
        }

        super.detectAndSendChanges();

        final IGridNode node = this.anchor.getActionableNode();
        if (node == null || !node.isActive()) {
            if (!this.wasOffline) {
                this.wasOffline = true;
                this.hasScanned = false;
                this.send(Collections.emptyList());
            }
            return;
        }
        this.wasOffline = false;

        if (!this.hasScanned || this.anchor.needsUpdate() || this.fingerprintChanged()) {
            this.scan(node);
            this.queryChanged = true;
        }

        if (this.queryChanged) {
            this.queryChanged = false;
            this.send(this.serialize(TransferResultFilter.apply(this.scanned, this.getQuery(), this.getMode())));
        }
    }

    private boolean fingerprintChanged() {
        if (++this.ticksSinceFingerprint < FINGERPRINT_INTERVAL) {
            return false;
        }
        this.ticksSinceFingerprint = 0;
        return TransferScanner.fingerprint(this.grids) != this.fingerprint;
    }

    private void scan(final IGridNode node) {
        final IGrid grid = node.getGrid();
        this.grids = new ArrayList<>(grid.getAllRecursiveGridConnections(PartStorageBus.class, true));
        this.scanned = TransferScanner.scan(this.grids, AEApi.instance().registries().transferInspectors());
        this.fingerprint = TransferScanner.fingerprint(this.grids);
        this.ticksSinceFingerprint = 0;
        this.hasScanned = true;
    }

    public IAEStack<?> getQuery() {
        return this.query.getAEStackInSlot(0);
    }

    private TransferDirection getMode() {
        final TransferDirection[] directions = TransferDirection.values();
        final int mode = this.modeSync.get();
        return mode >= 0 && mode < directions.length ? directions[mode] : TransferDirection.BOTH;
    }

    private List<byte[]> serialize(final List<TransferComponentInfo> rows) {
        final List<byte[]> result = new ArrayList<>(rows.size());
        for (final TransferComponentInfo row : rows) {
            final ByteBuf buffer = Unpooled.buffer();
            row.write(buffer);
            final byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            result.add(bytes);
        }
        return result;
    }

    private void send(final List<byte[]> rows) {
        if (this.isSameAsLastSent(rows)) {
            return;
        }
        this.lastSent = rows;

        final List<List<byte[]>> chunks = TransferPacketChunks.split(rows, MAX_PACKET_BYTES);
        for (int index = 0; index < chunks.size(); index++) {
            final PacketTransferTerminalUpdate packet = new PacketTransferTerminalUpdate(
                    index == 0,
                    index == chunks.size() - 1,
                    chunks.get(index));
            for (final Object crafter : this.crafters) {
                if (crafter instanceof EntityPlayerMP player) {
                    NetworkHandler.instance.sendTo(packet, player);
                }
            }
        }
    }

    private boolean isSameAsLastSent(final List<byte[]> rows) {
        if (this.lastSent == null || this.lastSent.size() != rows.size()) {
            return false;
        }
        for (int index = 0; index < rows.size(); index++) {
            if (!Arrays.equals(this.lastSent.get(index), rows.get(index))) {
                return false;
            }
        }
        return true;
    }
}
