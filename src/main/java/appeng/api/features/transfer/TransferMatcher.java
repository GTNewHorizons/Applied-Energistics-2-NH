package appeng.api.features.transfer;

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
}
