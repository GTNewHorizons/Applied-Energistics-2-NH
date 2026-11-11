package appeng.integration.modules;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;

import java.util.ArrayList;
import java.util.List;

import com.glodblock.github.common.parts.PartFluidAnnihilationPlane;
import com.glodblock.github.common.parts.PartFluidFormationPlane;
import com.glodblock.github.common.parts.PartFluidInterface;
import com.glodblock.github.common.parts.PartFluidP2PInterface;
import com.glodblock.github.common.tile.TileFluidInterface;
import com.glodblock.github.common.tile.TileSuperStockReplenisher;
import com.glodblock.github.inventory.IDualHost;
import com.glodblock.github.util.Util;

import appeng.api.AEApi;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.ITransferInspectorRegistry;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.features.transfer.TransferMatcher;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.transfer.AnnihilationPlaneInspector;
import appeng.helpers.transfer.InterfaceInspector;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.tile.inventory.IAEStackInventory;

/**
 * TEMPORARY - delete before the pull request, along with its call in {@code Registration.postInit} and
 * {@code TempAddonTransferInspectorTests}. These registrations belong in AE2FluidCraft.
 * <p>
 * AE2FC's buses and storage bus need nothing, they inherit AE2's inspectors. Registered here are its components that
 * store filters AE2 does not know about, and its formation plane, which works differently from AE2's. Thaumic
 * Energistics needs nothing at all.
 */
public final class TempAddonTransferInspectors {

    private TempAddonTransferInspectors() {}

    public static void register() {
        final ITransferInspectorRegistry registry = AEApi.instance().registries().transferInspectors();
        final DualInterfaceInspector dualInterface = new DualInterfaceInspector();

        registry.register(PartFluidInterface.class, dualInterface);
        registry.register(TileFluidInterface.class, dualInterface);
        registry.register(PartFluidP2PInterface.class, dualInterface);
        registry.register(TileSuperStockReplenisher.class, new StockReplenisherInspector());
        registry.register(PartFluidAnnihilationPlane.class, new AnnihilationPlaneInspector(() -> FLUID_STACK_TYPE));
        registry.register(PartFluidFormationPlane.class, new FluidFormationPlaneInspector());
    }

    /**
     * Unlike AE2's plane, the fluid plane is no storage: it pulls a bucket of the first configured fluid the network
     * has, reading every config slot whatever cards are installed, and places nothing when none is configured.
     */
    private static final class FluidFormationPlaneInspector implements ITransferInspector<PartFluidFormationPlane> {

        @Override
        public TransferDescription inspect(final PartFluidFormationPlane plane) {
            final List<IAEStack<?>> config = new ArrayList<>();
            final IAEStackInventory inventory = plane.getAEInventoryByName(StorageName.CONFIG);
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                if (inventory.getAEStackInSlot(slot) instanceof IAEFluidStack fluid) {
                    config.add(fluid);
                }
            }
            if (config.isEmpty()) {
                return null;
            }

            final TransferDescription.Builder builder = TransferDescription.builder().canExport(true);
            for (final IAEStack<?> stack : config) {
                builder.config(stack, false);
            }
            return builder.matcher(TransferMatcher.exportOf(config)).build();
        }
    }

    private static final class DualInterfaceInspector implements ITransferInspector<IInterfaceHost> {

        private final InterfaceInspector items = new InterfaceInspector();

        @Override
        public TransferDescription inspect(final IInterfaceHost host) {
            final TransferDescription base = this.items.describe(host);
            if (base == null) {
                return null;
            }

            final List<IAEFluidStack> fluids = new ArrayList<>();
            if (host instanceof IDualHost dual) {
                final AppEngInternalAEInventory config = dual.getConfig();
                for (int slot = 0; slot < config.getSizeInventory(); slot++) {
                    final IAEFluidStack fluid = Util.getAEFluidFromItem(config.getStackInSlot(slot));
                    if (fluid != null) {
                        fluids.add(fluid);
                    }
                }
            }

            if (base.config.isEmpty() && fluids.isEmpty()) {
                return null;
            }

            final TransferDescription.Builder builder = base.toBuilder().canExport(base.canExport || !fluids.isEmpty());
            for (final IAEFluidStack fluid : fluids) {
                builder.config(fluid, false);
            }

            final TransferMatcher fluidMatcher = TransferMatcher.exportOf(fluids);
            return builder.matcher((stack, direction) -> {
                if (stack == null || stack.getStackType() != FLUID_STACK_TYPE) {
                    return base.matcher.canTransfer(stack, direction);
                }

                return fluidMatcher.canTransfer(stack, direction);
            }).build();
        }
    }

    private static final class StockReplenisherInspector implements ITransferInspector<TileSuperStockReplenisher> {

        @Override
        public TransferDescription inspect(final TileSuperStockReplenisher replenisher) {
            final List<IAEStack<?>> config = new ArrayList<>();
            addAll(config, replenisher.getConfigItems());
            addAll(config, replenisher.getConfigFluids());

            final TransferDescription.Builder builder = TransferDescription.builder().canExport(true);
            for (final IAEStack<?> stack : config) {
                builder.config(stack, false);
            }

            return builder.matcher(TransferMatcher.exportOf(config)).build();
        }

        private static void addAll(final List<IAEStack<?>> config, final IAEStackInventory inventory) {
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                final IAEStack<?> stack = inventory.getAEStackInSlot(slot);
                if (stack != null) {
                    config.add(stack);
                }
            }
        }
    }
}
