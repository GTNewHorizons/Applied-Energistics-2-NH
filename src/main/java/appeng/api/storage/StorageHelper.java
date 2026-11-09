package appeng.api.storage;

import org.jspecify.annotations.NullMarked;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.stacks.AEKey;
import appeng.core.stats.Stats;
import appeng.util.Platform;

@NullMarked
public final class StorageHelper {

    private StorageHelper() {}

    /**
     * Inserts items into a {@link MEStorage} respecting power requirements.
     *
     * @param energy  Energy source.
     * @param storage Inventory to insert into.
     * @param what    Items to insert.
     * @param amount  Number of items to insert.
     * @param source  Action source.
     * @param mode    Simulate or modulate
     * @return the number of items inserted.
     */
    public static long poweredInsert(IEnergySource energy, MEStorage storage, AEKey what, long amount,
            BaseActionSource source, Actionable mode) {
        final long toInsert = limitByPower(energy, what, amount);
        if (toInsert <= 0) {
            return 0;
        }

        final long inserted = storage.insert(what, toInsert, mode, source);
        if (mode == Actionable.MODULATE && inserted > 0) {
            consumePower(energy, what, inserted);
            if (source instanceof PlayerSource playerSource && playerSource.player != null) {
                Stats.ItemsInserted.addToPlayer(playerSource.player, Platform.longToInt(inserted));
            }
        }
        return inserted;
    }

    /**
     * Extracts items from a {@link MEStorage} respecting power requirements.
     *
     * @param energy  Energy source.
     * @param storage Inventory to extract from.
     * @param what    Requested item.
     * @param amount  Requested count.
     * @param source  Action source.
     * @param mode    Simulate or modulate
     * @return the number of items extracted.
     */
    public static long poweredExtract(IEnergySource energy, MEStorage storage, AEKey what, long amount,
            BaseActionSource source, Actionable mode) {
        final long toExtract = limitByPower(energy, what, amount);
        if (toExtract <= 0) {
            return 0;
        }

        final long extracted = storage.extract(what, toExtract, mode, source);
        if (mode == Actionable.MODULATE && extracted > 0) {
            consumePower(energy, what, extracted);
            if (source instanceof PlayerSource playerSource && playerSource.player != null) {
                Stats.ItemsExtracted.addToPlayer(playerSource.player, Platform.longToInt(extracted));
            }
        }
        return extracted;
    }

    private static long limitByPower(IEnergySource energy, AEKey what, long amount) {
        if (amount <= 0) {
            return 0;
        }
        final int unit = what.getAmountPerUnit();
        final double available = energy
                .extractAEPower(Platform.ceilDiv(amount, unit), Actionable.SIMULATE, PowerMultiplier.CONFIG);
        return Math.min((long) (available * unit + 0.9), amount);
    }

    private static void consumePower(IEnergySource energy, AEKey what, long amount) {
        energy.extractAEPower(
                Platform.ceilDiv(amount, what.getAmountPerUnit()),
                Actionable.MODULATE,
                PowerMultiplier.CONFIG);
    }
}
