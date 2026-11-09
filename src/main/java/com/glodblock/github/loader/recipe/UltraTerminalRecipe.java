package com.glodblock.github.loader.recipe;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.common.item.ItemWirelessUltraTerminal;

/**
 * Recipe that combines terminals into the ultra terminal. The wireless terminals share one recipe, terminals without a
 * wireless terminal are absorbed by their own recipe.
 */
public class UltraTerminalRecipe extends ShapelessOreRecipe {

    private final boolean requireDifferentTerminals;
    private final boolean keepUltraTerminalData;
    @Nullable
    private final String terminalOredict;
    @Nullable
    private final ItemStack singleIngredient;

    private UltraTerminalRecipe(final ItemStack result, final boolean requireDifferentTerminals,
            final boolean keepUltraTerminalData, final @Nullable String terminalOredict,
            final @Nullable ItemStack singleIngredient, final Object... inputs) {
        super(result, inputs);
        this.requireDifferentTerminals = requireDifferentTerminals;
        this.keepUltraTerminalData = keepUltraTerminalData;
        this.terminalOredict = terminalOredict;
        this.singleIngredient = singleIngredient;
    }

    /**
     * Any two different wireless terminals are combined into an ultra terminal, the result contains both of them.
     */
    public static UltraTerminalRecipe combine(final ItemStack result, final String terminalOredict) {
        final UltraTerminalRecipe recipe = new UltraTerminalRecipe(
                result,
                true,
                false,
                terminalOredict,
                null,
                terminalOredict,
                terminalOredict);
        recipe.splitTerminalLists();
        return recipe;
    }

    /**
     * An ultra terminal and any wireless terminal are merged, the result keeps the data of the input terminal.
     */
    public static UltraTerminalRecipe merge(final ItemStack result, final ItemStack ultraTerminal,
            final String terminalOredict) {
        final UltraTerminalRecipe recipe = new UltraTerminalRecipe(
                result,
                false,
                true,
                terminalOredict,
                null,
                ultraTerminal,
                terminalOredict);
        recipe.copyTerminalList(1);
        return recipe;
    }

    /**
     * An ultra terminal and the item that provides a single terminal, for example the crafting terminal.
     */
    public static UltraTerminalRecipe absorb(final ItemStack result, final ItemStack ultraTerminal,
            final ItemStack terminal) {
        return new UltraTerminalRecipe(result, false, true, null, terminal, ultraTerminal, terminal);
    }

    /**
     * Splits the terminals over both slots, so nei never shows the same terminal twice, no matter which entry of a slot
     * is displayed. Every slot accepts every terminal, the split only affects the display.
     */
    private void splitTerminalLists() {
        final List<ItemStack> terminals = terminalIngredients();
        final ArrayList<ItemStack> first = new ArrayList<>();
        final ArrayList<ItemStack> second = new ArrayList<>();
        for (int i = 0; i < terminals.size(); i++) {
            (i % 2 == 0 ? first : second).add(terminals.get(i));
        }
        final ArrayList<Object> input = this.getInput();
        if (first.isEmpty() || second.isEmpty() || input.size() < 2) {
            return;
        }
        input.set(0, first);
        input.set(1, second);
    }

    /**
     * Gives the ore dictionary ingredient of the slot its own copy, so it does not share the list of the ore
     * dictionary.
     */
    private void copyTerminalList(final int slot) {
        final ArrayList<Object> input = this.getInput();
        final List<ItemStack> terminals = terminalIngredients();
        if (slot < input.size() && !terminals.isEmpty()) {
            input.set(slot, new ArrayList<>(terminals));
        }
    }

    /**
     * @return copies of the terminals in the ore dictionary, without duplicates.
     */
    private List<ItemStack> terminalIngredients() {
        final List<ItemStack> terminals = new ArrayList<>();
        final Set<String> seen = new LinkedHashSet<>();
        for (final ItemStack terminal : OreDictionary.getOres(this.terminalOredict)) {
            if (terminal == null) {
                continue;
            }
            final String key = Item.getIdFromItem(terminal.getItem()) + "@" + terminal.getItemDamage();
            if (seen.add(key)) {
                terminals.add(terminal.copy());
            }
        }
        return terminals;
    }

    private boolean isTerminal(final ItemStack stack) {
        for (final ItemStack terminal : OreDictionary.getOres(this.terminalOredict)) {
            if (terminal != null && terminal.getItem() == stack.getItem()
                    && terminal.getItemDamage() == stack.getItemDamage()) {
                return true;
            }
        }
        return false;
    }

    private boolean isIngredient(final ItemStack stack) {
        if (this.singleIngredient != null) {
            return this.singleIngredient.getItem() == stack.getItem()
                    && this.singleIngredient.getItemDamage() == stack.getItemDamage();
        }
        return isTerminal(stack);
    }

    @Override
    public boolean matches(final InventoryCrafting inventory, final World world) {
        ItemStack first = null;
        ItemStack second = null;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            final ItemStack stack = inventory.getStackInSlot(i);
            if (stack == null) {
                continue;
            }
            if (first == null) {
                first = stack;
            } else if (second == null) {
                second = stack;
            } else {
                return false;
            }
        }
        if (first == null || second == null) {
            return false;
        }
        if (this.keepUltraTerminalData) {
            if (first.getItem() instanceof ItemWirelessUltraTerminal) {
                return isIngredient(second);
            }
            return second.getItem() instanceof ItemWirelessUltraTerminal && isIngredient(first);
        }
        if (!isIngredient(first) || !isIngredient(second)) {
            return false;
        }
        return !this.requireDifferentTerminals || first.getItem() != second.getItem()
                || first.getItemDamage() != second.getItemDamage();
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
