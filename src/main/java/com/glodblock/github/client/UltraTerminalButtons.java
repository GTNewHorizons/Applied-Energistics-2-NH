package com.glodblock.github.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.client.gui.GuiFCImgButton;
import com.glodblock.github.client.gui.widget.GuiModeButton;
import com.glodblock.github.common.item.ItemWirelessUltraTerminal;
import com.glodblock.github.common.terminal.IUltraTerminalButtonState;
import com.glodblock.github.common.terminal.UltraTerminalButtonState;
import com.glodblock.github.network.CPacketFluidPatternTermBtns;
import com.glodblock.github.network.CPacketSwitchGuis;
import com.glodblock.github.util.NameConst;

import appeng.api.features.IUltraTerminalType;
import appeng.helpers.ICustomButtonDataObject;
import appeng.util.Platform;
import io.netty.buffer.ByteBuf;

/**
 * Custom buttons of the ultra terminal: one button shows the opened terminal, left clicking switches to the next one
 * and right clicking expands the other terminals.
 */
public class UltraTerminalButtons implements ICustomButtonDataObject, IUltraTerminalButtonState {

    private static final int SPACING = 20;

    private final ItemStack terminal;
    private final UltraTerminalButtonState state;
    private final boolean utilityButtons;
    private final List<GuiModeButton> flyoutButtons = new ArrayList<>();

    private GuiModeButton mainButton;
    private boolean expanded;
    private boolean flyoutDirty = true;

    private GuiFCImgButton magnetOff;
    private GuiFCImgButton magnetInv;
    private GuiFCImgButton magnetME;

    private GuiFCImgButton magnetFilter;

    private GuiFCImgButton restockEnableBtn;
    private GuiFCImgButton restockDisableBtn;

    private int offset;

    private List<GuiButton> modeButtonList;
    private int modeGuiLeft;
    private int modeGuiTop;
    private int modeGuiSizeY;

    public UltraTerminalButtons(ItemStack terminal) {
        this(terminal, true);
    }

    /**
     * @param utilityButtons true to add the magnet and restock buttons, false to only add the switch button
     */
    public UltraTerminalButtons(ItemStack terminal, boolean utilityButtons) {
        this.terminal = terminal;
        this.state = new UltraTerminalButtonState(terminal);
        this.utilityButtons = utilityButtons;
    }

    public void initCustomButtons(int guiLeft, int guiTop, int xSize, int ySize, int xOffset, int yOffset,
            List<GuiButton> buttonList) {
        this.offset = yOffset;
        // the gui positions are kept to refresh the switch button while the gui stays open
        this.modeButtonList = buttonList;
        this.modeGuiLeft = guiLeft;
        this.modeGuiTop = guiTop;
        this.modeGuiSizeY = ySize;
        if (this.utilityButtons) {
            buttonList.add(
                    this.magnetOff = new GuiFCImgButton(
                            guiLeft + xSize - 17,
                            guiTop + ySize - 65,
                            "MAGNET_CARD",
                            "OFF"));
            buttonList.add(
                    this.magnetInv = new GuiFCImgButton(
                            guiLeft + xSize - 17,
                            guiTop + ySize - 65,
                            "MAGNET_CARD",
                            "INV"));
            buttonList.add(
                    this.magnetME = new GuiFCImgButton(guiLeft + xSize - 17, guiTop + ySize - 65, "MAGNET_CARD", "ME"));

            buttonList.add(
                    this.magnetFilter = new GuiFCImgButton(
                            guiLeft + xSize - 17,
                            guiTop + ySize - 45,
                            "MAGNET_CARD",
                            "FILTER"));
            buttonList.add(
                    this.restockEnableBtn = new GuiFCImgButton(
                            guiLeft + xSize - 17,
                            guiTop + ySize - 25,
                            "RESTOCK",
                            "ENABLE"));
            buttonList.add(
                    this.restockDisableBtn = new GuiFCImgButton(
                            guiLeft + xSize - 17,
                            guiTop + ySize - 25,
                            "RESTOCK",
                            "DISABLE"));
        }

        this.initModeButtons(guiLeft, guiTop, ySize, buttonList);
        this.toggleMagnetButtonsVisibility();
        this.toggleRestockVisibility();
    }

