package appeng.util.p2p;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface IInventoryRouter<T> {

    final class SlotView<T> {

        public @Nullable T provider;
        public int size;
        public int slot;
        public boolean canInsert;
        public boolean canExtract;
    }

    /*
     * Add output P2P tunnel to the router. No need to rebuild the layout.
     */
    void addMember(T output);

    /*
     * Remove output P2P tunnel from the router. No need to rebuild the layout but should immediately invalidate access.
     */
    void removeMember(T output);

    /*
     * Rebuild the layout. Should give view sizes to the members through setViewSize().
     */
    void rebuildLayout();

    int getViewStackLimit(T viewer);

    /*
     * Computes the viewer's inventory view. offset determines the start index, used for load balancing. Returns true if
     * succeeds.
     */
    boolean resolve(T viewer, int slot, long offset, SlotView<T> out);
}
