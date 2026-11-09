package appeng.helpers.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public final class TransferPacketChunksTest {

    private static byte[] row(final int size) {
        return new byte[size];
    }

    @Test
    public void emptyResultStillProducesOneChunk() {
        final List<List<byte[]>> chunks = TransferPacketChunks.split(Collections.emptyList(), 10);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).isEmpty());
    }

    @Test
    public void rowsThatFitStayTogether() {
        assertEquals(1, TransferPacketChunks.split(Arrays.asList(row(4), row(6)), 10).size());
    }

    @Test
    public void overflowStartsANewChunkKeepingOrder() {
        final byte[] first = row(6);
        final byte[] second = row(6);
        final byte[] third = row(3);

        final List<List<byte[]>> chunks = TransferPacketChunks.split(Arrays.asList(first, second, third), 10);

        assertEquals(2, chunks.size());
        assertSame(first, chunks.get(0).get(0));
        assertSame(second, chunks.get(1).get(0));
        assertSame(third, chunks.get(1).get(1));
    }

    @Test
    public void oversizedRowGetsItsOwnChunk() {
        final List<List<byte[]>> chunks = TransferPacketChunks.split(Arrays.asList(row(50), row(1)), 10);

        assertEquals(2, chunks.size());
        assertEquals(1, chunks.get(0).size());
    }
}
