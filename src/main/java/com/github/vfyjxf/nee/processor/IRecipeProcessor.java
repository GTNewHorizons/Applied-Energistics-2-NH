package com.github.vfyjxf.nee.processor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NonNull;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

/**
 * @author vfyjxf
 */
public interface IRecipeProcessor {

    @NonNull
    default Set<String> getAllOverlayIdentifier() {
        return Collections.emptySet();
    }

    @NonNull
    String getRecipeProcessorId();

    @NonNull
    List<PositionedStack> getRecipeInput(IRecipeHandler recipe, int recipeIndex, String identifier);

    @NonNull
    List<PositionedStack> getRecipeOutput(IRecipeHandler recipe, int recipeIndex, String identifier);

    default boolean mergeStacks(IRecipeHandler recipe, int recipeIndex, String identifier) {
        return true;
    }
}
