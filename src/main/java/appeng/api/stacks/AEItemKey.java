package appeng.api.stacks;

import java.io.IOException;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import appeng.api.config.FuzzyMode;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import appeng.util.item.AESharedNBT;
import appeng.util.item.OreHelper;
import appeng.util.item.OreReference;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

/**
 * Identifies an item by {@link Item}, damage and NBT. The NBT is shared through {@link AESharedNBT}, so equality of
 * tags is a reference comparison. AESharedNBT goes away together with {@link AEItemStack}; until then it is not exposed
 * outside of {@link #getSharedTag()}.
 */
public final class AEItemKey extends AEKey {

    private static final int DAMAGE_BITS = 16;
    private static final long MAX_PACKET_NBT_SIZE = 2097152L;

    private final Item item;
    private final int damage;
    private static final int NOT_COMPUTED = -1;

    /**
     * The display damage, max damage and fuzzy damage level of damageable items. All are 0 for other items. Keys
     * created from an existing stack get them right away. Otherwise they need a new stack, so they are computed on
     * first use. The volatile level is written last and publishes all three values.
     */
    private int fuzzySearchValue;
    private int fuzzySearchMaxValue;
    private volatile int fuzzyDamageLevel;
    /**
     * Read-only stack of size 1, created on first use so that keys which are only counted do not keep a copy of the
     * NBT. Its tag is a private copy, never the shared one, because some items write to NBT while being displayed.
     */
    private volatile @Nullable ItemStack readonlyStack;
    private final @Nullable AESharedNBT tag;
    private @Nullable String modId;
    private final int hashCode;
    private @Nullable OreReference oreReference;
    private volatile boolean oreReferenceResolved;

    /**
     * Creates a key whose fuzzy values are computed on first use, if the item is damageable.
     */
    private AEItemKey(Item item, int damage, @Nullable AESharedNBT tag) {
        this.item = item;
        this.damage = Math.max(damage, 0);
        this.tag = tag;
        this.hashCode = computeHashCode(item, this.damage, tag);

        // Only damageable items use fuzzy damage levels, and only they may need NBT to compute their durability.
        this.fuzzyDamageLevel = item.isDamageable() ? NOT_COMPUTED : 0;
    }

    /**
     * Creates a key of a damageable item with fuzzy values read from an existing stack.
     */
    private AEItemKey(Item item, int damage, @Nullable AESharedNBT tag, int fuzzySearchValue, int fuzzySearchMaxValue) {
        this.item = item;
        this.damage = Math.max(damage, 0);
        this.tag = tag;
        this.hashCode = computeHashCode(item, this.damage, tag);
        this.fuzzySearchValue = fuzzySearchValue;
        this.fuzzySearchMaxValue = fuzzySearchMaxValue;
        this.fuzzyDamageLevel = FuzzySearch.getDamageLevel(fuzzySearchValue, fuzzySearchMaxValue);
    }

    private static int computeHashCode(Item item, int damage, @Nullable AESharedNBT tag) {
        return (Item.getIdFromItem(item) << DAMAGE_BITS | damage) ^ (tag == null ? 0 : tag.hashCode());
    }

    /**
     * Uses a fresh stack because the tag of {@link #getReadonlyStack()} may have been changed while it was displayed.
     */
    private int computeFuzzyValues() {
        final ItemStack stack = this.toStack();
        this.fuzzySearchValue = getDisplayDamage(stack, this.damage);
        this.fuzzySearchMaxValue = getMaxDamage(stack, this.item);
        final int level = FuzzySearch.getDamageLevel(this.fuzzySearchValue, this.fuzzySearchMaxValue);
        this.fuzzyDamageLevel = level;
        return level;
    }

