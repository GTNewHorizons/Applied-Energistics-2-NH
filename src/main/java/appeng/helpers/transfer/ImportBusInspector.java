package appeng.helpers.transfer;

import appeng.api.config.Upgrades;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.TransferDescription;
import appeng.helpers.transfer.TransferFilterBuilder.EmptyPolicy;
import appeng.parts.automation.PartBaseImportBus;

/**
 * An import bus with nothing configured imports everything.
 */
@SuppressWarnings("rawtypes")
public final class ImportBusInspector implements ITransferInspector<PartBaseImportBus> {

    @Override
    @SuppressWarnings("unchecked")
    public TransferDescription inspect(final PartBaseImportBus bus) {
        // An oredict card with a filter that does not parse imports nothing, one with an empty filter imports all.
        final boolean brokenOreFilter = bus.getInstalledUpgrades(Upgrades.ORE_FILTER) > 0 && !bus.getFilter().isEmpty();
        final EmptyPolicy emptyPolicy = brokenOreFilter ? EmptyPolicy.MATCH_NONE : EmptyPolicy.MATCH_ALL;

        return InspectorSupport.describeBus(bus).canImport(true)
                .matcher(InspectorSupport.importOnly(InspectorSupport.busFilter(bus, emptyPolicy))).build();
    }
}
