package appeng.container.implementations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.config.Settings;
import appeng.api.config.TransferDirection;
import appeng.api.config.YesNo;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.parts.ITransferTerminal;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IConfigManager;
import appeng.container.AEBaseContainer;
import appeng.container.interfaces.IVirtualSlotSource;
import appeng.container.sync.SyncCodec;
import appeng.container.sync.SyncCodecs;
import appeng.container.sync.SyncRegistrar;
import appeng.container.sync.handlers.AEStackInventorySyncHandler;
import appeng.container.sync.handlers.BooleanSyncHandler;
import appeng.container.sync.handlers.ConfigEnumSyncHandler;
import appeng.container.sync.handlers.IntSyncHandler;
import appeng.container.sync.handlers.LongSyncHandler;
import appeng.container.sync.handlers.ObjectSyncHandler;
import appeng.core.CommonHelper;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketTransferTerminalUpdate;
import appeng.helpers.transfer.TransferInsertSimulation;
import appeng.helpers.transfer.TransferNameFilter;
import appeng.helpers.transfer.TransferPacketChunks;
import appeng.helpers.transfer.TransferResultFilter;
import appeng.helpers.transfer.TransferRowOrder;
import appeng.helpers.transfer.TransferRowWindow;
import appeng.helpers.transfer.TransferScanner;
import appeng.parts.misc.PartStorageBus;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.Platform;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ContainerTransferTerminal extends AEBaseContainer implements IVirtualSlotSource {

    private static final int FINGERPRINT_INTERVAL = 20;
    private static final int SIMULATION_INTERVAL = 20;
    /** Fewest ticks between two scans caused by network changes. */
    private static final int MIN_RESCAN_INTERVAL = 10;
    /**
     * Most ticks between two scans. Filters edited on a component, changes on linked networks and new links made from
     * another network raise no event here, so the rows are refreshed this often anyway.
     */
    private static final int MAX_RESCAN_INTERVAL = 100;
    private static final int MAX_PACKET_BYTES = 512 * 1024;
    private static final int DEFAULT_VISIBLE_ROWS = 3;
    private static final SyncCodec<String> STRING_CODEC = SyncCodecs.of(
            "transferTerminalString",
            ByteBufUtils::writeUTF8String,
            ByteBufUtils::readUTF8String,
            UnaryOperator.identity(),
            Objects::equals);

    private final ITransferTerminal anchor;
    private final IAEStackInventory query;
    public final AEStackInventorySyncHandler querySync;
    public final ConfigEnumSyncHandler<TransferDirection> directionSync;
    public final ConfigEnumSyncHandler<YesNo> craftingOnlySync;
    public final ConfigEnumSyncHandler<YesNo> hideUnfilteredSync;
    public final ConfigEnumSyncHandler<YesNo> networkOnlySync;

    /** Text the server filters by, empty while the client filters by itself. */
    public final ObjectSyncHandler<String> searchTextSync;

    /** Text in the search box, kept by the terminal for the next time it is opened. */
    public final ObjectSyncHandler<String> savedSearchSync;
    public final IntSyncHandler windowStartSync;
    public final IntSyncHandler windowSizeSync;
    public final ObjectSyncHandler<String> serverLanguageSync;

    /** Number of rows at the top that sticky storage on this network claims the searched stack for. */
    public final IntSyncHandler stickyCountSync;

    /** Whether the rows show where a simulated insert of the searched stack would go. */
    public final ConfigEnumSyncHandler<YesNo> simulateSync;

    /** Whether the rows currently carry a simulation, which needs a searched stack as well as the setting. */
    public final BooleanSyncHandler simulatingSync;

    /** How much of the searched stack a simulated insert could not put anywhere. */
    public final LongSyncHandler leftoverSync;

    /** Whether a network refused the simulated insert because of its security settings. */
    public final BooleanSyncHandler deniedSync;

    /** Settings every screen open on this terminal shares, with the handler that shows each. */
    private final Map<Settings, ConfigEnumSyncHandler<?>> sharedSettings = new EnumMap<>(Settings.class);

    /** The value of each shared setting this screen last applied. */
    private final Map<Settings, Enum<?>> appliedSettings = new EnumMap<>(Settings.class);

    /** The searched stack this screen last applied, the slot is shared by every screen too. */
    private IAEStack<?> appliedQuery;

    /** The terminal's change count at the last scan, see {@link ITransferTerminal#getChangeCount()}. */
    private long seenChangeCount;
    private int ticksSinceScan;

    /** Whether the network changed while the last scan was too recent to scan again. */
    private boolean rescanPending;

    private final Map<TransferComponentInfo, List<String>> configNames = new IdentityHashMap<>();
    private List<TransferComponentInfo> scanned = Collections.emptyList();

    /** Rows matching the query and filters, before any simulation. */
    private List<TransferComponentInfo> matching = Collections.emptyList();
    private List<TransferComponentInfo> filtered = Collections.emptyList();
    private int ticksSinceSimulation;
    private List<IGrid> grids = Collections.emptyList();
    private long fingerprint;
    private boolean hasScanned;
    private boolean queryChanged;
    private boolean windowChanged;
    private boolean wasOffline;
    private int ticksSinceFingerprint;
    private int lastSentTotal;
    private int lastSentStart;
    private List<byte[]> lastSent;

    public ContainerTransferTerminal(final InventoryPlayer ip, final ITransferTerminal anchor) {
        super(ip, anchor);
        this.anchor = anchor;

        final SyncRegistrar sync = this.syncRegistrar();
        final IConfigManager settings = anchor.getConfigManager();
        this.query = anchor.getQueryInventory();
        this.querySync = sync.aeStackInventory("query", this.query).onServerChange(i -> this.queryChanged = true);
        this.directionSync = sync
                .configEnum("direction", Settings.TRANSFER_DIRECTION, TransferDirection.class, settings);
        this.directionSync.onServerChange((oldValue, newValue) -> this.queryChanged = true);
        this.craftingOnlySync = sync
                .configEnum("craftingOnly", Settings.TRANSFER_CRAFTING_CARD_ONLY, YesNo.class, settings);
        this.craftingOnlySync.onServerChange((oldValue, newValue) -> this.queryChanged = true);
        this.hideUnfilteredSync = sync
                .configEnum("hideUnfiltered", Settings.TRANSFER_HIDE_UNFILTERED, YesNo.class, settings);
        this.hideUnfilteredSync.onServerChange((oldValue, newValue) -> this.queryChanged = true);
        this.networkOnlySync = sync.configEnum("networkOnly", Settings.TRANSFER_NETWORK_ONLY, YesNo.class, settings);
        this.networkOnlySync.onServerChange((oldValue, newValue) -> this.hasScanned = false);
        this.searchTextSync = sync.objectC2S("searchText", STRING_CODEC, "")
                .onServerChange((oldValue, newValue) -> this.queryChanged = true);
        this.savedSearchSync = sync.object("savedSearch", STRING_CODEC, anchor.getSearchText())
                .onServerChange((oldValue, newValue) -> anchor.setSearchText(newValue));
        this.windowStartSync = sync.intC2S("windowStart")
                .onServerChange((oldValue, newValue) -> this.windowChanged = true);
        this.windowSizeSync = sync.intC2S("windowSize")
                .onServerChange((oldValue, newValue) -> this.windowChanged = true);
        this.windowSizeSync.setLocalValue(TransferRowWindow.requestSize(DEFAULT_VISIBLE_ROWS));
        this.serverLanguageSync = sync.objectS2C("serverLanguage", STRING_CODEC, "");
        if (Platform.isServer()) {
            this.serverLanguageSync.set(CommonHelper.proxy.getServerLanguage());
        }
        this.stickyCountSync = sync.intS2C("stickyCount");
        this.simulateSync = sync.configEnum("simulate", Settings.TRANSFER_SIMULATE, YesNo.class, settings);
        this.simulateSync.onServerChange((oldValue, newValue) -> this.queryChanged = true);
        this.simulatingSync = sync.booleanS2C("simulating");
        this.leftoverSync = sync.longS2C("simulateLeftover");
        this.deniedSync = sync.booleanS2C("simulateDenied");

        this.sharedSettings.put(Settings.TRANSFER_DIRECTION, this.directionSync);
        this.sharedSettings.put(Settings.TRANSFER_CRAFTING_CARD_ONLY, this.craftingOnlySync);
        this.sharedSettings.put(Settings.TRANSFER_HIDE_UNFILTERED, this.hideUnfilteredSync);
        this.sharedSettings.put(Settings.TRANSFER_NETWORK_ONLY, this.networkOnlySync);
        this.sharedSettings.put(Settings.TRANSFER_SIMULATE, this.simulateSync);

        this.bindPlayerInventory(ip, 0, -31);
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isClient()) {
            return;
        }

        this.followSharedState();
        super.detectAndSendChanges();

        final IGridNode node = this.anchor.getActionableNode();
        if (node == null || !node.isActive()) {
            if (!this.wasOffline) {
                this.wasOffline = true;
                this.hasScanned = false;
                this.matching = Collections.emptyList();
                this.filtered = Collections.emptyList();
                this.clearSimulationState();
                this.send(0, 0, Collections.emptyList());
            }
            return;
        }
        this.wasOffline = false;

        this.ticksSinceScan++;
        if (!this.hasScanned) {
            this.scan(node);
            this.queryChanged = true;
        } else if (this.ticksSinceScan >= MAX_RESCAN_INTERVAL || this.rescanPending
                || this.anchor.getChangeCount() != this.seenChangeCount
                || this.fingerprintChanged()) {
                    // Storage that keeps changing would otherwise have every open screen walk all linked networks each
                    // tick.
                    if (this.ticksSinceScan < MIN_RESCAN_INTERVAL) {
                        this.rescanPending = true;
                    } else {
                        this.scan(node);
                        this.queryChanged = true;
                    }
                }

        if (this.queryChanged) {
            this.queryChanged = false;
            this.matching = TransferResultFilter.apply(this.scanned, this.getQuery(), this.getMode(), this::keep);
            this.applySimulation();
            this.windowChanged = true;
        } else if (this.isSimulating() && ++this.ticksSinceSimulation >= SIMULATION_INTERVAL) {
            // Storage fills and empties while the terminal is open, and only sends rows that changed.
            this.applySimulation();
            this.windowChanged = true;
        }

        if (this.windowChanged) {
            this.windowChanged = false;
            this.sendWindow();
        }
    }

    /**
     * The query slot and the settings belong to the terminal, so a change made through another player's screen only
     * shows up there, without this screen's handlers hearing of it. A setting changed on this screen's server side is
     * handed to the terminal in turn, so the other screens see it.
     */
    private void followSharedState() {
        final IConfigManager config = this.anchor.getConfigManager();
        for (final Map.Entry<Settings, ConfigEnumSyncHandler<?>> shared : this.sharedSettings.entrySet()) {
            final Settings setting = shared.getKey();
            final ConfigEnumSyncHandler<?> handler = shared.getValue();
            Enum<?> value = config.getSetting(setting);
            final Enum<?> applied = this.appliedSettings.put(setting, value);

            if (applied != null && value != applied) {
                handler.syncFromConfig();
            } else if (handler.get() != null && handler.get() != value) {
                value = handler.get();
                config.putSetting(setting, value);
                this.appliedSettings.put(setting, value);
            } else {
                continue;
            }

            if (setting == Settings.TRANSFER_NETWORK_ONLY) {
                this.hasScanned = false;
            } else {
                this.queryChanged = true;
            }
        }

        final IAEStack<?> query = this.getQuery();
        if (!sameStack(query, this.appliedQuery)) {
            this.appliedQuery = query == null ? null : query.copy();
            this.queryChanged = true;
        }

        // There is nothing to simulate without a searched stack, so emptying the slot turns the setting off.
        if (query == null && isYes(this.simulateSync)) {
            config.putSetting(Settings.TRANSFER_SIMULATE, YesNo.NO);
            this.appliedSettings.put(Settings.TRANSFER_SIMULATE, YesNo.NO);
            this.simulateSync.syncFromConfig();
            this.queryChanged = true;
        }
    }

    private static boolean sameStack(final IAEStack<?> a, final IAEStack<?> b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.isSameType((Object) b) && a.getStackSize() == b.getStackSize();
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
        this.grids = isYes(this.networkOnlySync) ? Collections.singletonList(grid)
                : this.visibleGrids(grid, grid.getAllRecursiveGridConnections(PartStorageBus.class, true));
        this.scanned = TransferScanner.scan(this.grids, AEApi.instance().registries().transferInspectors());
        this.configNames.clear();
        this.fingerprint = TransferScanner.fingerprint(this.grids);
        this.seenChangeCount = this.anchor.getChangeCount();
        this.ticksSinceScan = 0;
        this.rescanPending = false;
        this.ticksSinceFingerprint = 0;
        this.hasScanned = true;
    }

    /**
     * Linked networks reach this one through any storage bus touching an interface, including other players' networks.
     * Only those the player may build on are shown, the same permission the terminal itself needs to open.
     */
    private List<IGrid> visibleGrids(final IGrid own, final Collection<? extends IGrid> linked) {
        final List<IGrid> visible = new ArrayList<>(linked.size());
        for (final IGrid grid : linked) {
            if (grid == own || this.canBuild(grid)) {
                visible.add(grid);
            }
        }
        return visible;
    }

    private boolean canBuild(final IGrid grid) {
        final ISecurityGrid security = grid.getCache(ISecurityGrid.class);
        return security == null || security.hasPermission(this.getInventoryPlayer().player, SecurityPermissions.BUILD);
    }

    public IAEStack<?> getQuery() {
        return this.query.getAEStackInSlot(0);
    }

    /**
     * Simulating only asks where a stack entering storage would go, so it forces export without touching the saved
     * direction, which comes back once simulating is turned off.
     */
    public TransferDirection getMode() {
        if (this.isSimulating()) {
            return TransferDirection.EXPORT;
        }
        final TransferDirection mode = this.directionSync.get();
        return mode == null ? TransferDirection.BOTH : mode;
    }

    /**
     * @return whether the rows should show where a simulated insert would go, which needs a searched stack
     */
    public boolean isSimulating() {
        return isYes(this.simulateSync) && this.getQuery() != null;
    }

    private void applySimulation() {
        this.ticksSinceSimulation = 0;
        final IGridNode node = this.anchor.getActionableNode();
        final TransferInsertSimulation.Result result = this.isSimulating() && node != null
                ? TransferInsertSimulation.run(
                        this.grids.isEmpty() ? Collections.singletonList(node.getGrid()) : this.grids,
                        this.matching,
                        this.getQuery(),
                        // Inserted the way a player puts a stack into a terminal, so security applies to them.
                        new PlayerSource(this.getInventoryPlayer().player, this.anchor))
                : null;

        if (result == null || !result.simulated()) {
            this.filtered = this.matching;
            this.clearSimulationState();
            this.stickyCountSync.set(this.countStickyRows());
            return;
        }

        final List<TransferComponentInfo> rows = new ArrayList<>(result.rows);
        for (final TransferComponentInfo cpu : result.cpuRows) {
            // CPUs have no config, so a search by config names never matches them.
            if (this.searchTextSync.get().isEmpty()) {
                rows.add(cpu);
            }
        }
        rows.sort(TransferRowOrder.simulated(TransferRowOrder.comparator(true, TransferDirection.EXPORT)));
        this.filtered = rows;
        this.simulatingSync.set(true);
        this.leftoverSync.set(result.leftover);
        this.deniedSync.set(result.denied);
        // The amounts show what sticky storage claims, the separator would only repeat it.
        this.stickyCountSync.set(0);
    }

    /**
     * Forgets the last simulation, so its notes do not stay on a screen whose rows no longer carry it.
     */
    private void clearSimulationState() {
        this.simulatingSync.set(false);
        this.leftoverSync.set(0L);
        this.deniedSync.set(false);
        this.stickyCountSync.set(0);
    }

    /**
     * Called when the amount dialog opened from the query slot reopens the terminal with the new amount.
     */
    @Override
    public void updateVirtualSlot(final StorageName invName, final int slotId, final IAEStack<?> aes) {
        this.query.putAEStackInSlot(0, aes);
        this.queryChanged = true;
    }

    /**
     * @return the rows the terminal lists, in order
     */
    public List<TransferComponentInfo> getFilteredRows() {
        return Collections.unmodifiableList(this.filtered);
    }

    public static boolean isYes(final ObjectSyncHandler<YesNo> sync) {
        return sync.get() == YesNo.YES;
    }

    /**
     * Sticky storage that claims a stack shuts out every other storage on its network, so the separator below these
     * rows is only meaningful when searching for one stack in export mode. Other networks route by themselves.
     */
    private int countStickyRows() {
        if (this.getQuery() == null || this.getMode() != TransferDirection.EXPORT) {
            return 0;
        }

        int count = 0;
        for (final TransferComponentInfo row : this.filtered) {
            if (row.networkTier != 0 || !row.stickyCapture) {
                break;
            }
            count++;
        }
        return count;
    }

    private boolean keep(final TransferComponentInfo row) {
        if (!TransferResultFilter
                .passesToggles(row.description, isYes(this.craftingOnlySync), isYes(this.hideUnfilteredSync))) {
            return false;
        }

        final String text = this.searchTextSync.get();
        return text.isEmpty() || TransferNameFilter
                .matches(this.configNames.computeIfAbsent(row, ContainerTransferTerminal::readConfigNames), text);
    }

    private static List<String> readConfigNames(final TransferComponentInfo row) {
        final List<String> names = new ArrayList<>(row.description.config.size());
        for (final TransferDescription.ConfigEntry entry : row.description.config) {
            names.add(displayName(entry.stack));
        }
        return names;
    }

    /**
     * Some mods' items reach for client only classes when naming themselves, which must not take a dedicated server
     * down.
     */
    private static String displayName(final IAEStack<?> stack) {
        try {
            final String name = stack.getDisplayName();
            return name == null ? "" : name;
        } catch (final RuntimeException | LinkageError e) {
            return "";
        }
    }

    private void sendWindow() {
        final int total = this.filtered.size();
        final int size = this.windowSizeSync.get();
        final int start = TransferRowWindow.clampStart(this.windowStartSync.get(), size, total);
        final int end = TransferRowWindow.clampEnd(start, size, total);
        this.send(total, start, this.serialize(this.filtered.subList(start, end)));
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

    private void send(final int total, final int start, final List<byte[]> rows) {
        if (total == this.lastSentTotal && start == this.lastSentStart && this.isSameAsLastSent(rows)) {
            return;
        }
        this.lastSentTotal = total;
        this.lastSentStart = start;
        this.lastSent = rows;

        final List<List<byte[]>> chunks = TransferPacketChunks.split(rows, MAX_PACKET_BYTES);
        for (int index = 0; index < chunks.size(); index++) {
            final PacketTransferTerminalUpdate packet = new PacketTransferTerminalUpdate(
                    index == 0,
                    index == chunks.size() - 1,
                    total,
                    start,
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
