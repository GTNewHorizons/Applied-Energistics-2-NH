package appeng.helpers.transfer;

import java.util.List;
import java.util.Locale;

/**
 * The Transfer Terminal's text search, it runs wherever item names are in the player's language.
 */
public final class TransferNameFilter {

    private TransferNameFilter() {}

    /**
     * @param configNames display names of the component's configured stacks
     * @param text        text the player typed, matched against the names only
     */
    public static boolean matches(final List<String> configNames, final String text) {
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
