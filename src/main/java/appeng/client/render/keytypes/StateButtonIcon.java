package appeng.client.render.keytypes;

import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import appeng.client.texture.ExtraBlockTextures;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A 16x16 icon on the 256x256 {@code guis/states.png} sheet, used for key type buttons.
 */
@SideOnly(Side.CLIENT)
public final class StateButtonIcon implements IIcon {

    private static final float SHEET_SIZE = 256;
    private static final int ICON_SIZE = 16;

    private final int u;
    private final int v;
    private final String name;

    public StateButtonIcon(int u, int v, String name) {
        this.u = u;
        this.v = v;
        this.name = name;
    }

    public static ResourceLocation texture() {
        return ExtraBlockTextures.GuiTexture("guis/states.png");
    }

    @Override
    public int getIconWidth() {
        return ICON_SIZE;
    }

    @Override
    public int getIconHeight() {
        return ICON_SIZE;
    }

    @Override
    public float getMinU() {
        return this.u / SHEET_SIZE;
    }

    @Override
    public float getMaxU() {
        return (this.u + ICON_SIZE) / SHEET_SIZE;
    }

    @Override
    public float getInterpolatedU(double offset) {
        return (float) ((this.u + offset) / SHEET_SIZE);
    }

    @Override
    public float getMinV() {
        return this.v / SHEET_SIZE;
    }

    @Override
    public float getMaxV() {
        return (this.v + ICON_SIZE) / SHEET_SIZE;
    }

    @Override
    public float getInterpolatedV(double offset) {
        return (float) ((this.v + offset) / SHEET_SIZE);
    }

    @Override
    public String getIconName() {
        return this.name;
    }
}
