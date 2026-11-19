package appeng.client.render.previewBlocks;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import appeng.api.util.AEColor;
import appeng.block.networking.BlockController;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketControllerPlacementQuery;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Client-side preview for ME Controller placement: draws a wireframe cube (red on conflict) and a warning text.
 */
public class ControllerPlacementPreview {

    private static final ControllerPlacementPreview INSTANCE = new ControllerPlacementPreview();
    private static final int REQUERY_TICKS = 20;
    private static final int WARNING_COLOR = 0xFF5555;

    public static ControllerPlacementPreview getInstance() {
        return INSTANCE;
    }

    private static final int KINDS = 2;

    private boolean active;
    private int posX;
    private int posY;
    private int posZ;

    // cable target, set each tick by ViewHelper while the stock cable preview is valid
    private boolean cableActive;
    private int cableX;
    private int cableY;
    private int cableZ;

    private final boolean[] hasQueried = new boolean[KINDS];
    private final int[] queriedX = new int[KINDS];
    private final int[] queriedY = new int[KINDS];
    private final int[] queriedZ = new int[KINDS];
    private final int[] queriedColor = new int[KINDS];
    private final int[] ticksSinceQuery = new int[KINDS];

    private final boolean[] hasResult = new boolean[KINDS];
    private final int[] resultX = new int[KINDS];
    private final int[] resultY = new int[KINDS];
    private final int[] resultZ = new int[KINDS];
    private final boolean[] resultConflict = new boolean[KINDS];

    public void setResult(int x, int y, int z, byte kind, boolean conflict) {
        if (kind < 0 || kind >= KINDS) return;
        this.hasResult[kind] = true;
        this.resultX[kind] = x;
        this.resultY[kind] = y;
        this.resultZ[kind] = z;
        this.resultConflict[kind] = conflict;
    }

    private boolean hasConflict(byte kind, int x, int y, int z) {
        return this.hasResult[kind] && this.resultX[kind] == x
                && this.resultY[kind] == y
                && this.resultZ[kind] == z
                && this.resultConflict[kind];
    }

    private boolean isConflict() {
        return this.active
                && this.hasConflict(PacketControllerPlacementQuery.KIND_CONTROLLER, this.posX, this.posY, this.posZ);
    }

    private boolean isCableConflict() {
        return this.cableActive
                && this.hasConflict(PacketControllerPlacementQuery.KIND_CABLE, this.cableX, this.cableY, this.cableZ);
    }

    /**
     * Sends a throttled query (on position/color change and every {@link #REQUERY_TICKS} ticks) and marks the cable
     * target as active for this tick.
     *
     * @return whether the cable placement at the position is known to conflict
     */
    public boolean queryCable(int x, int y, int z, AEColor color) {
        this.cableActive = true;
        this.cableX = x;
        this.cableY = y;
        this.cableZ = z;
        this.query(PacketControllerPlacementQuery.KIND_CABLE, x, y, z, color.ordinal());
        return this.isCableConflict();
    }

    private void query(byte kind, int x, int y, int z, int color) {
        this.ticksSinceQuery[kind]++;
        final boolean changed = !this.hasQueried[kind] || this.queriedX[kind] != x
                || this.queriedY[kind] != y
                || this.queriedZ[kind] != z
                || this.queriedColor[kind] != color;
        if (changed || this.ticksSinceQuery[kind] >= REQUERY_TICKS) {
            this.hasQueried[kind] = true;
            this.queriedX[kind] = x;
            this.queriedY[kind] = y;
            this.queriedZ[kind] = z;
            this.queriedColor[kind] = color;
            this.ticksSinceQuery[kind] = 0;
            NetworkHandler.instance.sendToServer(new PacketControllerPlacementQuery(x, y, z, kind, color));
        }
    }

    public void onClientTick() {
        this.active = false;
        this.cableActive = false;

        final Minecraft mc = Minecraft.getMinecraft();
        final EntityPlayer player = mc.thePlayer;
        final World world = mc.theWorld;
        if (player == null || world == null) {
            this.hasQueried[PacketControllerPlacementQuery.KIND_CONTROLLER] = false;
            return;
        }

        final ItemStack held = player.getHeldItem();
        if (held == null || !(Block.getBlockFromItem(held.getItem()) instanceof BlockController)) {
            this.hasQueried[PacketControllerPlacementQuery.KIND_CONTROLLER] = false;
            return;
        }

        final MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            this.hasQueried[PacketControllerPlacementQuery.KIND_CONTROLLER] = false;
            return;
        }

        int x = mop.blockX;
        int y = mop.blockY;
        int z = mop.blockZ;
        final Block hit = world.getBlock(x, y, z);
        if (hit != Block.getBlockFromName("snow_layer") && !hit.isReplaceable(world, x, y, z)) {
            final ForgeDirection side = ForgeDirection.getOrientation(mop.sideHit);
            x += side.offsetX;
            y += side.offsetY;
            z += side.offsetZ;
        }

        this.active = true;
        this.posX = x;
        this.posY = y;
        this.posZ = z;

        this.query(PacketControllerPlacementQuery.KIND_CONTROLLER, x, y, z, 0);
    }

    public void onRenderWorldLast(float partialTicks) {
        if (!this.active) return;

        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;

        final double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        final double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        final double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(-px, -py, -pz);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);

        if (this.isConflict()) {
            GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.6f);
        } else {
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.6f);
        }

        drawCube(this.posX, this.posY, this.posZ);

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static void drawCube(double minX, double minY, double minZ) {
        final double maxX = minX + 1;
        final double maxY = minY + 1;
        final double maxZ = minZ + 1;
        final Tessellator t = Tessellator.instance;

        GL11.glLineWidth(AEConfig.instance.previewLineWidth);
        t.startDrawing(GL11.GL_LINES);

        t.addVertex(minX, minY, minZ);
        t.addVertex(maxX, minY, minZ);
        t.addVertex(maxX, minY, minZ);
        t.addVertex(maxX, minY, maxZ);
        t.addVertex(maxX, minY, maxZ);
        t.addVertex(minX, minY, maxZ);
        t.addVertex(minX, minY, maxZ);
        t.addVertex(minX, minY, minZ);

        t.addVertex(minX, maxY, minZ);
        t.addVertex(maxX, maxY, minZ);
        t.addVertex(maxX, maxY, minZ);
        t.addVertex(maxX, maxY, maxZ);
        t.addVertex(maxX, maxY, maxZ);
        t.addVertex(minX, maxY, maxZ);
        t.addVertex(minX, maxY, maxZ);
        t.addVertex(minX, maxY, minZ);

        t.addVertex(minX, minY, minZ);
        t.addVertex(minX, maxY, minZ);
        t.addVertex(maxX, minY, minZ);
        t.addVertex(maxX, maxY, minZ);
        t.addVertex(maxX, minY, maxZ);
        t.addVertex(maxX, maxY, maxZ);
        t.addVertex(minX, minY, maxZ);
        t.addVertex(minX, maxY, maxZ);

        t.draw();
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        if (!AEConfig.instance.previewBlocks) return;

        final GuiText warning;
        if (this.isConflict()) {
            warning = GuiText.ControllerPlacementConflict;
        } else if (this.isCableConflict()) {
            warning = GuiText.CablePlacementConflict;
        } else {
            return;
        }

        final FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        final String text = warning.getLocal();
        final int cx = event.resolution.getScaledWidth() / 2;
        final int cy = event.resolution.getScaledHeight() / 2;
        font.drawStringWithShadow(text, cx - font.getStringWidth(text) / 2, cy + 12, WARNING_COLOR);
    }
}
