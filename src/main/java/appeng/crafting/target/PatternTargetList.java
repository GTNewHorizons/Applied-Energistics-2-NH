package appeng.crafting.target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.github.bsideup.jabel.Desugar;

import appeng.api.crafting.PatternMatch;
import appeng.api.storage.data.IAEStack;
import appeng.container.sync.StreamCodec;
import appeng.container.sync.StreamCodecs;
import cpw.mods.fml.common.network.ByteBufUtils;

/**
 * What the Pattern Terminal shows about the providers for the pattern in its output slot.
 */
public final class PatternTargetList {

    public static final PatternTargetList EMPTY = new PatternTargetList(
            0,
            false,
            Collections.emptyList(),
            Collections.emptyList(),
            null);

    public static final StreamCodec<PatternTargetList> CODEC = StreamCodecs
            .of(PatternTargetList.class.getName(), (buf, list) -> {
                buf.writeInt(list.revision);
                buf.writeBoolean(list.hasPattern);
                buf.writeInt(list.entries.size());
                for (Entry entry : list.entries) {
                    buf.writeInt(entry.index);
                    ByteBufUtils.writeItemStack(buf, entry.icon);
                    ByteBufUtils.writeUTF8String(buf, entry.rawName);
                    ByteBufUtils.writeUTF8String(buf, entry.suffix == null ? "" : entry.suffix);
                    buf.writeInt(entry.configIcons.size());
                    for (ItemStack icon : entry.configIcons) {
                        ByteBufUtils.writeItemStack(buf, icon);
                    }
                    buf.writeInt(entry.x);
                    buf.writeInt(entry.y);
                    buf.writeInt(entry.z);
                    buf.writeInt(entry.dim);
                    buf.writeByte(entry.match.ordinal());
                    buf.writeInt(entry.freeSlots);
                    buf.writeInt(entry.totalSlots);
                    buf.writeByte(entry.cardSource.ordinal());
                    buf.writeBoolean(entry.hasSamePattern);
                }
                buf.writeInt(list.conflicts.size());
                for (Conflict conflict : list.conflicts) {
                    buf.writeInt(conflict.index);
                    ByteBufUtils.writeItemStack(buf, conflict.icon);
                    ByteBufUtils.writeUTF8String(buf, conflict.rawName);
                    ByteBufUtils.writeUTF8String(buf, conflict.suffix == null ? "" : conflict.suffix);
                    buf.writeInt(conflict.x);
                    buf.writeInt(conflict.y);
                    buf.writeInt(conflict.z);
                    buf.writeInt(conflict.dim);
                    buf.writeBoolean(conflict.sameRecipe);
                }
                IAEStack.writeToPacketGeneric(buf, list.conflictOutput);
            }, buf -> {
                final int revision = buf.readInt();
                final boolean hasPattern = buf.readBoolean();
                final int size = buf.readInt();
                final List<Entry> entries = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    entries.add(
                            new Entry(
                                    buf.readInt(),
                                    ByteBufUtils.readItemStack(buf),
                                    ByteBufUtils.readUTF8String(buf),
                                    ByteBufUtils.readUTF8String(buf),
                                    readConfigIcons(buf),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    PatternMatch.values()[buf.readByte()],
                                    buf.readInt(),
                                    buf.readInt(),
                                    CardSource.values()[buf.readByte()],
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
                                    ByteBufUtils.readUTF8String(buf),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readInt(),
                                    buf.readBoolean()));
                }
                return new PatternTargetList(revision, hasPattern, entries, conflicts, IAEStack.fromPacketGeneric(buf));
            });

    public final int revision;
    public final boolean hasPattern;
    public final List<Entry> entries;
    public final List<Conflict> conflicts;
    public final IAEStack<?> conflictOutput;

    private static List<ItemStack> readConfigIcons(io.netty.buffer.ByteBuf buf) {
        final int count = buf.readInt();
        final List<ItemStack> icons = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final ItemStack icon = ByteBufUtils.readItemStack(buf);
            if (icon != null) icons.add(icon);
        }
        return icons;
    }

    public PatternTargetList(int revision, boolean hasPattern, List<Entry> entries, List<Conflict> conflicts,
            IAEStack<?> conflictOutput) {
        this.revision = revision;
        this.hasPattern = hasPattern;
        this.entries = entries;
        this.conflicts = conflicts;
        this.conflictOutput = conflictOutput;
    }

    @Desugar
    public record Click(int revision, int index) {

        public static final StreamCodec<Click> CODEC = StreamCodecs.of(Click.class.getName(), (buf, click) -> {
            buf.writeInt(click.revision);
            buf.writeInt(click.index);
        }, buf -> new Click(buf.readInt(), buf.readInt()));
    }

    /**
     * Where a Pattern Capacity Card comes from when the interface is full.
     */
    public enum CardSource {
        NONE,
        INVENTORY,
        NETWORK
    }

    public static final class Entry {

        /** Position in the server's list, sent back when the player picks this provider. */
        public final int index;
        public final ItemStack icon;
        public final String rawName;
        public final String suffix;
        public final List<ItemStack> configIcons;
        public final int x;
        public final int y;
        public final int z;
        public final int dim;
        public final PatternMatch match;
        public final int freeSlots;
        public final int totalSlots;
        /** Where a Pattern Capacity Card comes from when the interface is full; NONE if it has room. */
        public final CardSource cardSource;
        /** True when this provider already holds a pattern with the same inputs and outputs. */
        public final boolean hasSamePattern;

        public Entry(int index, ItemStack icon, String rawName, String suffix, List<ItemStack> configIcons, int x,
                int y, int z, int dim, PatternMatch match, int freeSlots, int totalSlots, CardSource cardSource,
                boolean hasSamePattern) {
            this.index = index;
            this.icon = icon;
            this.rawName = rawName;
            this.suffix = suffix;
            this.configIcons = configIcons;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dim = dim;
            this.match = match;
            this.freeSlots = freeSlots;
            this.totalSlots = totalSlots;
            this.cardSource = cardSource;
            this.hasSamePattern = hasSamePattern;
        }
    }

    public static final class Conflict {

        /** Position in the server's conflict list, sent back when the player extracts this pattern. */
        public final int index;
        public final ItemStack icon;
        public final String rawName;
        public final String suffix;
        public final int x;
        public final int y;
        public final int z;
        public final int dim;
        /** True when the existing pattern has the same inputs and outputs, not only the same result. */
        public final boolean sameRecipe;

        public Conflict(int index, ItemStack icon, String rawName, String suffix, int x, int y, int z, int dim,
                boolean sameRecipe) {
            this.index = index;
            this.icon = icon;
            this.rawName = rawName;
            this.suffix = suffix;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dim = dim;
            this.sameRecipe = sameRecipe;
        }
    }
}
