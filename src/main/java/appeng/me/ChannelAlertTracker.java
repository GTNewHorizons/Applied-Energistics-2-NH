package appeng.me;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.WorldCoord;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketChannelAlert;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.parts.AEBasePart;

/** Server-thread-only, short-lived attribution of channel recalculations to successful player placements. */
public final class ChannelAlertTracker {

    private static final int PLACEMENT_LIFETIME_TICKS = 20;
    private static final int MAX_PENDING_PLACEMENTS = 128;
    private static final Map<World, List<Placement>> PLACEMENTS = new WeakHashMap<>();
    private static long sequence;

    private ChannelAlertTracker() {}

    public static void recordPlacement(EntityPlayer player, World world, IGridProxyable machine) {
        if (world.isRemote || !(player instanceof EntityPlayerMP)) return;
        final List<Placement> placements = PLACEMENTS.computeIfAbsent(world, ignored -> new ArrayList<>());
        final AENetworkProxy proxy = machine.getProxy();
        placements.removeIf(
                p -> expired(p.tick, world.getTotalWorldTime()) || p.proxy.get() == null || p.proxy.get() == proxy);
        placements.add(new Placement(player.getUniqueID(), world.getTotalWorldTime(), ++sequence, proxy));
        while (placements.size() > MAX_PENDING_PLACEMENTS) {
            placements.remove(0);
        }
    }

    /** Consume matching placements once. Chunk loads, power changes and subsequent repaths have no actor. */
    public static EntityPlayerMP takePlacement(IGrid grid) {
        if (PLACEMENTS.isEmpty()) return null;
        Placement latest = null;
        World placementWorld = null;
        for (IGridNode node : grid.getNodes()) {
            final DimensionalCoord location = node.getGridBlock().getLocation();
            if (location == null) continue;
            final World world = location.getWorld();
            if (world == null || world.isRemote) continue;
            final List<Placement> placements = PLACEMENTS.get(world);
            if (placements == null) continue;
            for (Iterator<Placement> it = placements.iterator(); it.hasNext();) {
                final Placement placement = it.next();
                final AENetworkProxy proxy = placement.proxy.get();
                if (proxy == null || expired(placement.tick, world.getTotalWorldTime())) {
                    it.remove();
                } else if (proxy == node.getGridBlock()) {
                    // Match the actual primary node, not just coordinates: P2P/fibres can share a block across grids.
                    it.remove();
                    if (latest == null || placement.sequence > latest.sequence) {
                        latest = placement;
                        placementWorld = world;
                    }
                }
            }
            if (placements.isEmpty()) PLACEMENTS.remove(world);
        }
        if (latest == null) return null;
        for (EntityPlayer player : placementWorld.playerEntities) {
            if (player instanceof EntityPlayerMP && player.getUniqueID().equals(latest.player)) {
                return (EntityPlayerMP) player;
            }
        }
        return null;
    }

    static boolean expired(long placedAt, long now) {
        return now < placedAt || now - placedAt > PLACEMENT_LIFETIME_TICKS;
    }

    /** Snapshot finalized channel state, not isActive(): power and booting must not cause channel alerts. */
    public static Set<IGridNode> snapshot(Collection<IGridNode> nodes) {
        final Set<IGridNode> satisfied = new HashSet<>();
        for (IGridNode node : nodes) {
            if (node.hasFlag(GridFlags.REQUIRE_CHANNEL) && node.meetsChannelRequirements()) {
                satisfied.add(node);
            }
        }
        return satisfied;
    }

    /** Only report existing devices that actually lost a channel due to a capacity rejection. */
    public static List<IGridNode> findLostChannels(Set<IGridNode> previouslySatisfied,
            Collection<? extends IGridNode> capacityRejected, IGrid grid) {
        final List<IGridNode> lost = new ArrayList<>();
        for (IGridNode node : capacityRejected) {
            if (previouslySatisfied.contains(node) && node.getGrid() == grid && !node.meetsChannelRequirements()) {
                lost.add(node);
            }
        }
        return lost;
    }

    public static void notifyPlayer(EntityPlayerMP player, List<IGridNode> lost) {
        if (player == null || lost.isEmpty()) return;
        final List<PacketChannelAlert.Target> targets = new ArrayList<>();
        for (IGridNode node : lost) {
            final DimensionalCoord location = node.getGridBlock().getLocation();
            if (location == null || location.getWorld() == null) continue;
            final ForgeDirection side = node.getMachine() instanceof AEBasePart part ? part.getSide()
                    : ForgeDirection.UNKNOWN;
            final ItemStack representation = node.getGridBlock().getMachineRepresentation();
            // Send only the item identity, never machine inventories or settings NBT.
            final ItemStack icon = representation == null ? null
                    : new ItemStack(representation.getItem(), 1, representation.getItemDamage());
            targets.add(new PacketChannelAlert.Target(location, side, icon));
        }
        targets.sort(Comparator.comparingInt(t -> WorldCoord.getTaxicabDistance(t.location, player)));
        if (!targets.isEmpty()) {
            NetworkHandler.instance.sendTo(new PacketChannelAlert(targets), player);
        }
    }

    private static final class Placement {

        private final UUID player;
        private final long tick;
        private final long sequence;
        private final WeakReference<AENetworkProxy> proxy;

        private Placement(UUID player, long tick, long sequence, AENetworkProxy proxy) {
            this.player = player;
            this.tick = tick;
            this.sequence = sequence;
            this.proxy = new WeakReference<>(proxy);
        }
    }
}
