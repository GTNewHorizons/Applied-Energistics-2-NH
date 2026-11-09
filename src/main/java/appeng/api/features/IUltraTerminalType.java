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
 * Types are registered with {@link IUltraTerminalRegistry} and also generate the crafting recipes. Types with a
 * {@link #getCraftingIngredient() wireless terminal} share the recipes of the wireless terminals, types with only an
 * {@link #getAbsorbingIngredient() absorbing ingredient} get their own recipe. A type either hosts its terminal in the
 * ultra terminal item (see {@link AbstractHostedTerminalType}) or opens the terminal of the item that provides it (see
 * {@link AbstractDelegatedTerminalType}).
 */
public interface IUltraTerminalType {

    /**
     * Stable identifier, used in nbt and in the network protocol. Must be namespaced, e.g.
     * {@code appliedenergistics2:wireless}.
     */
    String getId();

    /**
     * The localized name shown in tooltips and as part of the ultra terminal item name.
     */
    String getDisplayName();

    /**
     * @return the wireless terminal that is combined with another wireless terminal to absorb this type, or null if
     *         this type is not part of the wireless terminal recipes.
     */
    @Nullable
    ItemStack getCraftingIngredient();

    /**
     * @return the stack that has to be merged into an ultra terminal to absorb this type, defaults to the crafting
     *         ingredient. Types without a wireless terminal (for example the crafting terminal) get their own recipe.
     */
    @Nullable
    default ItemStack getAbsorbingIngredient() {
        return this.getCraftingIngredient();
    }

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
        final ItemStack ingredient = this.getCraftingIngredient();
        return ingredient != null && stack != null
                && ingredient.getItem() == stack.getItem()
                && ingredient.getItemDamage() == stack.getItemDamage();
    }

    /**
     * Builds the host object of a hosted terminal, called on both sides when the gui is opened.
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
     * Builds the item inventory host of a hosted terminal that uses the item gui handler. Called on both sides.
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
     * Icon shown on the mode button.
     */
    @SideOnly(Side.CLIENT)
    @Nullable
    ItemStack getIcon();
}