    private void initModeButtons(final int guiLeft, final int guiTop, final int ySize,
            final List<GuiButton> buttonList) {
        this.flyoutButtons.clear();
        this.expanded = false;
        final IUltraTerminalType current = currentType();
        if (current == null) {
            this.mainButton = null;
            this.flyoutDirty = false;
            return;
        }
        this.mainButton = new GuiModeButton(guiLeft - 18, this.offset, current, mainButtonMessage(current));
        buttonList.add(this.mainButton);
        this.buildFlyout(current, guiLeft, guiTop, ySize, buttonList);
    }

    /**
     * Builds the flyout entries for every usable absorbed terminal except the current one.
     */
    private void buildFlyout(final IUltraTerminalType current, final int guiLeft, final int guiTop, final int ySize,
            final List<GuiButton> buttonList) {
        this.flyoutButtons.clear();
        // the flyout opens away from the gui, falling back to the other side when there is no room
        final int direction = guiLeft - 18 - SPACING - 16 >= 0 ? -1 : 1;
        int x = guiLeft - 18 + direction * SPACING;
        int y = this.offset;
        final int bottom = guiTop + ySize - SPACING;
        for (final IUltraTerminalType type : usableTypes(this.terminal)) {
            if (type.getId().equals(current.getId())) {
                continue;
            }
            final GuiModeButton button = new GuiModeButton(x, y, type);
            button.setVisibility(false);
            this.flyoutButtons.add(button);
            buttonList.add(button);
            y += SPACING;
            if (y > bottom) {
                y = this.offset;
                x += direction * SPACING;
            }
        }
        this.flyoutDirty = false;
    }

    @Nullable
    private IUltraTerminalType currentType() {
        return ItemWirelessUltraTerminal.resolveAvailableType(
                Minecraft.getMinecraft().thePlayer,
                this.terminal,
                ItemWirelessUltraTerminal.getCurrentType(this.terminal));
    }

    /**
     * @return the absorbed terminals that can be opened right now, in registration order.
     */
    private static List<IUltraTerminalType> usableTypes(final ItemStack terminal) {
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        final List<IUltraTerminalType> types = new ArrayList<>();
        for (final IUltraTerminalType type : ItemWirelessUltraTerminal.getTypes(terminal)) {
            if (type.isAvailable(player, terminal)) {
                types.add(type);
            }
        }
        return types;
    }

    private static String mainButtonMessage(final IUltraTerminalType current) {
        return current.getDisplayName() + '\n'
                + NameConst.i18n(NameConst.TT_ULTRA_TERMINAL_NEXT_HINT)
                + '\n'
                + NameConst.i18n(NameConst.TT_ULTRA_TERMINAL_SWITCH_HINT);
    }

    /**
     * Updates the switch button to the selected terminal. The flyout is rebuilt when it is expanded next time, so no
     * button is added to or removed from the gui while it handles a click.
     */
    private void refreshModeButtons() {
        this.setExpanded(false);
        if (this.mainButton == null) {
            return;
        }
        final IUltraTerminalType current = currentType();
        if (current == null) {
            return;
        }
        this.mainButton.setType(current, mainButtonMessage(current));
        this.flyoutDirty = true;
    }

    private void rebuildFlyoutIfDirty() {
        if (!this.flyoutDirty || this.modeButtonList == null || this.mainButton == null) {
            return;
        }
        final IUltraTerminalType current = currentType();
        if (current == null) {
            return;
        }
        this.modeButtonList.removeAll(this.flyoutButtons);
        this.buildFlyout(current, this.modeGuiLeft, this.modeGuiTop, this.modeGuiSizeY, this.modeButtonList);
    }

    /**
     * Shows or hides the flyout with the other terminals.
     */
    public void setExpanded(final boolean expanded) {
        if (expanded) {
            this.rebuildFlyoutIfDirty();
        }
        this.expanded = expanded;
        for (final GuiModeButton button : this.flyoutButtons) {
            button.setVisibility(expanded);
        }
    }

    public boolean isExpanded() {
        return this.expanded;
    }

    @Override
    public boolean mouseClickedCustomButtons(final int mouseX, final int mouseY, final int mouseButton) {
        if (this.mainButton == null) {
            return false;
        }
        if (mouseButton == 0) {
            // called after the gui processed the left click, close the flyout when the click went past it
            if (this.expanded && !this.isOverPopup(mouseX, mouseY)) {
                this.setExpanded(false);
            }
            return false;
        }
        if (mouseButton != 1) {
            return false;
        }
        if (this.mainButton.visible && isInside(this.mainButton, mouseX, mouseY)) {
            this.setExpanded(!this.expanded);
            return true;
        }
        if (this.expanded) {
            // right clicking anywhere else closes the flyout, the click itself is handled as usual
            this.setExpanded(false);
        }
        return false;
    }

