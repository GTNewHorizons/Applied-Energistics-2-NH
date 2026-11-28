package appeng.helpers;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.Nullable;

import appeng.api.config.CopyMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.implementations.tiles.ICellWorkbench;
import appeng.api.stacks.AEKey;
import appeng.api.storage.ICellWorkbenchItem;
import appeng.api.storage.KeyConfigInventory;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IConfigManager;
import appeng.helpers.ICellRestriction.CellData;
import appeng.helpers.ICellRestriction.CellRestrictionData;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.ConfigManager;
import appeng.util.Platform;

public class CellWorkbenchState {

    private static final String CELL_TAG = "cell";
    private static final String CONFIG_TAG = "config";
    private static final String RESTRICTION_TYPES_TAG = "cellRestrictTypes";
    private static final String RESTRICTION_AMOUNT_TAG = "cellRestrictAmount";

    private final ICellWorkbench host;
    private final AppEngInternalInventory cell;
    private final KeyConfigInventory config;
    private final ConfigManager manager;

    private IInventory cachedUpgrades;
    private @Nullable KeyConfigInventory cachedConfig;
    private boolean updatingCell;
    private long cellRestrictAmount;
    private byte cellRestrictTypes;
    @Nullable
    private IAEStackType<?> stackType;
    @Nullable
    private IAEStackType<?> previousStackType;

    public CellWorkbenchState(ICellWorkbench host) {
        this.host = host;
        this.cell = new AppEngInternalInventory(host, 1);
        this.config = new KeyConfigInventory(host, 63);
        this.manager = new ConfigManager((manager, settingName, newValue) -> {
            if (!this.updatingCell) {
                this.host.updateSetting(manager, settingName, newValue);
            }
        });
        this.manager.registerSetting(Settings.COPY_MODE, CopyMode.CLEAR_ON_REMOVE);
        this.cell.setEnableClientEvents(true);
    }

    public IInventory getCellInventory() {
        return this.cell;
    }

    public ICellWorkbenchItem getCell() {
        ItemStack stack = this.cell.getStackInSlot(0);
        return stack != null && stack.getItem() instanceof ICellWorkbenchItem cellWorkbenchItem ? cellWorkbenchItem
                : null;
    }

    public IInventory getCellUpgradeInventory() {
        if (this.cachedUpgrades == null) {
            ICellWorkbenchItem cellWorkbenchItem = this.getCell();
            ItemStack stack = this.cell.getStackInSlot(0);
            if (cellWorkbenchItem == null || stack == null) {
                return null;
            }
            this.cachedUpgrades = cellWorkbenchItem.getUpgradesInventory(stack);
        }
        return this.cachedUpgrades;
    }

    public int getInstalledUpgrades(Upgrades upgrade) {
        IInventory upgrades = this.getCellUpgradeInventory();
        if (upgrades == null) {
            return 0;
        }
        for (int slot = 0; slot < upgrades.getSizeInventory(); slot++) {
            ItemStack stack = upgrades.getStackInSlot(slot);
            if (stack != null && stack.getItem() instanceof IUpgradeModule upgradeModule
                    && upgradeModule.getType(stack) == upgrade) {
                return 1;
            }
        }
        return 0;
    }

    public IConfigManager getConfigManager() {
        return this.manager;
    }

    public void writeToNBT(NBTTagCompound data) {
        this.cell.writeToNBT(data, CELL_TAG);
        this.config.writeToNBT(data, CONFIG_TAG);
        this.manager.writeToNBT(data);
        data.setByte(RESTRICTION_TYPES_TAG, this.cellRestrictTypes);
        data.setLong(RESTRICTION_AMOUNT_TAG, this.cellRestrictAmount);
    }

    public void readFromNBT(NBTTagCompound data) {
        this.updatingCell = true;
        try {
            this.cachedUpgrades = null;
            this.cachedConfig = null;
            this.cell.readFromNBT(data, CELL_TAG);
            this.config.readFromNBT(data, CONFIG_TAG);
            this.manager.readFromNBT(data);
            this.cellRestrictTypes = data.getByte(RESTRICTION_TYPES_TAG);
            this.cellRestrictAmount = data.getLong(RESTRICTION_AMOUNT_TAG);
            this.updateStackType();
            this.previousStackType = this.stackType;
            if (this.previousStackType == null) {
                for (int slot = 0; slot < this.config.getSizeInventory(); slot++) {
                    AEKey key = this.config.getKey(slot);
                    if (key != null) {
                        this.previousStackType = key.toAEStack().getStackType();
                        break;
                    }
                }
            }
        } finally {
            this.updatingCell = false;
        }
    }

