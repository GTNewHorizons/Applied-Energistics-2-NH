package appeng.api.stacks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import it.unimi.dsi.fastutil.bytes.Byte2ReferenceMap;
import it.unimi.dsi.fastutil.bytes.Byte2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ByteMap;
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;

/**
 * Registry of all {@link AEKeyType}s. Network ids are assigned in {@link #finalizeRegistration()} with a stable order
 * (items, fluids, then the remaining types sorted by id) so that client and server agree on them.
 */
public final class AEKeyTypeRegistry {

    public static final byte NULL_NETWORK_ID = 0;
    private static final byte MINIMUM_NETWORK_ID = 1;

    private static final Map<String, AEKeyType> registry = new LinkedHashMap<>();
    private static final Reference2ByteMap<AEKeyType> typeToNetworkIdMap = new Reference2ByteOpenHashMap<>();
    private static final Byte2ReferenceMap<AEKeyType> networkIdToTypeMap = new Byte2ReferenceOpenHashMap<>();
    /** Set by {@link #finalizeRegistration()}; types registered later would have no network id. */
    private static boolean finalized;

    static {
        register(AEKeyType.items());
        register(AEKeyType.fluids());
    }

    private AEKeyTypeRegistry() {}

    public static synchronized void register(AEKeyType type) {
        Objects.requireNonNull(type, "type");
        if (finalized) {
            throw new IllegalStateException(
                    "Cannot register AE key type " + type.getId() + " after registration was finalized.");
        }
        if (registry.containsKey(type.getId())) {
            throw new IllegalArgumentException("Duplicate AE key type id: " + type.getId());
        }
        registry.put(type.getId(), type);
    }

    @ApiStatus.Internal
    public static synchronized void finalizeRegistration() {
        if (finalized) {
            throw new IllegalStateException("AE key type registration was already finalized.");
        }
        finalized = true;
        sortRegistry();

        if (registry.size() > Byte.MAX_VALUE - MINIMUM_NETWORK_ID + 1) {
            throw new IllegalStateException("Too many AE key types: " + registry.size());
        }

        byte id = MINIMUM_NETWORK_ID;
        for (AEKeyType type : registry.values()) {
            typeToNetworkIdMap.put(type, id);
            networkIdToTypeMap.put(id, type);
            id++;
        }
    }

    @Nullable
    public static AEKeyType get(String id) {
        return registry.get(id);
    }

    @Nullable
    public static AEKeyType getByNetworkId(byte id) {
        return networkIdToTypeMap.get(id);
    }

    public static byte getNetworkId(AEKeyType type) {
        byte id = typeToNetworkIdMap.getByte(type);
        if (id < MINIMUM_NETWORK_ID) {
            throw new IllegalStateException(
                    "Cannot get network id for key type " + type.getId()
                            + " because it is not registered or not initialized yet.");
        }
        return id;
    }

    public static Collection<AEKeyType> getAllTypes() {
        return Collections.unmodifiableCollection(registry.values());
    }

    private static void sortRegistry() {
        ArrayList<AEKeyType> others = new ArrayList<>();
        for (AEKeyType type : registry.values()) {
            if (type != AEKeyType.items() && type != AEKeyType.fluids()) {
                others.add(type);
            }
        }
        others.sort(Comparator.comparing(AEKeyType::getId));

        registry.clear();
        registry.put(AEKeyType.items().getId(), AEKeyType.items());
        registry.put(AEKeyType.fluids().getId(), AEKeyType.fluids());
        for (AEKeyType type : others) {
            registry.put(type.getId(), type);
        }
    }
}
