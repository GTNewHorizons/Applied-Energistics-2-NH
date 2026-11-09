package appeng.api.behaviors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import appeng.api.stacks.GenericStack;
import appeng.util.Platform;

public final class DisplayItemConverters {

    private static volatile List<DisplayItemConverter> converters = Collections.emptyList();

    static {
        if (Platform.isNEILoaded) {
            register(new FluidDisplayItemConverter());
        }
        register(new FluidPacketDisplayItemConverter());
    }

    private DisplayItemConverters() {}

    /**
     * Converters are tried in registration order, and the first one that handles a stack wins.
     */
    public static synchronized void register(DisplayItemConverter converter) {
        Objects.requireNonNull(converter, "converter");
        List<DisplayItemConverter> copy = new ArrayList<>(converters);
        copy.add(converter);
        converters = Collections.unmodifiableList(copy);
    }

    /**
     * @return the resource shown by {@code stack}, or null if no converter handles it.
     */
    public static @Nullable GenericStack convert(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null || ContainerItemStrategies.isContainer(stack)) {
            return null;
        }
        for (DisplayItemConverter converter : converters) {
            GenericStack result = converter.convert(stack);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