    private boolean isOverPopup(final int mouseX, final int mouseY) {
        if (this.mainButton.visible && isInside(this.mainButton, mouseX, mouseY)) {
            return true;
        }
        for (final GuiModeButton button : this.flyoutButtons) {
            if (button.visible && isInside(button, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInside(final GuiButton button, final int mouseX, final int mouseY) {
        return mouseX >= button.xPosition && mouseY >= button.yPosition
                && mouseX < button.xPosition + button.width
                && mouseY < button.yPosition + button.height;
    }

    @Override
    public boolean actionPerformedCustomButtons(final GuiButton btn) {
        if (btn == this.mainButton) {
            // the single button cycles through the absorbed terminals
            this.switchToNext();
            return true;
        }
        if (btn instanceof GuiModeButton modeButton) {
            this.switchTo(modeButton.getTypeId());
            return true;
        }
        if (btn instanceof GuiFCImgButton && this.utilityButtons) {
            if (btn == this.magnetOff || btn == this.magnetME || btn == this.magnetInv) {
                FluidCraft.proxy.netHandler.sendToServer(
                        new CPacketFluidPatternTermBtns(
                                CPacketFluidPatternTermBtns.Command.MAGNET_MODE,
                                this.state.getMagnetMode().ordinal()));
            } else if (btn == this.magnetFilter) {
                FluidCraft.proxy.netHandler.sendToServer(
                        new CPacketFluidPatternTermBtns(CPacketFluidPatternTermBtns.Command.MAGNET_OPEN_FILTER, 0));
            } else if (btn == this.restockDisableBtn || btn == this.restockEnableBtn) {
                FluidCraft.proxy.netHandler
                        .sendToServer(new CPacketFluidPatternTermBtns(CPacketFluidPatternTermBtns.Command.RESTOCK, 1));
            }
            return true;
        }
        return false;
    }

    /**
     * Opens the terminal with the given type.
     */
    private void switchTo(final String typeId) {
        ItemWirelessUltraTerminal.setModeId(this.terminal, typeId);
        this.refreshModeButtons();
        FluidCraft.proxy.netHandler.sendToServer(new CPacketSwitchGuis(typeId, true));
    }

    /**
     * Opens the next usable absorbed terminal, wrapping around.
     */
    private void switchToNext() {
        final String next = ItemWirelessUltraTerminal.getNextModeId(Minecraft.getMinecraft().thePlayer, this.terminal);
        if (next.isEmpty() || next.equals(ItemWirelessUltraTerminal.getModeId(this.terminal))) {
            return;
        }
        this.switchTo(next);
    }

    @Override
    public void toggleRestock() {
        this.state.toggleRestock();
    }

    public void toggleRestockVisibility() {
        if (Platform.isClient() && this.restockEnableBtn != null && this.restockDisableBtn != null) {
            if (this.state.isRestockEnabled()) {
                this.restockEnableBtn.setVisibility(true);
                this.restockDisableBtn.setVisibility(false);
            } else {
                this.restockEnableBtn.setVisibility(false);
                this.restockDisableBtn.setVisibility(true);
            }
        }
    }

    @Override
    public void toggleMagnetMode() {
        this.state.toggleMagnetMode();
    }

    public void toggleMagnetButtonsVisibility() {
        if (this.magnetOff == null || this.magnetME == null || this.magnetInv == null) {
            return;
        }
        switch (this.state.getMagnetMode()) {
            case Off -> {
                this.magnetME.setVisibility(false);
                this.magnetInv.setVisibility(false);
                this.magnetOff.setVisibility(true);
            }
            case ME -> {
                this.magnetME.setVisibility(true);
                this.magnetInv.setVisibility(false);
                this.magnetOff.setVisibility(false);
            }
            case Inv -> {
                this.magnetME.setVisibility(false);
                this.magnetInv.setVisibility(true);
                this.magnetOff.setVisibility(false);
            }
        }
    }

    @Override
    public void readData(NBTTagCompound tag) {
        this.state.readData(tag);
    }

    @Override
    public void writeData(NBTTagCompound tag) {
        this.state.writeData(tag);
    }

    @Override
    public void readByte(ByteBuf buf) {
        this.state.readByte(buf);
        // the widgets mirror the state, refresh them after the state changed
        this.refreshModeButtons();
        this.toggleMagnetButtonsVisibility();
        this.toggleRestockVisibility();
    }

    @Override
    public void writeByte(ByteBuf buf) {
        this.state.writeByte(buf);
    }
}
