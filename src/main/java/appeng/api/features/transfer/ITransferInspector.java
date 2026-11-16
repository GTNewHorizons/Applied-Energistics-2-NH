package appeng.api.features.transfer;

import org.jetbrains.annotations.Nullable;

import appeng.api.networking.IGridHost;

/**
 * Reads the filters and settings of a network component for the Transfer Terminal.
 * <p>
 * Inspectors run on the server every time the terminal rescans, so they must not change the component.
 *
 * @param <T> component class this inspector handles
 */
public interface ITransferInspector<T extends IGridHost> {

    /**
     * @return the component's current configuration, or null to leave it out of the results
     */
    @Nullable
    TransferDescription inspect(T host);
}
