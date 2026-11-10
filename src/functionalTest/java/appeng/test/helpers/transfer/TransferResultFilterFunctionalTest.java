package appeng.test.helpers.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import appeng.api.config.TransferDirection;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.features.transfer.TransferMatcher;
import appeng.api.storage.data.IAEStack;
import appeng.helpers.transfer.TransferResultFilter;
import appeng.util.item.AEItemStack;

public class TransferResultFilterFunctionalTest {

    private static final IAEStack<?> IRON = AEItemStack.create(new ItemStack(Items.iron_ingot));
    private static final IAEStack<?> GOLD = AEItemStack.create(new ItemStack(Items.gold_ingot));

    private static TransferComponentInfo row(final String name, final TransferMatcher matcher,
            final IAEStack<?>... config) {
        final TransferDescription.Builder builder = TransferDescription.builder().matcher(matcher);
        for (final IAEStack<?> stack : config) {
            builder.config(stack, false);
        }
        return new TransferComponentInfo(null, name, null, 0, 0, 0, 0, (byte) 0, 0, builder.build());
    }

    private static TransferMatcher only(final TransferDirection allowed) {
        return (stack, direction) -> direction == allowed;
    }

    @Test
    public void noQueryKeepsEveryRowUntouched() {
        final TransferComponentInfo row = row("a", TransferMatcher.NONE);

        final List<TransferComponentInfo> result = TransferResultFilter
                .apply(Arrays.asList(row), null, TransferDirection.BOTH, ignored -> true);

        assertEquals(1, result.size());
        assertSame(row, result.get(0));
    }

    @Test
    public void directionSelectsMatchingRows() {
        final List<TransferComponentInfo> rows = Arrays.asList(
                row("importer", only(TransferDirection.IMPORT)),
                row("exporter", only(TransferDirection.EXPORT)),
                row("nothing", TransferMatcher.NONE));

        assertEquals(
                "importer",
                TransferResultFilter.apply(rows, IRON, TransferDirection.IMPORT, ignored -> true).get(0).customName);
        assertEquals(1, TransferResultFilter.apply(rows, IRON, TransferDirection.IMPORT, ignored -> true).size());
        assertEquals(
                "exporter",
                TransferResultFilter.apply(rows, IRON, TransferDirection.EXPORT, ignored -> true).get(0).customName);
        assertEquals(2, TransferResultFilter.apply(rows, IRON, TransferDirection.BOTH, ignored -> true).size());
    }

    @Test
    public void exportPutsWhitelistedStorageAheadOfUnfilteredAtEqualPriority() {
        final TransferMatcher listed = new TransferMatcher() {

            @Override
            public boolean canTransfer(final IAEStack<?> stack, final TransferDirection direction) {
                return true;
            }

            @Override
            public boolean isExplicitlyListed(final IAEStack<?> stack) {
                return true;
            }
        };
        final List<TransferComponentInfo> rows = Arrays
                .asList(storageRow("a open", (stack, direction) -> true), storageRow("z listed", listed));

        final List<TransferComponentInfo> result = TransferResultFilter
                .apply(rows, IRON, TransferDirection.EXPORT, ignored -> true);

        assertEquals("z listed", result.get(0).customName);
        assertEquals("a open", result.get(1).customName);
    }

    private static TransferComponentInfo storageRow(final String name, final TransferMatcher matcher) {
        return storageRow(name, 3, 0, matcher);
    }

    private static TransferComponentInfo storageRow(final String name, final int priority, final int tier,
            final TransferMatcher matcher) {
        final TransferDescription description = TransferDescription.builder().storageHandler(true).priority(priority)
                .matcher(matcher).build();
        return new TransferComponentInfo(null, name, null, 0, 0, 0, 0, (byte) 0, tier, description);
    }

    /** Sticky storage that claims IRON only, like a sticky bus whose whitelist names it. */
    private static final TransferMatcher STICKY_FOR_IRON = new TransferMatcher() {

        @Override
        public boolean canTransfer(final IAEStack<?> stack, final TransferDirection direction) {
            return true;
        }

        @Override
        public boolean capturesAsSticky(final IAEStack<?> stack) {
            return stack.isSameType((Object) IRON);
        }
    };

    private static List<String> names(final List<TransferComponentInfo> rows) {
        final List<String> names = new ArrayList<>();
        for (final TransferComponentInfo row : rows) {
            names.add(row.customName);
        }
        return names;
    }

