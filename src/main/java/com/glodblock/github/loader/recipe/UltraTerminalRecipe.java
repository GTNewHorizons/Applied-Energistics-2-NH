package com.glodblock.github.loader.recipe;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.common.item.ItemBaseWirelessTerminal;
import com.glodblock.github.common.item.ItemWirelessUltraTerminal;

import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.util.Platform;

/**
 * Combines the terminals of the crafting grid into one ultra terminal, the result contains exactly the terminals that
 * were combined.
 */
public class UltraTerminalRecipe extends ShapelessOreRecipe {

    private static final String POWER_KEY = "internalCurrentPower";

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
            final ItemStack[] terminals = copyTerminals(inventory);
            final NBTTagCompound target = Platform.openNbtData(result);
            final Item item = result.getItem();
            if (item == null) return null;
            for (final ItemStack terminal : terminals) {
                final NBTTagCompound source = terminal == null ? null : terminal.getTagCompound();
                if (source == null) {
                    continue;
                }
                for (final String key : source.func_150296_c()) {
                    final NBTBase value = source.getTag(key);
                    if (!target.hasKey(key)) {
                        target.setTag(key, value.copy());
                        continue;
                    }
                    if (POWER_KEY.equals(key)) {
                        final double capacity = ((IAEItemPowerStorage) item).getAEMaxPower(result);
                        target.setDouble(
                                key,
                                Math.min(capacity, Math.max(target.getDouble(key), source.getDouble(key))));
                        continue;
                    }
                    if (ItemBaseWirelessTerminal.infinityBoosterCard.equals(key)
                            || ItemBaseWirelessTerminal.infinityEnergyCard.equals(key)) {
                        target.setBoolean(key, target.getBoolean(key) || source.getBoolean(key));
                        continue;
                    }
                    if (value.getId() == Constants.NBT.TAG_STRING && target.getString(key).isEmpty()) {
                        target.setString(key, source.getString(key));
                    }
                }
            }
            ItemWirelessUltraTerminal.absorb(result, this.keepUltraTerminalData, terminals);
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
