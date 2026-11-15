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

    private boolean active;
    private int posX;
    private int posY;
    private int posZ;

    private boolean hasQueried;
    private int queriedX;
    private int queriedY;
    private int queriedZ;
    private int ticksSinceQuery;

    private boolean hasResult;
    private int resultX;
    private int resultY;
    private int resultZ;
    private boolean resultConflict;

    public void setResult(int x, int y, int z, boolean conflict) {
        this.hasResult = true;
        this.resultX = x;
        this.resultY = y;
        this.resultZ = z;
        this.resultConflict = conflict;
    }

    private boolean isConflict() {
        return this.active && this.hasResult
                && this.resultX == this.posX
                && this.resultY == this.posY
                && this.resultZ == this.posZ
                && this.resultConflict;
    }

    public void onClientTick() {
        this.active = false;

        final Minecraft mc = Minecraft.getMinecraft();
        final EntityPlayer player = mc.thePlayer;
        final World world = mc.theWorld;
        if (player == null || world == null) {
            this.hasQueried = false;
            return;
        }

        final ItemStack held = player.getHeldItem();
        if (held == null || !(Block.getBlockFromItem(held.getItem()) instanceof BlockController)) {
            this.hasQueried = false;
            return;
        }

        final MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            this.hasQueried = false;
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

        this.ticksSinceQuery++;
        final boolean changed = !this.hasQueried || this.queriedX != x || this.queriedY != y || this.queriedZ != z;
        if (changed || this.ticksSinceQuery >= REQUERY_TICKS) {
            this.hasQueried = true;
            this.queriedX = x;
            this.queriedY = y;
            this.queriedZ = z;
            this.ticksSinceQuery = 0;
            NetworkHandler.instance.sendToServer(new PacketControllerPlacementQuery(x, y, z));
        }
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
        if (!AEConfig.instance.previewBlocks || !this.isConflict()) return;

        final FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        final String text = GuiText.ControllerPlacementConflict.getLocal();
        final int cx = event.resolution.getScaledWidth() / 2;
        final int cy = event.resolution.getScaledHeight() / 2;
        font.drawStringWithShadow(text, cx - font.getStringWidth(text) / 2, cy + 12, WARNING_COLOR);
    }
}
