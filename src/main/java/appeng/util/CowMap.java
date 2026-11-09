package appeng.util;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

import org.jspecify.annotations.NullMarked;

/**
 * Thread-safe copy-on-write map wrapper. Does not accept null keys or values.
 */
@NullMarked
public final class CowMap<K, V> {

    private final IntFunction<? extends Map<K, V>> mapSupplier;
    private volatile Map<K, V> map;

    public CowMap(IntFunction<? extends Map<K, V>> mapSupplier) {
        this.mapSupplier = mapSupplier;
        this.map = Collections.unmodifiableMap(mapSupplier.apply(0));
    }

    public static <K, V> CowMap<K, V> identityHashMap() {
        return new CowMap<>(IdentityHashMap::new);
    }

    /**
     * Unlike {@link Map#putIfAbsent}, throws if a value is already present.
     *
     * @throws IllegalArgumentException if the map already contains a value for {@code key}
     */
    public void putIfAbsent(K key, V value) {
        Objects.requireNonNull(key, "Key may not be null");
        Objects.requireNonNull(value, "Value may not be null");

        synchronized (this) {
            if (this.map.containsKey(key)) {
                throw new IllegalArgumentException("Map already contains a value for the following key: " + key);
            }
            Map<K, V> newMap = this.mapSupplier.apply(this.map.size() + 1);
            newMap.putAll(this.map);
            newMap.put(key, value);
            this.map = Collections.unmodifiableMap(newMap);
        }
    }

    /**
     * @return the current unmodifiable map. Later additions are not reflected in the returned object.
     */
    public Map<K, V> getMap() {
        return this.map;
    }
}
