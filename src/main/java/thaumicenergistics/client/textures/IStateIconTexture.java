package thaumicenergistics.client.textures;

import net.minecraft.util.ResourceLocation;

import org.jspecify.annotations.NonNull;

/**
 * Icon from a state texture.
 *
 * @author Nividica
 *
 */
public interface IStateIconTexture {

    /**
     * Height of the icon.
     *
     * @return
     */
    int getHeight();

    /**
     * Texture the icon is in.
     *
     * @return
     */
    @NonNull
    ResourceLocation getTexture();

    /**
     * U coordinate of the icon.
     *
     * @return
     */
    int getU();

    /**
     * V coordinate of the icon.
     *
     * @return
     */
    int getV();

    /**
     * Width of the icon.
     *
     * @return
     */
    int getWidth();
}
