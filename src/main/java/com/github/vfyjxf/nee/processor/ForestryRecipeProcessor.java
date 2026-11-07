package com.github.vfyjxf.nee.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

public class ForestryRecipeProcessor implements IRecipeProcessor {

    @NonNull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList(
                        "forestry.bottler",
                        "forestry.carpenter",
                        "forestry.centrifuge",
                        "forestry.fabricator",
                        "forestry.fermenter",
                        "forestry.moistener",
                        "forestry.squeezer",
                        "forestry.still",
                        RecipeProcessor.NULL_IDENTIFIER));
    }

    @NonNull
    @Override
    public String getRecipeProcessorId() {
        return "Forestry";
    }

    @NonNull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
        }
        return recipeInputs;
    }

    @NonNull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeOutput = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeOutput.add(recipe.getResultStack(recipeIndex));
        }
        return recipeOutput;
    }
}
