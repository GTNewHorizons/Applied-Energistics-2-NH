package com.glodblock.github.loader;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.p455w0rd.wirelesscraftingterminal.common.WCTGuiHandler;
import net.p455w0rd.wirelesscraftingterminal.items.ItemEnum;
import net.p455w0rd.wirelesscraftingterminal.items.ItemWirelessCraftingTerminal;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.common.terminal.TerminalIds;
import com.glodblock.github.common.terminal.UltraTerminals;
import com.glodblock.github.inventory.InventoryHandler;
import com.glodblock.github.inventory.gui.GuiType;
import com.glodblock.github.inventory.item.WirelessLevelTerminalInventory;
import com.glodblock.github.util.BlockPos;
import com.glodblock.github.util.ModAndClassUtil;

import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import appeng.api.features.AbstractDelegatedTerminalType;
import appeng.api.features.AbstractHostedTerminalType;
import appeng.api.features.IUltraTerminalGuiOpener;
import appeng.api.features.IUltraTerminalRegistry;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.networking.IGridNode;
import appeng.container.AEBaseContainer;
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

        // the crafting terminal and the processing pattern terminal are not wireless terminals, they are absorbed by
        // their own recipe
        final ItemStack craftingPart = stackOf(AEApi.instance().definitions().parts().craftingTerminal());
        final ItemStack patternExPart = stackOf(AEApi.instance().definitions().parts().patternTerminalEx());
        final ItemStack craftingIcon = craftingPart != null ? craftingPart
                : ItemAndBlockHolder.WIRELESS_ULTRA_TERM.stack();
        registry.register(
                new AeTerminalType(
                        TerminalIds.AE2FC_CRAFTING,
                        LANG_KEY + "crafting",
                        craftingIcon,
                        null,
                        craftingPart,
                        GuiBridge.GUI_CRAFTING_TERMINAL) {

                    @Override
                    public IGuiItemObject createGuiObject(final IWirelessTermHandler handler, final ItemStack terminal,
                            final EntityPlayer player, final World world, final int x, final int y, final int z) {
                        return new WirelessCraftingTerminalGuiObject(handler, terminal, player, world, x, y, z);
                    }
                });

        final ItemStack patternStack = ItemAndBlockHolder.WIRELESS_PATTERN_TERM.stack();
        registry.register(
                new PatternTerminalType(
                        TerminalIds.AE2FC_PATTERN,
                        LANG_KEY + "pattern",
                        patternStack,
                        patternStack,
                        GuiBridge.GUI_PATTERN_TERMINAL,
                        1));
        registry.register(
                new PatternTerminalType(
                        TerminalIds.AE2FC_PATTERN_EX,
                        LANG_KEY + "pattern_ex",
                        patternStack,
                        null,
                        patternExPart,
                        GuiBridge.GUI_PATTERN_TERMINAL_EX,
                        2));

        final ItemStack interfaceStack = ItemAndBlockHolder.WIRELESS_INTERFACE_TERM.stack();
        registry.register(
                new AeTerminalType(
                        TerminalIds.AE2FC_INTERFACE,
                        LANG_KEY + "interface",
                        interfaceStack,
                        interfaceStack,
                        GuiBridge.GUI_INTERFACE_TERMINAL) {

                    @Override
                    public IGuiItemObject createGuiObject(final IWirelessTermHandler handler, final ItemStack terminal,
                            final EntityPlayer player, final World world, final int x, final int y, final int z) {
                        return new WirelessInterfaceTerminalGuiObject(handler, terminal, x);
                    }
                });

        final ItemStack levelStack = ItemAndBlockHolder.WIRELESS_LEVEL_TERM.stack();
        registry.register(
                new AbstractHostedTerminalType(
                        TerminalIds.AE2FC_LEVEL,
                        LANG_KEY + "level",
                        levelStack,
                        levelStack,
                        (player, terminal, slotIndex) -> {
                            final int next = player.openContainer instanceof AEBaseContainer container
                                    ? container.getSwitchAbleGuiNext()
                                    : 0;
                            InventoryHandler.openGui(
                                    player,
                                    player.worldObj,
                                    new BlockPos(slotIndex, 0, next),
                                    ForgeDirection.UNKNOWN,
                                    GuiType.WIRELESS_LEVEL_TERMINAL);
                        }) {

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
                    new AeTerminalType(
                            TerminalIds.AE2_WIRELESS,
                            LANG_KEY + "wireless",
                            ae2Wireless,
                            ae2Wireless,
                            GuiBridge.GUI_ME) {

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
                    new AbstractDelegatedTerminalType(
                            TerminalIds.WCT_CRAFTING,
                            LANG_KEY + "wct",
                            wctStack,
                            wctStack,
                            (player, terminal, slotIndex) -> WCTGuiHandler.launchGui(
                                    Reference.GUI_WCT,
                                    player,
                                    player.worldObj,
                                    (int) player.posX,
                                    (int) player.posY,
                                    (int) player.posZ),
                            ItemWirelessCraftingTerminal.class) {});
        }
    }

    @Nullable
    private static ItemStack stackOf(final @Nullable IItemDefinition definition) {
        return definition == null ? null : definition.maybeStack(1).orNull();
    }

    private static IUltraTerminalGuiOpener openerOf(final GuiBridge bridge) {
        return (player, terminal, slotIndex) -> Platform.openGUI(player, null, null, bridge, slotIndex);
    }

    /**
     * Terminal hosted by an AE gui bridge, opened through the ultra terminal item.
     */
    private abstract static class AeTerminalType extends AbstractHostedTerminalType {

        AeTerminalType(final String id, final String nameKey, final @Nullable ItemStack icon,
                final @Nullable ItemStack ingredient, final GuiBridge bridge) {
            super(id, nameKey, icon, ingredient, openerOf(bridge));
        }

        AeTerminalType(final String id, final String nameKey, final @Nullable ItemStack icon,
                final @Nullable ItemStack ingredient, final @Nullable ItemStack absorbingIngredient,
                final GuiBridge bridge) {
            super(id, nameKey, icon, ingredient, absorbingIngredient, openerOf(bridge));
        }
    }

    /**
     * Pattern terminals share their gui object but differ in the pattern mode it is built with.
     */
    private static final class PatternTerminalType extends AeTerminalType {

        private final int hostedMode;

        PatternTerminalType(final String id, final String nameKey, final @Nullable ItemStack icon,
                final @Nullable ItemStack ingredient, final GuiBridge bridge, final int hostedMode) {
            super(id, nameKey, icon, ingredient, bridge);
            this.hostedMode = hostedMode;
        }

        PatternTerminalType(final String id, final String nameKey, final @Nullable ItemStack icon,
                final @Nullable ItemStack ingredient, final @Nullable ItemStack absorbingIngredient,
                final GuiBridge bridge, final int hostedMode) {
            super(id, nameKey, icon, ingredient, absorbingIngredient, bridge);
            this.hostedMode = hostedMode;
        }

        @Override
        public IGuiItemObject createGuiObject(final IWirelessTermHandler handler, final ItemStack terminal,
                final EntityPlayer player, final World world, final int x, final int y, final int z) {
            // standalone pattern terminal items keep using their legacy nbt mode, the ultra terminal passes the
            // mode of this type
            final int mode = UltraTerminals.isUltraTerminal(terminal) ? this.hostedMode : y;
            return new WirelessPatternTerminalGuiObject(handler, terminal, player, world, x, mode, z);
        }
    }

    private UltraTerminalLoader() {}
}
