package appeng.helpers.transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import appeng.api.config.TransferDirection;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.features.transfer.TransferMatcher;
import appeng.api.storage.data.IAEStack;

/**
 * Applies the Transfer Terminal's stack search to a scan result.
 */
public final class TransferResultFilter {

    private TransferResultFilter() {}

    /**
     * @param rows      every component found by the scan
     * @param query     stack to search for, null keeps every row
     * @param direction direction to search in
     * @param keep      additional test a row has to pass
     * @return the matching rows in display order
     */
    public static List<TransferComponentInfo> apply(final List<TransferComponentInfo> rows,
            @Nullable final IAEStack<?> query, final TransferDirection direction,
            final Predicate<TransferComponentInfo> keep) {
        final List<TransferComponentInfo> result = new ArrayList<>(rows.size());
        for (final TransferComponentInfo row : rows) {
            if (!keep.test(row)) {
                continue;
            }

            if (query == null) {
                result.add(row);
            } else {
                final TransferComponentInfo matched = matchedRow(row, query, direction);
                if (matched != null) {
                    result.add(matched);
                }
            }
        }

        result.sort(TransferRowOrder.comparator(query != null, direction));
        return result;
    }

    /**
     * Matchers from other mods may throw, or fail to link against a version of a mod they were not built for. Such a
     * row is treated as not matching.
     *
     * @return the row updated for the query, or null if it does not match
     */
    @Nullable
    private static TransferComponentInfo matchedRow(final TransferComponentInfo row, final IAEStack<?> query,
            final TransferDirection direction) {
        try {
            return matches(row.description.matcher, query, direction) ? row.withQuery(query) : null;
        } catch (final RuntimeException | LinkageError e) {
            return null;
        }
    }

    /**
     * @param craftingOnly   whether to keep only components with a crafting card
     * @param hideUnfiltered whether to drop components with no filter set
     */
    public static boolean passesToggles(final TransferDescription description, final boolean craftingOnly,
            final boolean hideUnfiltered) {
        return (!craftingOnly || description.hasCraftingCard) && (!hideUnfiltered || hasFilter(description));
    }

    private static boolean hasFilter(final TransferDescription description) {
        return !description.config.isEmpty() || (description.oreFilter != null && !description.oreFilter.isEmpty());
    }

    private static boolean matches(final TransferMatcher matcher, final IAEStack<?> query,
            final TransferDirection direction) {
        if (direction == TransferDirection.BOTH) {
            return matcher.canTransfer(query, TransferDirection.IMPORT)
                    || matcher.canTransfer(query, TransferDirection.EXPORT);
        }
        return matcher.canTransfer(query, direction);
    }
}
