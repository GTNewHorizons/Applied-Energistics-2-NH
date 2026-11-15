package net.p455w0rd.wirelesscraftingterminal.api;

import org.jspecify.annotations.NonNull;

public abstract class IWCTItems {

    /**
     * WirelessCraftingTerminal
     */
    public @NonNull IWCTItemDescription WirelessCraftingTerminal;

    /**
     * Infinity Booster Card
     */
    public @NonNull IWCTItemDescription InfinityBoosterCard;

    /**
     * Used for the creat6ive tab TODO: remove unneeded item and just add the icon properly
     */
    public @NonNull IWCTItemDescription WCTDummyIcon;

    /**
     * Item used solely for infinity booster card slot background icon
     */
    public @NonNull IWCTItemDescription WCTBoosterBGIcon;

    /**
     * Magnet Card
     */
    public @NonNull IWCTItemDescription MagnetCard;
}
