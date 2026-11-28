package thaumicenergistics.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import appeng.client.api.AEKeyRenderer;
import appeng.client.render.keytypes.StateButtonIcon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.Thaumcraft;
import thaumicenergistics.client.gui.ThEGuiHelper;
import thaumicenergistics.common.storage.AEEssentiaKey;

@NullMarked
@SideOnly(Side.CLIENT)
public final class EssentiaKeyRenderer implements AEKeyRenderer<AEEssentiaKey> {

    private static final IIcon BUTTON_ICON = new StateButtonIcon(128, 160, "EssentiaIcon");
    private static final ResourceLocation UNKNOWN_TEXTURE = new ResourceLocation(
            "thaumcraft",
            "textures/aspects/_unknown.png");

    @Override
    public void drawInGui(Minecraft minecraft, int x, int y, AEEssentiaKey key) {
        final Aspect aspect = key.getAspect();
        if (hasPlayerDiscovered(minecraft.thePlayer, aspect)) {
            UtilsFX.drawTag(x, y, aspect, 0, 0, 0);
            return;
        }

        // Undiscovered aspects are drawn as Thaumcraft's question mark, tinted with the aspect color.
        minecraft.renderEngine.bindTexture(UNKNOWN_TEXTURE);
        final int color = aspect.getColor();
        GL11.glColor4ub((byte) ((color >> 16) & 0xff), (byte) ((color >> 8) & 0xff), (byte) (color & 0xff), (byte) 255);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        UtilsFX.drawTexturedQuadFull(x, y, 0);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void drawOnBlockFace(World world, AEEssentiaKey key) {
        GL11.glPushMatrix();
        GL11.glTranslatef(0, -0.04F, 0);
        GL11.glScalef(1.0f / 42.0f, 1.0f / 42.0f, 1.0f / 42.0f);
        GL11.glTranslated(-8.0, -10.2, -10.6);
        UtilsFX.drawTag(0, 0, key.getAspect(), 0, 0, 0);
        GL11.glPopMatrix();
    }

    @Override
    public List<String> getTooltip(AEEssentiaKey key) {
        List<String> tooltip = new ArrayList<>();
        if (hasPlayerDiscovered(Minecraft.getMinecraft().thePlayer, key.getAspect())) {
            tooltip.add(ThEGuiHelper.INSTANCE.getAspectChatColor(key.getAspect()) + key.getDisplayName());
        } else {
            tooltip.add(StatCollector.translateToLocal("tc.aspect.unknown"));
        }
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

    private static boolean hasPlayerDiscovered(@Nullable EntityPlayer player, Aspect aspect) {
        return player != null
                && Thaumcraft.proxy.getPlayerKnowledge().hasDiscoveredAspect(player.getCommandSenderName(), aspect);
    }
}
