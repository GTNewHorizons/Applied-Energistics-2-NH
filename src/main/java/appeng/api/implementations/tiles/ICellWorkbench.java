package appeng.api.implementations.tiles;

import net.minecraft.inventory.IInventory;

import org.jspecify.annotations.Nullable;

import appeng.api.implementations.IUpgradeableHost;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.api.storage.IConfigInventoryHost;
import appeng.api.storage.KeyConfigInventory;
import appeng.api.storage.data.IAEStackType;
import appeng.helpers.ICellRestriction;
import appeng.helpers.IOreFilterable;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.util.IConfigManagerHost;

public interface ICellWorkbench extends IUpgradeableHost, IAEAppEngInventory, IConfigManagerHost, IOreFilterable,
        ICellRestriction, IConfigInventoryHost {

    ICellWorkbenchItem getCell();

    KeyConfigInventory getConfig();

    boolean isSame(ICellWorkbench cellWorkbench);

    IInventory getCellUpgradeInventory();

    @Nullable
    IAEStackType<?> getStackType();
}
