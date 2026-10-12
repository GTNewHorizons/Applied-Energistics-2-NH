package appeng.client.render.highlighter;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import appeng.core.sync.packets.PacketChannelAlert;
import appeng.core.sync.packets.PacketChannelAlert.Target;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Independent of the search highlighters: blinking outlines, but continuously visible navigation lines. */
public final class ChannelAlertHighlighter extends BlockPosHighlighter {

    public static final ChannelAlertHighlighter INSTANCE = new ChannelAlertHighlighter();
    private static final long DURATION = TimeUnit.SECONDS.toNanos(10);
    private static final long BLINK_INTERVAL = TimeUnit.MILLISECONDS.toNanos(500);
    private final Map<Target, Long> targets = new LinkedHashMap<>();
    private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final FloatBuffer lineStart = BufferUtils.createFloatBuffer(3);
    private World renderWorld;

    private ChannelAlertHighlighter() {}

    public static void highlight(List<Target> newTargets) {
        final World world = Minecraft.getMinecraft().theWorld;
        if (INSTANCE.renderWorld != world) INSTANCE.clear();
        INSTANCE.renderWorld = world;
        final long expiry = System.nanoTime() + DURATION;
        for (Target target : newTargets) {
            if (world != null && target.location.getDimension() == world.provider.dimensionId) {
                INSTANCE.targets.put(target, expiry);
            }
        }
        while (INSTANCE.targets.size() > PacketChannelAlert.MAX_TARGETS) {
            INSTANCE.targets.remove(INSTANCE.targets.keySet().iterator().next());
        }
    }

    @Override
    public void clear() {
        super.clear();
        targets.clear();
        renderWorld = null;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world == renderWorld) clear();
    }

    @Override
    @SubscribeEvent
    public void renderHighlightedBlocks(RenderWorldLastEvent event) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (renderWorld != mc.theWorld || mc.theWorld == null || mc.renderViewEntity == null) {
            clear();
            return;
        }
        final long now = System.nanoTime();
        targets.values().removeIf(expiry -> now - expiry >= 0);
        if (targets.isEmpty() || mc.gameSettings.hideGUI) return;
        updateCameraState(event);
        beginOutline();
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDepthMask(false);
            // Unproject the actual viewport centre, so the line starts at the crosshair even with view bobbing/F5.
            modelView.clear();
            projection.clear();
            viewport.clear();
            lineStart.clear();
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            final boolean drawLines = GLU.gluUnProject(
                    viewport.get(0) + viewport.get(2) / 2.0f,
                    viewport.get(1) + viewport.get(3) / 2.0f,
                    0.01f,
                    modelView,
                    projection,
                    viewport,
                    lineStart);
            for (Map.Entry<Target, Long> entry : targets.entrySet()) {
                final Target target = entry.getKey();
                if (target.location.getDimension() != dimension) continue;
                if (((entry.getValue() - now) / BLINK_INTERVAL & 1) == 1) {
                    renderHighLightedBlocksOutline(target.location.x, target.location.y, target.location.z);
                }
                if (drawLines) {
                    final Tessellator tess = Tessellator.instance;
                    tess.startDrawing(GL11.GL_LINES);
                    tess.setColorRGBA_F(1.0f, 0.15f, 0.15f, 1.0f);
                    tess.addVertex(lineStart.get(0), lineStart.get(1), lineStart.get(2));
                    tess.addVertex(
                            target.location.x + 0.5 + target.side.offsetX * 0.45,
                            target.location.y + 0.5 + target.side.offsetY * 0.45,
                            target.location.z + 0.5 + target.side.offsetZ * 0.45);
                    tess.draw();
                }
            }
        } finally {
            endOutline();
        }
    }
}
