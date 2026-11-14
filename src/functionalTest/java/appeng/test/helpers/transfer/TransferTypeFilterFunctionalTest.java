package appeng.test.helpers.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.features.transfer.TransferDescription;
import appeng.helpers.transfer.TransferTypeFilter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TransferTypeFilterFunctionalTest {

    private static TransferComponentInfo row(final ItemStack icon, final boolean hiddenByDefault) {
        return new TransferComponentInfo(
                icon,
                "",
                null,
                0,
                0,
                0,
                0,
                (byte) 0,
                0,
                TransferDescription.builder().hiddenByDefault(hiddenByDefault).build());
    }

    @Test
    public void kindsAreToldApartByItemAndDamage() {
        assertNull(TransferTypeFilter.keyOf(null));
        assertEquals(
                TransferTypeFilter.keyOf(new ItemStack(Items.apple, 5)),
                TransferTypeFilter.keyOf(new ItemStack(Items.apple, 1)),
                "The stack size does not matter");
        assertNotEquals(
                TransferTypeFilter.keyOf(new ItemStack(Items.dye, 1, 1)),
                TransferTypeFilter.keyOf(new ItemStack(Items.dye, 1, 2)),
                "Parts share an item and differ by damage");
        assertNotEquals(
                TransferTypeFilter.keyOf(new ItemStack(Items.apple)),
                TransferTypeFilter.keyOf(new ItemStack(Items.bread)));
    }

    @Test
    public void rowsFollowTheirDefaultUntilTheirKindIsTurnedOnOrOff() {
        final TransferComponentInfo drive = row(new ItemStack(Items.apple), true);
        final TransferComponentInfo bus = row(new ItemStack(Items.bread), false);
        final Map<String, Boolean> overrides = new HashMap<>();

        assertFalse(TransferTypeFilter.isShown(drive, overrides), "Hidden by default");
        assertTrue(TransferTypeFilter.isShown(bus, overrides), "Shown by default");

        overrides.put(TransferTypeFilter.keyOf(drive.selfRep), true);
        overrides.put(TransferTypeFilter.keyOf(bus.selfRep), false);
        assertTrue(TransferTypeFilter.isShown(drive, overrides), "Turned on");
        assertFalse(TransferTypeFilter.isShown(bus, overrides), "Turned off");
    }

    @Test
    public void rowsWithoutAnIconAreAlwaysShown() {
        final TransferComponentInfo noIcon = row(null, true);

        assertTrue(TransferTypeFilter.isShown(noIcon, Collections.emptyMap()));
        assertTrue(
                TransferTypeFilter.entries(Collections.singletonList(noIcon), Collections.emptyMap()).isEmpty(),
                "They have no entry in the drop-down");
    }

    @Test
    public void everyKindHasOneEntryWithItsState() {
        final List<TransferComponentInfo> rows = Arrays.asList(
                row(new ItemStack(Items.bread, 3), false),
                row(new ItemStack(Items.apple), true),
                row(new ItemStack(Items.bread), false));

        final List<TransferTypeFilter.Entry> entries = TransferTypeFilter.entries(rows, Collections.emptyMap());

        assertEquals(2, entries.size(), "Two rows of the same kind share an entry");
        for (final TransferTypeFilter.Entry entry : entries) {
            assertEquals(1, entry.icon.stackSize, "The icon is a single item");
            assertEquals(entry.icon.getItem() == Items.bread, entry.shown);
        }
    }

    @Test
    public void entriesSurviveTheNetwork() {
        final List<TransferTypeFilter.Entry> entries = TransferTypeFilter.entries(
                Arrays.asList(row(new ItemStack(Items.apple), true), row(new ItemStack(Items.bread), false)),
                Collections.emptyMap());
        final ByteBuf buffer = Unpooled.buffer();

        TransferTypeFilter.write(buffer, entries);
        final List<TransferTypeFilter.Entry> read = TransferTypeFilter.read(buffer);

        assertEquals(0, buffer.readableBytes(), "Everything written is read");
        assertTrue(TransferTypeFilter.sameEntries(entries, read));
        assertEquals(entries.get(0).icon.getItem(), read.get(0).icon.getItem());
    }

    @Test
    public void entriesDifferWhenAKindIsTurnedOnOrOff() {
        final List<TransferComponentInfo> rows = Collections.singletonList(row(new ItemStack(Items.apple), true));
        final List<TransferTypeFilter.Entry> before = TransferTypeFilter.entries(rows, Collections.emptyMap());
        final List<TransferTypeFilter.Entry> after = TransferTypeFilter
                .entries(rows, Collections.singletonMap(TransferTypeFilter.keyOf(new ItemStack(Items.apple)), true));

        assertFalse(TransferTypeFilter.sameEntries(before, after));
        assertTrue(TransferTypeFilter.sameEntries(before, TransferTypeFilter.entries(rows, Collections.emptyMap())));
    }
}
