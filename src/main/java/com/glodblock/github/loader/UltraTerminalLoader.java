package com.glodblock.github.loader;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.p455w0rd.wirelesscraftingterminal.common.WCTGuiHandler;
import net.p455w0rd.wirelesscraftingterminal.helpers.WTCGuiObject;
import net.p455w0rd.wirelesscraftingterminal.items.ItemEnum;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.common.item.ItemWirelessUltraTerminal;
import com.glodblock.github.common.terminal.TerminalIds;
import com.glodblock.github.inventory.InventoryHandler;
import com.glodblock.github.inventory.gui.GuiType;
import com.glodblock.github.inventory.item.WirelessLevelTerminalInventory;
import com.glodblock.github.util.BlockPos;
import com.glodblock.github.util.ModAndClassUtil;

import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import appeng.api.features.AbstractUltraTerminalType;
import appeng.api.features.IUltraTerminalGuiOpener;
import appeng.api.features.IUltraTerminalRegistry;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.networking.IGridNode;
import appeng.core.sync.GuiBridge;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.items.contents.WirelessCraftingTerminalGuiObject;
import appeng.items.contents.WirelessInterfaceTerminalGuiObject;
import appeng.items.contents.WirelessPatternTerminalGuiObject;
import appeng.util.Platform;

/**
 * Registers the terminal types that ship with AE2NH. Other mods register their own types with
 * {@link IUltraTerminalRegistry}, the ultra terminal item has no hardcoded terminal list.
 */
public final class UltraTerminalLoader implements Runnable {

    public static final UltraTerminalLoader INSTANCE = new UltraTerminalLoader();

    private static final String LANG_KEY = "item.wireless_ultra_terminal.terminal.";

