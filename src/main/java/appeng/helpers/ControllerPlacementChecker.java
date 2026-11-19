package appeng.helpers;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ControllerState;
import appeng.api.networking.pathing.IPathingGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.me.cache.PathGridCache;
import appeng.tile.networking.TileController;

/**
 * Server-side simulation of placing an ME Controller, to detect whether it would cause a controller conflict.
 */
public final class ControllerPlacementChecker {

    private ControllerPlacementChecker() {}

    public static boolean wouldControllerConflict(World w, int x, int y, int z) {
        try {
            return checkController(w, x, y, z);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Whether placing a cable of the given color at the position would join two or more distinct networks that each
     * have a controller.
     */
    public static boolean wouldCableConflict(World w, int x, int y, int z, AEColor cableColor) {
        try {
            final Set<IGrid> grids = new HashSet<>();

            if (w.blockExists(x, y, z) && w.getTileEntity(x, y, z) instanceof IPartHost host) {
                addGrid(grids, host.getPart(ForgeDirection.UNKNOWN));
                for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                    addGrid(grids, host.getPart(dir));
                }
            }

            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                final IGridNode node = getNeighborNode(w, x, y, z, dir);
                if (node == null) continue;
                // same rule as GridNode.canConnect: from.getColor().matches(this.getColor())
                if (!cableColor.matches(node.getGridBlock().getGridColor())) continue;
                grids.add(node.getGrid());
            }

            int controlled = 0;
            for (IGrid grid : grids) {
                if (grid.getCache(IPathingGrid.class) instanceof PathGridCache pgc && !pgc.getControllers().isEmpty()) {
                    controlled++;
                }
            }
            return controlled >= 2;
        } catch (Exception e) {
            return false;
        }
    }

    private static void addGrid(Set<IGrid> grids, IPart part) {
        if (part == null) return;
        final IGridNode node = part.getGridNode();
        if (node != null && node.getGrid() != null) grids.add(node.getGrid());
    }

    /**
     * Returns the neighbor's node facing the position if it is loaded, has a grid and is connectable from that side.
     */
    private static IGridNode getNeighborNode(World w, int x, int y, int z, ForgeDirection dir) {
        final int nx = x + dir.offsetX, ny = y + dir.offsetY, nz = z + dir.offsetZ;
        final TileEntity te = w.blockExists(nx, ny, nz) ? w.getTileEntity(nx, ny, nz) : null;
        if (!(te instanceof IGridHost host)) return null;

        final ForgeDirection opposite = dir.getOpposite();
        final IGridNode node = host.getGridNode(opposite);
        if (node == null || node.getGrid() == null) return null;
        if (!node.getGridBlock().getConnectableSides().contains(opposite)) return null;
        return node;
    }

    private static boolean checkController(World w, int x, int y, int z) {
        final Set<IGrid> grids = new HashSet<>();
        final Set<TileController> adjacentControllers = new HashSet<>();

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            final int nx = x + dir.offsetX, ny = y + dir.offsetY, nz = z + dir.offsetZ;
            if (w.blockExists(nx, ny, nz) && w.getTileEntity(nx, ny, nz) instanceof TileController tc) {
                adjacentControllers.add(tc);
            }
            final IGridNode node = getNeighborNode(w, x, y, z, dir);
            if (node != null) grids.add(node.getGrid());
        }

        final Set<PathGridCache> controlled = new HashSet<>();
        for (IGrid grid : grids) {
            if (grid.getCache(IPathingGrid.class) instanceof PathGridCache pgc && !pgc.getControllers().isEmpty()) {
                controlled.add(pgc);
            }
        }

        if (controlled.isEmpty()) return false;
        if (controlled.size() >= 2) return true;

        final PathGridCache cache = controlled.iterator().next();
        if (cache.getControllerState() == ControllerState.CONTROLLER_CONFLICT) return true;

        final Set<TileController> controllers = cache.getControllers();
        boolean adjacentToGrid = false;
        for (TileController tc : adjacentControllers) {
            if (controllers.contains(tc)) {
                adjacentToGrid = true;
                break;
            }
        }
        if (!adjacentToGrid) return true;

        int minX = x, maxX = x, minY = y, maxY = y, minZ = z, maxZ = z;
        for (TileController c : controllers) {
            minX = Math.min(c.xCoord, minX);
            maxX = Math.max(c.xCoord, maxX);
            minY = Math.min(c.yCoord, minY);
            maxY = Math.max(c.yCoord, maxY);
            minZ = Math.min(c.zCoord, minZ);
            maxZ = Math.max(c.zCoord, maxZ);
        }
        if (!(maxX - minX < 7 && maxY - minY < 7 && maxZ - minZ < 7)) return true;

        int axes = 0;
        if (isController(w, x - 1, y, z) && isController(w, x + 1, y, z)) axes++;
        if (isController(w, x, y - 1, z) && isController(w, x, y + 1, z)) axes++;
        if (isController(w, x, y, z - 1) && isController(w, x, y, z + 1)) axes++;
        return axes > 1;
    }

    private static boolean isController(World w, int x, int y, int z) {
        return w.blockExists(x, y, z) && w.getTileEntity(x, y, z) instanceof TileController;
    }
}
