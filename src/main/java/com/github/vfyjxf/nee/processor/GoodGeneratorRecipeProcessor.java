package com.github.vfyjxf.nee.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import com.github.vfyjxf.nee.utils.ItemUtils;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

public class GoodGeneratorRecipeProcessor implements IRecipeProcessor {

    @NonNull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(Arrays.asList("gg.recipe.neutron_activator", "gg.recipe.precise_assembler"));
    }

    @NonNull
    @Override
    public String getRecipeProcessorId() {
        return "Good Generator";
    }

    @NonNull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
            recipeInputs.removeIf(
                    positionedStack -> ItemUtils.getFluidFromDisplayStack(positionedStack.items[0]) != null
                            || positionedStack.item.stackSize == 0);
            return recipeInputs;
        }
        return recipeInputs;
    }

    @NonNull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeOutputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeOutputs.addAll(recipe.getOtherStacks(recipeIndex));
            recipeOutputs
                    .removeIf(positionedStack -> ItemUtils.getFluidFromDisplayStack(positionedStack.items[0]) != null);
            return recipeOutputs;
        }
        return recipeOutputs;
    }
}
