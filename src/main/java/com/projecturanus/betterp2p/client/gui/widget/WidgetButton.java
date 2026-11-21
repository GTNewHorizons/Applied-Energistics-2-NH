package com.projecturanus.betterp2p.client.gui.widget;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.projecturanus.betterp2p.client.gui.GuiAdvancedMemoryCard;
import com.projecturanus.betterp2p.client.gui.GuiHelper;

public abstract class WidgetButton extends GuiButton {

    protected final GuiAdvancedMemoryCard gui;
    private double texX;
    private double texY;
    private List<String> hoverText = Collections.emptyList();

    public WidgetButton(GuiAdvancedMemoryCard gui, int x, int y, int width, int height) {
        super(0, x, y, width, height, "");
        this.gui = gui;
    }

    public abstract boolean mousePressed(int mouseX, int mouseY, int button);

    public void draw(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        Tessellator tessellator = Tessellator.instance;
        drawBG(tessellator, mouseX, mouseY, partialTicks);
        GuiHelper.drawTexturedQuad(
                tessellator,
                xPosition + 1.0,
                yPosition + 1.0,
                xPosition + width - 1.0,
                yPosition + height - 1.0,
                texX / GuiAdvancedMemoryCard.GUI_WIDTH,
                texY / GuiAdvancedMemoryCard.GUI_TEX_HEIGHT,
                (texX + width) / GuiAdvancedMemoryCard.GUI_WIDTH,
                (texY + height) / GuiAdvancedMemoryCard.GUI_TEX_HEIGHT);
    }

    protected void drawBG(Tessellator tessellator, int mouseX, int mouseY, float partialTicks) {
        gui.bindTexture(GuiAdvancedMemoryCard.BACKGROUND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        field_146123_n = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width
                && mouseY < yPosition + height;
        int k = getHoverState(field_146123_n);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GuiHelper.drawTexturedQuad(
                tessellator,
                xPosition,
                yPosition,
                xPosition + width,
                yPosition + height,
                (32.0 * k) / GuiAdvancedMemoryCard.GUI_WIDTH,
                232.0 / GuiAdvancedMemoryCard.GUI_TEX_HEIGHT,
                (32.0 * (k + 1)) / GuiAdvancedMemoryCard.GUI_WIDTH,
                (232.0 + height) / GuiAdvancedMemoryCard.GUI_TEX_HEIGHT);
    }

    public List<String> getHoverText() {
        return hoverText;
    }

    public void setHoverText(List<String> hoverText) {
        this.hoverText = hoverText;
    }

    public void setPosition(int x, int y) {
        this.xPosition = x;
        this.yPosition = y;
    }

    public void setTexCoords(double x, double y) {
        this.texX = x;
        this.texY = y;
    }

    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX > xPosition && mouseX < xPosition + width && mouseY > yPosition && mouseY < yPosition + height;
    }
}
