package appeng.util.p2p;

import appeng.util.target.TileTarget;
import net.minecraft.inventory.IInventory;

public class InventoryTarget {

    public final IInventory inv;
    private final TileTarget tile;
    private final int[] layout;

    public InventoryTarget(IInventory inv, TileTarget tile, int[] layout) {
        this.inv = inv;
        this.tile = tile;
        this.layout = layout;
    }

    public int size() {
        return layout.length;
    }

    public boolean isValid() {
        return this.inv.getSizeInventory() == this.size() && this.tile.isValid();
    }
}
