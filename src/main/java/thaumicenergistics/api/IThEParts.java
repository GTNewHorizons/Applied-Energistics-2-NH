package thaumicenergistics.api;

import org.jspecify.annotations.NonNull;

/**
 * Thaumic Energistics AE2 cable parts.
 *
 * @author Nividica
 *
 */
public abstract class IThEParts {

    /**
     * Arcane crafting terminal.
     */
    @NonNull
    public IThEItemDescription ArcaneCrafting_Terminal;

    /**
     * Essentia export bus.
     */
    @NonNull
    public IThEItemDescription Essentia_ExportBus;

    /**
     * Essentia import bus.
     */
    @NonNull
    public IThEItemDescription Essentia_ImportBus;

    /**
     * Essentia level emitter.
     */
    @NonNull
    public IThEItemDescription Essentia_LevelEmitter;

    /**
     * Essentia storage bus.
     */
    @NonNull
    public IThEItemDescription Essentia_StorageBus;

    /**
     * Essentia terminal.
     */
    @NonNull
    public IThEItemDescription Essentia_Terminal;

    /**
     * Vis relay interface.
     */
    @NonNull
    public IThEItemDescription VisRelay_Interface;

    /**
     * Essentia storage monitor.
     */
    @NonNull
    public IThEItemDescription Essentia_StorageMonitor;

    /**
     * Essentia conversion monitor.
     */
    @NonNull
    public IThEItemDescription Essentia_ConversionMonitor;

    /**
     * Creative vis relay interface.
     */
    @NonNull
    public IThEItemDescription CreativeVisRelay_Interface;
}
