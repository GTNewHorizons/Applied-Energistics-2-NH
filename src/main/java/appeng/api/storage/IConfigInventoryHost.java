package appeng.api.storage;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface IConfigInventoryHost {

    /**
     * Called on both sides after a slot of one of the host's config inventories changed. Implementations that only act
     * on the server have to check the side themselves.
     */
    void onChangeConfigInventory(ConfigInventory inv, int slot);

    @Nullable
    ConfigInventory getAEInventoryByName(StorageName name);
}
