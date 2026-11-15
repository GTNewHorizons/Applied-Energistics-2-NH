package net.p455w0rd.wirelesscraftingterminal.common;

import net.minecraft.entity.player.EntityPlayer;
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

import org.jspecify.annotations.Nullable;

import appeng.container.ContainerOpenContext;
import appeng.util.Platform;
import cpw.mods.fml.common.network.IGuiHandler;

public class WCTGuiHandler implements IGuiHandler {

    public void registerRenderers() {}

    /**
     * For {@link Reference#GUI_WCT}, x is the inventory slot index of the terminal, in the same way as AE2 item GUIs.
     */
    @Override
    public @Nullable Object getServerGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
        if (guiId == Reference.GUI_WCT) {
            final WTCGuiObject obj = createGuiObject(player, world, x);
            if (obj == null) {
                return null;
            }

            final ContainerWirelessCraftingTerminal container = new ContainerWirelessCraftingTerminal(
                    player.inventory,
                    obj);
            final ContainerOpenContext context = new ContainerOpenContext(obj);
            context.setWorld(world);
            context.setX(x);
            context.setY(y);
            context.setZ(z);
            context.setSide(ForgeDirection.UNKNOWN);
            container.setOpenContext(context);
            return container;
        }
        if (guiId == Reference.GUI_MAGNET) {
            return new ContainerMagnet(player, player.inventory);
        }

        return null;
    }

    @Override
    public @Nullable Object getClientGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z) {
        if (guiId == Reference.GUI_WCT) {
            final WTCGuiObject obj = createGuiObject(player, world, x);
            if (obj == null) {
                return null;
            }
            return new GuiWirelessCraftingTerminal(player.inventory, obj);
        }
        if (guiId == Reference.GUI_MAGNET) {
            return new GuiMagnet(new ContainerMagnet(player, player.inventory));
        }

        return null;
    }

    private static @Nullable WTCGuiObject createGuiObject(EntityPlayer player, World world, int slotIndex) {
        final ItemStack terminal = Platform.getItemFromPlayerInventoryBySlotIndex(player, slotIndex);
        if (!RandomUtils.isWirelessCraftingTerminal(terminal)) {
            return null;
        }
        return ContainerWirelessCraftingTerminal.getGuiObject(terminal, player, world, slotIndex);
    }

    public static void openTerminalGui(final EntityPlayer player, final int slotIndex) {
        player.openGui(WirelessCraftingTerminal.INSTANCE, Reference.GUI_WCT, player.worldObj, slotIndex, 0, 0);
    }

    public static void launchGui(final int ID, final EntityPlayer player, final World world, final int x, final int y,
            final int z) {
        player.openGui(WirelessCraftingTerminal.INSTANCE, ID, world, x, y, z);
    }
}
