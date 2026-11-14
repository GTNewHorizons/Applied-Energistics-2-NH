package appeng.helpers.transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import appeng.api.features.transfer.TransferComponentInfo;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

/**
 * The Transfer Terminal's filter by kind of component. A kind is what the component's row shows as its icon, so every
 * block or part from any mod gets a name and icon without an API of its own. Rows without an icon are always shown.
 */
public final class TransferTypeFilter {

    private TransferTypeFilter() {}

    /** One kind of component in the drop-down, with whether its rows are shown. */
    public static final class Entry {

        public final ItemStack icon;
        public final String key;
        public final boolean shown;

        public Entry(final ItemStack icon, final String key, final boolean shown) {
            this.icon = icon;
            this.key = key;
            this.shown = shown;
        }
    }

    /**
     * @return the kind of component a row icon stands for, or null if there is no icon
     */
    @Nullable
    public static String keyOf(@Nullable final ItemStack icon) {
        if (icon == null || icon.getItem() == null) {
            return null;
        }
        final Object name = Item.itemRegistry.getNameForObject(icon.getItem());
        return (name == null ? icon.getUnlocalizedName() : name) + "@" + icon.getItemDamage();
    }

    /**
     * @param overrides the kinds the player turned on or off, every other kind follows its rows' default
     */
    public static boolean isShown(final TransferComponentInfo row, final Map<String, Boolean> overrides) {
        final String key = keyOf(row.selfRep);
        if (key == null) {
            return true;
        }
        final Boolean override = overrides.get(key);
        return override != null ? override : !row.description.hiddenByDefault;
    }

    /**
     * @return one entry for each kind of component among the rows, ordered by key so the list does not jump around
     */
    public static List<Entry> entries(final List<TransferComponentInfo> rows, final Map<String, Boolean> overrides) {
        final Map<String, Entry> byKey = new TreeMap<>();
        for (final TransferComponentInfo row : rows) {
            final String key = keyOf(row.selfRep);
            if (key != null && !byKey.containsKey(key)) {
                final ItemStack icon = row.selfRep.copy();
                icon.stackSize = 1;
                byKey.put(key, new Entry(icon, key, isShown(row, overrides)));
            }
        }
        return new ArrayList<>(byKey.values());
    }

    public static void write(final ByteBuf out, final List<Entry> entries) {
        out.writeShort(entries.size());
        for (final Entry entry : entries) {
            ByteBufUtils.writeItemStack(out, entry.icon);
            ByteBufUtils.writeUTF8String(out, entry.key);
            out.writeBoolean(entry.shown);
        }
    }

    public static List<Entry> read(final ByteBuf in) {
        final int count = in.readUnsignedShort();
        final List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final ItemStack icon = ByteBufUtils.readItemStack(in);
            final String key = ByteBufUtils.readUTF8String(in);
            entries.add(new Entry(icon, key, in.readBoolean()));
        }
        return Collections.unmodifiableList(entries);
    }

    /**
     * Entries with the same keys in the same order and the same states are equal, the icons follow from the keys.
     */
    public static boolean sameEntries(@Nullable final List<Entry> a, @Nullable final List<Entry> b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).key.equals(b.get(i).key) || a.get(i).shown != b.get(i).shown) {
                return false;
            }
        }
        return true;
    }
}
