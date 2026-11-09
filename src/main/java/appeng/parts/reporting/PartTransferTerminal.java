package appeng.parts.reporting;

import net.minecraft.item.ItemStack;

import appeng.api.AEApi;
import appeng.api.networking.events.MENetworkBootingStatusChange;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.parts.ITransferTerminal;
import appeng.client.texture.CableBusTextures;
import appeng.helpers.IPrimaryGuiIconProvider;

public class PartTransferTerminal extends AbstractPartDisplay implements ITransferTerminal, IPrimaryGuiIconProvider {

    private static final CableBusTextures FRONT_BRIGHT_ICON = CableBusTextures.PartTransferTerm_Bright;
    private static final CableBusTextures FRONT_DARK_ICON = CableBusTextures.PartTransferTerm_Dark;
    private static final CableBusTextures FRONT_COLORED_ICON = CableBusTextures.PartTransferTerm_Colored;
    private boolean needsUpdate;

    public PartTransferTerminal(final ItemStack is) {
        super(is);
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
    public boolean needsUpdate() {
        final boolean result = this.needsUpdate;
        this.needsUpdate = false;
        return result;
    }

    @MENetworkEventSubscribe
    public void onNetworkBootingChanged(final MENetworkBootingStatusChange event) {
        if (!event.isBooting) {
            this.needsUpdate = true;
        }
    }

    @MENetworkEventSubscribe
    public void onCellArrayChanged(final MENetworkCellArrayUpdate event) {
        this.needsUpdate = true;
    }

    @Override
    public ItemStack getPrimaryGuiIcon() {
        return AEApi.instance().definitions().parts().transferTerminal().maybeStack(1).orNull();
    }
}
