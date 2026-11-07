package net.p455w0rd.wirelesscraftingterminal.api;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jspecify.annotations.NonNull;

public interface IWCTItemDescription {

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
