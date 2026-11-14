package appeng.core.features.registries;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import appeng.api.crafting.IPatternTargetMatcher;
import appeng.api.features.IPatternTargetRegistry;
import appeng.crafting.target.CraftingMachineTarget;

public class PatternTargetRegistry implements IPatternTargetRegistry {

    private final Map<Class<?>, IPatternTargetMatcher<?>> matchers = new HashMap<>();

    public PatternTargetRegistry() {
        this.registerMatcher(CraftingMachineTarget.class, CraftingMachineTarget::match);
    }

    @Override
    public <K> void registerMatcher(Class<K> keyClass, IPatternTargetMatcher<K> matcher) {
        if (this.matchers.putIfAbsent(keyClass, matcher) != null) {
            throw new IllegalStateException("A pattern target matcher is already registered for " + keyClass);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <K> IPatternTargetMatcher<K> getMatcher(Class<K> keyClass) {
        return (IPatternTargetMatcher<K>) this.matchers.get(keyClass);
    }
}
