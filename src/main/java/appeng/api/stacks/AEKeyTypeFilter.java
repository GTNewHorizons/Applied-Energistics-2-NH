package appeng.api.stacks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;

import org.jspecify.annotations.Nullable;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/**
 * Selection of enabled {@link AEKeyType}s, saved by type ID. Types are enabled unless disabled, also types registered
 * after this filter was created. The NBT format must stay readable for data that was saved before keys existed. Copy
 * before retaining a mutable selection as a query identity.
 */
public class AEKeyTypeFilter {

    public static final String NBT_FILTERS = "typeFilters";
    private static final String NBT_TYPE_ID = "typeId";
    private static final String NBT_VALUE = "value";

    private final Set<AEKeyType> disabled = new ReferenceOpenHashSet<>();

    public AEKeyTypeFilter() {}

    public AEKeyTypeFilter(AEKeyTypeFilter other) {
        this.disabled.addAll(other.disabled);
    }

    /**
     * Reads the representation written by {@link #writeToPacket(ByteBuf)}.
     */
    public AEKeyTypeFilter(ByteBuf buf) {
        final int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            final AEKeyType type = AEKeyTypeRegistry.get(ByteBufUtils.readUTF8String(buf));
            if (type != null) {
                this.disabled.add(type);
            }
        }
    }

    public void readFromNBT(NBTTagCompound tag) {
        if (!tag.hasKey(NBT_FILTERS)) {
            return;
        }

        this.disabled.clear();

        // Older data also lists enabled types.
        final NBTTagList list = tag.getTagList(NBT_FILTERS, NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            final NBTTagCompound entryTag = list.getCompoundTagAt(i);
            final AEKeyType type = AEKeyTypeRegistry.get(entryTag.getString(NBT_TYPE_ID));
            if (type != null && !entryTag.getBoolean(NBT_VALUE)) {
                this.disabled.add(type);
            }
        }
    }

    public void writeToNBT(NBTTagCompound tag) {
        final NBTTagList list = new NBTTagList();
        for (AEKeyType type : this.disabled) {
            final NBTTagCompound entryTag = new NBTTagCompound();
            entryTag.setString(NBT_TYPE_ID, type.getId());
            entryTag.setBoolean(NBT_VALUE, false);
            list.appendTag(entryTag);
        }

        tag.setTag(NBT_FILTERS, list);
    }

    public void writeToPacket(ByteBuf buf) {
        buf.writeInt(this.disabled.size());
        for (AEKeyType type : this.disabled) {
            ByteBufUtils.writeUTF8String(buf, type.getId());
        }
    }

    public boolean isEnabled(AEKeyType type) {
        return !this.disabled.contains(type);
    }

    public boolean matches(AEKey key) {
        return this.isEnabled(key.getType());
    }

    public void copyFrom(AEKeyTypeFilter other) {
        this.disabled.clear();
        this.disabled.addAll(other.disabled);
    }

    /**
     * @return whether each registered type is enabled
     */
    public Reference2BooleanMap<AEKeyType> getKeyTypeFilters() {
        final Reference2BooleanMap<AEKeyType> result = new Reference2BooleanOpenHashMap<>();
        result.defaultReturnValue(true);
        for (AEKeyType type : AEKeyTypeRegistry.getAllTypes()) {
            result.put(type, this.isEnabled(type));
        }
        return Reference2BooleanMaps.unmodifiable(result);
    }

    public void setEnabled(AEKeyType type, boolean enabled) {
        if (enabled) {
            this.disabled.remove(type);
        } else {
            this.disabled.add(type);
        }
    }

    public boolean toggle(AEKeyType type) {
        final boolean next = !this.isEnabled(type);
        this.setEnabled(type, next);
        return next;
    }

    public void setOnlyEnabled(AEKeyType targetType) {
        this.disabled.clear();
        for (AEKeyType type : AEKeyTypeRegistry.getAllTypes()) {
            if (type != targetType) {
                this.disabled.add(type);
            }
        }
    }

    public void setAllEnabled(boolean enabled) {
        this.disabled.clear();
        if (!enabled) {
            this.disabled.addAll(AEKeyTypeRegistry.getAllTypes());
        }
    }

    public List<AEKeyType> getEnabledKeyTypes() {
        final List<AEKeyType> enabledTypes = new ArrayList<>();
        for (AEKeyType type : AEKeyTypeRegistry.getAllTypes()) {
            if (this.isEnabled(type)) {
                enabledTypes.add(type);
            }
        }
        return enabledTypes;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof AEKeyTypeFilter other && this.disabled.equals(other.disabled);
    }

    @Override
    public int hashCode() {
        return this.disabled.hashCode();
    }
}
