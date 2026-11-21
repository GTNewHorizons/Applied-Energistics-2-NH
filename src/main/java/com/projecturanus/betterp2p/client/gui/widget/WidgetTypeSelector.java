package com.projecturanus.betterp2p.client.gui.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;

import com.projecturanus.betterp2p.client.gui.GuiAdvancedMemoryCard;
import com.projecturanus.betterp2p.client.gui.GuiHelper;
import com.projecturanus.betterp2p.util.p2p.ClientTunnelInfo;

public class WidgetTypeSelector {

    private static final int ICONS_PER_ROW = 5;
    private static final int ICON_SIZE = 18;

    private int x;
    private int y;
    private final List<ClientTunnelInfo> p2pTypes;
    private final int width;
    private final int height;
    private final List<List<String>> translated = new ArrayList<>();
    private boolean visible;
    private int hoveredIdx;
    private boolean useAny;
    private ITypeReceiver parent;

    public WidgetTypeSelector(int x, int y, List<ClientTunnelInfo> p2pTypes, ITypeReceiver parent) {
        this.x = x;
        this.y = y;
        this.p2pTypes = p2pTypes;
        this.parent = parent;
        int typeCount = p2pTypes.size();
        width = Math.min(ICONS_PER_ROW, typeCount) * ICON_SIZE + 8;
        height = (typeCount + ICONS_PER_ROW - 1) * ICON_SIZE / ICONS_PER_ROW + 8;
        for (ClientTunnelInfo type : p2pTypes) {
            translated.add(Collections.singletonList(type.dispName));
        }
        translated.add(Collections.singletonList(I18n.format("gui.advanced_memory_card.types.any")));
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setUseAny(boolean useAny) {
        this.useAny = useAny;
    }

    public ITypeReceiver getParent() {
        return parent;
    }

    public void setParent(ITypeReceiver parent) {
        this.parent = parent;
    }

    public void render(GuiAdvancedMemoryCard gui, int mouseX, int mouseY, float partialTicks) {
        GuiScreen.drawRect(x, y, x + width, y + height, 0xAA000000);
        hoveredIdx = -1;
        for (int i = 0; i < p2pTypes.size(); i++) {
            ClientTunnelInfo type = p2pTypes.get(i);
            drawSlot(gui, i, mouseX, mouseY);
            GuiHelper
                    .drawBlockIcon(gui.mc, type.getIcon(), type.stack.getIconIndex(), getIconX(i) + 1, getIconY(i) + 1);
        }
        if (useAny) {
            int i = p2pTypes.size();
            drawSlot(gui, i, mouseX, mouseY);
            GuiHelper.drawBlockIcon(
                    gui.mc,
                    Blocks.coal_block.getIcon(0, 0),
                    p2pTypes.get(0).stack.getIconIndex(),
                    getIconX(i) + 1,
                    getIconY(i) + 1);
            gui.mc.fontRenderer.drawString("?", getIconX(i) + 6, getIconY(i) + 6, 0xFFFF0000);
        }
        if (hoveredIdx != -1) {
            gui.drawHoveringText(translated.get(hoveredIdx), mouseX, mouseY, gui.mc.fontRenderer);
        }
    }

    private void drawSlot(GuiAdvancedMemoryCard gui, int i, int mouseX, int mouseY) {
        int iconX = getIconX(i);
        int iconY = getIconY(i);
        if (mouseX > iconX && mouseX < iconX + ICON_SIZE && mouseY > iconY && mouseY < iconY + ICON_SIZE) {
            hoveredIdx = i;
            GuiScreen.drawRect(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, 0xFF00FF00);
        }
    }

    private int getIconX(int i) {
        return x + 4 + (i % ICONS_PER_ROW) * ICON_SIZE;
    }

    private int getIconY(int i) {
        return y + 4 + (i / ICONS_PER_ROW) * ICON_SIZE;
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void mousePressed(int mouseX, int mouseY, int mouseButton) {
        if (mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height) {
            if (hoveredIdx == -1) return;
            parent.accept(hoveredIdx < p2pTypes.size() ? p2pTypes.get(hoveredIdx) : null);
        } else {
            visible = false;
        }
    }
}
