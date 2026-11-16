package appeng.me;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.implementations.tiles.ITileStorageMonitorable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.helpers.IInterfaceHost;
import appeng.parts.AEBasePart;
import appeng.parts.misc.PartStorageBus;

/**
 * Finds the grids linked to a grid by a storage bus pointing at an interface, in either direction. Only that kind of
 * link is followed: quartz fibres share power only and pattern repeaters share only encoded patterns.
 */
public final class TransferGridLinks {

    private TransferGridLinks() {}

    /**
     * Breadth first walk from {@code start}, so nearer nodes always come before farther ones.
     *
     * @param start    first node of the result
     * @param links    the direct neighbours of a node
     * @param maxDepth how many links to follow at most, 0 returns only {@code start}
     * @return every node reachable within {@code maxDepth} links, each exactly once, in visiting order
     */
    public static <G> List<G> collect(final G start, final Function<G, Collection<G>> links, final int maxDepth) {
        final Set<G> visited = new LinkedHashSet<>();
        final Map<G, Integer> depths = new HashMap<>();
        final Deque<G> queue = new ArrayDeque<>();

        visited.add(start);
        depths.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            final G current = queue.poll();
            final int depth = depths.get(current);
            if (depth >= maxDepth) {
                continue;
            }

            for (final G next : links.apply(current)) {
                if (next != null && visited.add(next)) {
                    depths.put(next, depth + 1);
                    queue.add(next);
                }
            }
        }

        return new ArrayList<>(visited);
    }

    /**
     * @param grid       grid to look around
     * @param accessType only storage buses of this type count as links
     * @return the other grids directly linked to {@code grid}, without duplicates
     */
    static Collection<Grid> linkedGrids(final Grid grid, final Class<? extends IGridHost> accessType) {
        final Set<Grid> linked = new LinkedHashSet<>();

        for (final Class<? extends IGridHost> machineClass : grid.getMachinesClasses()) {
            final boolean isBus = accessType.isAssignableFrom(machineClass)
                    && PartStorageBus.class.isAssignableFrom(machineClass);
            final boolean isInterface = IInterfaceHost.class.isAssignableFrom(machineClass);
            if (!isBus && !isInterface) {
                continue;
            }

            for (final IGridNode node : grid.getMachines(machineClass)) {
                final IGridHost machine = node.getMachine();
                if (isBus && machine instanceof PartStorageBus bus) {
                    addIfOther(linked, grid, gridInFrontOf(bus));
                }
                if (isInterface && machine instanceof IInterfaceHost host) {
                    for (final PartStorageBus bus : storageBusesPointingAt(host)) {
                        if (accessType.isInstance(bus) && bus.getGridNode() != null) {
                            addIfOther(linked, grid, bus.getGridNode().getGrid());
                        }
                    }
                }
            }
        }

        return linked;
    }

    private static void addIfOther(final Set<Grid> linked, final Grid self, final IGrid candidate) {
        if (candidate instanceof Grid other && !other.equals(self)) {
            linked.add(other);
        }
    }

    /**
     * @return the grid a storage bus reads from, also when the interface it points at has stocking slots configured and
     *         therefore no longer exposes its network.
     */
    private static IGrid gridInFrontOf(final PartStorageBus bus) {
        final TileEntity front = neighbour(bus.getHost().getTile(), bus.getSide());
        if (front == null) {
            return null;
        }

        // Only a monitorable tile can hand the bus another network. Asking the bus about anything else would build a
        // throwaway storage wrapper around every chest, tank or drawer it faces, on every scan.
        if (front instanceof ITileStorageMonitorable) {
            final IGrid connected = bus.getConnectedGrid();
            if (connected != null) {
                return connected;
            }
        }

        final IInterfaceHost host = interfaceFacing(front, bus.getSide().getOpposite());
        final IGridNode node = host == null ? null : host.getGridNode(ForgeDirection.UNKNOWN);
        return node == null ? null : node.getGrid();
    }

    private static IInterfaceHost interfaceFacing(final TileEntity tile, final ForgeDirection side) {
        if (tile instanceof IInterfaceHost host) {
            return host;
        }
        if (tile instanceof IPartHost partHost && partHost.getPart(side) instanceof IInterfaceHost host) {
            return host;
        }
        return null;
    }

    private static List<PartStorageBus> storageBusesPointingAt(final IInterfaceHost host) {
        final List<PartStorageBus> buses = new ArrayList<>();
        final EnumSet<ForgeDirection> sides = host instanceof AEBasePart part ? EnumSet.of(part.getSide())
                : EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN));

        for (final ForgeDirection side : sides) {
            if (neighbour(host.getTileEntity(), side) instanceof IPartHost partHost) {
                final IPart part = partHost.getPart(side.getOpposite());
                if (part instanceof PartStorageBus bus) {
                    buses.add(bus);
                }
            }
        }

        return buses;
    }

    /**
     * @return the adjacent tile entity, or null if there is none or its chunk is not loaded
     */
    private static TileEntity neighbour(final TileEntity tile, final ForgeDirection side) {
        if (tile == null || tile.getWorldObj() == null) {
            return null;
        }

        final World world = tile.getWorldObj();
        final int x = tile.xCoord + side.offsetX;
        final int z = tile.zCoord + side.offsetZ;
        if (!world.getChunkProvider().chunkExists(x >> 4, z >> 4)) {
            return null;
        }

        return world.getTileEntity(x, tile.yCoord + side.offsetY, z);
    }
}
