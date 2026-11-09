package appeng.client.api;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import appeng.api.stacks.AEKey;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-side rendering of one {@link appeng.api.stacks.AEKeyType}. Register implementations with
 * {@link AEKeyRendering#register}.
 */
@SideOnly(Side.CLIENT)
public interface AEKeyRenderer<T extends AEKey> {

    /**
     * Draws the visual representation of a key, such as an item stack or fluid sprite, without an amount overlay.
     */
    void drawInGui(Minecraft minecraft, int x, int y, T key);

    /**
     * Draws the visual representation of a key in-world on the face of a block.
     */
    void drawOnBlockFace(World world, T key);

    /**
     * Returns the tooltip for the key. The returned list may be mutated by the caller.
     */
    List<String> getTooltip(T key);

    /**
     * Gets the texture resource location for the button representing this key type.
     */
    ResourceLocation getButtonTexture();

    /**
     * Gets the icon for the button representing this key type.
     */
    IIcon getButtonIcon();
}
