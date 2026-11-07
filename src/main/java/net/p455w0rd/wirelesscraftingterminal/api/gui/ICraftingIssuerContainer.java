package net.p455w0rd.wirelesscraftingterminal.api.gui;

import net.p455w0rd.wirelesscraftingterminal.api.grid.ICraftingIssuerHost;

import org.jspecify.annotations.NonNull;

public interface ICraftingIssuerContainer {

    @NonNull
    ICraftingIssuerHost getCraftingHost();
}
