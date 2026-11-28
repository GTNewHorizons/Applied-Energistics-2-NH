package appeng.api.behaviors;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.util.CowMap;
import appeng.util.Platform;

/**
 * Manages {@link ContainerItemStrategy} registrations for {@linkplain AEKeyType key types}.
 */
public final class ContainerItemStrategies {

    /** Tried in registration order, so that the strategy that claims a container does not change between runs. */
    private static final CowMap<AEKeyType, ContainerItemStrategy<AEKey>> strategies = new CowMap<>(LinkedHashMap::new);

    static {
        register(AEKeyType.fluids(), AEFluidKey.class, new FluidContainerItemStrategy());
    }

    private ContainerItemStrategies() {}

    @SuppressWarnings("unchecked")
    public static <T extends AEKey> void register(AEKeyType type, Class<T> keyClass,
            ContainerItemStrategy<T> strategy) {
        if (type.getKeyClass() != keyClass) {
            throw new IllegalArgumentException("Key class " + keyClass.getName() + " does not match type " + type);
        }

        // The key class check above guarantees that the strategy of a type accepts that type's keys.
        strategies.putIfAbsent(type, (ContainerItemStrategy<AEKey>) strategy);
    }

    public static Set<AEKeyType> getSupportedKeyTypes() {
        return strategies.getMap().keySet();
    }

    public static boolean isContainer(@Nullable ItemStack stack) {
        return isContainer(stack, type -> true);
    }

    public static boolean isContainer(@Nullable ItemStack stack, Predicate<? super AEKeyType> types) {
        if (stack == null) {
            return false;
        }
        for (Map.Entry<AEKeyType, ContainerItemStrategy<AEKey>> entry : strategies.getMap().entrySet()) {
            if (types.test(entry.getKey()) && entry.getValue().isContainer(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isContainer(@Nullable ItemStack stack, AEKeyType type) {
        return stack != null && findStrategy(stack, type) != null;
    }

    /**
     * Tries to get the content contained in the given item - if any. Allows inspecting the content of buckets, fluid
     * tanks and other containers. The amount is that of a single container.
     */
    public static @Nullable GenericStack getContainedStack(ItemStack container) {
        return getContainedStack(container, key -> true);
    }

    /**
     * @return the content of a single {@code container} if it is accepted by {@code filter}, otherwise null.
     */
    public static @Nullable GenericStack getContainedStack(ItemStack container, Predicate<? super AEKey> filter) {
        final ItemStack one = toSingle(container);
        for (ContainerItemStrategy<AEKey> strategy : strategies.getMap().values()) {
            if (!strategy.isContainer(one)) {
                continue;
            }
            GenericStack contained = strategy.getContainedStack(one);
            if (contained != null && filter.test(contained.getKey())) {
                return contained;
            }
        }
        return null;
    }

    /**
     * {@link ContainerItemStrategy#fill} on a single {@code container} with the strategy of the type of {@code key}.
     */
    public static ContainerItemResult fill(ItemStack container, AEKey key, long amount) {
        final ItemStack one = toSingle(container);
        ContainerItemStrategy<AEKey> strategy = findStrategy(one, key.getType());
        return strategy == null ? ContainerItemResult.none() : strategy.fill(one, key, amount);
    }

    /**
     * {@link ContainerItemStrategy#drain} on a single {@code container} with the strategy of the type of {@code key}.
     */
    public static ContainerItemResult drain(ItemStack container, AEKey key, long amount) {
        final ItemStack one = toSingle(container);
        ContainerItemStrategy<AEKey> strategy = findStrategy(one, key.getType());
        return strategy == null ? ContainerItemResult.none() : strategy.drain(one, key, amount);
    }

    private static ItemStack toSingle(ItemStack container) {
        return container.stackSize == 1 ? container : Platform.copyStackWithSizeOne(container);
    }

    private static @Nullable ContainerItemStrategy<AEKey> findStrategy(ItemStack stack, AEKeyType type) {
        ContainerItemStrategy<AEKey> strategy = strategies.getMap().get(type);
        return strategy != null && strategy.isContainer(stack) ? strategy : null;
    }
}
