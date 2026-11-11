package thaumicenergistics.api.gui;

import org.jspecify.annotations.NonNull;

import thaumicenergistics.api.grid.ICraftingIssuerHost;

/**
 * Container that can issue AE2 crafting jobs.
 *
 * @author Nividica
 *
 */
public interface ICraftingIssuerContainer {

    /**
     * Gets the crafting issuer host terminal.
     *
     * @return
     */
    @NonNull
    ICraftingIssuerHost getCraftingHost();
}
