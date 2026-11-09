package appeng.me.storage;

import java.util.function.Function;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.MEStorage;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;

/**
 * Exposes stack-type based {@link IMEInventory}s as a {@link MEStorage}. Temporary bridge until the storage system is
 * migrated to keys.
 */
@NullMarked
public final class LegacyInventoryStorage implements MEStorage {

    private final Function<IAEStackType<?>, @Nullable IMEInventory<?>> inventories;

    /**
     * @param inventories returns the inventory for a stack type, or null if that type is not available
     */
    public LegacyInventoryStorage(Function<IAEStackType<?>, @Nullable IMEInventory<?>> inventories) {
        this.inventories = inventories;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public long insert(AEKey what, long amount, Actionable mode, BaseActionSource source) {
        if (amount <= 0) {
            return 0;
        }
        final IAEStack<?> stack = what.toAEStack(amount);
        final IMEInventory inventory = this.inventories.apply(stack.getStackType());
        if (inventory == null) {
            return 0;
        }
        final IAEStack<?> leftover = (IAEStack<?>) inventory.injectItems(stack, mode, source);
        return leftover == null ? amount : amount - leftover.getStackSize();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public long extract(AEKey what, long amount, Actionable mode, BaseActionSource source) {
        if (amount <= 0) {
            return 0;
        }
        final IAEStack<?> stack = what.toAEStack(amount);
        final IMEInventory inventory = this.inventories.apply(stack.getStackType());
        if (inventory == null) {
            return 0;
        }
        final IAEStack<?> extracted = (IAEStack<?>) inventory.extractItems(stack, mode, source);
        return extracted == null ? 0 : extracted.getStackSize();
    }
}
