package com.glodblock.github.client.render;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.glodblock.github.client.render.tank.FluidTankRenderer;
import com.glodblock.github.common.tile.TileCertusQuartzTank;
import com.glodblock.github.util.RenderUtil;
import com.glodblock.github.util.Util;

public class RenderBlockCertusQuartzTank extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityAt(TileEntity tileentity, double x, double y, double z, float f) {
        TileCertusQuartzTank tank = (TileCertusQuartzTank) tileentity;

        FluidStack liquid = tank.tank.getFluid();
        if (liquid == null || liquid.getFluid() == null || liquid.amount <= 0) {
            return;
        }

        int color = liquid.getFluid().getColor(liquid);

        int[] displayList = FluidTankRenderer.getFluidDisplayLists(liquid, tileentity.getWorldObj(), false);
        if (displayList == null) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        bindTexture(TextureMap.locationBlocksTexture);
        RenderUtil.setGLColorFromInt(color);

        GL11.glTranslatef((float) x + 0.0625F, (float) y + 0.5F, (float) z + 0.0625F);
        GL11.glScalef(0.875F, 0.99F, 0.875F);
        GL11.glTranslatef(0, -0.5F, 0);

        int dl = (int) ((float) liquid.amount / (float) (tank.tank.getCapacity())
                * (FluidTankRenderer.DISPLAY_STAGES - 1));
        GL11.glCallList(displayList[Util.clamp(dl, 0, FluidTankRenderer.DISPLAY_STAGES - 1)]);

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
