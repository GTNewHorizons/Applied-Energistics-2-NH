package appeng.helpers.transfer;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.util.ArrayList;
import java.util.List;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.TransferDirection;
import appeng.api.config.Upgrades;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.features.transfer.TransferMatcher;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IConfigManager;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.parts.p2p.PartP2PInterface;
import appeng.tile.inventory.AppEngInternalAEInventory;

/**
 * An interface is only listed for the stacks it stocks from the network. Everything else it does works through encoded
 * patterns, which this terminal does not cover.
 */
public final class InterfaceInspector implements ITransferInspector<IInterfaceHost> {

    @Override
    public TransferDescription inspect(final IInterfaceHost host) {
        final TransferDescription description = this.describe(host);
        return description == null || description.config.isEmpty() ? null : description;
    }

    /**
     * @return the description of an interface even when nothing is stocked, or null for the output side of a tunnel
     */
    public TransferDescription describe(final IInterfaceHost host) {
        if (host instanceof PartP2PInterface tunnel && tunnel.isOutput()) {
            return null;
        }

        final DualityInterface duality = host.getInterfaceDuality();
        final IConfigManager settings = duality.getConfigManager();
        final int fuzzyCards = duality.getInstalledUpgrades(Upgrades.FUZZY);
        final FuzzyMode fuzzyMode = (FuzzyMode) settings.getSetting(Settings.FUZZY_MODE);

        final TransferDescription.Builder builder = InspectorSupport.withUpgrades(
                TransferDescription.builder(),
                duality.getInventoryByName("upgrades"),
                duality.getInstalledUpgrades(Upgrades.CRAFTING) > 0);

        final AppEngInternalAEInventory config = duality.getConfig();
        final List<Slot> slots = new ArrayList<>();
        for (int index = 0; index < config.getSizeInventory(); index++) {
            final IAEItemStack stack = config.getAEStackInSlot(index);
            if (stack != null) {
                final boolean fuzzy = isFuzzySlot(fuzzyCards, index);
                slots.add(new Slot(stack, fuzzy));
                builder.config(stack, fuzzy);
            }
        }
        builder.canExport(!slots.isEmpty());

        if (fuzzyCards > 0) {
            InspectorSupport.addSetting(builder, settings, Settings.FUZZY_MODE);
            builder.fuzzyMode(fuzzyMode);
        }

        return builder.matcher(new InterfaceMatcher(slots, fuzzyMode)).build();
    }

    /**
     * @return whether fuzzy cards apply to a config slot, they fill up from the last slot
     */
    public static boolean isFuzzySlot(final int fuzzyCards, final int slot) {
        return (fuzzyCards == 1 && slot > 5) || (fuzzyCards == 2 && slot > 2) || fuzzyCards == 3;
    }

    private static final class Slot {

        private final IAEItemStack stack;
        private final boolean fuzzy;

        private Slot(final IAEItemStack stack, final boolean fuzzy) {
            this.stack = stack;
            this.fuzzy = fuzzy;
        }
    }

    private static final class InterfaceMatcher implements TransferMatcher {

        private final List<Slot> slots;
        private final FuzzyMode fuzzyMode;

        private InterfaceMatcher(final List<Slot> slots, final FuzzyMode fuzzyMode) {
            this.slots = slots;
            this.fuzzyMode = fuzzyMode;
        }

        @Override
        public boolean canTransfer(final IAEStack<?> stack, final TransferDirection direction) {
            return direction == TransferDirection.EXPORT && stack != null
                    && stack.getStackType() == ITEM_STACK_TYPE
                    && this.isConfigured(stack);
        }

        private boolean isConfigured(final IAEStack<?> stack) {
            for (final Slot slot : this.slots) {
                if (slot.fuzzy ? slot.stack.fuzzyComparison(stack, this.fuzzyMode)
                        : slot.stack.isSameType((Object) stack)) {
                    return true;
                }
            }
            return false;
        }
    }
}
