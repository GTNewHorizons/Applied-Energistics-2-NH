package appeng.api.crafting;

import appeng.api.networking.crafting.ICraftingPatternDetails;

/**
 * Pattern target of anything that runs crafting table patterns, such as a Molecular Assembler.
 */
public enum CraftingMachineTarget implements IPatternTarget, IPatternTargetKey {

    INSTANCE;

    @Override
    public IPatternTargetKey getTargetKey() {
        return this;
    }

    /**
     * Matches crafting table patterns and rejects processing patterns.
     */
    @Override
    public PatternMatch match(ICraftingPatternDetails pattern) {
        return pattern.isCraftable() ? PatternMatch.MATCH : PatternMatch.NO_MATCH;
    }
}
