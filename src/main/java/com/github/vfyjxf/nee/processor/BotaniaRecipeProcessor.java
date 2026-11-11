package com.github.vfyjxf.nee.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerLexicaBotania;

public class BotaniaRecipeProcessor implements IRecipeProcessor {

    @NonNull
    @Override
    public Set<String> getAllOverlayIdentifier() {
        return new HashSet<>(
                Arrays.asList(
                        "botania.brewery",
                        "botania.elvenTrade",
                        "botania.manaPool",
                        "botania.petalApothecary",
                        "botania.pureDaisy",
                        "botania.runicAltar"));
    }

    @NonNull
    @Override
    public String getRecipeProcessorId() {
        return " Botania";
    }

    @NonNull
    @Override
    public List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeInputs = new ArrayList<>();
        if (recipe instanceof RecipeHandlerLexicaBotania) {
            // we don't need to get recipe from a book.
            // but botania doesn't provide an identifier for each recipe handler, so the button will still show.
            return recipeInputs;
        }
        recipeInputs.addAll(recipe.getIngredientStacks(recipeIndex));
        return recipeInputs;
    }

    @NonNull
    @Override
    public List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier) {
        List<PositionedStack> recipeOutputs = new ArrayList<>();
        if (recipe instanceof RecipeHandlerLexicaBotania) {
            // we don't need to get recipe from a book.
            // but botania doesn't provide an identifier for each recipe handler, so the button will still show.
            return recipeOutputs;
        }
        recipeOutputs.add(recipe.getResultStack(recipeIndex));
        return recipeOutputs;
    }
}
