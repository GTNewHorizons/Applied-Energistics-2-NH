package net.p455w0rd.wirelesscraftingterminal.helpers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.p455w0rd.wirelesscraftingterminal.api.networking.security.WCTIActionHost;
import net.p455w0rd.wirelesscraftingterminal.common.inventory.WCTInventoryCrafting;

import org.jspecify.annotations.Nullable;

import appeng.api.features.IWirelessTermHandler;
import appeng.items.contents.WirelessCraftingTerminalGuiObject;

public class WTCGuiObject extends WirelessCraftingTerminalGuiObject implements WCTIActionHost {

    // Created lazily because the super constructor calls readInventory() before the fields are initialized.
    private @Nullable WCTInventoryCrafting craftingGrid;

    /**
     * @param slotIndex the inventory slot index of the terminal. Like AE2 item GUIs, it is passed as x so that
     *                  {@link #getInventorySlot()} returns it.
     */
    public WTCGuiObject(final IWirelessTermHandler wh, final ItemStack is, final EntityPlayer ep, final World w,
            final int slotIndex) {
        super(wh, is, ep, w, slotIndex, 0, 0);
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
}
