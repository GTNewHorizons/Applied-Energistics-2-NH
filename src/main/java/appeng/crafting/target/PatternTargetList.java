package appeng.crafting.target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import appeng.api.crafting.PatternMatch;
import appeng.container.sync.StreamCodec;
import appeng.container.sync.StreamCodecs;
import cpw.mods.fml.common.network.ByteBufUtils;

/**
 * What the Pattern Terminal shows about the providers for the pattern in its output slot.
 */
public final class PatternTargetList {

    public static final PatternTargetList EMPTY = new PatternTargetList(
            false,
            Collections.emptyList(),
            Collections.emptyList());

    public static final StreamCodec<PatternTargetList> CODEC = StreamCodecs
            .of(PatternTargetList.class.getName(), (buf, list) -> {
                buf.writeBoolean(list.hasPattern);
                buf.writeInt(list.entries.size());
                for (Entry entry : list.entries) {
                    buf.writeInt(entry.index);
                    ByteBufUtils.writeItemStack(buf, entry.icon);
                    ByteBufUtils.writeUTF8String(buf, entry.rawName);
                    buf.writeInt(entry.x);
                    buf.writeInt(entry.y);
                    buf.writeInt(entry.z);
                    buf.writeInt(entry.dim);
                    buf.writeByte(entry.match.ordinal());
                    buf.writeInt(entry.freeSlots);
                    buf.writeByte(entry.cardSource);
                    buf.writeBoolean(entry.hasSamePattern);
                }
                buf.writeInt(list.conflicts.size());
                for (Conflict conflict : list.conflicts) {
                    buf.writeInt(conflict.index);
                    ByteBufUtils.writeItemStack(buf, conflict.icon);
                    ByteBufUtils.writeUTF8String(buf, conflict.rawName);
                    buf.writeInt(conflict.x);
                    buf.writeInt(conflict.y);
                    buf.writeInt(conflict.z);
                    buf.writeInt(conflict.dim);
                    buf.writeBoolean(conflict.sameRecipe);
                }
            }, buf -> {
                final boolean hasPattern = buf.readBoolean();
                final int size = buf.readInt();
                final List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(
                            new Entry(
                                    buf.readInt(),
                                    ByteBufUtils.readItemStack(buf),
                                    ByteBufUtils.readUTF8String(buf),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    PatternMatch.values()[buf.readByte()],
                                    buf.readInt(),
                                    buf.readByte(),
                                    buf.readBoolean()));
                }
                final int conflictCount = buf.readInt();
                final List<Conflict> conflicts = new ArrayList<>(conflictCount);
                for (int i = 0; i < conflictCount; i++) {
                    conflicts.add(
                            new Conflict(
                                    buf.readInt(),
                                    ByteBufUtils.readItemStack(buf),
                                    ByteBufUtils.readUTF8String(buf),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readBoolean()));
                }
                return new PatternTargetList(hasPattern, entries, conflicts);
            });

    public final boolean hasPattern;
    public final List<Entry> entries;
    public final List<Conflict> conflicts;

    public PatternTargetList(boolean hasPattern, List<Entry> entries, List<Conflict> conflicts) {
        this.hasPattern = hasPattern;
        this.entries = entries;
        this.conflicts = conflicts;
    }

    public static final class Entry {

        public static final int CARD_NONE = 0;
        public static final int CARD_FROM_INVENTORY = 1;
        public static final int CARD_FROM_NETWORK = 2;

        /** Position in the server's list, sent back when the player picks this provider. */
        public final int index;
        public final ItemStack icon;
        public final String rawName;
        public final int x;
        public final int y;
        public final int z;
        public final int dim;
        public final PatternMatch match;
        public final int freeSlots;
        /** Where a Pattern Capacity Card comes from when the interface is full; CARD_NONE if it has room. */
        public final int cardSource;
        /** True when this provider already holds a pattern with the same inputs and outputs. */
        public final boolean hasSamePattern;

        public Entry(int index, ItemStack icon, String rawName, int x, int y, int z, int dim, PatternMatch match,
                int freeSlots, int cardSource, boolean hasSamePattern) {
            this.index = index;
            this.icon = icon;
            this.rawName = rawName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dim = dim;
            this.match = match;
            this.freeSlots = freeSlots;
            this.cardSource = cardSource;
            this.hasSamePattern = hasSamePattern;
        }
    }

    public static final class Conflict {

        /** Position in the server's conflict list, sent back when the player extracts this pattern. */
        public final int index;
        public final ItemStack icon;
        public final String rawName;
        public final int x;
        public final int y;
        public final int z;
        public final int dim;
        /** True when the existing pattern has the same inputs and outputs, not only the same result. */
        public final boolean sameRecipe;

        public Conflict(int index, ItemStack icon, String rawName, int x, int y, int z, int dim, boolean sameRecipe) {
            this.index = index;
            this.icon = icon;
            this.rawName = rawName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dim = dim;
            this.sameRecipe = sameRecipe;
        }
    }
}
