package appeng.integration.modules.NEIHelpers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import codechicken.nei.BookmarkPanel;
import codechicken.nei.LayoutManager;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PanelWidget;
import codechicken.nei.Widget;

/**
 * Screen rectangles of NEI's buttons and labels, so a side panel can sit clear of them; only call it with NEI loaded.
 */
public final class NEIWidgetBounds {

    // Larger widgets are item grids, whose slots NEI already hides under the panel.
    private static final int MAX_CONTROL_HEIGHT = 40;

    private static Field drawWidgets;
    private static boolean unavailable;

    private NEIWidgetBounds() {}

    /**
     * Returns {x, y, w, h} of every NEI control on screen; empty when NEI is hidden or its layout changed.
     */
    public static List<int[]> controls() {
        final List<int[]> out = new ArrayList<>();
        try {
            if (!NEIClientConfig.isEnabled() || NEIClientConfig.isHidden()) {
                return out;
            }
            for (Widget widget : widgets()) {
                if (widget instanceof PanelWidget<?> panel) {
                    add(out, panel.pagePrev);
                    add(out, panel.pageNext);
                    add(out, panel.pageLabel);
                    if (panel instanceof BookmarkPanel bookmarks) {
                        add(out, bookmarks.namespacePrev);
                        add(out, bookmarks.namespaceNext);
                        add(out, bookmarks.namespaceLabel);
                        add(out, bookmarks.pullBookmarkedItems);
                    }
                } else {
                    add(out, widget);
                }
            }
        } catch (Throwable t) {
            // NEI keeps the active widgets in a private field; if it moves, the panel just stays where it is.
            unavailable = true;
            out.clear();
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Set<Widget> widgets() throws ReflectiveOperationException {
        if (unavailable) {
            return Collections.emptySet();
        }
        if (drawWidgets == null) {
            drawWidgets = LayoutManager.class.getDeclaredField("drawWidgets");
            drawWidgets.setAccessible(true);
        }
        return (Set<Widget>) drawWidgets.get(null);
    }

    private static void add(List<int[]> out, Widget widget) {
        if (widget != null && widget.w > 0 && widget.h > 0 && widget.h <= MAX_CONTROL_HEIGHT) {
            out.add(new int[] { widget.x, widget.y, widget.w, widget.h });
        }
    }
}
