package appeng.client.render.previewBlocks;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import appeng.api.parts.IPartHost;
import appeng.core.AEConfig;

public abstract class AbstractRendererPreview {

    private static final double DEPTH_BIAS = 0.0005;

    private final int lineWidth = AEConfig.instance.previewLineWidth;

    protected void renderWireframeCube(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ, null);
    }

    /** Draws the 12 edges of a box, leaving out the four edges of {@code skipFace} (null skips none). */
    protected void renderWireframeCube(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            ForgeDirection skipFace) {
        Tessellator tessellator = Tessellator.instance;

        GL11.glLineWidth(lineWidth);
        tessellator.startDrawing(GL11.GL_LINES);

        // an edge drawn by two boxes blends twice, so the box that touches another one skips the shared face
        addEdge(tessellator, skipFace, ForgeDirection.DOWN, ForgeDirection.NORTH, minX, minY, minZ, maxX, minY, minZ);
        addEdge(tessellator, skipFace, ForgeDirection.DOWN, ForgeDirection.EAST, maxX, minY, minZ, maxX, minY, maxZ);
        addEdge(tessellator, skipFace, ForgeDirection.DOWN, ForgeDirection.SOUTH, maxX, minY, maxZ, minX, minY, maxZ);
        addEdge(tessellator, skipFace, ForgeDirection.DOWN, ForgeDirection.WEST, minX, minY, maxZ, minX, minY, minZ);

        addEdge(tessellator, skipFace, ForgeDirection.UP, ForgeDirection.NORTH, minX, maxY, minZ, maxX, maxY, minZ);
        addEdge(tessellator, skipFace, ForgeDirection.UP, ForgeDirection.EAST, maxX, maxY, minZ, maxX, maxY, maxZ);
        addEdge(tessellator, skipFace, ForgeDirection.UP, ForgeDirection.SOUTH, maxX, maxY, maxZ, minX, maxY, maxZ);
        addEdge(tessellator, skipFace, ForgeDirection.UP, ForgeDirection.WEST, minX, maxY, maxZ, minX, maxY, minZ);

        addEdge(tessellator, skipFace, ForgeDirection.WEST, ForgeDirection.NORTH, minX, minY, minZ, minX, maxY, minZ);
        addEdge(tessellator, skipFace, ForgeDirection.EAST, ForgeDirection.NORTH, maxX, minY, minZ, maxX, maxY, minZ);
        addEdge(tessellator, skipFace, ForgeDirection.EAST, ForgeDirection.SOUTH, maxX, minY, maxZ, maxX, maxY, maxZ);
        addEdge(tessellator, skipFace, ForgeDirection.WEST, ForgeDirection.SOUTH, minX, minY, maxZ, minX, maxY, maxZ);

        tessellator.draw();
    }

    /** Adds a line to the batch unless it lies on the skipped face. */
    private void addEdge(Tessellator tessellator, ForgeDirection skipFace, ForgeDirection faceA, ForgeDirection faceB,
            double x1, double y1, double z1, double x2, double y2, double z2) {
        if (skipFace == faceA || skipFace == faceB) return;
        tessellator.addVertex(x1, y1, z1);
        tessellator.addVertex(x2, y2, z2);
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
        renderBase(minXBase, minYBase, minZBase, maxXBase, maxYBase, maxZBase, null);
    }

    protected void renderBase(double minXBase, double minYBase, double minZBase, double maxXBase, double maxYBase,
            double maxZBase, ForgeDirection skipFace) {
        double minX = minXBase / 16.0;
        double minY = minYBase / 16.0;
        double minZ = minZBase / 16.0;
        double maxX = maxXBase / 16.0;
        double maxY = maxYBase / 16.0;
        double maxZ = maxZBase / 16.0;

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ, skipFace);
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
