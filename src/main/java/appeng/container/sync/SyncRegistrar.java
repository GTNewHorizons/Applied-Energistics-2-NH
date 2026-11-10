package appeng.container.sync;

import org.jspecify.annotations.NonNull;

import appeng.api.config.Settings;
import appeng.api.storage.GenericStackSlots;
import appeng.api.util.IConfigManager;
import appeng.container.sync.handlers.BooleanSyncHandler;
import appeng.container.sync.handlers.ConfigEnumSyncHandler;
import appeng.container.sync.handlers.DeltaObjectSyncHandler;
import appeng.container.sync.handlers.DoubleSyncHandler;
import appeng.container.sync.handlers.GenericStackSlotsSyncHandler;
import appeng.container.sync.handlers.IntSyncHandler;
import appeng.container.sync.handlers.LongSyncHandler;
import appeng.container.sync.handlers.ObjectSyncHandler;

public interface SyncRegistrar {

    /**
     * Creates a child registrar whose handlers are registered under the given key as an additional fullKey scope
     * segment.
     */
    @NonNull
    SyncRegistrar child(@NonNull String key);

    @NonNull
    IntSyncHandler intS2C(@NonNull String key);

    @NonNull
    IntSyncHandler intC2S(@NonNull String key);

    @NonNull
    IntSyncHandler intSync(@NonNull String key);

    @NonNull
    LongSyncHandler longS2C(@NonNull String key);

    @NonNull
    LongSyncHandler longC2S(@NonNull String key);

    @NonNull
    LongSyncHandler longSync(@NonNull String key);

    @NonNull
    BooleanSyncHandler booleanS2C(@NonNull String key);

    @NonNull
    BooleanSyncHandler booleanC2S(@NonNull String key);

    @NonNull
    BooleanSyncHandler booleanSync(@NonNull String key);

    @NonNull
    DoubleSyncHandler doubleS2C(@NonNull String key);

    @NonNull
    DoubleSyncHandler doubleC2S(@NonNull String key);

    @NonNull
    DoubleSyncHandler doubleSync(@NonNull String key);

    @NonNull
    <T, D> DeltaObjectSyncHandler<T, D> objectS2C(@NonNull String key, @NonNull DeltaSyncCodec<T, D> codec,
            T initialValue);

    @NonNull
    <T> ObjectSyncHandler<T> objectS2C(@NonNull String key, @NonNull SyncCodec<T> codec, T initialValue);

    @NonNull
    <T, D> DeltaObjectSyncHandler<T, D> objectC2S(@NonNull String key, @NonNull DeltaSyncCodec<T, D> codec,
            T initialValue);

    @NonNull
    <T> ObjectSyncHandler<T> objectC2S(@NonNull String key, @NonNull SyncCodec<T> codec, T initialValue);

    @NonNull
    <T, D> DeltaObjectSyncHandler<T, D> object(@NonNull String key, @NonNull DeltaSyncCodec<T, D> codec,
            T initialValue);

    @NonNull
    <T> ObjectSyncHandler<T> object(@NonNull String key, @NonNull SyncCodec<T> codec, T initialValue);

    @NonNull
    <T> ActionHandler<T> actionS2C(@NonNull String key, @NonNull StreamCodec<T> codec);

    @NonNull
    ActionHandler<Void> actionS2C(@NonNull String key);

    @NonNull
    <T> ActionHandler<T> actionC2S(@NonNull String key, @NonNull StreamCodec<T> codec);

    @NonNull
    ActionHandler<Void> actionC2S(@NonNull String key);

    @NonNull
    <T> ActionHandler<T> action(@NonNull String key, @NonNull StreamCodec<T> codec);

    @NonNull
    ActionHandler<Void> action(@NonNull String key);

    @NonNull
    <E extends Enum<E>> ConfigEnumSyncHandler<E> configEnum(@NonNull String key, @NonNull Settings setting,
            @NonNull Class<E> enumClass, @NonNull IConfigManager configManager);

    @NonNull
    <T extends GenericStackSlots> GenericStackSlotsSyncHandler<T> genericStackSlotsS2C(@NonNull String key,
            @NonNull T slots);

    @NonNull
    <T extends GenericStackSlots> GenericStackSlotsSyncHandler<T> genericStackSlotsC2S(@NonNull String key,
            @NonNull T slots);

    @NonNull
    <T extends GenericStackSlots> GenericStackSlotsSyncHandler<T> genericStackSlots(@NonNull String key,
            @NonNull T slots);
}
