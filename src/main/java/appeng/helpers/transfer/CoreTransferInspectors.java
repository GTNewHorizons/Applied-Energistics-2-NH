package appeng.helpers.transfer;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import appeng.api.features.transfer.ITransferInspectorRegistry;
import appeng.core.features.registries.TransferInspectorRegistry;
import appeng.helpers.IInterfaceHost;
import appeng.parts.automation.PartAnnihilationPlane;
import appeng.parts.automation.PartBaseExportBus;
import appeng.parts.automation.PartBaseFormationPlane;
import appeng.parts.automation.PartBaseImportBus;
import appeng.parts.misc.PartStorageBus;
import appeng.tile.misc.TileSuperMEReplenisher;
import appeng.tile.storage.TileChest;
import appeng.tile.storage.TileDrive;

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
        registry.register(IInterfaceHost.class, new InterfaceInspector());
        registry.register(PartBaseFormationPlane.class, new FormationPlaneInspector());
        registry.register(PartAnnihilationPlane.class, new AnnihilationPlaneInspector(() -> ITEM_STACK_TYPE));
        registry.register(TileSuperMEReplenisher.class, new SuperReplenisherInspector());
        registry.register(TileDrive.class, new DriveInspector());
        registry.register(TileChest.class, new ChestInspector());
        return registry;
    }
}
