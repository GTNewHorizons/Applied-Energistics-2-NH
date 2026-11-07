package thaumicenergistics.api;

import org.jspecify.annotations.NonNull;

/**
 * Thaumic Energistics items.
 *
 * @author Nividica
 *
 */
public abstract class IThEItems {

    /**
     * Coalescence Core.
     */
    @NonNull
    public IThEItemDescription CoalescenceCore;

    /**
     * Diffusion Core.
     */
    @NonNull
    public IThEItemDescription DiffusionCore;

    /**
     * 16 KiloByte essentia storage cell.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_16k;

    /**
     * 1 KiloByte essentia storage cell.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_1k;

    /**
     * 4 KiloByte essentia storage cell.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_4k;

    /**
     * 64 KiloByte essentia storage cell.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_64k;

    /**
     * 256 KiloByte essentia storage cell.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_256k;

    /**
     * 1024 KiloByte essentia storage cell.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_1024k;

    /**
     * 4096 KiloByte essentia storage cell.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_4096k;

    /**
     * 16384 KiloByte essentia storage cell.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_16384k;

    /**
     * Quantum essentia storage cell.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_Quantum;

    /**
     * Singularity essentia storage cell.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_Singularity;

    /**
     * Creative essentia storage cell.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_Creative;

    /**
     * Essentia storage cell housing.
     */
    @NonNull
    public IThEItemDescription EssentiaCell_Casing;

    /**
     * 16 KiloByte essentia storage cell component.
     */
    @NonNull
    public IThEItemDescription EssentiaStorageComponent_16k;

    /**
     * 1 KiloByte essentia storage cell component.
     */
    @NonNull
    public IThEItemDescription EssentiaStorageComponent_1k;

    /**
     * 4 KiloByte essentia storage cell component.
     */
    @NonNull
    public IThEItemDescription EssentiaStorageComponent_4k;

    /**
     * 64 KiloByte essentia storage cell component.
     */
    @NonNull
    public IThEItemDescription EssentiaStorageComponent_64k;

    /**
     * 256 KiloByte essentia storage cell component.
     */
    @NonNull
    public IThEItemDescription EssentiaStorageComponent_256k;

    /**
     * 1024 KiloByte essentia storage cell component.
     */
    @NonNull
    public IThEItemDescription EssentiaStorageComponent_1024k;

    /**
     * 4096 KiloByte essentia storage cell component.
     */
    @NonNull
    public IThEItemDescription EssentiaStorageComponent_4096k;

    /**
     * 16384 KiloByte essentia storage cell component.
     */
    @NonNull
    public IThEItemDescription EssentiaStorageComponent_16384k;

    /**
     * Iron gear.
     */
    @NonNull
    public IThEItemDescription IronGear;

    /**
     * Knowledge core.
     */
    @NonNull
    public IThEItemDescription KnowledgeCore;

    /**
     * AE Wrench wand focus.
     */
    @NonNull
    public IThEItemDescription WandFocusAEWrench;

    /**
     * Golem wireless backpack.
     */
    @NonNull
    public IThEItemDescription GolemWifiBackpack;

    /**
     * Cell microscope
     */
    @NonNull
    public IThEItemDescription CellMicroscope;

    // EC2 compatibility
    @NonNull
    public IThEItemDescription WirelessEssentiaTerminal;
}
