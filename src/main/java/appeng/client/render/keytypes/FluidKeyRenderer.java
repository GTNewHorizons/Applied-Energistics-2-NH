package appeng.client.render.keytypes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

import org.lwjgl.opengl.GL11;

import appeng.api.stacks.AEFluidKey;
import appeng.client.api.AEKeyRenderer;
import appeng.client.texture.TextureUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class FluidKeyRenderer implements AEKeyRenderer<AEFluidKey> {

    private static final IIcon BUTTON_ICON = new StateButtonIcon(128, 48, "FluidIcon");

    @Override
    public void drawInGui(Minecraft minecraft, int x, int y, AEFluidKey key) {
        IIcon icon = getIcon(key.getFluid());
        int color = key.getFluid().getColor();

        minecraft.renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 0xFF);
        tessellator.addVertexWithUV(x + 16, y, 0, icon.getMaxU(), icon.getMinV());
        tessellator.addVertexWithUV(x, y, 0, icon.getMinU(), icon.getMinV());
        tessellator.addVertexWithUV(x, y + 16, 0, icon.getMinU(), icon.getMaxV());
        tessellator.addVertexWithUV(x + 16, y + 16, 0, icon.getMaxU(), icon.getMaxV());
        tessellator.draw();

        GL11.glPopAttrib();
    }

    @Override
    public void drawOnBlockFace(World world, AEFluidKey key) {
        GL11.glPushMatrix();

        GL11.glTranslatef(0, -0.04F, 0);
        GL11.glScalef(1.0f / 42.0f, 1.0f / 42.0f, 1.0f / 42.0f);
        GL11.glTranslated(-8.0, -10.2, -10.4);
        GL11.glScalef(1.0f, 1.0f, 0.005f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);

        final int color = key.getFluid().getColor();
        final IIcon icon = getIcon(key.getFluid());

        final Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 0xFF);
        tessellator.addVertexWithUV(16, 0, 0, icon.getMaxU(), icon.getMinV());
        tessellator.addVertexWithUV(0, 0, 0, icon.getMinU(), icon.getMinV());
        tessellator.addVertexWithUV(0, 16, 0, icon.getMinU(), icon.getMaxV());
        tessellator.addVertexWithUV(16, 16, 0, icon.getMaxU(), icon.getMaxV());
        tessellator.draw();

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glPopMatrix();
    }

    @Override
    public List<String> getTooltip(AEFluidKey key) {
        List<String> tooltip = new ArrayList<>();
        tooltip.add(key.getDisplayName());
        return tooltip;
    }

    @Override
    public ResourceLocation getButtonTexture() {
        return StateButtonIcon.texture();
    }

    @Override
    public IIcon getButtonIcon() {
        return BUTTON_ICON;
    }

    private static IIcon getIcon(Fluid fluid) {
        IIcon fluidIcon = fluid.getIcon();
        if (fluidIcon != null) {
            return fluidIcon;
        }

        if (fluid.getBlock() != null) {
            IIcon blockIcon = fluid.getBlock().getIcon(0, 0);
            if (blockIcon != null) {
                return blockIcon;
            }
        }

        return TextureUtils.getMissingBlock();
    }
}
