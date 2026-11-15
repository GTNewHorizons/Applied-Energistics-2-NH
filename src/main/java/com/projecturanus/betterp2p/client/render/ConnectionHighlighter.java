package com.projecturanus.betterp2p.client.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import com.projecturanus.betterp2p.item.ItemAdvancedMemoryCard;
import com.projecturanus.betterp2p.network.data.P2PInfo;

/** Client-side connection locator visible while holding an Advanced Memory Card. */
public final class ConnectionHighlighter {

    private static final FloatBuffer MODEL_VIEW = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer LINE_START = BufferUtils.createFloatBuffer(3);
    private static volatile Session session;

    private ConnectionHighlighter() {}

    /** Replace the current locator with a snapshot of server-supplied targets. Call on the client thread. */
    public static void show(List<P2PInfo> targets) {
        final Minecraft mc = Minecraft.getMinecraft();
        final World world = mc.theWorld;
        if (world == null || mc.thePlayer == null) {
            clear();
            return;
        }
        final int dimension = world.provider.dimensionId;
        final List<Target> snapshot = new ArrayList<>();
        for (P2PInfo info : targets) {
            if (info.getDim() == dimension) {
                snapshot.add(new Target(info));
            }
        }
        session = snapshot.isEmpty() ? null : new Session(world, dimension, snapshot.toArray(new Target[0]));
    }

    public static void clear() {
        session = null;
    }

    /** @return true when locator outlines were rendered, so normal card overlays should be skipped. */
    public static boolean render(RenderWorldLastEvent event) {
        final Session active = session;
        if (active == null) return false;
        final Minecraft mc = Minecraft.getMinecraft();
        final EntityLivingBase camera = mc.renderViewEntity;
        if (mc.thePlayer == null || mc.theWorld == null
                || camera == null
                || mc.theWorld != active.world
                || mc.theWorld.provider.dimensionId != active.dimension) {
            clear();
            return false;
        }
        if (mc.thePlayer.getHeldItem() == null
                || mc.thePlayer.getHeldItem().getItem() != ItemAdvancedMemoryCard.INSTANCE)
            return false;
        final double cameraX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks;
        final double cameraY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks;
        final double cameraZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(-cameraX, -cameraY, -cameraZ);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDepthMask(false);
            GL11.glLineWidth(3.0f);

            // Capture after world translation: both unprojected start and target vertices are world coordinates.
            MODEL_VIEW.clear();
            PROJECTION.clear();
            VIEWPORT.clear();
            LINE_START.clear();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODEL_VIEW);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
            GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
            final boolean drawLines = GLU.gluUnProject(
                    VIEWPORT.get(0) + VIEWPORT.get(2) / 2.0f,
                    VIEWPORT.get(1) + VIEWPORT.get(3) / 2.0f,
                    0.01f,
                    MODEL_VIEW,
                    PROJECTION,
                    VIEWPORT,
                    LINE_START);

            for (Target target : active.targets) {
                renderOutline(target);
            }
            if (drawLines) {
                final Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawing(GL11.GL_LINES);
                for (Target target : active.targets) {
                    tessellator.setColorRGBA(target.red, target.green, target.blue, 255);
                    tessellator.addVertex(LINE_START.get(0), LINE_START.get(1), LINE_START.get(2));
                    tessellator.addVertex(
                            target.x + 0.5 + target.facing.offsetX * 0.45,
                            target.y + 0.5 + target.facing.offsetY * 0.45,
                            target.z + 0.5 + target.facing.offsetZ * 0.45);
                }
                tessellator.draw();
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        return true;
    }

    private static void renderOutline(Target target) {
        GL11.glPushMatrix();
        try {
            GL11.glTranslated(target.x + 0.5, target.y + 0.5, target.z + 0.5);
            // OutlineRenderer's part geometry is on the west face before rotation.
            switch (target.facing) {
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
                default:
                    break;
            }
            GL11.glTranslated(-0.5, -0.5, -0.5);
            final Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawing(GL11.GL_LINES);
            OutlineRenderer
                    .renderHighLightedBlocksOutlineForFacing(tessellator, target.red, target.green, target.blue, 255);
            tessellator.draw();
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static final class Session {

        private final World world;
        private final int dimension;

        private final Target[] targets;

        private Session(World world, int dimension, Target[] targets) {
            this.world = world;
            this.dimension = dimension;

            this.targets = targets;
        }
    }

    private static final class Target {

        private final int x;
        private final int y;
        private final int z;
        private final ForgeDirection facing;
        private final int red;
        private final int green;
        private final int blue;

        private Target(P2PInfo info) {
            x = info.getPosX();
            y = info.getPosY();
            z = info.getPosZ();
            facing = info.getFacing();
            red = info.getOutput() ? 0x33 : 0x45;
            green = info.getOutput() ? 0xDD : 0xDA;
            blue = info.getOutput() ? 0xFF : 0x75;
        }
    }
}
