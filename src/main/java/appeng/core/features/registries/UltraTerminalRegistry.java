package appeng.core.features.registries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import appeng.api.features.IUltraTerminalRegistry;
import appeng.api.features.IUltraTerminalType;
import appeng.core.AELog;

/**
 * Default implementation of {@link IUltraTerminalRegistry}.
 */
public final class UltraTerminalRegistry implements IUltraTerminalRegistry {

    private final List<IUltraTerminalType> types = new ArrayList<>();
    private final List<IUltraTerminalType> readOnlyTypes = Collections.unmodifiableList(this.types);
    private final Map<String, IUltraTerminalType> byId = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();
    private boolean registrationOpen = true;

    @Override
    public void register(final IUltraTerminalType type) {
        if (type == null) {
            throw new IllegalArgumentException("Cannot register a null ultra terminal type");
        }
        final String id = type.getId();
        if (!this.registrationOpen) {
            throw new IllegalStateException(
                    "Ultra terminal registration is closed, cannot register " + id
                            + " (register terminals during pre-initialization)");
        }
        if (this.byId.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate ultra terminal type id: " + id);
        }
        this.byId.put(id, type);
        this.types.add(type);
    }

    @Override
    public void registerAlias(final String legacyId, final String id) {
        if (legacyId == null || id == null) {
            throw new IllegalArgumentException("Alias ids must not be null");
        }
        if (this.byId.containsKey(legacyId) || this.aliases.containsKey(legacyId)) {
            AELog.warn("[UltraTerminal] Ignoring duplicate alias %s -> %s", legacyId, id);
            return;
        }
        this.aliases.put(legacyId, id);
    }

    @Override
    public void closeRegistration() {
        this.registrationOpen = false;
    }

    @Override
    public boolean isRegistrationOpen() {
        return this.registrationOpen;
    }

    @Override
    public List<IUltraTerminalType> getTypes() {
        return this.readOnlyTypes;
    }

    @Override
    public List<ItemStack> getIngredients() {
        // keyed by item id + damage so that two types sharing an item do not produce duplicated recipes
        final Map<String, ItemStack> ingredients = new LinkedHashMap<>();
        for (final IUltraTerminalType type : this.types) {
            final ItemStack ingredient = type.getIngredient();
            if (ingredient == null) {
                continue;
            }
            final String key = Item.getIdFromItem(ingredient.getItem()) + "@" + ingredient.getItemDamage();
            ingredients.putIfAbsent(key, ingredient);
        }
        return Collections.unmodifiableList(new ArrayList<>(ingredients.values()));
    }

    @Nullable
    @Override
    public IUltraTerminalType getById(final String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        final IUltraTerminalType type = this.byId.get(id);
        if (type != null) {
            return type;
        }
        final String aliased = this.aliases.get(id);
        return aliased == null ? null : this.byId.get(aliased);
    }

    @Nullable
    @Override
    public IUltraTerminalType getByIndex(final int index) {
        return index < 0 || index >= this.types.size() ? null : this.types.get(index);
    }

    @Override
    public int getIndex(final @Nullable IUltraTerminalType type) {
        return type == null ? -1 : this.types.indexOf(type);
    }

    @Nullable
    @Override
    public IUltraTerminalType getDefault() {
        return this.types.isEmpty() ? null : this.types.get(0);
    }

    @Nullable
    @Override
    public IUltraTerminalType findType(final ItemStack stack) {
        if (stack == null) {
            return null;
        }
        for (final IUltraTerminalType type : this.types) {
            if (type.isBackedBy(stack)) {
                return type;
            }
        }
        return null;
    }
}
