package appeng.api.crafting;

import java.util.Map;
import java.util.Set;

import appeng.api.networking.crafting.ICraftingPatternDetails;

/**
 * Checks a pattern against target keys of one class. Register it with
 * {@link appeng.api.features.IPatternTargetRegistry}.
 */
@FunctionalInterface
public interface IPatternTargetMatcher<K> {

    /**
     * Returns a result for each distinct key; AE2 treats a missing key as {@link PatternMatch#UNKNOWN}.
     */
    Map<K, PatternMatch> match(ICraftingPatternDetails pattern, Set<K> keys);
}
