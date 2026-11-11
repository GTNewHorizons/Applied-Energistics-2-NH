package com.glodblock.github.client.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import appeng.api.features.IUltraTerminalType;
import appeng.client.gui.ScreenColor;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.texture.ExtraBlockTextures;

/**
 * Button that switches the ultra terminal to the given terminal type. The icon of the terminal type is rendered
 * directly, so addons do not need to add sprites to the AE2 states texture.
 */
public class GuiModeButton extends GuiButton implements ITooltip {

    private static final RenderItem ITEM_RENDERER = new RenderItem();

    private IUltraTerminalType type;
    private ItemStack icon;
    private String message;

    public GuiModeButton(final int x, final int y, final IUltraTerminalType type) {
        super(0, x, y, 16, 16, "");
        this.type = type;
        this.icon = type.getIcon();
        this.message = type.getDisplayName();
    }

    public GuiModeButton(final int x, final int y, final IUltraTerminalType type, final String message) {
        this(x, y, type);
        this.message = message;
    }

    /**
     * Changes the terminal shown by this button, used by the single switch button when the selected terminal changes.
     */
    public void setType(final IUltraTerminalType type, final String message) {
        this.type = type;
        this.icon = type.getIcon();
        this.message = message;
    }

    public IUltraTerminalType getType() {
        return this.type;
    }

    public String getTypeId() {
        return this.type.getId();
    }

    public void setVisibility(final boolean visible) {
        this.visible = visible;
        this.enabled = visible;
    }

    @Override
    public void drawButton(final Minecraft mc, final int mouseX, final int mouseY) {
        if (!this.visible) {
            return;
        }
        this.field_146123_n = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width
                && mouseY < this.yPosition + this.height;

        // rendering an item stack changes the gl state for the widgets drawn after it
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT);
        try {
            ScreenColor.setGuiColor();
            mc.renderEngine.bindTexture(ExtraBlockTextures.GuiTexture("guis/states.png"));
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 256 - 16, 256 - 16, 16, 16);
            ScreenColor.resetGuiColor();

            final ItemStack icon = this.icon;
            if (icon != null) {
                this.zLevel = 100.0F;
                ITEM_RENDERER.zLevel = 100.0F;
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                RenderHelper.enableGUIStandardItemLighting();
                ITEM_RENDERER.renderItemAndEffectIntoGUI(
                        mc.fontRenderer,
                        mc.renderEngine,
                        icon,
                        this.xPosition,
                        this.yPosition);
                GL11.glDisable(GL11.GL_LIGHTING);
                ITEM_RENDERER.zLevel = 0.0F;
                this.zLevel = 0.0F;
            }

            this.mouseDragged(mc, mouseX, mouseY);
        } finally {
            GL11.glPopAttrib();
        }
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public int xPos() {
        return this.xPosition;
    }

    @Override
    public int yPos() {
        return this.yPosition;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }
}
