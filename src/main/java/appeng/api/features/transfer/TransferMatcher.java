package appeng.api.features.transfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import appeng.api.config.TransferDirection;
import appeng.api.storage.data.IAEStack;

/**
 * Answers whether a component could move a given stack. Runs on the server only.
 */
public interface TransferMatcher {

    /** A component that can never move anything, ie. one with no possible direction. */
    TransferMatcher NONE = (stack, direction) -> false;

    /**
     * @param stack     stack to test
     * @param direction {@link TransferDirection#IMPORT} or {@link TransferDirection#EXPORT}, callers expand
     *                  {@link TransferDirection#BOTH} themselves
     * @return true if the component's current settings would allow the stack through
     */
    boolean canTransfer(IAEStack<?> stack, TransferDirection direction);

    /**
     * @return true if a whitelist on the component names this stack. AE fills such storage before unfiltered storage of
     *         the same priority.
     */
    default boolean isExplicitlyListed(final IAEStack<?> stack) {
        return false;
    }

    /**
     * @param config stacks the component exports, other stacks and every import are refused
     * @return a matcher for a component that pulls exactly the configured stacks out of the network
     */
    static TransferMatcher exportOf(final Collection<? extends IAEStack<?>> config) {
        final List<IAEStack<?>> copy = new ArrayList<>(config);
        return (stack, direction) -> {
            if (direction != TransferDirection.EXPORT || stack == null) {
                return false;
            }
            for (final IAEStack<?> configured : copy) {
                if (configured.getStackType() == stack.getStackType() && configured.isSameType((Object) stack)) {
                    return true;
                }
            }
            return false;
        };
    }
}
