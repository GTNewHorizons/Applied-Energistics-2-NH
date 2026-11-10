package com.glodblock.github.loader.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.common.item.ItemWirelessUltraTerminal;

/**
 * Combines the terminals of the crafting grid into one ultra terminal, the result contains exactly the terminals that
 * were combined.
 */
public class UltraTerminalRecipe extends ShapelessOreRecipe {

    private final boolean keepUltraTerminalData;

    private UltraTerminalRecipe(final ItemStack result, final boolean keepUltraTerminalData, final ItemStack first,
            final ItemStack second) {
        super(result, first, second);
        this.keepUltraTerminalData = keepUltraTerminalData;
    }

    /**
     * Two wireless terminals make an ultra terminal that contains both of them.
     */
    public static UltraTerminalRecipe combine(final ItemStack result, final ItemStack first, final ItemStack second) {
        return new UltraTerminalRecipe(result, false, first, second);
    }

    /**
     * A terminal is merged into the ultra terminal of the grid, which keeps its terminals.
     */
    public static UltraTerminalRecipe merge(final ItemStack result, final ItemStack ultraTerminal,
            final ItemStack terminal) {
        return new UltraTerminalRecipe(result, true, ultraTerminal, terminal);
    }

    @Override
    public ItemStack getCraftingResult(final InventoryCrafting inventory) {
        ItemStack result = this.keepUltraTerminalData ? copyUltraTerminal(inventory) : null;
        if (result == null) {
            result = super.getCraftingResult(inventory);
        }
        if (result != null) {
            ItemWirelessUltraTerminal.absorb(result, this.keepUltraTerminalData, copyTerminals(inventory));
        }
        return result;
    }

    @Nullable
    private static ItemStack copyUltraTerminal(final InventoryCrafting inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            final ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemWirelessUltraTerminal) {
                final ItemStack copy = stack.copy();
                copy.stackSize = 1;
                return copy;
            }
        }
        return null;
    }

    private static ItemStack[] copyTerminals(final InventoryCrafting inventory) {
        final List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            final ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null) {
                stacks.add(stack);
            }
        }
        return stacks.toArray(new ItemStack[0]);
    }
}
