package appeng.helpers.transfer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public final class TransferClientFilterTest {

    private static final List<String> NAMES = Arrays.asList("Iron Ingot", "Gold Dust");

    @Test
    public void emptyTextMatchesEverything() {
        assertTrue(TransferClientFilter.matches(Collections.emptyList(), false, "", false));
        assertTrue(TransferClientFilter.matches(NAMES, false, "   ", false));
    }

    @Test
    public void matchesAnySubstringIgnoringCase() {
        assertTrue(TransferClientFilter.matches(NAMES, false, "iron", false));
        assertTrue(TransferClientFilter.matches(NAMES, false, "OLD DU", false));
    }

    @Test
    public void rejectsTextFoundInNoName() {
        assertFalse(TransferClientFilter.matches(NAMES, false, "copper", false));
    }

    @Test
    public void componentsWithoutConfigNeverMatchText() {
        assertFalse(TransferClientFilter.matches(Collections.emptyList(), false, "iron", false));
    }

    @Test
    public void craftingOnlyHidesComponentsWithoutTheCard() {
        assertFalse(TransferClientFilter.matches(NAMES, false, "", true));
        assertTrue(TransferClientFilter.matches(NAMES, true, "", true));
    }

    @Test
    public void craftingOnlyAndTextMustBothHold() {
        assertTrue(TransferClientFilter.matches(NAMES, true, "iron", true));
        assertFalse(TransferClientFilter.matches(NAMES, true, "copper", true));
        assertFalse(TransferClientFilter.matches(NAMES, false, "iron", true));
    }
}
