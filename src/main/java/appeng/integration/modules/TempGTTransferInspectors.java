package appeng.integration.modules;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.AEApi;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.ITransferInspectorRegistry;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.features.transfer.TransferMatcher;
import appeng.api.storage.data.IAEStack;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.MTEHatchInputME;

/**
 * TEMPORARY - delete before the pull request, along with its call in {@code Registration.postInit}. These registrations
 * belong in GregTech.
 * <p>
 * Stocking input buses and hatches pull their configured stacks out of the network into a multiblock. The advanced
 * variants can fill their config automatically from whatever the network holds most of, which changes all the time, so
 * a bus in that mode is left out rather than shown with a config that says nothing about what it takes.
 */
public final class TempGTTransferInspectors {

    private TempGTTransferInspectors() {}

    public static void register() {
        final ITransferInspectorRegistry registry = AEApi.instance().registries().transferInspectors();
        registry.register(MTEHatchInputBusME.class, new StockingBusInspector());
        registry.register(MTEHatchInputME.class, new StockingHatchInspector());
    }

    private static final class StockingBusInspector implements ITransferInspector<MTEHatchInputBusME> {

        @Override
        public TransferDescription inspect(final MTEHatchInputBusME bus) {
            if (bus.isAutoPullItemList()) {
                return null;
            }

            final List<IAEStack<?>> config = new ArrayList<>();
            for (int slot = 0; slot < MTEHatchInputBusME.SLOT_COUNT; slot++) {
                final ItemStack stack = bus.getSlotConfig(slot);
                if (stack != null) {
                    config.add(AEItemStack.create(stack));
                }
            }
            return describe(config);
        }
    }

    private static final class StockingHatchInspector implements ITransferInspector<MTEHatchInputME> {

        @Override
        public TransferDescription inspect(final MTEHatchInputME hatch) {
            if (hatch.isAutoPullFluidList()) {
                return null;
            }

            final List<IAEStack<?>> config = new ArrayList<>();
            for (int slot = 0; slot < MTEHatchInputME.SLOT_COUNT; slot++) {
                final FluidStack stack = hatch.getSlotConfig(slot);
                if (stack != null) {
                    config.add(AEFluidStack.create(stack));
                }
            }
            return describe(config);
        }
    }

    private static TransferDescription describe(final List<IAEStack<?>> config) {
        final TransferDescription.Builder builder = TransferDescription.builder().canExport(true);
        for (final IAEStack<?> stack : config) {
            builder.config(stack, false);
        }
        return builder.matcher(TransferMatcher.exportOf(config)).build();
    }
}