    /**
     * Some items throw from {@link ItemStack#getItemDamageForDisplay()}; fall back to the raw damage.
     */
    private static int getDisplayDamage(ItemStack stack, int fallback) {
        try {
            return stack.getItemDamageForDisplay();
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static int getMaxDamage(ItemStack stack, Item item) {
        try {
            return stack.getMaxDamage();
        } catch (Throwable e) {
            return item.getMaxDamage();
        }
    }

    /**
     * Reads the raw damage value. {@link ItemStack#getItemDamage()} delegates to {@link Item#getDamage(ItemStack)},
     * which some items override; asking an item that does not override it returns the stored value.
     */
    private static int getRawDamage(ItemStack stack) {
        return Items.blaze_rod.getDamage(stack);
    }

    @Nullable
    public static AEItemKey of(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }

        final Item item = stack.getItem();
        final int damage = getRawDamage(stack);
        final AESharedNBT tag = share(stack.getTagCompound(), stack);
        if (!item.isDamageable()) {
            return new AEItemKey(item, damage, tag);
        }
        // The stack is at hand, so read the durability now. The tag was shared first, since reading the durability may
        // write to the NBT of the stack.
        return new AEItemKey(item, damage, tag, getDisplayDamage(stack, damage), getMaxDamage(stack, item));
    }

    public static AEItemKey of(Item item) {
        return of(item, 0, null);
    }

    public static AEItemKey of(Item item, int damage) {
        return of(item, damage, null);
    }

    public static AEItemKey of(Item item, int damage, @Nullable NBTTagCompound tag) {
        return new AEItemKey(item, damage, tag == null ? null : AESharedNBT.getSharedTagCompound(tag, item, damage));
    }

    /**
     * @return the shared instance of {@code tag} for the item and damage of {@code stack}, or null if {@code tag} is
     *         null or empty
     */
    private static @Nullable AESharedNBT share(@Nullable NBTTagCompound tag, ItemStack stack) {
        return tag == null ? null : (AESharedNBT) AESharedNBT.getSharedTagCompound(tag, stack);
    }

    @Nullable
    public static AEItemKey fromNBT(@Nullable NBTTagCompound input) {
        if (input == null || input.hasNoTags()) {
            return null;
        }

        final ItemStack stack = ItemStack.loadItemStackFromNBT(input);
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        stack.stackSize = 1;
        return of(stack);
    }

    @Nullable
    public static AEItemKey fromPacket(ByteBuf input) throws IOException {
        int id = ByteBufUtils.readVarInt(input, 5);
        int damage = input.readShort();
        NBTTagCompound tag = input.readBoolean()
                ? CompressedStreamTools
                        .func_152456_a(new ByteBufInputStream(input), new NBTSizeTracker(MAX_PACKET_NBT_SIZE))
                : null;

        Item item = Item.getItemById(id);
        return item == null ? null : of(item, damage, tag);
    }

    @Override
    public AEKeyType getType() {
        return AEKeyType.items();
    }

    /**
     * Drops the NBT, and the damage of damageable items. The metadata of other items identifies a variant, like a
     * separate item, so it is kept.
     */
    @Override
    public AEItemKey dropSecondary() {
        if (!this.item.isDamageable()) {
            return this.tag == null ? this : of(this.item, this.damage);
        }
        return this.damage == 0 && this.tag == null ? this : of(this.item);
    }

    @Override
    public Item getPrimaryKey() {
        return this.item;
    }

    @Override
    protected void writeIdentityToNBT(NBTTagCompound output) {
        // ItemStack writes the item ID in the format of the ID extension mod, if one is installed (e.g. EndlessIDs
        // stores IDs above the vanilla range in an extra int tag).
        this.toStack().writeToNBT(output);
        output.removeTag("Count");
    }

    @Override
    protected void writeIdentityToPacket(ByteBuf output) throws IOException {
        ByteBufUtils.writeVarInt(output, Item.getIdFromItem(this.item), 5);
        output.writeShort(this.damage);
        output.writeBoolean(this.tag != null);
        if (this.tag != null) {
            CompressedStreamTools.write(this.tag, new ByteBufOutputStream(output));
        }
    }

    public Item getItem() {
        return this.item;
    }

    /**
     * @return a stack of size 1 that represents this key. It is shared and must not be modified; use {@link #toStack()}
     *         for a stack that can be changed.
     */
    public ItemStack getReadonlyStack() {
        ItemStack result = this.readonlyStack;
        if (result == null) {
            // Racing threads create equal stacks, so publishing either of them is fine.
            result = this.toStack();
            this.readonlyStack = result;
        }
        return result;
    }

    public int getItemDamage() {
        return this.damage;
    }

    /**
     * The display damage, which may differ from the raw damage {@link #getItemDamage()}.
     */
    @Override
    public int getFuzzySearchValue() {
        // Reading the volatile level first makes the other fuzzy values visible.
        if (this.fuzzyDamageLevel == NOT_COMPUTED) {
            this.computeFuzzyValues();
        }
        return this.fuzzySearchValue;
    }

    @Override
    public int getFuzzySearchMaxValue() {
        if (this.fuzzyDamageLevel == NOT_COMPUTED) {
            this.computeFuzzyValues();
        }
        return this.fuzzySearchMaxValue;
    }

    @Override
    int getFuzzyDamageLevel() {
        final int level = this.fuzzyDamageLevel;
        return level != NOT_COMPUTED ? level : this.computeFuzzyValues();
    }

    /**
     * Same rules as {@link KeyCounter#findFuzzy}: if this key has OreDictionary entries, the registered ore stacks are
     * used as filters instead of this key. Ore stacks with the wildcard damage match every damage value; the others
     * match by fuzzy damage level if the item is damageable, otherwise by exact damage.
     */
    @Override
    public boolean fuzzyEquals(@Nullable AEKey other, FuzzyMode mode) {
        if (!(other instanceof AEItemKey candidate)) {
            return false;
        }

        final OreReference oreReference = this.getOreReference();
        if (oreReference == null) {
            return this.fuzzyEqualsIgnoringOres(candidate, mode, false);
        }
        for (AEItemKey equivalentKey : oreReference.getAEEquivalentKeys()) {
            boolean ignoreMeta = equivalentKey.damage == OreDictionary.WILDCARD_VALUE;
            if (equivalentKey.fuzzyEqualsIgnoringOres(candidate, mode, ignoreMeta)) {
                return true;
            }
        }
        return false;
    }

    private boolean fuzzyEqualsIgnoringOres(AEItemKey candidate, FuzzyMode mode, boolean ignoreMeta) {
        if (candidate.item != this.item) {
            return false;
        }
        if (ignoreMeta) {
            return true;
        }
        if (this.item.isDamageable()) {
            return FuzzySearch.matches(this.getFuzzyDamageLevel(), mode, candidate.getFuzzyDamageLevel());
        }
        return candidate.damage == this.damage;
    }

    /**
     * @return the ore dictionary entries of this key, or null if it has none
     */
    @Nullable
    OreReference getOreReference() {
        if (!this.oreReferenceResolved) {
            // Racing threads get the same reference from OreHelper, so publishing either of them is fine.
            this.oreReference = OreHelper.INSTANCE.getReference(this);
            this.oreReferenceResolved = true;
        }
        return this.oreReference;
    }

    @Override
    public String getId() {
        return String.valueOf(Item.itemRegistry.getNameForObject(this.item));
    }

    @Override
    public String getModId() {
        if (this.modId == null) {
            UniqueIdentifier uid = GameRegistry.findUniqueIdentifierFor(this.item);
            this.modId = uid == null || uid.modId == null ? "** Null" : uid.modId;
        }

        return this.modId;
    }

    @Override
    public String getDisplayName() {
        final ItemStack stack = this.getReadonlyStack();
        try {
            final String name = stack.getDisplayName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
            return String.valueOf(this.item.getUnlocalizedName(stack));
        } catch (Exception e) {
            try {
                return String.valueOf(stack.getUnlocalizedName());
            } catch (Exception e2) {
                return "** Exception";
            }
        }
    }

    @Override
    public IChatComponent getDisplayComponent() {
        return this.getReadonlyStack().func_151000_E();
    }

    @Override
    public boolean hasTag() {
        return this.tag != null;
    }

    /**
     * @return a mutable copy of the tag, or null if this key has no NBT.
     */
    @Nullable
    public NBTTagCompound getTag() {
        return this.tag == null ? null : (NBTTagCompound) this.tag.copy();
    }

    /**
     * For {@link AEItemStack}, which exposes the shared tag as {@link appeng.api.storage.data.IAETagCompound}. Goes
     * away together with it.
     *
     * @return the shared tag. Must not be modified.
     */
    @Nullable
    @ApiStatus.Internal
    public AESharedNBT getSharedTag() {
        return this.tag;
    }

    public ItemStack toStack() {
        return this.toStack(1);
    }

    /**
     * @return a new stack of this key with a private copy of the tag.
     */
    public ItemStack toStack(long amount) {
        final ItemStack result = new ItemStack(this.item, Platform.longToInt(amount), this.damage);
        if (this.tag != null) {
            result.setTagCompound((NBTTagCompound) this.tag.copy());
        }
        return result;
    }

    @Override
    public IAEItemStack toAEStack(long amount) {
        return AEItemStack.create(this.toStack()).setStackSize(amount);
    }

    @Override
    public ItemStack wrapForDisplayOrFilter() {
        return this.wrapForDisplayOrFilter(1L);
    }

    @Override
    public ItemStack wrapForDisplayOrFilter(long amount) {
        return this.toStack(amount);
    }

    public boolean matches(@Nullable ItemStack other) {
        if (other == null || other.getItem() != this.item || getRawDamage(other) != this.damage) {
            return false;
        }

        final NBTTagCompound otherTag = other.getTagCompound();
        if (otherTag == null || otherTag.hasNoTags()) {
            return this.tag == null;
        }
        return this.tag != null && Platform.NBTEqualityTest(this.tag, otherTag);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof AEItemKey other && this.item == other.item
                && this.damage == other.damage
                && this.tag == other.tag;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return this.getReadonlyStack().toString();
    }
}
