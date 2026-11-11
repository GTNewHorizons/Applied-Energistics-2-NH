package appeng.parts.reporting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

import appeng.api.AEApi;
import appeng.api.config.Settings;
import appeng.api.config.TransferDirection;
import appeng.api.config.YesNo;
import appeng.api.networking.events.MENetworkBootingStatusChange;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.parts.ITransferTerminal;
import appeng.api.storage.StorageName;
import appeng.api.util.IConfigManager;
import appeng.client.texture.CableBusTextures;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IPrimaryGuiIconProvider;
import appeng.tile.inventory.IAEStackInventory;
import appeng.tile.inventory.IIAEStackInventory;
import appeng.util.ConfigManager;
import appeng.util.IConfigManagerHost;
import appeng.util.Platform;

public class PartTransferTerminal extends AbstractPartDisplay
        implements ITransferTerminal, IPrimaryGuiIconProvider, IConfigManagerHost, IIAEStackInventory {

    private static final CableBusTextures FRONT_BRIGHT_ICON = CableBusTextures.PartTransferTerm_Bright;
    private static final CableBusTextures FRONT_DARK_ICON = CableBusTextures.PartTransferTerm_Dark;
    private static final CableBusTextures FRONT_COLORED_ICON = CableBusTextures.PartTransferTerm_Colored;
    private static final String QUERY_TAG = "transferQuery";
    private static final String SEARCH_TAG = "transferSearch";

    private final IConfigManager settings = new ConfigManager(this);
    private final IAEStackInventory query = new IAEStackInventory(this, 1);
    private String searchText = "";
    private long changeCount;

    public PartTransferTerminal(final ItemStack is) {
        super(is);

        this.settings.registerSetting(Settings.TRANSFER_DIRECTION, TransferDirection.BOTH);
        this.settings.registerSetting(Settings.TRANSFER_CRAFTING_CARD_ONLY, YesNo.NO);
        this.settings.registerSetting(Settings.TRANSFER_HIDE_UNFILTERED, YesNo.NO);
        this.settings.registerSetting(Settings.TRANSFER_NETWORK_ONLY, YesNo.NO);
        this.settings.registerSetting(Settings.TRANSFER_SIMULATE, YesNo.NO);
    }

    @Override
    public boolean onPartActivate(final EntityPlayer player, final Vec3 pos) {
        if (super.onPartActivate(player, pos) || player.isSneaking()) {
            return false;
        }
        if (Platform.isServer()) {
            Platform.openGUI(player, this.getHost().getTile(), this.getSide(), GuiBridge.GUI_TRANSFER_TERMINAL);
        }
        return true;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.settings.readFromNBT(data);
        this.query.readFromNBT(data, QUERY_TAG);
        this.searchText = data.getString(SEARCH_TAG);
    }

    @Override
    public void writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        this.settings.writeToNBT(data);
        this.query.writeToNBT(data, QUERY_TAG);
        if (this.searchText.isEmpty()) {
            data.removeTag(SEARCH_TAG);
        } else {
            data.setString(SEARCH_TAG, this.searchText);
        }
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.settings;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void updateSetting(final IConfigManager manager, final Enum settingName, final Enum newValue) {
        this.save();
    }

    /**
     * Loading the settings from NBT reports each one as changed, which can happen before the part has a host.
     */
    private void save() {
        if (this.getHost() != null) {
            this.saveChanges();
        }
    }

    @Override
    public IAEStackInventory getQueryInventory() {
        return this.query;
    }

    @Override
    public void saveAEStackInv() {
        this.save();
    }

    @Override
    public IAEStackInventory getAEInventoryByName(final StorageName name) {
        return this.query;
    }

    @Override
    public String getSearchText() {
        return this.searchText;
    }

    @Override
    public void setSearchText(final String text) {
        final String value = text == null ? "" : text;
        if (!value.equals(this.searchText)) {
            this.searchText = value;
            this.save();
        }
    }

    @Override
    public CableBusTextures getFrontBright() {
        return FRONT_BRIGHT_ICON;
    }

    @Override
    public CableBusTextures getFrontColored() {
        return FRONT_COLORED_ICON;
    }

    @Override
    public CableBusTextures getFrontDark() {
        return FRONT_DARK_ICON;
    }

    @Override
    public long getChangeCount() {
        return this.changeCount;
    }

    @MENetworkEventSubscribe
    public void onNetworkBootingChanged(final MENetworkBootingStatusChange event) {
        if (!event.isBooting) {
            this.changeCount++;
        }
    }

    @MENetworkEventSubscribe
    public void onCellArrayChanged(final MENetworkCellArrayUpdate event) {
        this.changeCount++;
    }

    @Override
    public ItemStack getPrimaryGuiIcon() {
        return AEApi.instance().definitions().parts().transferTerminal().maybeStack(1).orNull();
    }
}
