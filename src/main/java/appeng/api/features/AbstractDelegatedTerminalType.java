package appeng.api.features;

import java.util.Objects;
import java.util.function.BiPredicate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * A terminal type of another item: the ultra terminal only forwards the open request and the player has to carry the
 * item that provides the terminal.
 */
public abstract class AbstractDelegatedTerminalType extends AbstractUltraTerminalType {

    private final IUltraTerminalGuiOpener opener;
    private final BiPredicate<EntityPlayer, ItemStack> availability;

    protected AbstractDelegatedTerminalType(final String id, final @Nullable String nameKey,
            final @Nullable ItemStack icon, final @Nullable ItemStack craftingIngredient,
            final IUltraTerminalGuiOpener opener, final BiPredicate<EntityPlayer, ItemStack> availability) {
        super(id, nameKey, icon, craftingIngredient);
        this.opener = Objects.requireNonNull(opener, "opener");
        this.availability = Objects.requireNonNull(availability, "availability");
    }

    /**
     * Creates a delegated type that is available while the player carries an item of the given class.
     */
    protected AbstractDelegatedTerminalType(final String id, final @Nullable String nameKey,
            final @Nullable ItemStack icon, final @Nullable ItemStack craftingIngredient,
            final IUltraTerminalGuiOpener opener, final Class<? extends Item> carriedItem) {
        this(id, nameKey, icon, craftingIngredient, opener, (player, terminal) -> carries(player, carriedItem));
    }

    @Override
    public boolean isAvailable(final EntityPlayer player, final ItemStack ultraTerminal) {
        return player != null && this.availability.test(player, ultraTerminal);
    }

    @Override
    public void openGui(final EntityPlayer player, final ItemStack terminal, final int slotIndex,
            final int previousIndex) {
        this.opener.open(player, terminal, slotIndex);
    }

    public static boolean carries(final EntityPlayer player, final Class<? extends Item> itemClass) {
        if (player == null || player.inventory == null) {
            return false;
        }
        for (final ItemStack stack : player.inventory.mainInventory) {
            if (stack != null && itemClass.isInstance(stack.getItem())) {
                return true;
            }
        }
        return false;
    }
}