    @Test
    public void exportPutsClaimingStickyStorageAheadOfAnyPriority() {
        final List<TransferComponentInfo> rows = Arrays.asList(
                storageRow("high", 100, 0, (stack, direction) -> true),
                storageRow("sticky", 0, 0, STICKY_FOR_IRON));

        final List<TransferComponentInfo> result = TransferResultFilter
                .apply(rows, IRON, TransferDirection.EXPORT, ignored -> true);

        assertEquals(Arrays.asList("sticky", "high"), names(result));
        assertTrue(result.get(0).stickyCapture);
        assertFalse(result.get(1).stickyCapture);
    }

    @Test
    public void stickyStorageThatDoesNotClaimTheStackSortsByPriority() {
        final List<TransferComponentInfo> rows = Arrays.asList(
                storageRow("high", 100, 0, (stack, direction) -> true),
                storageRow("sticky", 0, 0, STICKY_FOR_IRON));

        final List<TransferComponentInfo> result = TransferResultFilter
                .apply(rows, GOLD, TransferDirection.EXPORT, ignored -> true);

        assertEquals(Arrays.asList("high", "sticky"), names(result));
        assertFalse(result.get(1).stickyCapture);
    }

    @Test
    public void claimingStickyStorageOnlyLeadsItsOwnNetwork() {
        final List<TransferComponentInfo> rows = Arrays.asList(
                storageRow("sub sticky", 0, 1, STICKY_FOR_IRON),
                storageRow("own", 0, 0, (stack, direction) -> true));

        final List<TransferComponentInfo> result = TransferResultFilter
                .apply(rows, IRON, TransferDirection.EXPORT, ignored -> true);

        assertEquals(Arrays.asList("own", "sub sticky"), names(result));
    }

    @Test
    public void bothDirectionsIgnoreStickyStorage() {
        final List<TransferComponentInfo> rows = Arrays.asList(
                storageRow("high", 100, 0, (stack, direction) -> true),
                storageRow("sticky", 0, 0, STICKY_FOR_IRON));

        final List<TransferComponentInfo> result = TransferResultFilter
                .apply(rows, IRON, TransferDirection.BOTH, ignored -> true);

        assertEquals(Arrays.asList("high", "sticky"), names(result));
    }

    @Test
    public void exportOfMatchesOnlyConfiguredStacksLeavingTheNetwork() {
        final TransferMatcher matcher = TransferMatcher.exportOf(Arrays.asList(IRON));

        assertTrue(matcher.canTransfer(IRON, TransferDirection.EXPORT));
        assertFalse(matcher.canTransfer(IRON, TransferDirection.IMPORT));
        assertFalse(matcher.canTransfer(GOLD, TransferDirection.EXPORT));
    }

    @Test
    public void matchingConfigEntriesAreMarked() {
        final TransferComponentInfo row = row("a", only(TransferDirection.EXPORT), GOLD, IRON);

        final TransferDescription result = TransferResultFilter
                .apply(Arrays.asList(row), IRON, TransferDirection.EXPORT, ignored -> true).get(0).description;

        assertFalse(result.config.get(0).matchesQuery);
        assertTrue(result.config.get(1).matchesQuery);
    }

    @Test
    public void rowsFailingTheExtraTestAreDropped() {
        final List<TransferComponentInfo> rows = Arrays
                .asList(row("keep", TransferMatcher.NONE), row("drop", TransferMatcher.NONE));

        final List<TransferComponentInfo> result = TransferResultFilter
                .apply(rows, null, TransferDirection.BOTH, candidate -> candidate.customName.equals("keep"));

        assertEquals(1, result.size());
        assertEquals("keep", result.get(0).customName);
    }

    @Test
    public void craftingOnlyNeedsACraftingCard() {
        final TransferDescription withCard = TransferDescription.builder().hasCraftingCard(true).build();
        final TransferDescription withoutCard = TransferDescription.builder().build();

        assertTrue(TransferResultFilter.passesToggles(withCard, true, false));
        assertFalse(TransferResultFilter.passesToggles(withoutCard, true, false));
        assertTrue(TransferResultFilter.passesToggles(withoutCard, false, false));
    }

    @Test
    public void hideUnfilteredKeepsOnlyComponentsWithAFilter() {
        final TransferDescription none = TransferDescription.builder().build();
        final TransferDescription configured = TransferDescription.builder().config(IRON, false).build();
        final TransferDescription oreFiltered = TransferDescription.builder().oreFilter("ingot*").build();
        final TransferDescription emptyOreFilter = TransferDescription.builder().oreFilter("").build();

        assertFalse(TransferResultFilter.passesToggles(none, false, true));
        assertFalse(TransferResultFilter.passesToggles(emptyOreFilter, false, true));
        assertTrue(TransferResultFilter.passesToggles(configured, false, true));
        assertTrue(TransferResultFilter.passesToggles(oreFiltered, false, true));
        assertTrue(TransferResultFilter.passesToggles(none, false, false));
    }
}
