package appeng.client.render.previewBlocks;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.github.bsideup.jabel.Desugar;

import appeng.api.parts.IPartHost;
import appeng.core.AEConfig;

public abstract class AbstractRendererPreview {

    private static final double DEPTH_BIAS = 0.0005;
    private static final Set<Edge> drawnEdges = new HashSet<>();

    private boolean wasBlend;
    private boolean wasTexture;
    private boolean wasLighting;
    private boolean wasCullFace;
    private boolean wasDepthMask;

    private final int lineWidth = AEConfig.instance.previewLineWidth;

    protected void renderWireframeCube(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        Tessellator tessellator = Tessellator.instance;

        GL11.glLineWidth(lineWidth);
        tessellator.startDrawing(GL11.GL_LINES);

        addEdge(tessellator, minX, minY, minZ, maxX, minY, minZ);
        addEdge(tessellator, maxX, minY, minZ, maxX, minY, maxZ);
        addEdge(tessellator, maxX, minY, maxZ, minX, minY, maxZ);
        addEdge(tessellator, minX, minY, maxZ, minX, minY, minZ);

        addEdge(tessellator, minX, maxY, minZ, maxX, maxY, minZ);
        addEdge(tessellator, maxX, maxY, minZ, maxX, maxY, maxZ);
        addEdge(tessellator, maxX, maxY, maxZ, minX, maxY, maxZ);
        addEdge(tessellator, minX, maxY, maxZ, minX, maxY, minZ);

        addEdge(tessellator, minX, minY, minZ, minX, maxY, minZ);
        addEdge(tessellator, maxX, minY, minZ, maxX, maxY, minZ);
        addEdge(tessellator, maxX, minY, maxZ, maxX, maxY, maxZ);
        addEdge(tessellator, minX, minY, maxZ, minX, maxY, maxZ);

        tessellator.draw();
    }

    /** Adds a line unless this pass already drew it, so edges shared by two boxes do not blend twice. */
    private void addEdge(Tessellator tessellator, double x1, double y1, double z1, double x2, double y2, double z2) {
        final Edge edge = Edge.of(x1, y1, z1, x2, y2, z2);
        if (drawnEdges.add(edge)) {
            tessellator.addVertex(x1, y1, z1);
            tessellator.addVertex(x2, y2, z2);
        }
    }

    @Desugar
    private record Edge(int x1, int y1, int z1, int x2, int y2, int z2) {

        static Edge of(double x1, double y1, double z1, double x2, double y2, double z2) {
            final int ax = key(x1), ay = key(y1), az = key(z1);
            final int bx = key(x2), by = key(y2), bz = key(z2);
            // order the ends so both directions of a line give the same key
            if (ax < bx || ax == bx && (ay < by || ay == by && az <= bz)) {
                return new Edge(ax, ay, az, bx, by, bz);
            }
            return new Edge(bx, by, bz, ax, ay, az);
        }

        private static int key(double value) {
            return (int) Math.round(value * 1024.0);
        }
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

        renderWireframeCube(minX, minY, minZ, maxX, maxY, maxZ);
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

        // the highlight event runs inside the world pass, so put back whatever GL state it finds
        wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        wasTexture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        wasLighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        wasCullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        wasDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        drawnEdges.clear();
        // push lines back, so edges lying on an existing cable are hidden by its faces
        GL11.glDepthRange(DEPTH_BIAS, 1.0);

        getValidColorGL11();
    }

    protected void clearGlState() {
        GL11.glDepthRange(0.0, 1.0);
        GL11.glDepthMask(wasDepthMask);
        setEnabled(GL11.GL_CULL_FACE, wasCullFace);
        setEnabled(GL11.GL_TEXTURE_2D, wasTexture);
        setEnabled(GL11.GL_LIGHTING, wasLighting);
        setEnabled(GL11.GL_BLEND, wasBlend);
        GL11.glPopMatrix();
    }

    private static void setEnabled(int capability, boolean enabled) {
        if (enabled) {
            GL11.glEnable(capability);
        } else {
            GL11.glDisable(capability);
        }
    }
}
