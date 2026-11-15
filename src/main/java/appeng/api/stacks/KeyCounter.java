package appeng.api.stacks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.item.Item;
import net.minecraftforge.oredict.OreDictionary;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import appeng.api.config.FuzzyMode;
import appeng.util.item.OreReference;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/**
 * Associates amounts with AE keys and makes key/amount pairs searchable with fuzzy mode semantics.
 */
public final class KeyCounter implements Iterable<Object2LongMap.Entry<AEKey>> {

    @FunctionalInterface
    public interface EntryConsumer {

        void accept(AEKey key, long amount);
    }

    /**
     * Counters for item keys, grouped by item. Damageable items use {@link VariantCounter.FuzzyVariantMap}s, the others
     * {@link VariantCounter.MetadataVariantMap}s.
     */
    private final Reference2ObjectMap<Item, VariantCounter> itemCounters = new Reference2ObjectOpenHashMap<>();
    /**
     * Counters for non-item keys of {@linkplain AEKeyType#hasVariants() types with variants}, grouped by primary key.
     * Types that {@linkplain AEKeyType#supportsFuzzyRangeSearch() support fuzzy range search} use
     * {@link VariantCounter.FuzzyVariantMap}s.
     */
    private final Reference2ObjectMap<Object, VariantCounter> simpleCounters = new Reference2ObjectOpenHashMap<>();
    /**
     * Keys of {@linkplain AEKeyType#hasVariants() types without variants}, such as fluids. Each of them is the only key
     * with its primary key, so they need no grouping.
     */
    private final VariantCounter.UnorderedVariantMap uniqueCounter = new VariantCounter.UnorderedVariantMap();

    // The methods accessing a single key call each kind of counter from its own call site, so that every call site
    // sees at most two counter classes and stays inlinable.

    public Collection<Object2LongMap.Entry<AEKey>> findFuzzy(AEKey key, FuzzyMode fuzzy) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(fuzzy, "fuzzy");

        if (key instanceof AEItemKey itemKey) {
            return this.findFuzzyItem(itemKey, fuzzy);
        }
        if (!key.getType().hasVariants()) {
            return this.uniqueCounter.findPrecise(key);
        }

