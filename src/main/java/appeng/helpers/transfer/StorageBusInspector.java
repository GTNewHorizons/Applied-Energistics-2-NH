package appeng.helpers.transfer;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.List;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.ExtractionMode;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.TransferDirection;
import appeng.api.config.Upgrades;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.features.transfer.TransferMatcher;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IConfigManager;
import appeng.helpers.transfer.TransferFilterBuilder.EmptyPolicy;
import appeng.helpers.transfer.TransferFilterBuilder.Filter;
import appeng.me.storage.MEInventoryHandler;
import appeng.parts.misc.PartStorageBus;

/**
 * Follows the partition rules of {@code PartStorageBus.getInternalHandler}, without touching the live handler. Only the
 * sticky check asks the live handler, since whether the bus already holds a stack is only known there.
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

        // The sticky card shows with the other cards, it has no setting of its own to draw.
        InspectorSupport.addSetting(builder, settings, Settings.ACCESS);
        InspectorSupport.addSetting(builder, settings, Settings.EXTRACTION_MODE);
        InspectorSupport.addSetting(builder, settings, Settings.STORAGE_FILTER);
        if (fuzzy) {
            InspectorSupport.addSetting(builder, settings, Settings.FUZZY_MODE);
            builder.fuzzyMode(fuzzyMode);
        }

        if (oreFilter) {
            builder.oreFilter(bus.getFilter());
        } else {
            InspectorSupport.addConfig(builder, config, fuzzy);
        }

        return builder.matcher(
                new StorageBusMatcher(type, partition, canImport, canExport, extractFilterActive, sticky ? bus : null))
                .build();
    }

    private static final class StorageBusMatcher implements TransferMatcher {

        private final IAEStackType<?> type;
        private final Filter partition;
        private final boolean canImport;
        private final boolean canExport;
        private final boolean extractFilterActive;

        /** The bus, if it has a sticky card. */
        private final PartStorageBus stickyBus;

        private StorageBusMatcher(final IAEStackType<?> type, final Filter partition, final boolean canImport,
                final boolean canExport, final boolean extractFilterActive, final PartStorageBus stickyBus) {
            this.type = type;
            this.partition = partition;
            this.canImport = canImport;
            this.canExport = canExport;
            this.extractFilterActive = extractFilterActive;
            this.stickyBus = stickyBus;
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

        /**
         * Asks the live handler the same question as the sticky pass of {@code NetworkInventoryHandler.injectItems}:
         * the stack has to be accepted, and either whitelisted or already extractable. A bus without read access can
         * therefore only claim stacks through its whitelist.
         */
        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public boolean capturesAsSticky(final IAEStack<?> stack) {
            if (this.stickyBus == null || stack == null || stack.getStackType() != this.type) {
                return false;
            }

            // AE leaves a bus without power or channel out of its storage, see PartStorageBus.getCellArray.
            if (!this.stickyBus.getProxy().isActive()) {
                return false;
            }

            try {
                final MEInventoryHandler handler = this.stickyBus.getInternalHandler();
                if (handler == null || !handler.getSticky()) {
                    return false;
                }

                final IAEStack probe = stack.copy();
                probe.setStackSize(1);
                if (!handler.canAccept(probe)) {
                    return false;
                }
                return handler.isPrioritized(probe)
                        || handler.extractItems(probe, Actionable.SIMULATE, new MachineSource(this.stickyBus)) != null;
            } catch (final RuntimeException e) {
                // The bus may face an inventory from another mod, which must not take the terminal down.
                return false;
            }
        }
    }
}
