package appeng.api.features;

import java.util.Objects;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * A terminal type hosted by the ultra terminal item itself, the items of the type are only used as ingredients and
 * icon.
 */
public abstract class AbstractHostedTerminalType extends AbstractUltraTerminalType {

    private final IUltraTerminalGuiOpener opener;

    protected AbstractHostedTerminalType(final String id, final @Nullable String nameKey,
            final @Nullable ItemStack icon, final @Nullable ItemStack craftingIngredient,
            final IUltraTerminalGuiOpener opener) {
        super(id, nameKey, icon, craftingIngredient);
        this.opener = Objects.requireNonNull(opener, "opener");
    }

    protected AbstractHostedTerminalType(final String id, final @Nullable String nameKey,
            final @Nullable ItemStack icon, final @Nullable ItemStack craftingIngredient,
            final @Nullable ItemStack absorbingIngredient, final IUltraTerminalGuiOpener opener) {
        super(id, nameKey, icon, craftingIngredient, absorbingIngredient);
        this.opener = Objects.requireNonNull(opener, "opener");
    }

    @Override
    public void openGui(final EntityPlayer player, final ItemStack terminal, final int slotIndex,
            final int previousIndex) {
        this.opener.open(player, terminal, slotIndex);
    }
}
