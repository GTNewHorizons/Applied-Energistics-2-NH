package appeng.api.crafting;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import appeng.api.networking.crafting.ICraftingPatternDetails;

/**
 * Pattern target of a machine that exists but cannot run anything now, such as an unformed multiblock; AE2 registers
 * its matcher.
 */
public enum NotReadyTarget implements IPatternTarget {

    INSTANCE;

    @Override
    public Object getTargetKey() {
        return this;
    }

    /**
     * Reports every key as not ready.
     */
    public static Map<NotReadyTarget, PatternMatch> match(ICraftingPatternDetails pattern, Set<NotReadyTarget> keys) {
        final Map<NotReadyTarget, PatternMatch> results = new HashMap<>();
        for (NotReadyTarget key : keys) {
            results.put(key, PatternMatch.NOT_READY);
        }
        return results;
    }
}
