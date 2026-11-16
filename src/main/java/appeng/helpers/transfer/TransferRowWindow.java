package appeng.helpers.transfer;

/**
 * Works out which part of the Transfer Terminal's results the client needs, the rows on screen plus a page either side
 * so that scrolling usually finds its rows already there.
 */
public final class TransferRowWindow {

    /** Window size that asks for every row. */
    public static final int ALL = Integer.MAX_VALUE;

    private TransferRowWindow() {}

    public static int requestStart(final int firstVisible, final int visibleRows) {
        return Math.max(0, firstVisible - visibleRows);
    }

    public static int requestSize(final int visibleRows) {
        return visibleRows * 3;
    }

    /**
     * @return whether the cached rows hold everything on screen and at least half a page beyond it
     */
    public static boolean covers(final int cachedStart, final int cachedCount, final int firstVisible,
            final int visibleRows, final int total) {
        final int margin = visibleRows / 2;
        final int neededStart = Math.max(0, firstVisible - margin);
        final int neededEnd = Math.min(total, firstVisible + visibleRows + margin);
        return neededStart >= cachedStart && neededEnd <= cachedStart + cachedCount;
    }

    /**
     * @return the first row to send, moved back when the requested window runs past the end of the list
     */
    public static int clampStart(final int requestedStart, final int size, final int total) {
        return Math.max(0, Math.min(requestedStart, total - Math.max(0, size)));
    }

    /**
     * @return the end of the rows to send, exclusive
     */
    public static int clampEnd(final int start, final int size, final int total) {
        return Math.max(0, size) >= total - start ? total : start + Math.max(0, size);
    }
}
