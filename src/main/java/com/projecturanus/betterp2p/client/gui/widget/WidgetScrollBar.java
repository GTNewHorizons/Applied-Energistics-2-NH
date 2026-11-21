package com.projecturanus.betterp2p.client.gui.widget;

import net.minecraft.client.gui.Gui;

import com.projecturanus.betterp2p.client.TextureBound;

public class WidgetScrollBar {

    private int displayX;
    private int displayY;
    private final int width = 12;
    private int height = 16;

    private int maxScroll = 0;
    private int minScroll = 0;

    private int currentScroll = 0;
    private boolean moving = false;

    public WidgetScrollBar(int displayX, int displayY) {
        this.displayX = displayX;
        this.displayY = displayY;
    }

    public <T extends Gui & TextureBound> void draw(T g) {
        g.bindTexture("minecraft", "textures/gui/container/creative_inventory/tabs.png");
        if (getRange() == 0) {
            g.drawTexturedModalRect(displayX, displayY, 232 + width, 0, width, 15);
        } else {
            int offset = (currentScroll - minScroll) * (height - 15) / getRange();
            g.drawTexturedModalRect(displayX, offset + displayY, 232, 0, width, 15);
        }
    }

    public void setPosition(int displayX, int displayY) {
        this.displayX = displayX;
        this.displayY = displayY;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getCurrentScroll() {
        return currentScroll;
    }

    public void stopMoving() {
        moving = false;
    }

    private int getRange() {
        return maxScroll - minScroll;
    }

    public void setRange(int min, int max) {
        minScroll = min;
        maxScroll = max;
        if (minScroll > maxScroll) {
            maxScroll = minScroll;
        }
        applyRange();
    }

    private void applyRange() {
        currentScroll = Math.max(minScroll, Math.min(maxScroll, currentScroll));
    }

    public void click(int x, int y) {
        if (getRange() == 0) {
            return;
        }
        if (y > displayY && y <= displayY + height) {
            if (moving || (x > displayX && x <= displayX + width)) {
                currentScroll = y - displayY;
                currentScroll = minScroll + currentScroll * 2 * getRange() / height;
                currentScroll = (currentScroll + 1) >> 1;
                applyRange();
                moving = true;
            }
        }
    }

    public void wheel(int delta) {
        currentScroll += Math.max(-1, Math.min(1, -delta));
        applyRange();
    }
}
