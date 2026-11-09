package appeng.api.stacks;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.Nullable;

import io.netty.buffer.ByteBuf;

/**
 * Describes a kind of {@link AEKey} (items, fluids, ...). Instances are singletons registered in
 * {@link AEKeyTypeRegistry}.
 */
public abstract class AEKeyType {

    private static final AEKeyType ITEMS = new ItemKeyType();
    private static final AEKeyType FLUIDS = new FluidKeyType();

    private final String id;
    private final Class<? extends AEKey> keyClass;

    protected AEKeyType(String id, Class<? extends AEKey> keyClass) {
        this.id = id;
        this.keyClass = keyClass;
    }

    public static AEKeyType items() {
        return ITEMS;
    }

    public static AEKeyType fluids() {
        return FLUIDS;
    }

    @Nullable
    public static AEKeyType fromId(String id) {
        return AEKeyTypeRegistry.get(id);
    }

    public final String getId() {
        return this.id;
    }

    public final Class<? extends AEKey> getKeyClass() {
        return this.keyClass;
    }

    public final boolean contains(@Nullable AEKey key) {
        return this.keyClass.isInstance(key);
    }

    @Nullable
    public final AEKey tryCast(@Nullable AEKey key) {
        return this.keyClass.isInstance(key) ? this.keyClass.cast(key) : null;
    }

    public abstract int getAmountPerOperation();

    public abstract int getAmountPerByte();

    public abstract int getAmountPerUnit();

    @Nullable
    public abstract String getUnitSymbol();

    public boolean supportsFuzzyRangeSearch() {
        return false;
    }

    /**
     * Whether distinct keys of this type can share a {@linkplain AEKey#getPrimaryKey() primary key}, for example
     * because they carry NBT. If false, every key is the only key with its primary key: {@link KeyCounter} stores such
     * keys without grouping them by primary key, and fuzzy searches for them only match the key itself.
     */
    public boolean hasVariants() {
        return true;
    }

    @Nullable
    public abstract AEKey readFromNBT(NBTTagCompound input);

    @Nullable
    public abstract AEKey readFromPacket(ByteBuf input) throws IOException;

    @Override
    public String toString() {
        return this.id;
    }

}
