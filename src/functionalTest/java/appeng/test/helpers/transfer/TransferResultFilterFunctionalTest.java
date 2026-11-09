package appeng.test.helpers.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                .apply(Arrays.asList(row), null, TransferDirection.BOTH);

        assertEquals(1, result.size());
        assertSame(row, result.get(0));
    }

    @Test
    public void directionSelectsMatchingRows() {
        final List<TransferComponentInfo> rows = Arrays.asList(
                row("importer", only(TransferDirection.IMPORT)),
                row("exporter", only(TransferDirection.EXPORT)),
                row("nothing", TransferMatcher.NONE));

        assertEquals("importer", TransferResultFilter.apply(rows, IRON, TransferDirection.IMPORT).get(0).customName);
        assertEquals(1, TransferResultFilter.apply(rows, IRON, TransferDirection.IMPORT).size());
        assertEquals("exporter", TransferResultFilter.apply(rows, IRON, TransferDirection.EXPORT).get(0).customName);
        assertEquals(2, TransferResultFilter.apply(rows, IRON, TransferDirection.BOTH).size());
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

        final List<TransferComponentInfo> result = TransferResultFilter.apply(rows, IRON, TransferDirection.EXPORT);

        assertEquals("z listed", result.get(0).customName);
        assertEquals("a open", result.get(1).customName);
    }

    private static TransferComponentInfo storageRow(final String name, final TransferMatcher matcher) {
        final TransferDescription description = TransferDescription.builder().storageHandler(true).priority(3)
                .matcher(matcher).build();
        return new TransferComponentInfo(null, name, null, 0, 0, 0, 0, (byte) 0, 0, description);
    }

    @Test
    public void matchingConfigEntriesAreMarked() {
        final TransferComponentInfo row = row("a", only(TransferDirection.EXPORT), GOLD, IRON);

        final TransferDescription result = TransferResultFilter
                .apply(Arrays.asList(row), IRON, TransferDirection.EXPORT).get(0).description;

        assertFalse(result.config.get(0).matchesQuery);
        assertTrue(result.config.get(1).matchesQuery);
    }
}
