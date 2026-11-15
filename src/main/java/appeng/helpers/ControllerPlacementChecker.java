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
import appeng.me.cache.PathGridCache;
import appeng.tile.networking.TileController;

/**
 * Server-side simulation of placing an ME Controller, to detect whether it would cause a controller conflict.
 */
public final class ControllerPlacementChecker {

    private ControllerPlacementChecker() {}

    public static boolean wouldConflict(World w, int x, int y, int z) {
        try {
            return check(w, x, y, z);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean check(World w, int x, int y, int z) {
        final Set<IGrid> grids = new HashSet<>();
        final Set<TileController> adjacentControllers = new HashSet<>();

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            final TileEntity te = w.blockExists(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ)
                    ? w.getTileEntity(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ)
                    : null;
            if (te == null) continue;
            if (te instanceof TileController tc) {
                adjacentControllers.add(tc);
            }
            if (!(te instanceof IGridHost host)) continue;

            final ForgeDirection opposite = dir.getOpposite();
            final IGridNode node = host.getGridNode(opposite);
            if (node == null) continue;
            final IGrid grid = node.getGrid();
            if (grid == null) continue;
            if (!node.getGridBlock().getConnectableSides().contains(opposite)) continue;
            grids.add(grid);
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
