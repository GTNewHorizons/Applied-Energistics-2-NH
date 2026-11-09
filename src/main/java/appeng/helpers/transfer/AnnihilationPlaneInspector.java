package appeng.helpers.transfer;

import java.util.function.Supplier;

import appeng.api.config.TransferDirection;
import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.storage.data.IAEStackType;
import appeng.parts.automation.PartBaseAnnihilationPlane;

/**
 * An annihilation plane has no filter and imports whatever it breaks.
 */
public final class AnnihilationPlaneInspector implements ITransferInspector<PartBaseAnnihilationPlane> {

    private final Supplier<IAEStackType<?>> type;

    /**
     * @param type the stack type the plane produces, read lazily because stack types do not exist during API setup
     */
    public AnnihilationPlaneInspector(final Supplier<IAEStackType<?>> type) {
        this.type = type;
    }

    @Override
    public TransferDescription inspect(final PartBaseAnnihilationPlane plane) {
        return TransferDescription.builder().canImport(true)
                .matcher(
                        (stack, direction) -> direction == TransferDirection.IMPORT && stack != null
                                && stack.getStackType() == this.type.get())
                .build();
    }
}
