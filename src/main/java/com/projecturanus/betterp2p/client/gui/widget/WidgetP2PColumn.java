package com.projecturanus.betterp2p.client.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.client.gui.Gui;

import org.jspecify.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import com.projecturanus.betterp2p.BetterP2P;
import com.projecturanus.betterp2p.client.gui.GuiAdvancedMemoryCard;
import com.projecturanus.betterp2p.client.gui.InfoList;
import com.projecturanus.betterp2p.client.gui.InfoWrapper;
import com.projecturanus.betterp2p.item.BetterMemoryCardModes;
import com.projecturanus.betterp2p.network.ModNetwork;
import com.projecturanus.betterp2p.network.data.P2PLocation;
import com.projecturanus.betterp2p.network.packet.C2SLinkP2P;
import com.projecturanus.betterp2p.network.packet.C2SRenameP2P;
import com.projecturanus.betterp2p.network.packet.C2SUnlinkP2P;

import appeng.core.localization.ColorUtils;

public class WidgetP2PColumn {

    private static final int NAME_OFFSET_X = 50;
    private static final int NAME_WIDTH = 160;

    private final GuiAdvancedMemoryCard gui;
    private final InfoList infos;
    private int x;
    private int y;
    private final Supplier<BetterMemoryCardModes> mode;
    private final WidgetScrollBar scrollBar;

    private final List<WidgetP2PDevice> entries = new ArrayList<>();
    private final IGuiTextField renameBar = new IGuiTextField(NAME_WIDTH, 12);

    public WidgetP2PColumn(GuiAdvancedMemoryCard gui, InfoList infos, int x, int y,
            Supplier<BetterMemoryCardModes> mode, WidgetScrollBar scrollBar) {
        this.gui = gui;
        this.infos = infos;
        this.x = x;
        this.y = y;
        this.mode = mode;
        this.scrollBar = scrollBar;
        renameBar.setMaxStringLength(50);
    }

    public List<WidgetP2PDevice> getEntries() {
        return entries;
    }

    public void resize(GuiScale scale, int availableHeight) {
        entries.clear();
        for (int i = 0; i < scale.size(availableHeight); i++) {
            int offset = i;
            entries.add(
                    new WidgetP2PDevice(
                            infos::getSelectedInfo,
                            mode,
                            () -> getFilteredInfo(offset + scrollBar.getCurrentScroll()),
                            gui,
                            x,
                            y + i * (P2PEntryConstants.HEIGHT + 1)));
        }
    }

    private @Nullable InfoWrapper getFilteredInfo(int index) {
        List<InfoWrapper> filtered = infos.getFiltered();
        return index >= 0 && index < filtered.size() ? filtered.get(index) : null;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        for (int i = 0; i < entries.size(); i++) {
            WidgetP2PDevice entry = entries.get(i);
            entry.x = x;
            entry.y = y + i * (P2PEntryConstants.HEIGHT + 1);
        }
    }

    public void finishRename() {
        for (WidgetP2PDevice widget : entries) {
            widget.renderNameTextField = true;
        }
        InfoWrapper info = renameBar.getInfo();
        if (info != null && !renameBar.getText().isEmpty() && !info.name.equals(renameBar.getText())) {
            renameBar.setText(renameBar.getText().trim());
            ModNetwork.CHANNEL.sendToServer(new C2SRenameP2P(info.loc, renameBar.getText()));
        }
        renameBar.setVisible(false);
        renameBar.setText("");
        renameBar.setFocus(false);
        renameBar.setInfo(null);
    }

    private void onRenameButtonClicked(InfoWrapper info, int index) {
        renameBar.setVisible(true);
        renameBar.y = y + index * (P2PEntryConstants.HEIGHT + 1) + 1;
        renameBar.x = x + NAME_OFFSET_X;
        renameBar.setText(info.name);
        renameBar.setFocus(true, 0);
        renameBar.setInfo(info);
    }

    private void onSelectButtonClicked(WidgetP2PDevice widget, InfoWrapper info, int mouseButton) {
        if (mouseButton == 1) {
            gui.openTypeSelector(widget, false);
        } else if (!info.equals(infos.getSelectedInfo())) {
            gui.selectInfo(info.loc);
        }
        info.bindButton.func_146113_a(gui.mc.getSoundHandler());
    }

