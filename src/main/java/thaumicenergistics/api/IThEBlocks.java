package thaumicenergistics.api;

import org.jspecify.annotations.NonNull;

/**
 * Thaumic Energistics Blocks.
 *
 * @author Nividica
 *
 */
public abstract class IThEBlocks {

    /**
     * Arcane Assembler.
     */
    @NonNull
    public IThEItemDescription ArcaneAssembler;

    /**
     * Distilation Pattern Encoder.
     */
    @NonNull
    public IThEItemDescription DistillationPatternEncoder;

    /**
     * Essentia Provider.
     */
    @NonNull
    public IThEItemDescription EssentiaProvider;

    /**
     * Essentia Vibration Chamber.
     */
    @NonNull
    public IThEItemDescription EssentiaVibrationChamber;

    /**
     * Infusion Provider.
     */
    @NonNull
    public IThEItemDescription InfusionProvider;

    /**
     * Advanced Infusion Provider.
     */
    @NonNull
    public IThEItemDescription AdvancedInfusionProvider;

    /**
     * Iron Gearbox.
     */
    @NonNull
    public IThEItemDescription IronGearBox;

    /**
     * Knowledge Inscriber.
     */
    @NonNull
    public IThEItemDescription KnowledgeInscriber;

    /**
     * Thaumium Gearbox.
     */
    @NonNull
    public IThEItemDescription ThaumiumGearBox;
}
