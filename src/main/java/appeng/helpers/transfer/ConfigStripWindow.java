package appeng.helpers.transfer;

/**
 * Which part of a component's filter fits on a Transfer Terminal row.
 */
public final class ConfigStripWindow {

    public final int start;
    public final int end;
    public final boolean leftEllipsis;
    public final boolean rightEllipsis;

    private ConfigStripWindow(final int start, final int end, final boolean leftEllipsis, final boolean rightEllipsis) {
        this.start = start;
        this.end = end;
        this.leftEllipsis = leftEllipsis;
        this.rightEllipsis = rightEllipsis;
    }

    /**
     * @param total      number of filter entries
     * @param visible    how many entries fit
     * @param focusIndex entry to keep centered in view, negative to start at the beginning
     * @return the entries from {@code start} up to but excluding {@code end}, and on which sides more are hidden
     */
    public static ConfigStripWindow compute(final int total, final int visible, final int focusIndex) {
        if (total <= visible) {
            return new ConfigStripWindow(0, total, false, false);
        }

        final int centered = focusIndex < 0 ? 0 : focusIndex - visible / 2;
        final int start = Math.max(0, Math.min(centered, total - visible));
        final int end = start + visible;
        return new ConfigStripWindow(start, end, start > 0, end < total);
    }
}
