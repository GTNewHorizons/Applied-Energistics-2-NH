package com.github.vfyjxf.nee.processor;

import static com.github.vfyjxf.nee.processor.RecipeProcessor.ThaumicEnergistics_isLoaded;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * @author vfyjxf
 */
public class TCNEIPluginRecipeProcessor implements IRecipeProcessor {

    @NonNull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList(
                        "arcaneshapedrecipes",
                        "arcaneshapelessrecipes",
                        "aspectsRecipe",
                        "cruciblerecipe",
                        "infusionCrafting"));
    }

    @NonNull
    @Override
    public String getRecipeProcessorId() {
        return "TCNEIPlugin";
    }

    @NonNull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (this.getAllOverlayIdentifier().contains(identifier)) {
            recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));

            if (!ThaumicEnergistics_isLoaded) {
                recipeInputs.removeIf(positionedStack -> positionedStack.item.getItem() instanceof ItemAspect);
            }

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

            if (!ThaumicEnergistics_isLoaded) {
                recipeOutputs.removeIf(positionedStack -> positionedStack.item.getItem() instanceof ItemAspect);
            }

            return recipeOutputs;
        }
        return recipeOutputs;
    }
}
