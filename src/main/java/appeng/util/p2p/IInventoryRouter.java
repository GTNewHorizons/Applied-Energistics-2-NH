package appeng.util.p2p;

import net.minecraft.inventory.IInventory;

import org.jspecify.annotations.Nullable;

import appeng.util.target.InventoryTarget;

public interface IInventoryRouter {

    final class SlotView {

        public IInventory inv;
        public int slot;
        public boolean canInsert;
        public boolean canExtract;
    }

    /*
     * Add output P2P tunnel to the router. Can be lazy.
     */
    void addMember(IRoutedTunnel output);

    /*
     * Remove output P2P tunnel from the router. Should immediately invalidate access but layout can be out of date
     */
    void removeMember(IRoutedTunnel output);

    /*
     * Clear all members. Should immediately invalidate access.
     */
    void clearMembers();

    /*
     * Set inventory target for the member. This will typically be the inventory in front of the tunnel. Should
     * immediately invalidate the old target if different from the current one. Size change is notified by returning
     * true. Layout can be out of date.
     */
    boolean setTarget(IRoutedTunnel member, @Nullable InventoryTarget target);

    /*
     * Commit all changes and rebuild the layout. Should notify size changes to the members through onViewSizeChange(),
     * if any.
     */
    void rebuildLayout();

    int getViewSize(IRoutedTunnel viewer);

    int getViewStackLimit(IRoutedTunnel viewer);

    /*
     * Computes the viewer's inventory view. offset determines the start index, used for load balancing. If the slot
     * turns out to be invalid, that should be notified to the tunnel through onInvalidTarget(). Returns true if the
     * slot is available.
     */
    boolean resolve(IRoutedTunnel viewer, int slot, long offset, SlotView out);
}
