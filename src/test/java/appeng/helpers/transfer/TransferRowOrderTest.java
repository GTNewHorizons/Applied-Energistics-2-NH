package appeng.helpers.transfer;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import appeng.api.config.TransferDirection;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.features.transfer.TransferDescription;

public final class TransferRowOrderTest {

    private static TransferDescription.Builder storage() {
        return TransferDescription.builder().storageHandler(true);
    }

    private static TransferComponentInfo row(final String name, final int tier, final TransferDescription description) {
        return new TransferComponentInfo(null, name, null, 0, 0, 0, 0, (byte) 0, tier, description);
    }

    private static List<String> sorted(final boolean hasQuery, final TransferDirection direction,
            final TransferComponentInfo... rows) {
        final List<TransferComponentInfo> list = new ArrayList<>(Arrays.asList(rows));
        list.sort(TransferRowOrder.comparator(hasQuery, direction));
        return list.stream().map(row -> row.customName).collect(Collectors.toList());
    }

    @Test
    public void ownNetworkComesBeforeHigherPriorityElsewhere() {
        assertEquals(
                Arrays.asList("own", "other"),
                sorted(
                        true,
                        TransferDirection.EXPORT,
                        row("other", 1, storage().priority(100).build()),
                        row("own", 0, storage().priority(-100).build())));
    }

    @Test
    public void storageHandlersComeBeforeTickDrivenDevicesWhenSearching() {
        assertEquals(
                Arrays.asList("storage", "bus"),
                sorted(
                        true,
                        TransferDirection.EXPORT,
                        row("bus", 0, TransferDescription.builder().build()),
                        row("storage", 0, storage().priority(-5).build())));
    }

    @Test
    public void withoutQueryDeviceKindDoesNotSplitTheList() {
        assertEquals(
                Arrays.asList("a bus", "storage"),
                sorted(
                        false,
                        TransferDirection.EXPORT,
                        row("storage", 0, storage().build()),
                        row("a bus", 0, TransferDescription.builder().build())));
    }

    /**
     * A sticky card only matters for stacks the storage claims, which needs a query. Those cases are covered by
     * {@code TransferResultFilterFunctionalTest}.
     */
    @Test
    public void stickyCardAloneDoesNotReorder() {
        assertEquals(
                Arrays.asList("high", "low", "sticky"),
                sorted(
                        true,
                        TransferDirection.EXPORT,
                        row("low", 0, storage().priority(1).build()),
                        row("sticky", 0, storage().sticky(true).priority(0).build()),
                        row("high", 0, storage().priority(9).build())));
    }

    @Test
    public void importDrainsLowestPriorityFirst() {
        assertEquals(
                Arrays.asList("low", "high"),
                sorted(
                        true,
                        TransferDirection.IMPORT,
                        row("high", 0, storage().priority(5).build()),
                        row("low", 0, storage().priority(-5).build())));
    }

    @Test
    public void bothAndNoQueryUseDescendingPriority() {
        final TransferComponentInfo high = row("high", 0, storage().priority(5).build());
        final TransferComponentInfo low = row("low", 0, storage().priority(-5).build());

        assertEquals(Arrays.asList("high", "low"), sorted(true, TransferDirection.BOTH, low, high));
        assertEquals(Arrays.asList("high", "low"), sorted(false, TransferDirection.BOTH, low, high));
    }

    @Test
    public void interfacePriorityDoesNotAffectOrder() {
        assertEquals(
                Arrays.asList("a", "b"),
                sorted(
                        false,
                        TransferDirection.BOTH,
                        row("b", 0, TransferDescription.builder().priority(50).build()),
                        row("a", 0, TransferDescription.builder().priority(-50).build())));
    }

    @Test
    public void tiesFallBackToNameThenLocation() {
        final TransferDescription description = storage().build();
        final TransferComponentInfo east = new TransferComponentInfo(
                null,
                "same",
                null,
                5,
                0,
                0,
                0,
                (byte) 0,
                0,
                description);
        final TransferComponentInfo west = new TransferComponentInfo(
                null,
                "same",
                null,
                -5,
                0,
                0,
                0,
                (byte) 0,
                0,
                description);

        final List<TransferComponentInfo> list = new ArrayList<>(Arrays.asList(east, west));
        list.sort(TransferRowOrder.comparator(true, TransferDirection.EXPORT));

        assertEquals(-5, list.get(0).x);
        assertEquals(5, list.get(1).x);
    }

    private static List<String> simulated(final TransferComponentInfo... rows) {
        final List<TransferComponentInfo> list = new ArrayList<>(Arrays.asList(rows));
        list.sort(TransferRowOrder.simulated(TransferRowOrder.comparator(true, TransferDirection.EXPORT)));
        return list.stream().map(row -> row.customName).collect(Collectors.toList());
    }

    private static TransferComponentInfo receiving(final String name, final long amount, final int order,
            final TransferDescription description) {
        return row(name, 0, description).withSimulation(amount, order, false, Collections.emptyList());
    }

    @Test
    public void receiversComeFirstInTheOrderTheNetworkReachedThem() {
        assertEquals(
                Arrays.asList("reached first", "reached second", "high priority but full"),
                simulated(
                        receiving("high priority but full", 0, 0, storage().priority(10).build()),
                        receiving("reached second", 5, 2, storage().priority(-3).build()),
                        receiving("reached first", 5, 1, storage().priority(-5).build())));
    }

    @Test
    public void rowsThatReceiveNothingKeepTheirUsualOrder() {
        assertEquals(
                Arrays.asList("receiver", "high", "low", "bus"),
                simulated(
                        row("bus", 0, TransferDescription.builder().build()),
                        receiving("low", 0, 0, storage().priority(1).build()),
                        receiving("high", 0, 0, storage().priority(9).build()),
                        receiving("receiver", 1, 7, storage().priority(-9).build())));
    }
}
