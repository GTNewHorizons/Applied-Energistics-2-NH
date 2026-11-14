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

    public static final PatternTargetList EMPTY = new PatternTargetList(false, Collections.emptyList());

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
                                    buf.readInt()));
                }
                return new PatternTargetList(hasPattern, entries);
            });

    public final boolean hasPattern;
    public final List<Entry> entries;

    public PatternTargetList(boolean hasPattern, List<Entry> entries) {
        this.hasPattern = hasPattern;
        this.entries = entries;
    }

    public static final class Entry {

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

        public Entry(int index, ItemStack icon, String rawName, int x, int y, int z, int dim, PatternMatch match,
                int freeSlots) {
            this.index = index;
            this.icon = icon;
            this.rawName = rawName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dim = dim;
            this.match = match;
            this.freeSlots = freeSlots;
        }
    }
}
