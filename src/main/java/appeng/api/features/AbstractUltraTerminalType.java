package appeng.api.features;

import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.jspecify.annotations.Nullable;

import appeng.api.AEApi;
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
    private final ItemStack ingredient;
    private final IUltraTerminalGuiOpener opener;

    protected AbstractUltraTerminalType(final String id, final @Nullable String nameKey,
            final @Nullable ItemStack ingredient, final IUltraTerminalGuiOpener opener) {
        this(id, nameKey, null, ingredient, opener);
    }

    protected AbstractUltraTerminalType(final String id, final @Nullable String nameKey, final @Nullable ItemStack icon,
            final @Nullable ItemStack ingredient, final IUltraTerminalGuiOpener opener) {
        this.id = Objects.requireNonNull(id, "id");
        this.nameKey = nameKey;
        this.icon = icon;
        this.ingredient = ingredient;
        this.opener = Objects.requireNonNull(opener, "opener");
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
        final ItemStack fallback = this.icon != null ? this.icon : this.ingredient;
        return fallback != null ? fallback.getDisplayName() : this.id;
    }

    @Nullable
    @Override
    public ItemStack getIngredient() {
        return this.ingredient == null ? null : this.ingredient.copy();
    }

    @SideOnly(Side.CLIENT)
    @Nullable
    @Override
    public ItemStack getIcon() {
        final ItemStack stack = this.icon != null ? this.icon : this.ingredient;
        return stack == null ? null : stack.copy();
    }

    @Override
    public void openGui(final EntityPlayer player, final ItemStack terminal, final int slotIndex) {
        final int index = AEApi.instance().registries().ultraTerminal().getIndex(this);
        this.opener.open(player, terminal, slotIndex, index < 0 ? Integer.MIN_VALUE : index);
    }

    @Override
    public boolean isBackedBy(final @Nullable ItemStack stack) {
        // no copy, this is called while resolving the terminal type of an item stack
        return this.ingredient != null && stack != null
                && this.ingredient.getItem() == stack.getItem()
                && this.ingredient.getItemDamage() == stack.getItemDamage();
    }

    @Override
    public String toString() {
        return "UltraTerminalType[" + this.id + ']';
    }
}
