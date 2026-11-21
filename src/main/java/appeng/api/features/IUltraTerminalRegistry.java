package appeng.api.features;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * Registry of the terminal types the ultra terminal can absorb. Registration is closed in post-initialization, before
 * the recipes are generated.
 */
public interface IUltraTerminalRegistry {

    /**
     * Registers a terminal type. Register during pre-initialization, later calls do not reach the generated recipes and
     * keybindings.
     *
     * @throws IllegalArgumentException if the id is already registered
     * @throws IllegalStateException    if the registration is closed
     */
    void register(IUltraTerminalType type);

    /**
     * Registers an alternative id for the given type, used to keep old saves working after a rename.
     */
    void registerAlias(String legacyId, String id);

    /**
     * Closes the registration, called in post-initialization.
     */
    void closeRegistration();

    boolean isRegistrationOpen();

    /**
     * @return all registered types in registration order.
     */
    List<IUltraTerminalType> getTypes();

    /**
     * @return the ingredients of all registered types in registration order, without duplicates.
     */
    List<ItemStack> getIngredients();

    @Nullable
    IUltraTerminalType getById(String id);

    @Nullable
    IUltraTerminalType getByIndex(int index);

    /**
     * @return the index of the given type, or -1 if it is not registered.
     */
    int getIndex(@Nullable IUltraTerminalType type);

    /**
     * @return the first registered type, or null if nothing is registered.
     */
    @Nullable
    IUltraTerminalType getDefault();

    /**
     * @return the type backed by the given stack, or null if no type is backed by it.
     */
    @Nullable
    IUltraTerminalType findType(ItemStack stack);
}
