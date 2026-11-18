package com.projecturanus.betterp2p.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class GuiHelper {

    private GuiHelper() {}

    /**
     * Draws a textured quad from the top left corner (x0, y0) to the bottom right corner (x1, y1).
     */
    public static void drawTexturedQuad(Tessellator tessellator, double x0, double y0, double x1, double y1, double u0,
            double v0, double u1, double v1) {
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x0, y1, 0.0, u0, v1);
        tessellator.addVertexWithUV(x1, y1, 0.0, u1, v1);
        tessellator.addVertexWithUV(x1, y0, 0.0, u1, v0);
        tessellator.addVertexWithUV(x0, y0, 0.0, u0, v0);
        tessellator.draw();
    }

    public static void drawBlockIcon(Minecraft mc, IIcon icon, IIcon overlay, int x, int y) {
        drawBlockIcon(mc, icon, overlay, x, y, 16.0, 16.0);
    }

    public static void drawBlockIcon(Minecraft mc, IIcon icon, IIcon overlay, int x, int y, double width,
            double height) {
        Tessellator tessellator = Tessellator.instance;
        mc.getTextureManager().bindTexture(mc.renderEngine.getResourceLocation(0));
        GL11.glPushAttrib(GL11.GL_BLEND | GL11.GL_TEXTURE_2D | GL11.GL_COLOR);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor3f(255f, 255f, 255f);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        drawTexturedQuad(
                tessellator,
                x + 2.0,
                y + 2.0,
                x + width - 2,
                y + height - 2,
                icon.getMinU(),
                icon.getMinV(),
                icon.getMaxU(),
                icon.getMaxV());
        drawTexturedQuad(
                tessellator,
                x,
                y,
                x + width,
                y + height,
                overlay.getMinU(),
                overlay.getMinV(),
                overlay.getMaxU(),
                overlay.getMaxV());
        GL11.glPopAttrib();
    }
}
