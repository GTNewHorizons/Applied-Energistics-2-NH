package appeng.helpers.transfer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public final class TransferNameFilterTest {

    private static final List<String> NAMES = Arrays.asList("Iron Ingot", "Gold Dust");

    @Test
    public void emptyTextMatchesEverything() {
        assertTrue(TransferNameFilter.matches(Collections.emptyList(), ""));
        assertTrue(TransferNameFilter.matches(NAMES, "   "));
    }

    @Test
    public void matchesAnySubstringIgnoringCase() {
        assertTrue(TransferNameFilter.matches(NAMES, "iron"));
        assertTrue(TransferNameFilter.matches(NAMES, "OLD DU"));
    }

    @Test
    public void rejectsTextFoundInNoName() {
        assertFalse(TransferNameFilter.matches(NAMES, "copper"));
    }

    @Test
    public void componentsWithoutConfigNeverMatchText() {
        assertFalse(TransferNameFilter.matches(Collections.emptyList(), "iron"));
    }
}
