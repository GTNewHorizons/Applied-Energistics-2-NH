package appeng.helpers.transfer;

import java.util.List;
import java.util.Locale;

/**
 * The filters the Transfer Terminal applies on the client, where item names are in the player's language.
 */
public final class TransferClientFilter {

    private TransferClientFilter() {}

    /**
     * @param configNames     display names of the component's configured stacks
     * @param hasCraftingCard whether the component has a crafting card installed
     * @param text            text the player typed, matched against the names only
     * @param craftingOnly    whether to keep only components with a crafting card
     */
    public static boolean matches(final List<String> configNames, final boolean hasCraftingCard, final String text,
            final boolean craftingOnly) {
        if (craftingOnly && !hasCraftingCard) {
            return false;
        }

        final String needle = text.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return true;
        }

        for (final String name : configNames) {
            if (name.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