        VariantCounter counter = this.simpleCounters.get(key.getPrimaryKey());
        return counter == null ? Collections.emptyList() : counter.findFuzzy(key, fuzzy);
    }

    public void removeZeros() {
        removeEmptySubmaps(true);
    }

    public void removeEmptySubmaps() {
        removeEmptySubmaps(false);
    }

    private void removeEmptySubmaps(boolean removeZeros) {
        removeEmptySubmaps(this.itemCounters, removeZeros);
        removeEmptySubmaps(this.simpleCounters, removeZeros);
        this.uniqueCounter.removeEmptySubmaps(removeZeros);
    }

    private static void removeEmptySubmaps(Reference2ObjectMap<?, VariantCounter> counters, boolean removeZeros) {
        Iterator<VariantCounter> iterator = counters.values().iterator();
        while (iterator.hasNext()) {
            VariantCounter counter = iterator.next();
            counter.removeEmptySubmaps(removeZeros);
            if (counter.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public void addAll(KeyCounter other) {
        merge(other, false);
    }

    public void removeAll(KeyCounter other) {
        merge(other, true);
    }

    private void merge(KeyCounter other, boolean subtract) {
        Objects.requireNonNull(other, "other");
        merge(this.itemCounters, other.itemCounters, subtract);
        merge(this.simpleCounters, other.simpleCounters, subtract);
        this.uniqueCounter.merge(other.uniqueCounter, subtract);
    }

    private static <K> void merge(Reference2ObjectMap<K, VariantCounter> target,
            Reference2ObjectMap<K, VariantCounter> source, boolean subtract) {
        for (Reference2ObjectMap.Entry<K, VariantCounter> entry : source.reference2ObjectEntrySet()) {
            VariantCounter sourceCounter = entry.getValue();
            // Buckets emptied by clear() are kept for reuse and must not be copied into the target.
            if (sourceCounter.isEmpty()) {
                continue;
            }
            VariantCounter counter = target.get(entry.getKey());
            if (counter != null) {
                counter.merge(sourceCounter, subtract);
            } else {
                target.put(entry.getKey(), sourceCounter.copy(subtract));
            }
        }
    }

    public void add(AEKey key, long amount) {
        Objects.requireNonNull(key, "key");
        if (key instanceof AEItemKey itemKey) {
            getItemCounter(itemKey).add(key, amount);
        } else if (!key.getType().hasVariants()) {
            this.uniqueCounter.add(key, amount);
        } else {
            getSimpleCounter(key).add(key, amount);
        }
    }

    /**
     * Subtracts the given amount from the value associated with the given key.
     */
    public void remove(AEKey key, long amount) {
        add(key, -amount);
    }

    /**
     * Removes the given key from this counter, and returns the old value (or 0).
     */
    public long remove(AEKey key) {
        Objects.requireNonNull(key, "key");
        if (key instanceof AEItemKey itemKey) {
            Item item = itemKey.getItem();
            VariantCounter counter = this.itemCounters.get(item);
            if (counter == null) {
                return 0L;
            }
            long removed = counter.remove(key);
            if (counter.isEmpty()) {
                this.itemCounters.remove(item);
            }
            return removed;
        }
        if (!key.getType().hasVariants()) {
            return this.uniqueCounter.remove(key);
        }

        Object primaryKey = key.getPrimaryKey();
        VariantCounter counter = this.simpleCounters.get(primaryKey);
        if (counter == null) {
            return 0L;
        }
        long removed = counter.remove(key);
        if (counter.isEmpty()) {
            this.simpleCounters.remove(primaryKey);
        }
        return removed;
    }

    public void set(AEKey key, long amount) {
        Objects.requireNonNull(key, "key");
        if (key instanceof AEItemKey itemKey) {
            getItemCounter(itemKey).set(key, amount);
        } else if (!key.getType().hasVariants()) {
            this.uniqueCounter.set(key, amount);
        } else {
            getSimpleCounter(key).set(key, amount);
        }
    }

    public long get(AEKey key) {
        Objects.requireNonNull(key, "key");
        if (key instanceof AEItemKey itemKey) {
            VariantCounter counter = this.itemCounters.get(itemKey.getItem());
            return counter == null ? 0L : counter.get(key);
        }
        if (!key.getType().hasVariants()) {
            return this.uniqueCounter.get(key);
        }

        VariantCounter counter = this.simpleCounters.get(key.getPrimaryKey());
        return counter == null ? 0L : counter.get(key);
    }

    public void reset() {
        for (VariantCounter counter : this.itemCounters.values()) {
            counter.reset();
        }
        for (VariantCounter counter : this.simpleCounters.values()) {
            counter.reset();
        }
        this.uniqueCounter.reset();
    }

    /**
     * Clears entries but keeps existing buckets for reuse.
     */
    public void clear() {
        for (VariantCounter counter : this.itemCounters.values()) {
            counter.clear();
        }
        for (VariantCounter counter : this.simpleCounters.values()) {
            counter.clear();
        }
        this.uniqueCounter.clear();
    }

    public boolean isEmpty() {
        for (VariantCounter counter : this.itemCounters.values()) {
            if (!counter.isEmpty()) {
                return false;
            }
        }
        for (VariantCounter counter : this.simpleCounters.values()) {
            if (!counter.isEmpty()) {
                return false;
            }
        }
        return this.uniqueCounter.isEmpty();
    }

    public int size() {
        int total = 0;
        for (VariantCounter counter : this.itemCounters.values()) {
            total += counter.size();
        }
        for (VariantCounter counter : this.simpleCounters.values()) {
            total += counter.size();
        }
        total += this.uniqueCounter.size();
        return total;
    }

    @Override
    public Iterator<Object2LongMap.Entry<AEKey>> iterator() {
        return Iterators.concat(
                Iterables.concat(this.itemCounters.values()).iterator(),
                Iterables.concat(this.simpleCounters.values()).iterator(),
                this.uniqueCounter.iterator());
    }

    /**
     * Prefer {@link #forEachEntry} unless the entries are needed, such as to {@linkplain Object2LongMap.Entry#setValue
     * set values}.
     */
    @Override
    public void forEach(Consumer<? super Object2LongMap.Entry<AEKey>> action) {
        Objects.requireNonNull(action, "action");
        for (VariantCounter counter : this.itemCounters.values()) {
            counter.forEach(action);
        }
        for (VariantCounter counter : this.simpleCounters.values()) {
            counter.forEach(action);
        }
        this.uniqueCounter.forEach(action);
    }

    /**
     * Faster than {@link #forEach} since no entries are created. Use it for read-only iteration.
     */
    public void forEachEntry(EntryConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (VariantCounter counter : this.itemCounters.values()) {
            counter.forEachEntry(consumer);
        }
        for (VariantCounter counter : this.simpleCounters.values()) {
            counter.forEachEntry(consumer);
        }
        this.uniqueCounter.forEachEntry(consumer);
    }

    @Nullable
    public AEKey getFirstKey() {
        Object2LongMap.Entry<AEKey> entry = getFirstEntry();
        return entry == null ? null : entry.getKey();
    }

    @Nullable
    public <T extends AEKey> T getFirstKey(Class<T> keyClass) {
        Object2LongMap.Entry<AEKey> entry = getFirstEntry(keyClass);
        return entry == null ? null : keyClass.cast(entry.getKey());
    }

    public Object2LongMap.@Nullable Entry<AEKey> getFirstEntry() {
        return getFirstEntry(AEKey.class);
    }

    public <T extends AEKey> Object2LongMap.@Nullable Entry<AEKey> getFirstEntry(Class<T> keyClass) {
        Objects.requireNonNull(keyClass, "keyClass");
        // AEItemKey is final, so the class alone tells whether item buckets can hold matching keys.
        if (keyClass.isAssignableFrom(AEItemKey.class)) {
            for (VariantCounter counter : this.itemCounters.values()) {
                Object2LongMap.Entry<AEKey> entry = findFirst(counter, keyClass);
                if (entry != null) {
                    return entry;
                }
            }
            if (keyClass == AEItemKey.class) {
                return null;
            }
        }
        for (VariantCounter counter : this.simpleCounters.values()) {
            Object2LongMap.Entry<AEKey> entry = findFirst(counter, keyClass);
            if (entry != null) {
                return entry;
            }
        }
        return findFirst(this.uniqueCounter, keyClass);
    }

    private static Object2LongMap.@Nullable Entry<AEKey> findFirst(VariantCounter counter, Class<?> keyClass) {
        for (Object2LongMap.Entry<AEKey> entry : counter) {
            if (keyClass.isInstance(entry.getKey())) {
                return entry;
            }
        }
        return null;
    }

    public Set<AEKey> keySet() {
        Set<AEKey> keys = new ObjectOpenHashSet<>(size());
        this.forEachEntry((key, amount) -> keys.add(key));
        return keys;
    }

    private Collection<Object2LongMap.Entry<AEKey>> findFuzzyItem(AEItemKey key, FuzzyMode fuzzy) {
        OreReference oreReference = key.getOreReference();
        if (oreReference == null) {
            return findFuzzyItem(key, fuzzy, false);
        }

        Collection<AEItemKey> equivalentKeys = oreReference.getAEEquivalentKeys();
        if (equivalentKeys.size() == 1) {
            AEItemKey equivalentKey = equivalentKeys.iterator().next();
            return findFuzzyItem(equivalentKey, fuzzy, equivalentKey.getItemDamage() == OreDictionary.WILDCARD_VALUE);
        }

        ArrayList<Object2LongMap.Entry<AEKey>> output = new ArrayList<>();
        // Duplicates come from a bucket visited for several equivalent keys, so they share the key instance.
        ReferenceOpenHashSet<AEKey> seen = new ReferenceOpenHashSet<>();
        for (AEItemKey equivalentKey : equivalentKeys) {
            boolean ignoreMeta = equivalentKey.getItemDamage() == OreDictionary.WILDCARD_VALUE;
            for (Object2LongMap.Entry<AEKey> entry : findFuzzyItem(equivalentKey, fuzzy, ignoreMeta)) {
                if (seen.add(entry.getKey())) {
                    output.add(entry);
                }
            }
        }
        return output;
    }

    private Collection<Object2LongMap.Entry<AEKey>> findFuzzyItem(AEItemKey key, FuzzyMode fuzzy, boolean ignoreMeta) {
        VariantCounter counter = this.itemCounters.get(key.getItem());
        if (counter == null) {
            return Collections.emptyList();
        }
        return ignoreMeta ? counter.entries() : counter.findFuzzy(key, fuzzy);
    }

    private VariantCounter getItemCounter(AEItemKey key) {
        Item item = key.getItem();
        VariantCounter counter = this.itemCounters.get(item);
        if (counter == null) {
            counter = item.isDamageable() ? new VariantCounter.FuzzyVariantMap()
                    : new VariantCounter.MetadataVariantMap();
            this.itemCounters.put(item, counter);
        }
        return counter;
    }

    private VariantCounter getSimpleCounter(AEKey key) {
        Object primaryKey = key.getPrimaryKey();
        VariantCounter counter = this.simpleCounters.get(primaryKey);
        if (counter == null) {
            // Fuzzy searches of such types select variants by damage level, like damageable items.
            counter = key.supportsFuzzyRangeSearch() ? new VariantCounter.FuzzyVariantMap()
                    : new VariantCounter.UnorderedVariantMap();
            this.simpleCounters.put(primaryKey, counter);
        }
        return counter;
    }
}
