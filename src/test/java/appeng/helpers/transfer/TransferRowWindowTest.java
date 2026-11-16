package appeng.helpers.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TransferRowWindowTest {

    @Test
    public void requestIncludesAPageEitherSide() {
        assertEquals(7, TransferRowWindow.requestStart(10, 3));
        assertEquals(9, TransferRowWindow.requestSize(3));
    }

    @Test
    public void requestDoesNotStartBeforeTheList() {
        assertEquals(0, TransferRowWindow.requestStart(1, 3));
    }

    @Test
    public void scrollingInsideTheBufferNeedsNothingNew() {
        assertTrue(TransferRowWindow.covers(7, 9, 10, 3, 100));
        assertTrue(TransferRowWindow.covers(7, 9, 12, 3, 100));
    }

    @Test
    public void nearingTheEdgeOfTheBufferNeedsNewRows() {
        assertFalse(TransferRowWindow.covers(7, 9, 13, 3, 100));
        assertFalse(TransferRowWindow.covers(7, 9, 7, 3, 100));
    }

    @Test
    public void theEndsOfTheListNeedNoMargin() {
        assertTrue(TransferRowWindow.covers(0, 9, 0, 3, 100));
        assertTrue(TransferRowWindow.covers(91, 9, 97, 3, 100));
    }

    @Test
    public void anEmptyListIsAlwaysCovered() {
        assertTrue(TransferRowWindow.covers(0, 0, 0, 3, 0));
    }

    @Test
    public void windowPastTheEndMovesBack() {
        assertEquals(91, TransferRowWindow.clampStart(95, 9, 100));
        assertEquals(100, TransferRowWindow.clampEnd(91, 9, 100));
    }

    @Test
    public void wholeListRequestDoesNotOverflow() {
        assertEquals(0, TransferRowWindow.clampStart(0, TransferRowWindow.ALL, 100));
        assertEquals(100, TransferRowWindow.clampEnd(0, TransferRowWindow.ALL, 100));
    }

    @Test
    public void invalidRequestsStayInsideTheList() {
        assertEquals(0, TransferRowWindow.clampStart(-5, 9, 100));
        assertEquals(0, TransferRowWindow.clampEnd(0, -9, 100));
        assertEquals(0, TransferRowWindow.clampStart(0, 9, 0));
        assertEquals(0, TransferRowWindow.clampEnd(0, 9, 0));
    }
}
