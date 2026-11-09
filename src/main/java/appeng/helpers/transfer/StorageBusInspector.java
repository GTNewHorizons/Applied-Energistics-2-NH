package appeng.helpers.transfer;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.List;

import appeng.api.config.AccessRestriction;
import appeng.api.config.ExtractionMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.TransferDirection;
import appeng.api.config.Upgrades;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.features.transfer.TransferMatcher;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IConfigManager;
import appeng.helpers.transfer.TransferFilterBuilder.EmptyPolicy;
import appeng.helpers.transfer.TransferFilterBuilder.Filter;
import appeng.parts.misc.PartStorageBus;

/**
 * Follows the partition rules of {@code PartStorageBus.getInternalHandler}, without touching the live handler.
 */
public final class StorageBusInspector implements ITransferInspector<PartStorageBus> {

    @Override
    public TransferDescription inspect(final PartStorageBus bus) {
        final IConfigManager settings = bus.getConfigManager();
        final IAEStackType<?> type = bus.getStackType();
        final AccessRestriction access = (AccessRestriction) settings.getSetting(Settings.ACCESS);
        final ExtractionMode extraction = (ExtractionMode) settings.getSetting(Settings.EXTRACTION_MODE);
        final FuzzyMode fuzzyMode = (FuzzyMode) settings.getSetting(Settings.FUZZY_MODE);

        final boolean canImport = access.hasPermission(AccessRestriction.READ);
        final boolean canExport = access.hasPermission(AccessRestriction.WRITE);
        final boolean extractFilterActive = access == AccessRestriction.READ
                || (access == AccessRestriction.READ_WRITE && extraction == ExtractionMode.STRICT);

        final boolean fuzzy = type == ITEM_STACK_TYPE && InspectorSupport.has(bus, Upgrades.FUZZY);
        final boolean oreFilter = InspectorSupport.has(bus, Upgrades.ORE_FILTER);
        final boolean sticky = InspectorSupport.has(bus, Upgrades.STICKY);
        final List<IAEStack<?>> config = TransferFilterBuilder.activeConfig(
                bus.getAEInventoryByName(StorageName.CONFIG),
                TransferFilterBuilder.usableWideSlots(bus.getInstalledUpgrades(Upgrades.CAPACITY)),
                type);
        final Filter partition = TransferFilterBuilder.build(
                type,
                config,
                fuzzy,
                fuzzyMode,
                oreFilter,
                bus.getFilter(),
                InspectorSupport.has(bus, Upgrades.INVERTER),
                EmptyPolicy.MATCH_ALL);

        final TransferDescription.Builder builder = InspectorSupport.withUpgrades(
                TransferDescription.builder().storageHandler(true).priority(bus.getPriority()).sticky(sticky)
                        .canImport(canImport).canExport(canExport),
                bus.getInventoryByName("upgrades"),
                InspectorSupport.has(bus, Upgrades.CRAFTING));

        InspectorSupport.addSetting(builder, settings, Settings.ACCESS);
        InspectorSupport.addSetting(builder, settings, Settings.EXTRACTION_MODE);
        InspectorSupport.addSetting(builder, settings, Settings.STORAGE_FILTER);
        if (fuzzy) {
            InspectorSupport.addSetting(builder, settings, Settings.FUZZY_MODE);
            builder.fuzzyMode(fuzzyMode);
        }
        if (sticky) {
            InspectorSupport.addSetting(builder, settings, Settings.STICKY_MODE);
        }

        if (oreFilter) {
            builder.oreFilter(bus.getFilter());
        } else {
            InspectorSupport.addConfig(builder, config, fuzzy);
        }

        return builder.matcher(new StorageBusMatcher(type, partition, canImport, canExport, extractFilterActive))
                .build();
    }

    private static final class StorageBusMatcher implements TransferMatcher {

        private final IAEStackType<?> type;
        private final Filter partition;
        private final boolean canImport;
        private final boolean canExport;
        private final boolean extractFilterActive;

        private StorageBusMatcher(final IAEStackType<?> type, final Filter partition, final boolean canImport,
                final boolean canExport, final boolean extractFilterActive) {
            this.type = type;
            this.partition = partition;
            this.canImport = canImport;
            this.canExport = canExport;
            this.extractFilterActive = extractFilterActive;
        }

        @Override
        public boolean canTransfer(final IAEStack<?> stack, final TransferDirection direction) {
            if (stack == null || stack.getStackType() != this.type) {
                return false;
            }
            if (direction == TransferDirection.EXPORT) {
                return this.canExport && this.partition.test(stack);
            }
            return this.canImport && (!this.extractFilterActive || this.partition.test(stack));
        }

        @Override
        public boolean isExplicitlyListed(final IAEStack<?> stack) {
            return this.partition.isListed(stack);
        }
    }
}
