package com.github.vfyjxf.nee.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * @author vfyjxf
 */
public class ICRecipeProcessor implements IRecipeProcessor {

    @NonNull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList(
                        "blastfurnace",
                        "BlockCutter",
                        "centrifuge",
                        "compressor",
                        "extractor",
                        "fluidcanner",
                        "macerator",
                        "metalformer",
                        "oreWashing",
                        "solidcanner"));
    }

    @NonNull
    @Override
    public String getRecipeProcessorId() {
        return "IC2";
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
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeOutputs.add(recipe.getResultStack(recipeIndex));
            recipeOutputs.addAll(recipe.getOtherStacks(recipeIndex));
            return recipeOutputs;
        }
        return recipeOutputs;
    }
}
