package appeng.client.api;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.util.CowMap;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class AEKeyRendering {

    private static final CowMap<AEKeyType, AEKeyRenderer<?>> renderers = CowMap.identityHashMap();

    private AEKeyRendering() {}

    public static <T extends AEKey> void register(AEKeyType type, Class<T> keyClass, AEKeyRenderer<T> renderer) {
        if (type.getKeyClass() != keyClass) {
            throw new IllegalArgumentException("Key class " + keyClass.getName() + " does not match type " + type);
        }

        renderers.putIfAbsent(type, renderer);
    }

    @Nullable
    public static AEKeyRenderer<?> get(AEKeyType type) {
        return renderers.getMap().get(type);
    }

    private static AEKeyRenderer<?> getOrThrow(AEKeyType type) {
        AEKeyRenderer<?> renderer = get(type);
        if (renderer == null) {
            throw new IllegalArgumentException("Missing AE key renderer for type " + type);
        }
        return renderer;
    }

    @SuppressWarnings("unchecked")
    private static <T extends AEKey> AEKeyRenderer<T> getOrThrow(T key) {
        // register() guarantees that the renderer of a type accepts that type's key class.
        return (AEKeyRenderer<T>) getOrThrow(key.getType());
    }

    public static void drawInGui(Minecraft minecraft, int x, int y, AEKey key) {
        getOrThrow(key).drawInGui(minecraft, x, y, key);
    }

    public static void drawOnBlockFace(World world, AEKey key) {
        getOrThrow(key).drawOnBlockFace(world, key);
    }

    public static List<String> getTooltip(AEKey key) {
        return getOrThrow(key).getTooltip(key);
    }

    public static ResourceLocation getButtonTexture(AEKeyType type) {
        return getOrThrow(type).getButtonTexture();
    }

    public static IIcon getButtonIcon(AEKeyType type) {
        return getOrThrow(type).getButtonIcon();
    }
}
