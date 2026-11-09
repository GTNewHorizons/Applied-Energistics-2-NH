package appeng.api.parts;

import appeng.api.networking.security.IActionHost;

/**
 * Host of the Transfer Terminal's screen.
 */
public interface ITransferTerminal extends IActionHost {

    /**
     * @return true if the network changed since the last call, calling this resets the flag
     */
    boolean needsUpdate();
}
