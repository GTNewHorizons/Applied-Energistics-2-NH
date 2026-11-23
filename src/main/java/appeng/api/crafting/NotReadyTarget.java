package appeng.api.crafting;

import appeng.api.networking.crafting.ICraftingPatternDetails;

/**
 * Pattern target of a machine that exists but cannot run anything now, such as an unformed multiblock.
 */
public enum NotReadyTarget implements IPatternTargetKeyProvider, IPatternTargetKey {

    INSTANCE;

    @Override
    public IPatternTargetKey getTargetKey() {
        return this;
    }

    /**
     * Reports every pattern as not ready.
     */
    @Override
    public PatternMatch match(ICraftingPatternDetails pattern) {
        return PatternMatch.NOT_READY;
    }
}
