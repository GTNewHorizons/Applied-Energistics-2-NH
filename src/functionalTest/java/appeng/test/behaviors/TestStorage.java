package appeng.test.behaviors;

import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

final class TestStorage implements MEStorage {

    private final Object2LongOpenHashMap<AEKey> stored = new Object2LongOpenHashMap<>();
    private long capacity = Long.MAX_VALUE;
    // Imitate a storage that changed after it was simulated.
    private long maxInsertPerModulate = Long.MAX_VALUE;
    private long maxExtractPerModulate = Long.MAX_VALUE;
    private boolean insertable = true;
    private boolean extractable = true;

    /**
     * Limits the total amount over all keys.
     */
    void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    void setMaxInsertPerModulate(long maxInsertPerModulate) {
        this.maxInsertPerModulate = maxInsertPerModulate;
    }

    void setMaxExtractPerModulate(long maxExtractPerModulate) {
        this.maxExtractPerModulate = maxExtractPerModulate;
    }

    void setInsertable(boolean insertable) {
        this.insertable = insertable;
    }

    void setExtractable(boolean extractable) {
        this.extractable = extractable;
    }

    /**
     * Adds content regardless of the limits.
     */
    void add(AEKey what, long amount) {
        this.stored.addTo(what, amount);
    }

    long getAmount(AEKey what) {
        return this.stored.getLong(what);
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, BaseActionSource source) {
        if (!this.insertable) {
            return 0;
        }
        long inserted = Math.min(amount, this.capacity - this.getTotal());
        if (mode == Actionable.MODULATE) {
            inserted = Math.min(inserted, this.maxInsertPerModulate);
            this.stored.addTo(what, inserted);
        }
        return inserted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, BaseActionSource source) {
        if (!this.extractable) {
            return 0;
        }
        long extracted = Math.min(amount, this.getAmount(what));
        if (mode == Actionable.MODULATE) {
            extracted = Math.min(extracted, this.maxExtractPerModulate);
            this.stored.addTo(what, -extracted);
        }
        return extracted;
    }

    private long getTotal() {
        long total = 0;
        for (LongIterator it = this.stored.values().iterator(); it.hasNext();) {
            total += it.nextLong();
        }
        return total;
    }
}
