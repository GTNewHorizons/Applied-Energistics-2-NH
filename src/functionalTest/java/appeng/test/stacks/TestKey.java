package appeng.test.stacks;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;

import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.storage.data.IAEStack;
import io.netty.buffer.ByteBuf;

/**
 * A key of a non-item type with variants, which needs no game data. Keys with the same primary key are variants of each
 * other, told apart by their name and damage.
 */
final class TestKey extends AEKey {

    /** Supports fuzzy range search by damage. */
    static final AEKeyType FUZZY = new Type("test_fuzzy", true);
    /** Does not support fuzzy range search, so its damage only tells variants apart. */
    static final AEKeyType PLAIN = new Type("test_plain", false);

    private static final Object FUZZY_PRIMARY_KEY = new Object();
    private static final Object PLAIN_PRIMARY_KEY = new Object();

    private final AEKeyType type;
    private final Object primaryKey;
    private final String name;
    private final int value;
    private final int maxValue;

    TestKey(AEKeyType type, Object primaryKey, String name, int value, int maxValue) {
        this.type = type;
        this.primaryKey = primaryKey;
        this.name = name;
        this.value = value;
        this.maxValue = maxValue;
    }

    static TestKey fuzzy(String name, int value, int maxValue) {
        return new TestKey(FUZZY, FUZZY_PRIMARY_KEY, name, value, maxValue);
    }

    static TestKey plain(String name, int value, int maxValue) {
        return new TestKey(PLAIN, PLAIN_PRIMARY_KEY, name, value, maxValue);
    }

    /**
     * @return a variant of this key with another primary key
     */
    TestKey withOtherPrimaryKey() {
        return new TestKey(this.type, new Object(), this.name, this.value, this.maxValue);
    }

    /**
     * @return a variant of this key with the same primary key
     */
    TestKey renamed(String name) {
        return new TestKey(this.type, this.primaryKey, name, this.value, this.maxValue);
    }

    @Override
    public AEKeyType getType() {
        return this.type;
    }

    @Override
    public AEKey dropSecondary() {
        return this;
    }

    @Override
    public Object getPrimaryKey() {
        return this.primaryKey;
    }

    @Override
    public int getFuzzySearchValue() {
        return this.value;
    }

    @Override
    public int getFuzzySearchMaxValue() {
        return this.maxValue;
    }

    @Override
    protected void writeIdentityToNBT(NBTTagCompound output) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void writeIdentityToPacket(ByteBuf output) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IAEStack<?> toAEStack(long amount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() {
        return this.name;
    }

    @Override
    public String getModId() {
        return "test";
    }

    @Override
    public String getDisplayName() {
        return this.name;
    }

    @Override
    public IChatComponent getDisplayComponent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable ItemStack wrapForDisplayOrFilter() {
        return null;
    }

    @Override
    public @Nullable ItemStack wrapForDisplayOrFilter(long amount) {
        return null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof TestKey other && this.type == other.type
                && this.primaryKey == other.primaryKey
                && this.name.equals(other.name)
                && this.value == other.value
                && this.maxValue == other.maxValue;
    }

    @Override
    public int hashCode() {
        return ((System.identityHashCode(this.primaryKey) * 31 + this.name.hashCode()) * 31 + this.value) * 31
                + this.maxValue;
    }

    @Override
    public String toString() {
        return this.type + ":" + this.name + "@" + this.value + "/" + this.maxValue;
    }

    private static final class Type extends AEKeyType {

        private final boolean fuzzy;

        private Type(String id, boolean fuzzy) {
            super(id, TestKey.class);
            this.fuzzy = fuzzy;
        }

        @Override
        public int getAmountPerOperation() {
            return 1;
        }

        @Override
        public int getAmountPerByte() {
            return 1;
        }

        @Override
        public int getAmountPerUnit() {
            return 1;
        }

        @Override
        public @Nullable String getUnitSymbol() {
            return null;
        }

        @Override
        public boolean supportsFuzzyRangeSearch() {
            return this.fuzzy;
        }

        @Override
        public @Nullable AEKey readFromNBT(NBTTagCompound input) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable AEKey readFromPacket(ByteBuf input) {
            throw new UnsupportedOperationException();
        }
    }
}
