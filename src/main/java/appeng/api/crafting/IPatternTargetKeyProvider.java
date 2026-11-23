package appeng.api.crafting;

import org.jspecify.annotations.Nullable;

/**
 * The machine a pattern provider pushes into. Expose it as a capability on the machine's tile entity, the same way as
 * {@link ICraftingIconProvider}.
 */
public interface IPatternTargetKeyProvider {

    /**
     * Returns the key that checks patterns for this machine, or null if the machine is unknown.
     */
    @Nullable
    IPatternTargetKey getTargetKey();
}
