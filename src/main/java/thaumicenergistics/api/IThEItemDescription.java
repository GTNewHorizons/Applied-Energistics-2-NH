package thaumicenergistics.api;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Item information.
 *
 * @author Nividica
 *
 */
public interface IThEItemDescription {

    /**
     * Gets the block of this item, if it has one.
     *
     * @return
     */
    @Nullable
    Block getBlock();

    /**
     * Gets the damage, or meta, value of the item.
     *
     * @return
     */
    int getDamage();

    /**
     * Gets the item.
     *
     * @return
     */
    @NonNull
    Item getItem();

    /**
     * Gets a stack of size 1.
     *
     * @return
     */
    @NonNull
    ItemStack getStack();

    /**
     * Gets a stack of the specified size.
     *
     * @param amount
     * @return
     */
    @NonNull
    ItemStack getStacks(int amount);
}
