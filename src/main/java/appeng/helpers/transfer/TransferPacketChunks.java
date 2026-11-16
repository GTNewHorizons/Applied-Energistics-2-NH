package appeng.helpers.transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits serialized rows so that large networks do not exceed the size of a single packet.
 */
public final class TransferPacketChunks {

    private TransferPacketChunks() {}

    /**
     * @param rows     serialized rows
     * @param maxBytes largest chunk, a row bigger than this gets a chunk of its own
     * @return rows in their original order, always at least one chunk so the client hears about an empty result
     */
    public static List<List<byte[]>> split(final List<byte[]> rows, final int maxBytes) {
        final List<List<byte[]>> chunks = new ArrayList<>();
        List<byte[]> current = new ArrayList<>();
        int currentBytes = 0;

        for (final byte[] row : rows) {
            if (!current.isEmpty() && currentBytes + row.length > maxBytes) {
                chunks.add(current);
                current = new ArrayList<>();
                currentBytes = 0;
            }
            current.add(row);
            currentBytes += row.length;
        }

        if (!current.isEmpty()) {
            chunks.add(current);
        }
        return chunks.isEmpty() ? Collections.singletonList(Collections.emptyList()) : chunks;
    }
}
