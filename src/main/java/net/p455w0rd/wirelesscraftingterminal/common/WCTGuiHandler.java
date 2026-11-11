package net.p455w0rd.wirelesscraftingterminal.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.p455w0rd.wirelesscraftingterminal.client.gui.GuiMagnet;
import net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal;
import net.p455w0rd.wirelesscraftingterminal.common.container.ContainerMagnet;
import net.p455w0rd.wirelesscraftingterminal.common.container.ContainerWirelessCraftingTerminal;
import net.p455w0rd.wirelesscraftingterminal.common.utils.RandomUtils;
import net.p455w0rd.wirelesscraftingterminal.helpers.WTCGuiObject;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jspecify.annotations.Nullable;

import com.glodblock.github.client.UltraTerminalButtons;
import com.glodblock.github.common.item.ItemWirelessUltraTerminal;
import com.glodblock.github.common.terminal.TerminalIds;
import com.glodblock.github.util.Util;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.util.Platform;
import baubles.api.BaublesApi;
import cpw.mods.fml.common.network.IGuiHandler;
import it.unimi.dsi.fastutil.ints.IntObjectPair;

public class WCTGuiHandler implements IGuiHandler {

    public void registerRenderers() {}

    @Override
    public Object getServerGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
        if (guiId == Reference.GUI_MAGNET) {
            return new ContainerMagnet(player, player.inventory);
        }

        final WTCGuiObject term = createGuiObject(guiId, player, world, x, y, z);
        if (term == null) {
            return null;
        }

        return switch (guiId) {
            case Reference.GUI_WCT, Reference.GUI_ULTRA_WCT -> updateGui(
                    new ContainerWirelessCraftingTerminal(player.inventory, term),
                    world,
                    term.getInventorySlot(),
                    y,
                    z,
                    ForgeDirection.UNKNOWN,
                    term);
            default -> null;
        };
    }

    @Override
    public Object getClientGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
        if (guiId == Reference.GUI_MAGNET) {
            return new GuiMagnet(new ContainerMagnet(player, player.inventory));
        }
        final WTCGuiObject obj = createGuiObject(guiId, player, world, x, y, z);
        if (obj == null) {
            return null;
        }
        attachUltraTerminalButtons(player, obj);
        return new GuiWirelessCraftingTerminal(player.inventory, obj);
    }

    @Nullable
    private static WTCGuiObject createGuiObject(final int guiId, final EntityPlayer player, final World world,
            final int x, final int y, final int z) {
        final ItemStack stack;
        final int slot;
        if (guiId == Reference.GUI_ULTRA_WCT) {
            // both sides must use the item that opened this gui, do not search another one
            if (x < 0) {
                return null;
            }
            slot = x;
            if (Platform.isBaublesLoaded && slot >= Platform.baublesSlotsOffset) {
                final IInventory baubles = BaublesApi.getBaubles(player);
                if (baubles == null || slot - Platform.baublesSlotsOffset >= baubles.getSizeInventory()) {
                    return null;
                }
            } else if (slot >= player.inventory.getSizeInventory()) {
                return null;
            }
            stack = Platform.getItemFromPlayerInventoryBySlotIndex(player, slot);
            if (stack == null || !(stack.getItem() instanceof ItemWirelessUltraTerminal)
                    || !ItemWirelessUltraTerminal.isAbsorbed(stack, TerminalIds.WCT_CRAFTING)) {
                return null;
            }
        } else if (guiId == Reference.GUI_WCT) {
            final IntObjectPair<ItemStack> terminal = RandomUtils.getWirelessTermWithSlot(player.inventory);
            if (terminal == null) {
                return null;
            }
            stack = terminal.second();
            slot = terminal.firstInt();
        } else {
            return null;
        }
        if (stack.getItem() instanceof ItemWirelessUltraTerminal) {
            ItemWirelessUltraTerminal.setModeId(stack, TerminalIds.WCT_CRAFTING);
        }
        final IWirelessTermHandler handler = AEApi.instance().registries().wireless().getWirelessTerminalHandler(stack);
        return handler == null ? null : new WTCGuiObject(handler, stack, player, world, slot, y, z, slot);
    }

    /**
     * Adds the ultra terminal switch button to the gui of another terminal. The magnet and restock buttons are left
     * out, they belong to the ultra terminal itself.
     */
    private static void attachUltraTerminalButtons(final EntityPlayer player, final WTCGuiObject term) {
        if (term.getItemStack().getItem() instanceof ItemWirelessUltraTerminal) {
            // the hosted item already created its own buttons, including the utility ones
            return;
        }
        final ImmutablePair<Integer, ItemStack> ultraTerminal = Util.getUltraWirelessTerm(player);
        if (ultraTerminal != null) {
            term.setDataObject(new UltraTerminalButtons(ultraTerminal.getRight(), false));
        }
    }

    public static void launchGui(final int ID, final EntityPlayer player, final World world, final int x, final int y,
            final int z) {
        player.openGui(WirelessCraftingTerminal.INSTANCE, ID, world, x, y, z);
    }

    private Object updateGui(final Object newContainer, final World w, final int x, final int y, final int z,
            final ForgeDirection side, final Object myItem) {
        if (newContainer instanceof AEBaseContainer) {
            final AEBaseContainer bc = (AEBaseContainer) newContainer;
            bc.setOpenContext(new ContainerOpenContext(myItem));
            bc.getOpenContext().setWorld(w);
            bc.getOpenContext().setX(x);
            bc.getOpenContext().setY(y);
            bc.getOpenContext().setZ(z);
            bc.getOpenContext().setSide(side);
        }

        return newContainer;
    }
}