    public void onChangeInventory(IInventory inventory, InvOperation operation) {
        if (operation == InvOperation.markDirty || inventory != this.cell || this.updatingCell) {
            return;
        }

        this.cachedUpgrades = null;
        this.cachedConfig = null;
        if (Platform.isClient()) {
            this.updateStackType();
            return;
        }

        this.updatingCell = true;
        try {
            ItemStack stack = this.cell.getStackInSlot(0);
            if (this.manager.getSetting(Settings.COPY_MODE) == CopyMode.KEEP_ON_REMOVE && stack != null
                    && stack.getItem() instanceof ICellRestriction cellRestriction) {
                cellRestriction.setCellRestriction(stack, this.getCopiedRestriction(stack));
            }

            if (this.updateStackType()) {
                if (this.stackType != this.previousStackType) {
                    this.clearConfig();
                }
                this.previousStackType = this.stackType;
            }

            KeyConfigInventory cellConfig = this.getCellConfigInventory();
            if (cellConfig != null) {
                if (!cellConfig.isEmpty()) {
                    this.copyConfig(cellConfig, this.config);
                } else {
                    this.copyConfig(this.config, cellConfig);
                }
            } else if (this.manager.getSetting(Settings.COPY_MODE) == CopyMode.CLEAR_ON_REMOVE) {
                this.clearConfig();
            }
        } finally {
            this.updatingCell = false;
        }
    }

    public void onChangeConfigInventory(int slot) {
        if (this.updatingCell || Platform.isClient()) {
            return;
        }
        KeyConfigInventory cellConfig = this.getCellConfigInventory();
        if (cellConfig != null && slot < cellConfig.getSizeInventory()) {
            cellConfig.setKey(slot, this.config.getKey(slot));
        }
        this.host.saveChanges();
    }

    public KeyConfigInventory getConfig() {
        return this.config;
    }

    public String getFilter() {
        ItemStack stack = this.cell.getStackInSlot(0);
        return stack != null && stack.getItem() instanceof ICellWorkbenchItem cellWorkbenchItem
                ? cellWorkbenchItem.getOreFilter(stack)
                : "";
    }

    public void setFilter(String filter) {
        ItemStack stack = this.cell.getStackInSlot(0);
        if (stack != null && stack.getItem() instanceof ICellWorkbenchItem cellWorkbenchItem) {
            cellWorkbenchItem.setOreFilter(stack, filter);
        }
    }

    @Nullable
    public CellData getCellData() {
        ItemStack stack = this.cell.getStackInSlot(0);
        return stack != null && stack.getItem() instanceof ICellRestriction cellRestriction
                ? cellRestriction.getCellData(stack)
                : null;
    }

    @Nullable
    public CellRestrictionData getCellRestrictionData() {
        ItemStack stack = this.cell.getStackInSlot(0);
        return stack != null && stack.getItem() instanceof ICellRestriction cellRestriction
                ? cellRestriction.getCellRestrictionData(stack)
                : null;
    }

    public void setCellRestriction(CellRestrictionData restriction) {
        if (restriction.isReset() || this.manager.getSetting(Settings.COPY_MODE) == CopyMode.KEEP_ON_REMOVE) {
            this.cellRestrictTypes = restriction.restrictionTypes;
            this.cellRestrictAmount = restriction.restrictionAmount;
        }
        ItemStack stack = this.cell.getStackInSlot(0);
        if (stack != null && stack.getItem() instanceof ICellRestriction cellRestriction) {
            cellRestriction.setCellRestriction(stack, restriction);
        }
    }

    @Nullable
    public IAEStackType<?> getStackType() {
        return this.stackType;
    }

    private @Nullable KeyConfigInventory getCellConfigInventory() {
        if (this.cachedConfig == null) {
            ICellWorkbenchItem cellWorkbenchItem = this.getCell();
            ItemStack stack = this.cell.getStackInSlot(0);
            if (cellWorkbenchItem == null || stack == null) {
                return null;
            }
            this.cachedConfig = cellWorkbenchItem.getConfigAEInventory(stack);
        }
        return this.cachedConfig;
    }

    private boolean updateStackType() {
        ItemStack stack = this.cell.getStackInSlot(0);
        if (stack != null && stack.getItem() instanceof ICellWorkbenchItem cellWorkbenchItem) {
            this.stackType = cellWorkbenchItem.getStackType();
            return true;
        }
        this.stackType = null;
        return false;
    }

    private void clearConfig() {
        for (int slot = 0; slot < this.config.getSizeInventory(); slot++) {
            this.config.setKey(slot, null);
        }
    }

    private void copyConfig(KeyConfigInventory source, KeyConfigInventory destination) {
        for (int slot = 0; slot < destination.getSizeInventory(); slot++) {
            destination.setKey(slot, slot < source.getSizeInventory() ? source.getKey(slot) : null);
        }
    }

    private CellRestrictionData getCopiedRestriction(ItemStack stack) {
        if (stack.getItem() instanceof IStorageCell storageCell) {
            int totalTypes = storageCell.getTotalTypes(stack);
            long effectiveTypes = this.cellRestrictTypes == 0 ? totalTypes
                    : Math.min(this.cellRestrictTypes, totalTypes);
            long maxAmount = (storageCell.getBytesLong(stack)
                    - (long) storageCell.getBytesPerType(stack) * effectiveTypes)
                    * storageCell.getStackType().getAmountPerByte();
            return new CellRestrictionData(
                    (byte) Math.min(this.cellRestrictTypes, totalTypes),
                    Math.min(this.cellRestrictAmount, maxAmount));
        }
        return new CellRestrictionData(this.cellRestrictTypes, this.cellRestrictAmount);
    }
}
