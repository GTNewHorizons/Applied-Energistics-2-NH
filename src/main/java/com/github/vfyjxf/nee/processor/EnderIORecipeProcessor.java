package com.github.vfyjxf.nee.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import crazypants.enderio.nei.SagMillRecipeHandler;

public class EnderIORecipeProcessor implements IRecipeProcessor {

    @NonNull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList(
                        "EnderIOAlloySmelter",
                        "EIOEnchanter",
                        "EnderIOSagMill",
                        "EnderIOSliceAndSplice",
                        "EnderIOSoulBinder",
                        "EnderIOVat"));
    }

    @NonNull
    @Override
    public String getRecipeProcessorId() {
        return "EnderIO";
    }

    @NonNull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
            return recipeInputs;
        }
        return recipeInputs;
    }

    @NonNull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeOutputs = new ArrayList<>();
        if (identifier != null) {
            if (this.getAllOverlayIdentifier().contains(identifier)) {
                recipeOutputs.add(recipe.getResultStack(recipeIndex));
                recipeOutputs.addAll(recipe.getOtherStacks(recipeIndex));
                // remove output if it's chance != 1
                if (recipe instanceof SagMillRecipeHandler) {
                    SagMillRecipeHandler.MillRecipe millRecipe = (SagMillRecipeHandler.MillRecipe) ((SagMillRecipeHandler) recipe).arecipes
                            .get(recipeIndex);
                    recipeOutputs
                            .removeIf(positionedStack -> millRecipe.getChanceForOutput(positionedStack.item) != 1.0F);
                }
                return recipeOutputs;
            }
        }

        return recipeOutputs;
    }
}
