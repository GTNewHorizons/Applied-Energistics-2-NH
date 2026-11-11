package appeng.helpers.transfer;

import java.util.List;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.TransferDirection;
import appeng.api.config.Upgrades;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.features.transfer.TransferMatcher;
import appeng.api.storage.ICellProvider;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IConfigManager;
import appeng.helpers.transfer.TransferFilterBuilder.EmptyPolicy;
import appeng.helpers.transfer.TransferFilterBuilder.Filter;
import appeng.parts.automation.PartBaseFormationPlane;

/**
 * A formation plane takes stacks out of the network, nothing configured means it takes everything.
 */
public final class FormationPlaneInspector implements ITransferInspector<PartBaseFormationPlane> {

    @Override
    public TransferDescription inspect(final PartBaseFormationPlane plane) {
        final IConfigManager settings = plane.getConfigManager();
        // Only the item plane registers a fuzzy mode, asking a plane without one throws.
        final boolean hasFuzzyMode = settings.getSettings().contains(Settings.FUZZY_MODE);
        final FuzzyMode fuzzyMode = hasFuzzyMode ? (FuzzyMode) settings.getSetting(Settings.FUZZY_MODE)
                : FuzzyMode.IGNORE_ALL;
        final boolean fuzzy = hasFuzzyMode && plane.supportFuzzy() && InspectorSupport.has(plane, Upgrades.FUZZY);
        final List<IAEStack<?>> config = TransferFilterBuilder.activeConfig(
                plane.getAEInventoryByName(StorageName.CONFIG),
                TransferFilterBuilder.usableWideSlots(plane.getInstalledUpgrades(Upgrades.CAPACITY)),
                plane.getStackType());
        final Filter filter = TransferFilterBuilder.build(
                plane.getStackType(),
                config,
                fuzzy,
                fuzzyMode,
                false,
                "",
                InspectorSupport.has(plane, Upgrades.INVERTER),
                EmptyPolicy.MATCH_ALL);

        final TransferDescription.Builder builder = InspectorSupport.withUpgrades(
                // A plane that is a cell provider is storage AE routes into, others pull stacks out by themselves.
                TransferDescription.builder().canExport(true).storageHandler(plane instanceof ICellProvider)
                        .priority(plane.getPriority()),
                plane.getInventoryByName("upgrades"),
                false);
        InspectorSupport.addConfig(builder, config, fuzzy);
        InspectorSupport.addSetting(builder, settings, Settings.PLACE_BLOCK);
        if (fuzzy) {
            InspectorSupport.addSetting(builder, settings, Settings.FUZZY_MODE);
            builder.fuzzyMode(fuzzyMode);
        }

        return builder.matcher(new TransferMatcher() {

            @Override
            public boolean canTransfer(final IAEStack<?> stack, final TransferDirection direction) {
                return direction == TransferDirection.EXPORT && filter.test(stack);
            }

            @Override
            public boolean isExplicitlyListed(final IAEStack<?> stack) {
                return filter.isListed(stack);
            }
        }).build();
    }
}
