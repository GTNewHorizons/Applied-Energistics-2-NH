package appeng.helpers.transfer;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.List;

import net.minecraft.inventory.IInventory;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.TransferDirection;
import appeng.api.config.Upgrades;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.features.transfer.TransferMatcher;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.util.IConfigManager;
import appeng.helpers.transfer.TransferFilterBuilder.EmptyPolicy;
import appeng.helpers.transfer.TransferFilterBuilder.Filter;
import appeng.parts.automation.PartSharedItemBus;
import appeng.parts.automation.PartUpgradeable;

/**
 * Pieces shared by the inspectors of AE2's own components.
 */
final class InspectorSupport {

    private InspectorSupport() {}

    static boolean has(final PartUpgradeable part, final Upgrades upgrade) {
        return part.getInstalledUpgrades(upgrade) > 0;
    }

    static void addSetting(final TransferDescription.Builder builder, final IConfigManager settings,
            final Settings setting) {
        if (settings.getSettings().contains(setting)) {
            builder.setting(setting, settings.getSetting(setting));
        }
    }

    static void addConfig(final TransferDescription.Builder builder, final List<IAEStack<?>> config,
            final boolean fuzzy) {
        for (final IAEStack<?> stack : config) {
            builder.config(stack, fuzzy);
        }
    }

    static TransferDescription.Builder withUpgrades(final TransferDescription.Builder builder,
            final IInventory upgrades, final boolean hasCraftingCard) {
        return builder.upgrades(TransferFilterBuilder.installedUpgrades(upgrades)).hasCraftingCard(hasCraftingCard);
    }

    static TransferMatcher importOnly(final Filter filter) {
        return (stack, direction) -> direction == TransferDirection.IMPORT && filter.test(stack);
    }

    static TransferMatcher exportOnly(final Filter filter) {
        return (stack, direction) -> direction == TransferDirection.EXPORT && filter.test(stack);
    }

    /**
     * @return the settings, cards and config of an import or export bus, the direction is left to the caller
     */
    static TransferDescription.Builder describeBus(final PartSharedItemBus<?> bus) {
        final IConfigManager settings = bus.getConfigManager();
        final boolean fuzzy = isBusFuzzy(bus);
        final boolean oreFilter = has(bus, Upgrades.ORE_FILTER);

        final TransferDescription.Builder builder = withUpgrades(
                TransferDescription.builder(),
                bus.getInventoryByName("upgrades"),
                has(bus, Upgrades.CRAFTING));

        if (has(bus, Upgrades.REDSTONE)) {
            addSetting(builder, settings, Settings.REDSTONE_CONTROLLED);
        }
        if (fuzzy) {
            addSetting(builder, settings, Settings.FUZZY_MODE);
            builder.fuzzyMode((FuzzyMode) settings.getSetting(Settings.FUZZY_MODE));
        }
        if (oreFilter) {
            builder.oreFilter(bus.getFilter());
        } else {
            addConfig(builder, busConfig(bus), fuzzy);
        }

        return builder;
    }

    static Filter busFilter(final PartSharedItemBus<?> bus, final EmptyPolicy emptyPolicy) {
        final IAEStackType<?> type = bus.getStackType();
        return TransferFilterBuilder.build(
                type,
                busConfig(bus),
                isBusFuzzy(bus),
                (FuzzyMode) bus.getConfigManager().getSetting(Settings.FUZZY_MODE),
                has(bus, Upgrades.ORE_FILTER),
                bus.getFilter(),
                false,
                emptyPolicy);
    }

    private static boolean isBusFuzzy(final PartSharedItemBus<?> bus) {
        return bus.getStackType() == ITEM_STACK_TYPE && has(bus, Upgrades.FUZZY);
    }

    private static List<IAEStack<?>> busConfig(final PartSharedItemBus<?> bus) {
        return TransferFilterBuilder.activeConfig(
                bus.getAEInventoryByName(StorageName.CONFIG),
                TransferFilterBuilder.usableBusSlots(bus.getInstalledUpgrades(Upgrades.CAPACITY)),
                bus.getStackType());
    }
}
