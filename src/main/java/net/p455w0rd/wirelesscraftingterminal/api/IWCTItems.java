package net.p455w0rd.wirelesscraftingterminal.api;

import org.jspecify.annotations.NonNull;

public abstract class IWCTItems {

    /**
     * WirelessCraftingTerminal
     */
    @NonNull
    public IWCTItemDescription WirelessCraftingTerminal;

    /**
     * Infinity Booster Card
     */
    @NonNull
    public IWCTItemDescription InfinityBoosterCard;

    /**
     * Used for the creat6ive tab TODO: remove unneeded item and just add the icon properly
     */
    @NonNull
    public IWCTItemDescription WCTDummyIcon;

    /**
     * Item used solely for infinity booster card slot background icon
     */
    @NonNull
    public IWCTItemDescription WCTBoosterBGIcon;

    /**
     * Magnet Card
     */
    @NonNull
    public IWCTItemDescription MagnetCard;
}
