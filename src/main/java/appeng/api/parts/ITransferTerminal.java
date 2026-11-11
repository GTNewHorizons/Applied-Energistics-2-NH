package appeng.api.parts;

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
}
