package appeng.api.crafting;

import appeng.api.networking.crafting.ICraftingPatternDetails;

/**
 * Describes what a machine can run. Make it immutable with value equality: AE2 checks each distinct key once per
 * pattern and reuses the result for every machine with an equal key.
 */
public interface IPatternTargetKey {

    IPatternTargetKey NOT_READY = pattern -> PatternMatch.NOT_READY;

    /**
     * Returns whether a machine with this key can run the pattern.
     */
    PatternMatch match(ICraftingPatternDetails pattern);
}
