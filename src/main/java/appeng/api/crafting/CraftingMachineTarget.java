package appeng.api.crafting;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import appeng.api.networking.crafting.ICraftingPatternDetails;

/**
 * Pattern target of anything that runs crafting table patterns, such as a Molecular Assembler; AE2 registers its
 * matcher.
 */
public enum CraftingMachineTarget implements IPatternTarget {

    INSTANCE;

    @Override
    public Object getTargetKey() {
        return this;
    }

    /**
     * Matches crafting table patterns and rejects processing patterns.
     */
    public static Map<CraftingMachineTarget, PatternMatch> match(ICraftingPatternDetails pattern,
            Set<CraftingMachineTarget> keys) {
        final PatternMatch result = pattern.isCraftable() ? PatternMatch.MATCH : PatternMatch.NO_MATCH;
        final Map<CraftingMachineTarget, PatternMatch> results = new HashMap<>();
        for (CraftingMachineTarget key : keys) {
            results.put(key, result);
        }
        return results;
    }
}
