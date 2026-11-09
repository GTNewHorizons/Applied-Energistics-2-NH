package appeng.api.features;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.jspecify.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Convenience base implementation for {@link IUltraTerminalType}.
 */
public abstract class AbstractUltraTerminalType implements IUltraTerminalType {

    private final String id;
    @Nullable
    private final String nameKey;
    @Nullable
    private final ItemStack icon;
    @Nullable
    private final ItemStack craftingIngredient;
    @Nullable
    private final ItemStack absorbingIngredient;

    protected AbstractUltraTerminalType(final String id, final @Nullable String nameKey, final @Nullable ItemStack icon,
            final @Nullable ItemStack craftingIngredient) {
        this(id, nameKey, icon, craftingIngredient, craftingIngredient);
    }

    protected AbstractUltraTerminalType(final String id, final @Nullable String nameKey, final @Nullable ItemStack icon,
            final @Nullable ItemStack craftingIngredient, final @Nullable ItemStack absorbingIngredient) {
        this.id = Objects.requireNonNull(id, "id");
        this.nameKey = nameKey;
        this.icon = icon;
        this.craftingIngredient = craftingIngredient;
        this.absorbingIngredient = absorbingIngredient;
    }

    @Override
    public final String getId() {
        return this.id;
    }

    @Override
    public String getDisplayName() {
        if (this.nameKey != null) {
            final String localized = StatCollector.translateToLocal(this.nameKey);
            if (localized != null && !localized.isEmpty() && !this.nameKey.equals(localized)) {
                return localized;
            }
        }
        final ItemStack iconStack = this.icon;
        if (iconStack != null) {
            return iconStack.getDisplayName();
        }
        return this.id;
    }

    @Nullable
    @Override
    public ItemStack getCraftingIngredient() {
        return this.craftingIngredient == null ? null : this.craftingIngredient.copy();
    }

    @Nullable
    @Override
    public ItemStack getAbsorbingIngredient() {
        return this.absorbingIngredient == null ? null : this.absorbingIngredient.copy();
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public ItemStack getIcon() {
        return this.icon == null ? null : this.icon.copy();
    }

    @Override
    public boolean isBackedBy(final @Nullable ItemStack stack) {
        // no copy, this is called while resolving the terminal type of an item stack
        return matches(this.craftingIngredient, stack) || matches(this.absorbingIngredient, stack);
    }

    private static boolean matches(final @Nullable ItemStack ingredient, final @Nullable ItemStack stack) {
        return ingredient != null && stack != null
                && ingredient.getItem() == stack.getItem()
                && ingredient.getItemDamage() == stack.getItemDamage();
    }

    @Override
    public String toString() {
        return "UltraTerminalType[" + this.id + ']';
    }
}
