package appeng.api.stacks;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Iterators;

import appeng.api.config.FuzzyMode;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

/**
 * Tallies a negative or positive amount for sub-variants of an {@link AEKey}.
 */
abstract class VariantCounter implements Iterable<Object2LongMap.Entry<AEKey>> {

    private @Nullable Collection<Object2LongMap.Entry<AEKey>> entries;

    public abstract long get(AEKey key);

    public abstract void add(AEKey key, long amount);

    public abstract void set(AEKey key, long amount);

    public abstract long remove(AEKey key);

    /**
     * @param other a counter of the same class. {@link KeyCounter} picks the class of a bucket from its primary key, so
     *              buckets with the same primary key always share it.
     */
    public abstract void merge(VariantCounter other, boolean subtract);

    public abstract void forEachEntry(KeyCounter.EntryConsumer consumer);

    /**
     * Like {@link #iterator()}, may pass the same mutable entry instance for several entries.
     */
    @Override
    public abstract void forEach(Consumer<? super Object2LongMap.Entry<AEKey>> action);

    public abstract Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey filter, FuzzyMode fuzzy);

    public abstract int size();

    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Unlike {@link #iterator()}, returns entries that stay valid after the next call, which the fast iterators of
     * fastutil maps don't guarantee.
     */
    abstract Iterator<Object2LongMap.Entry<AEKey>> distinctIterator();

    /** A live view with distinct entries, suitable for collecting fuzzy results. */
    final Collection<Object2LongMap.Entry<AEKey>> entries() {
        if (this.entries == null) {
            this.entries = new AbstractCollection<>() {

                @Override
                public Iterator<Object2LongMap.Entry<AEKey>> iterator() {
                    return VariantCounter.this.distinctIterator();
                }

                @Override
                public int size() {
                    return VariantCounter.this.size();
                }
            };
        }
        return this.entries;
    }

    public abstract void reset();

    public abstract void clear();

    public abstract VariantCounter copy(boolean negate);

    /**
     * Removes zero amounts if {@code removeZeros}, then drops sub-indexes left empty. Counters that keep their
     * sub-indexes for reuse only remove zero amounts.
     */
    public abstract void removeEmptySubmaps(boolean removeZeros);

    static final class UnorderedVariantMap extends VariantCounter {

        private @Nullable AEKey singleKey;
        private long singleAmount;
        private @Nullable Object2LongOpenHashMap<AEKey> records;

        @Override
        public long get(AEKey key) {
            if (this.records != null) {
                return this.records.getLong(key);
            }
            return key.equals(this.singleKey) ? this.singleAmount : 0L;
        }

        @Override
        public void add(AEKey key, long amount) {
            if (this.records != null) {
                this.records.addTo(key, amount);
            } else if (this.singleKey == null) {
                this.singleKey = key;
                this.singleAmount = amount;
            } else if (key.equals(this.singleKey)) {
                this.singleAmount += amount;
            } else {
                promote().put(key, amount);
            }
        }

        @Override
        public void set(AEKey key, long amount) {
            if (this.records != null) {
                this.records.put(key, amount);
                return;
            }
            if (this.singleKey == null) {
                this.singleKey = key;
            } else if (!key.equals(this.singleKey)) {
                promote().put(key, amount);
                return;
            }
            this.singleAmount = amount;
        }

        private Object2LongOpenHashMap<AEKey> promote() {
            // The second distinct key triggers allocation. Most buckets never need a map.
            this.records = new Object2LongOpenHashMap<>(2);
            this.records.put(this.singleKey, this.singleAmount);
            this.singleKey = null;
            this.singleAmount = 0L;
            return this.records;
        }

        @Override
        public long remove(AEKey key) {
            if (this.records != null) {
                return this.records.removeLong(key);
            }
            if (!key.equals(this.singleKey)) {
                return 0L;
            }
            long amount = this.singleAmount;
            this.singleKey = null;
            this.singleAmount = 0L;
            return amount;
        }

        @Override
        public int size() {
            return this.records != null ? this.records.size() : this.singleKey == null ? 0 : 1;
        }

        @Override
        public void clear() {
            if (this.records != null) {
                // Keep promoted maps for reuse, just like the enclosing KeyCounter keeps its buckets.
                this.records.clear();
            } else {
                this.singleKey = null;
                this.singleAmount = 0L;
            }
        }

        @Override
        public void reset() {
            if (this.records != null) {
                for (Object2LongMap.Entry<AEKey> entry : Object2LongMaps.fastIterable(this.records)) {
                    entry.setValue(0L);
                }
            } else {
                this.singleAmount = 0L;
            }
        }

        @Override
        public void merge(VariantCounter other, boolean subtract) {
            UnorderedVariantMap source = (UnorderedVariantMap) other;
            if (source.records == null) {
                if (source.singleKey != null) {
                    add(source.singleKey, subtract ? -source.singleAmount : source.singleAmount);
                }
                return;
            }
            if (source.records.isEmpty()) {
                return;
            }
            if (this.records == null) {
                // Size the map once instead of growing it from the initial capacity of a promotion.
                Object2LongOpenHashMap<AEKey> records = copyRecords(source.records, subtract, 1);
                if (this.singleKey != null) {
                    records.addTo(this.singleKey, this.singleAmount);
                    this.singleKey = null;
                    this.singleAmount = 0L;
                }
                this.records = records;
                return;
            }
            for (Object2LongMap.Entry<AEKey> entry : Object2LongMaps.fastIterable(source.records)) {
                long amount = entry.getLongValue();
                this.records.addTo(entry.getKey(), subtract ? -amount : amount);
            }
        }

        private static Object2LongOpenHashMap<AEKey> copyRecords(Object2LongOpenHashMap<AEKey> records, boolean negate,
                int extraCapacity) {
            Object2LongOpenHashMap<AEKey> result = new Object2LongOpenHashMap<>(records.size() + extraCapacity);
            for (Object2LongMap.Entry<AEKey> entry : Object2LongMaps.fastIterable(records)) {
                long amount = entry.getLongValue();
                result.put(entry.getKey(), negate ? -amount : amount);
            }
            return result;
        }

        @Override
        public void removeEmptySubmaps(boolean removeZeros) {
            if (!removeZeros) {
                return;
            }
            if (this.records != null) {
                this.records.values().removeIf(value -> value == 0L);
            } else if (this.singleAmount == 0L) {
                this.singleKey = null;
            }
        }

        @Override
        public void forEachEntry(KeyCounter.EntryConsumer consumer) {
            if (this.records != null) {
                for (Object2LongMap.Entry<AEKey> entry : Object2LongMaps.fastIterable(this.records)) {
                    consumer.accept(entry.getKey(), entry.getLongValue());
                }
            } else if (this.singleKey != null) {
                consumer.accept(this.singleKey, this.singleAmount);
            }
        }

        @Override
        public void forEach(Consumer<? super Object2LongMap.Entry<AEKey>> action) {
            if (this.records != null) {
                Object2LongMaps.fastForEach(this.records, action);
            } else if (this.singleKey != null) {
                action.accept(new SingleEntry(this.singleKey));
            }
        }

        @Override
        public Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey filter, FuzzyMode fuzzy) {
            return entries();
        }

        /**
         * @return a live entry of {@code key} as the only element, or an empty collection if this map does not contain
         *         the key.
         */
        Collection<Object2LongMap.Entry<AEKey>> findPrecise(AEKey key) {
            boolean contains = this.records != null ? this.records.containsKey(key) : key.equals(this.singleKey);
            return contains ? Collections.singletonList(new SingleEntry(key)) : Collections.emptyList();
        }

        @Override
        Iterator<Object2LongMap.Entry<AEKey>> distinctIterator() {
            return this.records != null ? this.records.object2LongEntrySet().iterator() : singleIterator();
        }

        @Override
        public Iterator<Object2LongMap.Entry<AEKey>> iterator() {
            return this.records != null ? Object2LongMaps.fastIterator(this.records) : singleIterator();
        }

        private Iterator<Object2LongMap.Entry<AEKey>> singleIterator() {
            return new Iterator<>() {

                private final @Nullable AEKey key = singleKey;
                private boolean consumed;
                private boolean canRemove;

                @Override
                public boolean hasNext() {
                    return this.key != null && !this.consumed;
                }

                @Override
                public Object2LongMap.Entry<AEKey> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    this.consumed = true;
                    this.canRemove = true;
                    return new SingleEntry(this.key);
                }

                @Override
                public void remove() {
                    if (!this.canRemove) {
                        throw new IllegalStateException();
                    }
                    UnorderedVariantMap.this.remove(this.key);
                    this.canRemove = false;
                }
            };
        }

        private final class SingleEntry implements Object2LongMap.Entry<AEKey> {

            private final AEKey key;

            private SingleEntry(AEKey key) {
                this.key = key;
            }

            @Override
            public AEKey getKey() {
                return this.key;
            }

            @Override
            public long getLongValue() {
                return get(this.key);
            }

            @Override
            public long setValue(long value) {
                long old = get(this.key);
                set(this.key, value);
                return old;
            }

            @Override
            public boolean equals(@Nullable Object other) {
                return other instanceof Map.Entry<?, ?>entry && this.key.equals(entry.getKey())
                        && entry.getValue() instanceof Long value
                        && getLongValue() == value;
            }

            @Override
            public int hashCode() {
                return this.key.hashCode() ^ Long.hashCode(getLongValue());
            }

            @Override
            public String toString() {
                return this.key + "=>" + getLongValue();
            }
        }

        @Override
        public UnorderedVariantMap copy(boolean negate) {
            UnorderedVariantMap result = new UnorderedVariantMap();
            if (this.records != null) {
                if (!this.records.isEmpty()) {
                    result.records = copyRecords(this.records, negate, 0);
                }
            } else {
                result.singleKey = this.singleKey;
                result.singleAmount = negate ? -this.singleAmount : this.singleAmount;
            }
            return result;
        }
    }

    /**
     * Buckets keys by {@linkplain AEKey#getFuzzyDamageLevel() fuzzy damage level}, so that a fuzzy search selects a
     * contiguous range of buckets. Iterates the most damaged keys first. Empty buckets are kept for reuse, like the
     * buckets of {@link KeyCounter}.
     */
    static final class FuzzyVariantMap extends VariantCounter {

        private final @Nullable UnorderedVariantMap[] levels = new UnorderedVariantMap[FuzzySearch.LEVEL_COUNT];

        @Override
        public long get(AEKey key) {
            UnorderedVariantMap level = this.levels[key.getFuzzyDamageLevel()];
            return level == null ? 0L : level.get(key);
        }

        @Override
        public void add(AEKey key, long amount) {
            this.getOrCreateLevel(key).add(key, amount);
        }

        @Override
        public void set(AEKey key, long amount) {
            this.getOrCreateLevel(key).set(key, amount);
        }

        @Override
        public long remove(AEKey key) {
            UnorderedVariantMap level = this.levels[key.getFuzzyDamageLevel()];
            return level == null ? 0L : level.remove(key);
        }

        private UnorderedVariantMap getOrCreateLevel(AEKey key) {
            int index = key.getFuzzyDamageLevel();
            UnorderedVariantMap level = this.levels[index];
            if (level == null) {
                level = new UnorderedVariantMap();
                this.levels[index] = level;
            }
            return level;
        }

        @Override
        public int size() {
            return this.size(0, this.levels.length - 1);
        }

        private int size(int lowest, int highest) {
            int size = 0;
            for (int i = lowest; i <= highest; i++) {
                UnorderedVariantMap level = this.levels[i];
                if (level != null) {
                    size += level.size();
                }
            }
            return size;
        }

        @Override
        public boolean isEmpty() {
            for (UnorderedVariantMap level : this.levels) {
                if (level != null && !level.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void clear() {
            for (UnorderedVariantMap level : this.levels) {
                if (level != null) {
                    level.clear();
                }
            }
        }

        @Override
        public void reset() {
            for (UnorderedVariantMap level : this.levels) {
                if (level != null) {
                    level.reset();
                }
            }
        }

        @Override
        public void merge(VariantCounter other, boolean subtract) {
            FuzzyVariantMap source = (FuzzyVariantMap) other;
            for (int i = 0; i < this.levels.length; i++) {
                UnorderedVariantMap sourceLevel = source.levels[i];
                if (sourceLevel == null || sourceLevel.isEmpty()) {
                    continue;
                }
                UnorderedVariantMap level = this.levels[i];
                if (level == null) {
                    this.levels[i] = sourceLevel.copy(subtract);
                } else {
                    level.merge(sourceLevel, subtract);
                }
            }
        }

        @Override
        public void removeEmptySubmaps(boolean removeZeros) {
            for (UnorderedVariantMap level : this.levels) {
                if (level != null) {
                    level.removeEmptySubmaps(removeZeros);
                }
            }
        }

        @Override
        public void forEachEntry(KeyCounter.EntryConsumer consumer) {
            for (int i = this.levels.length - 1; i >= 0; i--) {
                UnorderedVariantMap level = this.levels[i];
                if (level != null) {
                    level.forEachEntry(consumer);
                }
            }
        }

        @Override
        public void forEach(Consumer<? super Object2LongMap.Entry<AEKey>> action) {
            for (int i = this.levels.length - 1; i >= 0; i--) {
                UnorderedVariantMap level = this.levels[i];
                if (level != null) {
                    level.forEach(action);
                }
            }
        }

        @Override
        public Iterator<Object2LongMap.Entry<AEKey>> iterator() {
            return new LevelIterator(0, this.levels.length - 1, false);
        }

        @Override
        Iterator<Object2LongMap.Entry<AEKey>> distinctIterator() {
            return new LevelIterator(0, this.levels.length - 1, true);
        }

        @Override
        public Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey key, FuzzyMode fuzzy) {
            int level = key.getFuzzyDamageLevel();
            int lowest = FuzzySearch.getLowestLevel(level, fuzzy);
            int highest = FuzzySearch.getHighestLevel(level, fuzzy);
            return new AbstractCollection<>() {

                @Override
                public Iterator<Object2LongMap.Entry<AEKey>> iterator() {
                    return new LevelIterator(lowest, highest, true);
                }

                @Override
                public int size() {
                    return FuzzyVariantMap.this.size(lowest, highest);
                }
            };
        }

        /**
         * Iterates the levels from {@code highest} down to {@code lowest}.
         */
        private final class LevelIterator implements Iterator<Object2LongMap.Entry<AEKey>> {

            private final int lowest;
            private final boolean distinctEntries;
            private int nextLevel;
            private Iterator<Object2LongMap.Entry<AEKey>> current = Collections.emptyIterator();
            /** The iterator that returned the last entry, which may differ from {@link #current} after hasNext(). */
            private @Nullable Iterator<Object2LongMap.Entry<AEKey>> lastReturned;

            private LevelIterator(int lowest, int highest, boolean distinctEntries) {
                this.lowest = lowest;
                this.nextLevel = highest;
                this.distinctEntries = distinctEntries;
            }

            @Override
            public boolean hasNext() {
                while (!this.current.hasNext()) {
                    if (this.nextLevel < this.lowest) {
                        return false;
                    }
                    UnorderedVariantMap level = levels[this.nextLevel--];
                    if (level != null) {
                        this.current = this.distinctEntries ? level.distinctIterator() : level.iterator();
                    }
                }
                return true;
            }

            @Override
            public Object2LongMap.Entry<AEKey> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                this.lastReturned = this.current;
                return this.current.next();
            }

            @Override
            public void remove() {
                if (this.lastReturned == null) {
                    throw new IllegalStateException();
                }
                this.lastReturned.remove();
                this.lastReturned = null;
            }
        }

        @Override
        public FuzzyVariantMap copy(boolean negate) {
            FuzzyVariantMap result = new FuzzyVariantMap();
            for (int i = 0; i < this.levels.length; i++) {
                UnorderedVariantMap level = this.levels[i];
                if (level != null && !level.isEmpty()) {
                    result.levels[i] = level.copy(negate);
                }
            }
            return result;
        }
    }

    /**
     * Groups the keys of a non-damageable item by metadata, so that a fuzzy search selects the metadata of the filter
     * and a search ignoring metadata selects every key. Empty sub-indexes are kept for reuse until
     * {@link #removeEmptySubmaps(boolean)}.
     */
    static final class MetadataVariantMap extends VariantCounter {

        /** The counter of the only metadata in use. */
        private @Nullable UnorderedVariantMap singleCounter;
        private int singleMetadata;
        /** Counters by metadata, once more than one metadata is in use. Replaces {@link #singleCounter}. */
        private @Nullable Int2ObjectMap<UnorderedVariantMap> metadataCounters;

        private static int metadataOf(AEKey key) {
            return ((AEItemKey) key).getItemDamage();
        }

        @Override
        public long get(AEKey key) {
            UnorderedVariantMap counter = getSubIndexOrNull(metadataOf(key));
            return counter == null ? 0L : counter.get(key);
        }

        @Override
        public void add(AEKey key, long amount) {
            getSubIndex(metadataOf(key)).add(key, amount);
        }

        @Override
        public void set(AEKey key, long amount) {
            getSubIndex(metadataOf(key)).set(key, amount);
        }

        @Override
        public long remove(AEKey key) {
            UnorderedVariantMap counter = getSubIndexOrNull(metadataOf(key));
            return counter == null ? 0L : counter.remove(key);
        }

        private UnorderedVariantMap getSubIndex(int metadata) {
            if (this.metadataCounters != null) {
                UnorderedVariantMap counter = this.metadataCounters.get(metadata);
                if (counter == null) {
                    counter = new UnorderedVariantMap();
                    this.metadataCounters.put(metadata, counter);
                }
                return counter;
            }

            if (this.singleCounter == null) {
                this.singleMetadata = metadata;
                this.singleCounter = new UnorderedVariantMap();
                return this.singleCounter;
            }

            if (this.singleMetadata == metadata) {
                return this.singleCounter;
            }

            this.metadataCounters = new Int2ObjectOpenHashMap<>();
            this.metadataCounters.put(this.singleMetadata, this.singleCounter);
            this.singleCounter = null;

            UnorderedVariantMap counter = new UnorderedVariantMap();
            this.metadataCounters.put(metadata, counter);
            return counter;
        }

        private @Nullable UnorderedVariantMap getSubIndexOrNull(int metadata) {
            if (this.metadataCounters != null) {
                return this.metadataCounters.get(metadata);
            }
            return this.singleCounter != null && this.singleMetadata == metadata ? this.singleCounter : null;
        }

        private void compactMetadataCounters() {
            if (this.metadataCounters == null) {
                return;
            }
            if (this.metadataCounters.isEmpty()) {
                this.metadataCounters = null;
            } else if (this.metadataCounters.size() == 1) {
                Int2ObjectMap.Entry<UnorderedVariantMap> entry = this.metadataCounters.int2ObjectEntrySet().iterator()
                        .next();
                this.singleMetadata = entry.getIntKey();
                this.singleCounter = entry.getValue();
                this.metadataCounters = null;
            }
        }

        /**
         * Skips empty sub-indexes of {@code other}, so that they neither create sub-indexes here nor promote
         * {@link #singleCounter} to {@link #metadataCounters}.
         */
        @Override
        public void merge(VariantCounter other, boolean subtract) {
            MetadataVariantMap source = (MetadataVariantMap) other;
            if (source.singleCounter != null && !source.singleCounter.isEmpty()) {
                getSubIndex(source.singleMetadata).merge(source.singleCounter, subtract);
            }
            if (source.metadataCounters != null) {
                for (Int2ObjectMap.Entry<UnorderedVariantMap> entry : source.metadataCounters.int2ObjectEntrySet()) {
                    UnorderedVariantMap counter = entry.getValue();
                    if (!counter.isEmpty()) {
                        getSubIndex(entry.getIntKey()).merge(counter, subtract);
                    }
                }
            }
        }

        @Override
        public MetadataVariantMap copy(boolean negate) {
            MetadataVariantMap result = new MetadataVariantMap();
            if (this.singleCounter != null && !this.singleCounter.isEmpty()) {
                result.singleMetadata = this.singleMetadata;
                result.singleCounter = this.singleCounter.copy(negate);
            }
            if (this.metadataCounters != null) {
                result.metadataCounters = new Int2ObjectOpenHashMap<>(this.metadataCounters.size());
                for (Int2ObjectMap.Entry<UnorderedVariantMap> entry : this.metadataCounters.int2ObjectEntrySet()) {
                    UnorderedVariantMap counter = entry.getValue();
                    if (!counter.isEmpty()) {
                        result.metadataCounters.put(entry.getIntKey(), counter.copy(negate));
                    }
                }
                result.compactMetadataCounters();
            }
            return result;
        }

        @Override
        public Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey filter, FuzzyMode fuzzy) {
            UnorderedVariantMap counter = getSubIndexOrNull(metadataOf(filter));
            return counter == null ? Collections.emptyList() : counter.entries();
        }

        @Override
        Iterator<Object2LongMap.Entry<AEKey>> distinctIterator() {
            if (this.metadataCounters != null) {
                return Iterators.concat(
                        Iterators.transform(
                                this.metadataCounters.values().iterator(),
                                UnorderedVariantMap::distinctIterator));
            }
            return this.singleCounter == null ? Collections.emptyIterator() : this.singleCounter.distinctIterator();
        }

        @Override
        public Iterator<Object2LongMap.Entry<AEKey>> iterator() {
            if (this.metadataCounters != null) {
                return Iterators.concat(
                        Iterators.transform(this.metadataCounters.values().iterator(), UnorderedVariantMap::iterator));
            }
            return this.singleCounter == null ? Collections.emptyIterator() : this.singleCounter.iterator();
        }

        @Override
        public int size() {
            if (this.metadataCounters == null) {
                return this.singleCounter == null ? 0 : this.singleCounter.size();
            }
            int size = 0;
            for (UnorderedVariantMap counter : this.metadataCounters.values()) {
                size += counter.size();
            }
            return size;
        }

        @Override
        public boolean isEmpty() {
            if (this.metadataCounters == null) {
                return this.singleCounter == null || this.singleCounter.isEmpty();
            }
            for (UnorderedVariantMap counter : this.metadataCounters.values()) {
                if (!counter.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void reset() {
            if (this.metadataCounters == null) {
                if (this.singleCounter != null) {
                    this.singleCounter.reset();
                }
                return;
            }
            for (UnorderedVariantMap counter : this.metadataCounters.values()) {
                counter.reset();
            }
        }

        @Override
        public void clear() {
            if (this.metadataCounters == null) {
                if (this.singleCounter != null) {
                    this.singleCounter.clear();
                }
                return;
            }
            for (UnorderedVariantMap counter : this.metadataCounters.values()) {
                counter.clear();
            }
        }

        @Override
        public void forEachEntry(KeyCounter.EntryConsumer consumer) {
            if (this.metadataCounters == null) {
                if (this.singleCounter != null) {
                    this.singleCounter.forEachEntry(consumer);
                }
                return;
            }
            for (UnorderedVariantMap counter : this.metadataCounters.values()) {
                counter.forEachEntry(consumer);
            }
        }

        @Override
        public void forEach(Consumer<? super Object2LongMap.Entry<AEKey>> action) {
            if (this.metadataCounters == null) {
                if (this.singleCounter != null) {
                    this.singleCounter.forEach(action);
                }
                return;
            }
            for (UnorderedVariantMap counter : this.metadataCounters.values()) {
                counter.forEach(action);
            }
        }

        @Override
        public void removeEmptySubmaps(boolean removeZeros) {
            if (this.metadataCounters == null) {
                if (this.singleCounter != null) {
                    this.singleCounter.removeEmptySubmaps(removeZeros);
                    if (this.singleCounter.isEmpty()) {
                        this.singleCounter = null;
                    }
                }
                return;
            }
            Iterator<UnorderedVariantMap> iterator = this.metadataCounters.values().iterator();
            while (iterator.hasNext()) {
                UnorderedVariantMap counter = iterator.next();
                counter.removeEmptySubmaps(removeZeros);
                if (counter.isEmpty()) {
                    iterator.remove();
                }
            }
            this.compactMetadataCounters();
        }
    }
}
