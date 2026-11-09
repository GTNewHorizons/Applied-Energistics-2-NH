package appeng.api.stacks;

import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEStack;
import io.netty.buffer.ByteBuf;

/**
 * Uniquely identifies something that "stacks" within an ME inventory.
 * <p/>
 * For example for items, this is the combination of an {@link net.minecraft.item.Item}, damage and optional
 * {@link NBTTagCompound}. To account for common indexing scenarios, a key is (optionally) split into a primary and
 * secondary component, which serves two purposes:
 * <ul>
 * <li>Fuzzy cards allow setting filters for the primary component of a key, i.e. for an
 * {@link net.minecraft.item.Item}, while disregarding the compound tag.</li>
 * <li>When indexing resources, it is usually assumed that indexing by the primary key alone offers a good trade-off
 * between memory usage and lookup speed.</li>
 * </ul>
 */
public abstract class AEKey {

    private static final Logger LOG = LogManager.getLogger(AEKey.class);

    public static final String TYPE_FIELD = "StackType";

    /**
     * @return An object giving additional properties about the type of key.
     */
    public abstract AEKeyType getType();

    /**
     * @return This object if it has no secondary component, otherwise a copy of this resource key with the secondary
     *         component removed.
     */
    public abstract AEKey dropSecondary();

    public abstract Object getPrimaryKey();

    protected abstract void writeIdentityToNBT(NBTTagCompound output);

    protected abstract void writeIdentityToPacket(ByteBuf output) throws IOException;

    /**
     * @return If {@link #getFuzzySearchMaxValue()} is greater than 0, the damage of this key, which fuzzy modes compare
     *         to the max value in percent. Values above the max value count as fully damaged.
     */
    public int getFuzzySearchValue() {
        return 0;
    }

    /**
     * @return The max damage for {@link #getFuzzySearchValue()}. If it is not greater than 0, the key counts as
     *         undamaged in fuzzy searches.
     */
    public int getFuzzySearchMaxValue() {
        return 0;
    }

    /**
     * The fuzzy damage level that fuzzy searches index and match this key by. Must be the same for equal keys.
     */
    int getFuzzyDamageLevel() {
        return FuzzySearch.getDamageLevel(this.getFuzzySearchValue(), this.getFuzzySearchMaxValue());
    }

    /**
     * True to indicate that this type of {@link AEKey} supports damage-based fuzzy search using
     * {@link AEKey#getFuzzySearchValue()} and {@link AEKey#getFuzzySearchMaxValue()}.
     * <p/>
     * For items this is used for damage-based search and filtering.
     */
    public final boolean supportsFuzzyRangeSearch() {
        return this.getType().supportsFuzzyRangeSearch();
    }

    /**
     * Tests if the given AE key is in the same fuzzy partition as this key given a specific fuzzy matching mode. This
     * key acts as the filter, and the result matches {@link KeyCounter#findFuzzy} with this key.
     */
    public boolean fuzzyEquals(@Nullable AEKey other, FuzzyMode mode) {
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }
        // For any fuzzy mode, the primary key (item, fluid) must still match
        if (this.getPrimaryKey() != other.getPrimaryKey()) {
            return false;
        }
        // If the type doesn't support fuzzy range search, it always behaves like IGNORE_ALL, which just ignores NBT
        return !this.supportsFuzzyRangeSearch()
                || FuzzySearch.matches(this.getFuzzyDamageLevel(), mode, other.getFuzzyDamageLevel());
    }

    public final int getAmountPerOperation() {
        return this.getType().getAmountPerOperation();
    }

    public final int getAmountPerByte() {
        return this.getType().getAmountPerByte();
    }

    public final int getAmountPerUnit() {
        return this.getType().getAmountPerUnit();
    }

    /**
     * Like {@link #toAEStack(long)}, with {@link #getAmountPerUnit()} as the amount.
     */
    public final IAEStack<?> toAEStack() {
        return this.toAEStack(this.getAmountPerUnit());
    }

    /**
     * Converts this key to an {@link IAEStack} for code that does not use keys yet. Goes away together with
     * {@link IAEStack}.
     */
    public abstract IAEStack<?> toAEStack(long amount);

    /**
     * @return The ID of the resource identified by this key.
     */
    public abstract String getId();

    /**
     * @return The ID of the mod this resource belongs to.
     */
    public abstract String getModId();

    public abstract String getDisplayName();

    public abstract IChatComponent getDisplayComponent();

    /**
     * Wraps a key in an ItemStack that can be unwrapped into a key later.
     */
    @Nullable
    public abstract ItemStack wrapForDisplayOrFilter();

    /**
     * Like {@link #wrapForDisplayOrFilter()}, with {@code amount} shown on the stack.
     */
    @Nullable
    public abstract ItemStack wrapForDisplayOrFilter(long amount);

    public boolean hasTag() {
        return false;
    }

    public final NBTTagCompound toNBT() {
        NBTTagCompound output = new NBTTagCompound();
        this.writeToNBT(output);
        return output;
    }

    public final void writeToNBT(NBTTagCompound output) {
        output.setString(TYPE_FIELD, this.getType().getId());
        this.writeIdentityToNBT(output);
    }

    public final void writeToPacket(ByteBuf output) throws IOException {
        output.writeByte(AEKeyTypeRegistry.getNetworkId(this.getType()));
        this.writeIdentityToPacket(output);
    }

    @Nullable
    public static AEKey fromNBT(@Nullable NBTTagCompound input) {
        if (input == null || input.hasNoTags()) {
            return null;
        }

        String typeId = input.getString(TYPE_FIELD);
        AEKeyType type = typeId.isEmpty() ? null : AEKeyType.fromId(typeId);
        if (type == null) {
            LOG.warn("Cannot deserialize AEKey from {} because the key type is missing.", input);
            return null;
        }

        return type.readFromNBT(input);
    }

    public static void writeKey(ByteBuf output, @Nullable AEKey key) throws IOException {
        if (key == null) {
            output.writeByte(AEKeyTypeRegistry.NULL_NETWORK_ID);
        } else {
            key.writeToPacket(output);
        }
    }

    @Nullable
    public static AEKey readKey(ByteBuf input) throws IOException {
        byte typeId = input.readByte();
        if (typeId == AEKeyTypeRegistry.NULL_NETWORK_ID) {
            return null;
        }

        AEKeyType keyType = AEKeyTypeRegistry.getByNetworkId(typeId);
        if (keyType == null) {
            LOG.error("Received unknown key type id {}", typeId);
            return null;
        }

        return keyType.readFromPacket(input);
    }

    public final boolean matches(@Nullable AEKey key) {
        return this.equals(key);
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();
}
