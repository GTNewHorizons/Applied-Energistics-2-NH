package appeng.api.storage;

import org.jspecify.annotations.NullMarked;

import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.stacks.AEKey;

/**
 * AE's Equivalent to IInventory, used to reading contents, and manipulating contents of ME Inventories.
 * <p>
 * Implementations should COMPLETELY ignore stack size limits from an external view point.
 */
@NullMarked
public interface MEStorage {

    /**
     * Store new items, or simulate the addition of new items into the ME Inventory.
     *
     * @param what   what to insert
     * @param amount how much of it to insert. must not be negative
     * @param mode   action type
     * @return returns the number of items inserted.
     */
    long insert(AEKey what, long amount, Actionable mode, BaseActionSource source);

    /**
     * Extract the specified item from the ME Inventory
     *
     * @param what   what to extract
     * @param amount how much of it to extract (at most)
     * @param mode   simulate, or perform action?
     * @return returns the number of items extracted
     */
    long extract(AEKey what, long amount, Actionable mode, BaseActionSource source);
}
