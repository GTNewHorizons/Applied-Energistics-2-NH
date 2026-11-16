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

    /**
     * A component or inspector from another mod may throw anywhere while its row is built, or fail to link against a
     * version of a mod it was not built for. That only loses its own row.
     */
    private static void addRow(final List<TransferComponentInfo> rows, final Set<String> seen,
            final ITransferInspector<?> inspector, final IGridNode node, final int tier) {
        try {
            buildRow(rows, seen, inspector, node, tier);
        } catch (final RuntimeException | LinkageError e) {
            // Skip the broken component.
        }
    }

    @SuppressWarnings("unchecked")
    private static void buildRow(final List<TransferComponentInfo> rows, final Set<String> seen,
            final ITransferInspector<?> inspector, final IGridNode node, final int tier) {
        final IGridHost machine = node.getMachine();
        final TransferDescription inspected = ((ITransferInspector<IGridHost>) inspector).inspect(machine);

        if (inspected == null) {
            return;
        }
        addLocatedRow(rows, seen, node, machine, tier, withLiveHandlers(inspected, machine));
    }

    /**
     * Storage that ended up without a row, because no inspector knows it or its inspector left it out or failed, can
     * still receive part of a simulated insert. These rows let the terminal show where that part went.
     *
     * @param grids     networks to scan, in the same order as for {@link #scan}
     * @param inspected the rows {@link #scan} found on those networks
     * @return one row for each cell provider without a row, which only knows where it is, its priority and its storage
     *         handlers
     */
    public static List<TransferComponentInfo> scanUninspected(final Collection<? extends IGrid> grids,
            final List<TransferComponentInfo> inspected) {
        final List<TransferComponentInfo> rows = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (final TransferComponentInfo row : inspected) {
            seen.add(locationKey(row.dim, row.x, row.y, row.z, row.side));
        }

        int tier = 0;
        for (final IGrid grid : grids) {
            for (final Class<? extends IGridHost> machineClass : grid.getMachinesClasses()) {
                if (!ICellProvider.class.isAssignableFrom(machineClass)) {
                    continue;
                }
                for (final IGridNode node : grid.getMachines(machineClass)) {
                    try {
                        final IGridHost machine = node.getMachine();
                        addLocatedRow(rows, seen, node, machine, tier, genericDescription((ICellProvider) machine));
                    } catch (final RuntimeException | LinkageError e) {
                        // Skip the broken component.
                    }
                }
            }
            tier++;
        }

        return rows;
    }

    private static TransferDescription genericDescription(final ICellProvider provider) {
        return TransferDescription.builder().generic(true).storageHandler(true).canExport(true)
                .priority(provider.getPriority()).liveHandlers(provider::getCellArray).build();
    }

    /**
     * Adds a row for the component at the node's location, unless a row for the same location and side was added
     * already.
     */
    private static void addLocatedRow(final List<TransferComponentInfo> rows, final Set<String> seen,
            final IGridNode node, final IGridHost machine, final int tier, final TransferDescription description) {
        final IGridBlock block = node.getGridBlock();
        final DimensionalCoord location = block.getLocation();
        final byte side = (byte) (machine instanceof AEBasePart part ? part.getSide() : ForgeDirection.UNKNOWN)
                .ordinal();
        if (!seen.add(locationKey(location.getDimension(), location.x, location.y, location.z, side))) {
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

    private static String locationKey(final int dim, final int x, final int y, final int z, final byte side) {
        return dim + ":" + x + ":" + y + ":" + z + ":" + side;
    }

    /**
     * Storage handlers of any cell provider are the instances its network routes stacks into, so they are attached here
     * for every inspector, which also marks the row as storage. An inspector that set its own handlers is left alone.
     */
    static TransferDescription withLiveHandlers(final TransferDescription description, final IGridHost machine) {
        if (description.liveHandlers != null || !(machine instanceof ICellProvider provider)) {
            return description;
        }

        final TransferDescription.Builder builder = description.toBuilder().liveHandlers(provider::getCellArray)
                .storageHandler(true);
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
