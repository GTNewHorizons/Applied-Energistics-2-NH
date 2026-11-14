package appeng.crafting.target;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import appeng.api.crafting.IPatternTarget;
import appeng.api.crafting.PatternMatch;
import appeng.api.networking.crafting.ICraftingPatternDetails;

/**
 * Pattern target for an adjacent {@link appeng.api.implementations.tiles.ICraftingMachine}, such as a Molecular
 * Assembler.
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
