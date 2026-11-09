package appeng.helpers.transfer;

import appeng.api.features.transfer.ITransferInspectorRegistry;
import appeng.core.features.registries.TransferInspectorRegistry;
import appeng.parts.automation.PartBaseExportBus;
import appeng.parts.automation.PartBaseImportBus;
import appeng.parts.misc.PartStorageBus;

/**
 * Registers the inspectors for AE2's own components. Subclasses from other mods inherit them.
 */
public final class CoreTransferInspectors {

    private CoreTransferInspectors() {}

    public static ITransferInspectorRegistry createRegistry() {
        final ITransferInspectorRegistry registry = new TransferInspectorRegistry();
        registry.register(PartBaseImportBus.class, new ImportBusInspector());
        registry.register(PartBaseExportBus.class, new ExportBusInspector());
        registry.register(PartStorageBus.class, new StorageBusInspector());
        return registry;
    }
}
