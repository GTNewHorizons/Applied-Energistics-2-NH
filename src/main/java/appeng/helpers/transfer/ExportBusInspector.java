package appeng.helpers.transfer;

import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.TransferDescription;
import appeng.helpers.transfer.TransferFilterBuilder.EmptyPolicy;
import appeng.parts.automation.PartBaseExportBus;

/**
 * An export bus with nothing configured exports nothing.
 */
@SuppressWarnings("rawtypes")
public final class ExportBusInspector implements ITransferInspector<PartBaseExportBus> {

    @Override
    @SuppressWarnings("unchecked")
    public TransferDescription inspect(final PartBaseExportBus bus) {
        final TransferDescription.Builder builder = InspectorSupport.describeBus(bus).canExport(true)
                .matcher(InspectorSupport.exportOnly(InspectorSupport.busFilter(bus, EmptyPolicy.MATCH_NONE)));

        if (bus.getInstalledUpgrades(Upgrades.CRAFTING) > 0) {
            InspectorSupport.addSetting(builder, bus.getConfigManager(), Settings.CRAFT_ONLY);
        }
        if (bus.getInstalledUpgrades(Upgrades.CAPACITY) > 0) {
            InspectorSupport.addSetting(builder, bus.getConfigManager(), Settings.SCHEDULING_MODE);
        }

        return builder.build();
    }
}
