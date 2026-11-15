package appeng.util.p2p;

import java.util.Collections;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.chunk.Chunk;

public class InventoryTarget {

    private static final class Tile {

        final TileEntity tile;
        final Chunk chunk;

        Tile(TileEntity tile) {
            this.tile = tile;
            this.chunk = tile.getWorldObj().getChunkFromBlockCoords(tile.xCoord, tile.zCoord);
        }
    }

    public final IInventory inv;
    public final int size;
    private final Tile[] tiles;

    public InventoryTarget(IInventory inv, List<TileEntity> tiles) {
        this.inv = inv;
        this.size = inv.getSizeInventory();
        this.tiles = new Tile[tiles.size()];
        for (int i = 0; i < tiles.size(); i++) {
            this.tiles[i] = new Tile(tiles.get(i));
        }
    }

    public InventoryTarget(IInventory inv, TileEntity te) {
        this(inv, Collections.singletonList(te));
    }

    public boolean isValid() {
        for (Tile tile : tiles) {
            if (tile.tile.isInvalid() || !tile.chunk.isChunkLoaded) {
                return false;
            }
        }
        return this.inv.getSizeInventory() == this.size;
    }

    public static boolean isSameTarget(InventoryTarget t1, InventoryTarget t2) {
        if (t1 == t2) {
            return true;
        }
        if (t1 == null || t2 == null) {
            return false;
        }
        if (t1.size != t2.size || t1.tiles.length != t2.tiles.length) {
            return false;
        }
        for (int i = 0; i < t1.tiles.length; i++) {
            if (t1.tiles[i].tile != t2.tiles[i].tile) return false;
        }
        return true;
    }
}
