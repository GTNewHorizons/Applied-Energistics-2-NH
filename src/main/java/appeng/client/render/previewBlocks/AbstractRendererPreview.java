package appeng.client.render.previewBlocks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.api.OutlineShape;

import appeng.api.parts.IPartHost;
import appeng.core.AEConfig;
import it.unimi.dsi.fastutil.doubles.DoubleList;

public abstract class AbstractRendererPreview {

    private static final double DEPTH_BIAS = 0.0005;

    private final int lineWidth = AEConfig.instance.previewLineWidth;

    private final List<AxisAlignedBB> boxes = new ArrayList<>();
    private OutlineShape shape = OutlineShape.EMPTY;

    /** Adds a box to the outline that {@link #renderOutline()} draws. */
    protected void addBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        boxes.add(AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
    }

    /** Draws the boxes added since the last call as one outline, without lines where they meet. */
    protected void renderOutline() {
        OutlineShape built = OutlineShape.of(boxes);
        boxes.clear();
        // the shape merges its lines once, so keep the old one while the boxes stay the same
        if (!built.hasSameBoxes(shape)) shape = built;

        DoubleList lines = shape.getLines();
        if (lines.isEmpty()) return;

        Tessellator tessellator = Tessellator.instance;
        GL11.glLineWidth(lineWidth);
        tessellator.startDrawing(GL11.GL_LINES);
        for (int i = 0; i < lines.size(); i += 3) {
            tessellator.addVertex(lines.getDouble(i), lines.getDouble(i + 1), lines.getDouble(i + 2));
        }
        tessellator.draw();
    }

    protected void getValidColorGL11() {
        if (ViewHelper.getValidPosition()) {
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.6f);
        } else {
            GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.6f);
        }
    }

    protected void setPreviewOffset(int x, int y, int z, ForgeDirection side) {
        ViewHelper.setPreviewX(x + side.offsetX);
        ViewHelper.setPreviewY(y + side.offsetY);
        ViewHelper.setPreviewZ(z + side.offsetZ);
    }

    protected boolean canPlaceBlockAt(World world, int x, int y, int z) {
        if (world.isAirBlock(x, y, z)) {
            return true;
        }
        Block block = world.getBlock(x, y, z);
        return block != null && block.isReplaceable(world, x, y, z);
    }

    protected boolean hasParts(IPartHost partHost) {
        if (partHost.getPart(ForgeDirection.UNKNOWN) != null) {
            return true;
        }

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if (partHost.getPart(dir) != null) {
                return true;
            }
        }

        return false;
    }

    protected void applySideRotation(double x, double y, double z, ForgeDirection side) {
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);

        switch (side) {
            case DOWN:
                GL11.glRotatef(90, 1, 0, 0);
                break;
            case UP:
                GL11.glRotatef(-90, 1, 0, 0);
                break;
            case NORTH:
                GL11.glRotatef(180, 0, 1, 0);
                break;
            case SOUTH:
                break;
            case WEST:
                GL11.glRotatef(-90, 0, 1, 0);
                break;
            case EAST:
                GL11.glRotatef(90, 0, 1, 0);
                break;
        }

        GL11.glTranslated(-0.5, -0.5, -0.5);
    }

    protected boolean shouldPlaceOnNeighborBlock() {
        TileEntity te = ViewHelper.getWorld()
                .getTileEntity(ViewHelper.getPreviewX(), ViewHelper.getPreviewY(), ViewHelper.getPreviewZ());

        if (!(te instanceof IPartHost partHost)) {
            return !canPlaceBlockAt(
                    ViewHelper.getWorld(),
                    ViewHelper.getPreviewX(),
                    ViewHelper.getPreviewY(),
                    ViewHelper.getPreviewZ());
        }

        return !partHost.canAddPart(ViewHelper.getCachedItemStack(), ViewHelper.getPlacementSide());
    }

    protected boolean canPlace(World world, ForgeDirection side, int x, int y, int z) {
        if (!shouldPlaceOnNeighborBlock()) {
            return true;
        }

        int neighborX = x + side.offsetX;
        int neighborY = y + side.offsetY;
        int neighborZ = z + side.offsetZ;

        TileEntity te = world.getTileEntity(neighborX, neighborY, neighborZ);

        if (!(te instanceof IPartHost partHost)) {
            return canPlaceBlockAt(world, neighborX, neighborY, neighborZ);
        }

        return partHost.canAddPart(ViewHelper.getCachedItemStack(), side.getOpposite());
    }

    protected void renderBase(double minXBase, double minYBase, double minZBase, double maxXBase, double maxYBase,
            double maxZBase) {
        double minX = minXBase / 16.0;
        double minY = minYBase / 16.0;
        double minZ = minZBase / 16.0;
        double maxX = maxXBase / 16.0;
        double maxY = maxYBase / 16.0;
        double maxZ = maxZBase / 16.0;

        addBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    protected void renderCommonPreview(Runnable renderSpecificContent) {
        EntityPlayer player = ViewHelper.getPlayer();
        if (player == null) return;

        setupGlState(player);

        boolean shouldPlaceOnNeighborBlock = shouldPlaceOnNeighborBlock();
        int previewX = ViewHelper.getPreviewX();
        int previewY = ViewHelper.getPreviewY();
        int previewZ = ViewHelper.getPreviewZ();
        ForgeDirection placementSide = ViewHelper.getPlacementSide();

        if (shouldPlaceOnNeighborBlock) {
            int partX = previewX + placementSide.offsetX;
            int partY = previewY + placementSide.offsetY;
            int partZ = previewZ + placementSide.offsetZ;
            applySideRotation(partX, partY, partZ, placementSide.getOpposite());
        } else {
            applySideRotation(previewX, previewY, previewZ, placementSide);
        }

        renderSpecificContent.run();
        renderOutline();

        clearGlState();
    }

    protected void setupGlState(EntityPlayer player) {
        double playerX = player.lastTickPosX
                + (player.posX - player.lastTickPosX) * ViewHelper.getCurrentPartialTicks();
        double playerY = player.lastTickPosY
                + (player.posY - player.lastTickPosY) * ViewHelper.getCurrentPartialTicks();
        double playerZ = player.lastTickPosZ
                + (player.posZ - player.lastTickPosZ) * ViewHelper.getCurrentPartialTicks();

        GL11.glPushMatrix();
        GL11.glTranslated(-playerX, -playerY, -playerZ);

        // the highlight event runs inside the world pass, so restore the GL state it had
        GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                        | GL11.GL_DEPTH_BUFFER_BIT
                        | GL11.GL_VIEWPORT_BIT
                        | GL11.GL_CURRENT_BIT
                        | GL11.GL_LINE_BIT);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        // push lines back so the faces of an existing cable hide edges lying on it
        GL11.glDepthRange(DEPTH_BIAS, 1.0);

        getValidColorGL11();
    }

    protected void clearGlState() {
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
