package appeng.api.parts;

import java.util.Map;

import appeng.api.networking.security.IActionHost;
import appeng.api.util.IConfigurableObject;
import appeng.tile.inventory.IAEStackInventory;

/**
 * Host of the Transfer Terminal's screen. It keeps the screen's filters, so they survive closing it.
 */
public interface ITransferTerminal extends IActionHost, IConfigurableObject {

    /**
     * @return a number that grows each time the network changes in a way that calls for a rescan, so each open screen
     *         can tell for itself whether it has seen the change
     */
    long getChangeCount();

    /**
     * @return the one slot holding the stack being searched for
     */
    IAEStackInventory getQueryInventory();

    /**
     * @return the text last typed in the search box
     */
    String getSearchText();

    void setSearchText(String text);

    /**
     * @return the kinds of component the player turned on or off, by key, every other kind follows its default
     */
    Map<String, Boolean> getTypeOverrides();

    /**
     * Turns rows of one kind of component on or off.
     */
    void setTypeShown(String key, boolean shown);

    /**
     * Puts every kind of component back to its default.
     */
    void resetTypeOverrides();

    /**
     * @return a number that grows each time {@link #getTypeOverrides()} changes, so each open screen can tell for
     *         itself whether it has seen the change
     */
    long getTypeOverridesRevision();
}