    @Override
    public void run() {
        final IUltraTerminalRegistry registry = AEApi.instance().registries().ultraTerminal();

        // the crafting terminal and the processing pattern terminal have no wireless item, their part adds them
        final ItemStack craftingPart = stackOf(AEApi.instance().definitions().parts().craftingTerminal());
        if (craftingPart != null) {
            registry.register(
                    new AbstractUltraTerminalType(
                            TerminalIds.AE2FC_CRAFTING,
                            LANG_KEY + "crafting",
                            craftingPart,
                            opener(GuiBridge.GUI_CRAFTING_TERMINAL)) {

                        @Nullable
                        @Override
                        public IGuiItemObject createGuiObject(final IWirelessTermHandler handler,
                                final ItemStack terminal, final EntityPlayer player, final World world, final int x,
                                final int y, final int z) {
                            return new WirelessCraftingTerminalGuiObject(handler, terminal, player, world, x, y, z);
                        }
                    });
        }

        final ItemStack patternPart = stackOf(AEApi.instance().definitions().parts().patternTerminalEx());
        final ItemStack wirelessPattern = ItemAndBlockHolder.WIRELESS_PATTERN_TERM.stack();
        // standalone pattern terminal items keep using their nbt mode, the ultra terminal passes the mode of this type
        registry.register(
                new AbstractUltraTerminalType(
                        TerminalIds.AE2FC_PATTERN,
                        LANG_KEY + "pattern",
                        wirelessPattern,
                        opener(GuiBridge.GUI_PATTERN_TERMINAL)) {

                    @Nullable
                    @Override
                    public IGuiItemObject createGuiObject(final IWirelessTermHandler handler, final ItemStack terminal,
                            final EntityPlayer player, final World world, final int x, final int y, final int z) {
                        final int mode = ItemWirelessUltraTerminal.isUltraTerminal(terminal) ? 1 : y;
                        return new WirelessPatternTerminalGuiObject(handler, terminal, player, world, x, mode, z);
                    }
                });
        if (patternPart != null) {
            registry.register(
                    new AbstractUltraTerminalType(
                            TerminalIds.AE2FC_PATTERN_EX,
                            LANG_KEY + "pattern_ex",
                            patternPart,
                            opener(GuiBridge.GUI_PATTERN_TERMINAL_EX)) {

                        @Nullable
                        @Override
                        public IGuiItemObject createGuiObject(final IWirelessTermHandler handler,
                                final ItemStack terminal, final EntityPlayer player, final World world, final int x,
                                final int y, final int z) {
                            final int mode = ItemWirelessUltraTerminal.isUltraTerminal(terminal) ? 2 : y;
                            return new WirelessPatternTerminalGuiObject(handler, terminal, player, world, x, mode, z);
                        }
                    });
        }

        final ItemStack interfaceStack = ItemAndBlockHolder.WIRELESS_INTERFACE_TERM.stack();
        registry.register(
                new AbstractUltraTerminalType(
                        TerminalIds.AE2FC_INTERFACE,
                        LANG_KEY + "interface",
                        interfaceStack,
                        opener(GuiBridge.GUI_INTERFACE_TERMINAL)) {

                    @Nullable
                    @Override
                    public IGuiItemObject createGuiObject(final IWirelessTermHandler handler, final ItemStack terminal,
                            final EntityPlayer player, final World world, final int x, final int y, final int z) {
                        return new WirelessInterfaceTerminalGuiObject(handler, terminal, x);
                    }
                });

        final ItemStack levelStack = ItemAndBlockHolder.WIRELESS_LEVEL_TERM.stack();
        registry.register(
                new AbstractUltraTerminalType(
                        TerminalIds.AE2FC_LEVEL,
                        LANG_KEY + "level",
                        levelStack,
                        itemGuiOpener(GuiType.WIRELESS_LEVEL_TERMINAL)) {

                    @Nullable
                    @Override
                    public Object createItemInventory(final ItemStack terminal, final int slotIndex,
                            final IGridNode node, final EntityPlayer player) {
                        return new WirelessLevelTerminalInventory(terminal, slotIndex, node, player);
                    }
                });

        final ItemStack ae2Wireless = stackOf(AEApi.instance().definitions().items().wirelessTerminal());
        if (ae2Wireless != null) {
            registry.register(
                    new AbstractUltraTerminalType(
                            TerminalIds.AE2_WIRELESS,
                            LANG_KEY + "wireless",
                            ae2Wireless,
                            opener(GuiBridge.GUI_ME)) {

                        @Nullable
                        @Override
                        public IGuiItemObject createGuiObject(final IWirelessTermHandler handler,
                                final ItemStack terminal, final EntityPlayer player, final World world, final int x,
                                final int y, final int z) {
                            return new WirelessTerminalGuiObject(handler, terminal, player, world, x, y, z);
                        }
                    });
        }

        if (ModAndClassUtil.WCT) {
            final ItemStack wctStack = ItemEnum.WIRELESS_CRAFTING_TERMINAL.getStack();
            registry.register(
                    new AbstractUltraTerminalType(
                            TerminalIds.WCT_CRAFTING,
                            LANG_KEY + "wct",
                            wctStack,
                            (player, terminal, slotIndex, terminalSlotIndex) -> WCTGuiHandler
                                    .launchGui(Reference.GUI_ULTRA_WCT, player, player.worldObj, slotIndex, 0, 0)) {

                        @Nullable
                        @Override
                        public IGuiItemObject createGuiObject(final IWirelessTermHandler handler,
                                final ItemStack terminal, final EntityPlayer player, final World world, final int x,
                                final int y, final int z) {
                            // the gui opening channel adds the item gui offset to the slot
                            final int slot = x >= Platform.itemGuiSlotOffset ? x - Platform.itemGuiSlotOffset : x;
                            return new WTCGuiObject(handler, terminal, player, world, x, y, z, slot);
                        }
                    });
        }
    }

    @Nullable
    private static ItemStack stackOf(final @Nullable IItemDefinition definition) {
        return definition == null ? null : definition.maybeStack(1).orNull();
    }

    private static IUltraTerminalGuiOpener opener(final GuiBridge bridge) {
        return (player, terminal, slotIndex, terminalSlotIndex) -> Platform
                .openGUI(player, null, null, bridge, slotIndex, terminalSlotIndex);
    }

    private static IUltraTerminalGuiOpener itemGuiOpener(final GuiType guiType) {
        return (player, terminal, slotIndex, terminalSlotIndex) -> InventoryHandler.openGui(
                player,
                player.worldObj,
                new BlockPos(slotIndex, 0, terminalSlotIndex),
                ForgeDirection.UNKNOWN,
                guiType);
    }

    private UltraTerminalLoader() {}
}
