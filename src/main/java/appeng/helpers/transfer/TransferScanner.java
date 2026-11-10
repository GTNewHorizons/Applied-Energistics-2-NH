package appeng.helpers.transfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.ITransferInspectorRegistry;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.storage.ICellProvider;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.IInterfaceViewable;
import appeng.helpers.ICustomNameObject;
import appeng.parts.AEBasePart;
import appeng.tile.storage.TileDrive;

/**
 * Finds every component the Transfer Terminal can show on a set of networks.
 */
public final class TransferScanner {

    private TransferScanner() {}

    /**
     * @param grids    networks to scan, the first one being the terminal's own
     * @param registry source of the inspectors
     * @return one row for each inspectable component
     */
    public static List<TransferComponentInfo> scan(final Collection<? extends IGrid> grids,
            final ITransferInspectorRegistry registry) {
        final List<TransferComponentInfo> rows = new ArrayList<>();
        final Set<String> seen = new HashSet<>();

        int tier = 0;
        for (final IGrid grid : grids) {
            for (final Class<? extends IGridHost> machineClass : grid.getMachinesClasses()) {
                final ITransferInspector<?> inspector = registry.getInspector(machineClass);
                if (inspector != null) {
                    for (final IGridNode node : grid.getMachines(machineClass)) {
                        addRow(rows, seen, inspector, node, tier);
                    }
                }
            }
            tier++;
        }

        return rows;
    }

    /**
     * @return a value that changes when components are added to or removed from the networks
     */
    public static long fingerprint(final Collection<? extends IGrid> grids) {
        long result = 17;
        for (final IGrid grid : grids) {
            result = 31 * result + grid.getId().hashCode();
            for (final Class<? extends IGridHost> machineClass : grid.getMachinesClasses()) {
                result = 31 * result + machineClass.hashCode() * (long) grid.getMachines(machineClass).size();
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void addRow(final List<TransferComponentInfo> rows, final Set<String> seen,
            final ITransferInspector<?> inspector, final IGridNode node, final int tier) {
        final IGridHost machine = node.getMachine();
        final TransferDescription inspected;
        try {
            inspected = ((ITransferInspector<IGridHost>) inspector).inspect(machine);
        } catch (final RuntimeException e) {
            // A broken inspector from another mod must not take the whole terminal down.
            return;
        }

        if (inspected == null) {
            return;
        }
        final TransferDescription description = withLiveHandlers(inspected, machine);

        final IGridBlock block = node.getGridBlock();
        final DimensionalCoord location = block.getLocation();
        final byte side = (byte) (machine instanceof AEBasePart part ? part.getSide() : ForgeDirection.UNKNOWN)
                .ordinal();
        if (!seen.add(location.getDimension() + ":" + location.x + ":" + location.y + ":" + location.z + ":" + side)) {
            return;
        }

        rows.add(
                new TransferComponentInfo(
                        block.getMachineRepresentation(),
                        customName(machine),
                        nameSuffix(machine),
                        location.x,
                        location.y,
                        location.z,
                        location.getDimension(),
                        side,
                        tier,
                        description));
    }

    /**
     * Storage handlers of any cell provider are the instances its network routes stacks into, so they are attached here
     * for every inspector. An inspector that set its own is left alone.
     */
    static TransferDescription withLiveHandlers(final TransferDescription description, final IGridHost machine) {
        if (description.liveHandlers != null || !(machine instanceof ICellProvider provider)) {
            return description;
        }

        final TransferDescription.Builder builder = description.toBuilder().liveHandlers(provider::getCellArray);
        if (description.slotOf == null && machine instanceof TileDrive drive) {
            builder.slotOf(handler -> driveSlotOf(drive, handler));
        }
        return builder.build();
    }

    private static int driveSlotOf(final TileDrive drive, final IMEInventoryHandler<?> handler) {
        for (int slot = 0; slot < drive.getSizeInventory(); slot++) {
            if (drive.getCellInvBySlot(slot) == handler) {
                return slot;
            }
        }
        return -1;
    }

    private static String customName(final IGridHost machine) {
        return machine instanceof ICustomNameObject named && named.hasCustomName() ? named.getCustomName() : "";
    }

    private static String nameSuffix(final IGridHost machine) {
        final IChatComponent suffix = machine instanceof IInterfaceViewable viewable ? viewable.getNameSuffix() : null;
        return suffix == null ? null : IChatComponent.Serializer.func_150696_a(suffix);
    }
}
