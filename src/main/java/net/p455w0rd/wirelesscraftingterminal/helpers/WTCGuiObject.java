package net.p455w0rd.wirelesscraftingterminal.helpers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.p455w0rd.wirelesscraftingterminal.api.networking.security.WCTIActionHost;
import net.p455w0rd.wirelesscraftingterminal.common.inventory.WCTInventoryCrafting;

import org.jspecify.annotations.Nullable;

import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.items.contents.WirelessCraftingTerminalGuiObject;

/**
 * GUI object of the wireless crafting terminal. It remembers the inventory slot that holds the terminal so that the
 * AE2 containers can track the terminal item.
 */
public class WTCGuiObject extends WirelessCraftingTerminalGuiObject implements WCTIActionHost {

    private final int slotIndex;
    // Created lazily because the super constructor calls readInventory() before the fields are initialized.
    private @Nullable WCTInventoryCrafting craftingGrid;

    public WTCGuiObject(final IWirelessTermHandler wh, final ItemStack is, final EntityPlayer ep, final World w,
            final int x, final int y, final int z, final int slotIndex) {
        super(wh, is, ep, w, x, y, z);
        this.slotIndex = slotIndex;
    }

    private WCTInventoryCrafting getCraftingGrid() {
        if (this.craftingGrid == null) {
            this.craftingGrid = new WCTInventoryCrafting(this);
        }
        return this.craftingGrid;
    }

    @Override
    public void readInventory() {
        this.getCraftingGrid().readFromTerminal(this.getItemStack());
    }

    @Override
    public void writeInventory() {
        this.getCraftingGrid().writeToTerminal(this.getItemStack());
    }

    @Override
    public @Nullable IInventory getInventoryByName(final String name) {
        if (name.equals("crafting")) {
            return this.getCraftingGrid();
        }
        return null;
    }

    @Override
    public @Nullable IGridNode getActionableNode(final boolean ignoreRange) {
        final IGridNode node = this.getActionableNode();
        if (node != null) {
            return node;
        }

        final IGrid grid = this.getGrid();
        if (ignoreRange && grid != null) {
            return grid.getPivot();
        }
        return null;
    }

    @Override
    public int getInventorySlot() {
        return this.slotIndex;
    }
}
