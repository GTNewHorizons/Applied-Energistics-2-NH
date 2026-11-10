package appeng.api.features;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;

import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.networking.IGridNode;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A terminal type that can be absorbed by the ultra terminal.
 * <p>
 * Types are registered with {@link IUltraTerminalRegistry} and also generate the recipes: the ingredient of a type is
 * merged into an ultra terminal by its own recipe, and a type whose ingredient is a wireless terminal also takes part
 * in the recipe that combines any two wireless terminals. Use {@link AbstractUltraTerminalType} for the default
 * implementation.
 */
public interface IUltraTerminalType {

    /**
     * Stable identifier, used in nbt and in the network protocol. Must be namespaced, e.g.
     * {@code appliedenergistics2:wireless}.
     */
    String getId();

    String getDisplayName();

    /**
     * @return the stack that adds this type to an ultra terminal, or null if this type is part of every ultra terminal.
     */
    @Nullable
    ItemStack getIngredient();

    /**
     * @return true if this type can be selected and opened right now. Called on both sides, must not load client only
     *         classes.
     */
    default boolean isAvailable(final EntityPlayer player, final ItemStack ultraTerminal) {
        return true;
    }

    /**
     * @return true if the given stack represents this terminal type, used to absorb terminals and to resolve the
     *         terminal of a standalone wireless terminal item.
     */
    default boolean isBackedBy(final @Nullable ItemStack stack) {
        final ItemStack ingredient = this.getIngredient();
        return ingredient != null && stack != null
                && ingredient.getItem() == stack.getItem()
                && ingredient.getItemDamage() == stack.getItemDamage();
    }

    /**
     * Builds the host object of a terminal that is opened through an AE gui bridge, called on both sides when the gui
     * is opened. A terminal needs either this or {@link #createItemInventory}.
     * <p>
     * {@code x} is the inventory slot of the terminal item, {@code y} the index of this type in the registry or
     * {@link Integer#MIN_VALUE} if no terminal was selected, {@code z} is reserved.
     *
     * @return the gui host, or null if this type is not hosted by the ultra terminal item
     */
    @Nullable
    default IGuiItemObject createGuiObject(final IWirelessTermHandler handler, final ItemStack terminal,
            final EntityPlayer player, final World world, final int x, final int y, final int z) {
        return null;
    }

    /**
     * Builds the item inventory host of a terminal that is opened through the item gui handler, called on both sides. A
     * terminal needs either this or {@link #createGuiObject}.
     *
     * @return the inventory host, or null if this type does not use an item inventory.
     */
    @Nullable
    default Object createItemInventory(final ItemStack terminal, final int slotIndex, final IGridNode node,
            final EntityPlayer player) {
        return null;
    }

    /**
     * Opens the gui of this terminal. Called on the server side only.
     *
     * @param previousIndex the index of the mode that was opened before, {@link Integer#MIN_VALUE} if unknown.
     */
    void openGui(EntityPlayer player, ItemStack terminal, int slotIndex, int previousIndex);

    /**
     * @return the stack shown for this terminal in the guis and the tooltips, the ingredient by default.
     */
    @SideOnly(Side.CLIENT)
    @Nullable
    default ItemStack getIcon() {
        return this.getIngredient();
    }
}
