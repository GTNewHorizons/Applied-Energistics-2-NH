package appeng.util.target;

import net.minecraft.inventory.IInventory;

public class InventoryTarget {

    public final IInventory inv;
    private final TileTarget tile;
    private final int size;

    public InventoryTarget(IInventory inv, TileTarget tile) {
        this.inv = inv;
        this.tile = tile;
        this.size = inv.getSizeInventory();
    }

    public int size() {
        return size;
    }

    public boolean isValid() {
        return this.tile.isValid() && inv.getSizeInventory() == this.size;
    }
}