    private void onBindButtonClicked(InfoWrapper info) {
        P2PLocation selectedEntry = infos.getSelectedEntry();
        if (selectedEntry == null) return;
        switch (mode.get()) {
            case INPUT -> {
                BetterP2P.logger.debug("Bind {} as input", info.loc);
                ModNetwork.CHANNEL.sendToServer(new C2SLinkP2P(info.loc, selectedEntry));
            }
            case OUTPUT -> {
                BetterP2P.logger.debug("Bind {} as output", info.loc);
                ModNetwork.CHANNEL.sendToServer(new C2SLinkP2P(selectedEntry, info.loc));
            }
            case COPY -> {
                BetterP2P.logger.debug("Copy {} to {}", selectedEntry, info.loc);
                ModNetwork.CHANNEL.sendToServer(new C2SLinkP2P(selectedEntry, info.loc));
            }
            default -> BetterP2P.logger.debug("Somehow bind button was pressed while in UNBIND mode.");
        }
        info.bindButton.func_146113_a(gui.mc.getSoundHandler());
    }

    private void onUnbindButtonClicked(InfoWrapper info) {
        if (info.getFrequency() != 0L) {
            ModNetwork.CHANNEL.sendToServer(new C2SUnlinkP2P(info.loc, gui.getTypeID()));
            info.setFrequency(0L);
        }
    }

    public @Nullable InfoWrapper findInput(long frequency) {
        for (InfoWrapper info : infos.getFiltered()) {
            if (info.getFrequency() == frequency && !info.output) {
                return info;
            }
        }
        return null;
    }

    public @Nullable InfoWrapper findOutput(long frequency) {
        for (InfoWrapper info : infos.getFiltered()) {
            if (info.getFrequency() == frequency && info.output) {
                return info;
            }
        }
        return null;
    }

    private static boolean isInNameArea(WidgetP2PDevice widget, int mouseX, int mouseY) {
        return mouseX > widget.x + NAME_OFFSET_X && mouseX < widget.x + NAME_OFFSET_X + NAME_WIDTH
                && mouseY > widget.y + 1
                && mouseY < widget.y + 1 + 13;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean clickRenameButton = false;
        for (int index = 0; index < entries.size(); index++) {
            WidgetP2PDevice widget = entries.get(index);
            InfoWrapper info = widget.getInfo();
            if (info == null) {
                continue;
            }
            if (info.bindButton.mousePressed(gui.mc, mouseX, mouseY)) {
                onBindButtonClicked(info);
            } else if (info.unbindButton.mousePressed(gui.mc, mouseX, mouseY)) {
                onUnbindButtonClicked(info);
            } else if (isInNameArea(widget, mouseX, mouseY)) {
                if (!Objects.equals(renameBar.getInfo(), info)) {
                    finishRename();
                }
                widget.renderNameTextField = false;
                onRenameButtonClicked(info, index);
                clickRenameButton = true;
            } else if (mouseX > widget.x && mouseX < widget.x + P2PEntryConstants.WIDTH
                    && mouseY > widget.y
                    && mouseY < widget.y + P2PEntryConstants.HEIGHT) {
                        onSelectButtonClicked(widget, info, mouseButton);
                    }
        }
        renameBar.mouseClicked(mouseX, mouseY, mouseButton);
        if (!clickRenameButton && renameBar.isVisible()) {
            finishRename();
        }
    }

    public void mouseHovered(int mouseX, int mouseY) {
        for (WidgetP2PDevice widget : entries) {
            InfoWrapper info = widget.getInfo();
            if (info == null) {
                continue;
            }
            if (mouseX > widget.x && mouseX < widget.x + 20
                    && mouseY > widget.y
                    && mouseY < widget.y + P2PEntryConstants.HEIGHT) {
                gui.drawHoveringText(info.hoverInfo, widget.x + 168, widget.y + 15, gui.mc.fontRenderer);
            } else if (isInNameArea(widget, mouseX, mouseY) && !renameBar.isFocused()) {
                Gui.drawRect(
                        widget.x + NAME_OFFSET_X,
                        widget.y + 1,
                        widget.x + NAME_OFFSET_X + NAME_WIDTH,
                        widget.y + 1 + 12,
                        ColorUtils.searchboxFocused.getColor());
            }
        }
    }

    public boolean keyTyped(char c, int key) {
        if (!renameBar.isFocused()) {
            return false;
        }
        if (key == Keyboard.KEY_RETURN) {
            finishRename();
        } else {
            renameBar.textboxKeyTyped(c, key);
        }
        return true;
    }

    public void render(int mouseX, int mouseY, float partialTicks) {
        for (WidgetP2PDevice widget : entries) {
            widget.render(mouseX, mouseY, partialTicks);
        }
        renameBar.drawTextBox();
    }

    public void onGuiClosed() {
        finishRename();
    }
}
