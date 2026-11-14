package appeng.api.features;

import org.jetbrains.annotations.Nullable;

import appeng.api.crafting.IPatternTargetMatcher;

/**
 * Matchers that tell which pattern targets can run a pattern, keyed by the exact class of the target key.
 */
public interface IPatternTargetRegistry {

    /**
     * Registers the matcher for keys of exactly this class; call it during init.
     */
    <K> void registerMatcher(Class<K> keyClass, IPatternTargetMatcher<K> matcher);

    /**
     * Returns the matcher for keys of exactly this class, or null if none is registered.
     */
    @Nullable
    <K> IPatternTargetMatcher<K> getMatcher(Class<K> keyClass);
}
