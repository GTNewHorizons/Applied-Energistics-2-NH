package appeng.helpers.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ConfigStripWindowTest {

    private static final int VISIBLE = 6;

    private static ConfigStripWindow window(final int total, final int focus) {
        return ConfigStripWindow.compute(total, VISIBLE, focus);
    }

    @Test
    public void shortFiltersShowEverythingWithoutEllipsis() {
        final ConfigStripWindow window = window(4, 2);

        assertEquals(0, window.start);
        assertEquals(4, window.end);
        assertFalse(window.leftEllipsis);
        assertFalse(window.rightEllipsis);
    }

    @Test
    public void exactFitNeedsNoEllipsis() {
        final ConfigStripWindow window = window(VISIBLE, 5);

        assertFalse(window.leftEllipsis);
        assertFalse(window.rightEllipsis);
    }

    @Test
    public void withoutFocusStartsAtTheBeginning() {
        final ConfigStripWindow window = window(20, -1);

        assertEquals(0, window.start);
        assertEquals(VISIBLE, window.end);
        assertFalse(window.leftEllipsis);
        assertTrue(window.rightEllipsis);
    }

    @Test
    public void focusNearTheStartHasOneEllipsis() {
        final ConfigStripWindow window = window(20, 1);

        assertEquals(0, window.start);
        assertFalse(window.leftEllipsis);
        assertTrue(window.rightEllipsis);
    }

    @Test
    public void focusNearTheEndHasOneEllipsis() {
        final ConfigStripWindow window = window(20, 19);

        assertEquals(14, window.start);
        assertEquals(20, window.end);
        assertTrue(window.leftEllipsis);
        assertFalse(window.rightEllipsis);
    }

    @Test
    public void focusInTheMiddleHasEllipsisOnBothSides() {
        final ConfigStripWindow window = window(20, 10);

        assertEquals(7, window.start);
        assertEquals(13, window.end);
        assertTrue(window.leftEllipsis);
        assertTrue(window.rightEllipsis);
    }

    @Test
    public void focusedEntryIsAlwaysInsideTheWindow() {
        for (int focus = 0; focus < 63; focus++) {
            final ConfigStripWindow window = ConfigStripWindow.compute(63, VISIBLE, focus);

            assertTrue(focus >= window.start && focus < window.end);
        }
    }
}
