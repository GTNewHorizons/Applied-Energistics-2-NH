package com.projecturanus.betterp2p.client.render;

import java.util.Collection;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import org.lwjgl.opengl.GL11;

import com.projecturanus.betterp2p.network.data.P2PLocation;

/**
 * This class is based on the <a href=
 * "https://github.com/romelo333/notenoughwands1.8.8/blob/20952f50e7c1ab3fd676ed3da302666295e3cac8/src/main/java/romelo333/notenoughwands/Items/GenericWand.java">GenericWand</a>
 * class from a mod with MIT license.
 * <a href="https://github.com/romelo333/notenoughwands1.8.8/blob/master/LICENSE">Source license</a>
 */
public class OutlineRenderer {

    public static void renderOutlinesWithFacing(RenderWorldLastEvent evt, EntityPlayer p,
            Collection<P2PLocation> coordinates, int r, int g, int b) {
        double doubleX = p.lastTickPosX + (p.posX - p.lastTickPosX);
        double doubleY = p.lastTickPosY + (p.posY - p.lastTickPosY);
        double doubleZ = p.lastTickPosZ + (p.posZ - p.lastTickPosZ);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        renderOutlinesWithFacing(coordinates, -doubleX, -doubleY, -doubleZ, r, g, b, 4);
        GL11.glPopAttrib();
    }

    private static void renderOutlinesWithFacing(Collection<P2PLocation> coordinates, double x, double y, double z,
            int r, int g, int b, int thickness) {
        Tessellator tessellator = Tessellator.instance;

        for (P2PLocation pos : coordinates) {
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            tessellator.startDrawing(GL11.GL_LINES);
            GL11.glLineWidth(thickness);
            tessellator.setTranslation(pos.x, pos.y, pos.z);

            renderHighLightedBlocksOutlineForFacing(tessellator, r, g, b, 255);
            tessellator.setTranslation(0, 0, 0);
            GL11.glTranslated(pos.x, pos.y, pos.z);
            GL11.glTranslated(0.5, 0.5, 0.5);

            switch (pos.facing) {
                case DOWN:
                    GL11.glRotated(90, 0, 1, 0);
                    GL11.glRotated(90, 0, 0, 1);
                    break;
                case UP:
                    GL11.glRotated(90, 0, 1, 0);
                    GL11.glRotated(270, 0, 0, 1);
                    break;
                case NORTH:
                    GL11.glRotated(-90, 0, 1, 0);
                    break;
                case SOUTH:
                    GL11.glRotated(90, 0, 1, 0);
                    break;
                case EAST:
                    GL11.glRotated(180, 0, 0, 1);
                    break;
                case WEST:
                    break;
            }

            GL11.glTranslated(-0.5, -0.5, -0.5);
            GL11.glTranslated(-pos.x, -pos.y, -pos.z);
            tessellator.draw();
            GL11.glPopMatrix();
        }
    }

    private static void renderHighLightedBlocksOutlineForFacing(Tessellator tessellator, int r, int g, int b, int a) {
        double minX = 0;
        double minY = 0.125;
        double minZ = 0.125;

        double maxX = 0.1875;
        double maxY = 0.875;
        double maxZ = 0.875;

        tessellator.setColorRGBA(r, g, b, a);

        tessellator.addVertex(minX, minY, minZ);
        tessellator.addVertex(maxX, minY, minZ);

        tessellator.addVertex(minX, minY, minZ);
        tessellator.addVertex(minX, maxY, minZ);

        tessellator.addVertex(minX, minY, minZ);
        tessellator.addVertex(minX, minY, maxZ);

        tessellator.addVertex(maxX, maxY, maxZ);
        tessellator.addVertex(minX, maxY, maxZ);

        tessellator.addVertex(maxX, maxY, maxZ);
        tessellator.addVertex(maxX, minY, maxZ);

        tessellator.addVertex(maxX, maxY, maxZ);
        tessellator.addVertex(maxX, maxY, minZ);

        tessellator.addVertex(minX, maxY, minZ);
        tessellator.addVertex(minX, maxY, maxZ);

        tessellator.addVertex(minX, maxY, minZ);
        tessellator.addVertex(maxX, maxY, minZ);

        tessellator.addVertex(maxX, minY, minZ);
        tessellator.addVertex(maxX, minY, maxZ);

        tessellator.addVertex(maxX, minY, minZ);
        tessellator.addVertex(maxX, maxY, minZ);

        tessellator.addVertex(minX, minY, maxZ);
        tessellator.addVertex(maxX, minY, maxZ);

        tessellator.addVertex(minX, minY, maxZ);
        tessellator.addVertex(minX, maxY, maxZ);
    }
}
