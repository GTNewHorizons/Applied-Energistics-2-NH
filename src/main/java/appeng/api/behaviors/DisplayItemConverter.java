package appeng.api.behaviors;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import appeng.api.stacks.GenericStack;

/**
 * Converts items that only represent a resource, such as GT fluid display items or TC4 aspect items, into the resource
 * they show. Unlike {@link ContainerItemStrategy}, these items do not hold anything. Register implementations with
 * {@link DisplayItemConverters#register}.
 */
@FunctionalInterface
public interface DisplayItemConverter {

    /**
     * Must not mutate {@code stack}. Container items never reach converters; they are left to
     * {@link ContainerItemStrategy}.
     *
     * @return the resource and amount shown by {@code stack}, or null if this converter does not handle it.
     */
    @Nullable
    GenericStack convert(ItemStack stack);
}
