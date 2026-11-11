package com.glodblock.github.common.terminal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import org.jspecify.annotations.Nullable;

import appeng.api.features.IUltraTerminalRegistry;
import appeng.api.features.IUltraTerminalType;
import appeng.core.AELog;

/** Storage compatibility for the ultra terminal, independent of item and mod initialization. */
public final class UltraTerminalData {

    public static final String MODE = "mode";
    public static final String TYPES = "types";

    public static Set<String> readTypes(final @Nullable NBTTagCompound tag, final IUltraTerminalRegistry registry) {
        final Set<String> ids = new LinkedHashSet<>();
        if (tag != null && tag.hasKey(TYPES, Constants.NBT.TAG_LIST)) {
            final NBTTagList list = tag.getTagList(TYPES, Constants.NBT.TAG_STRING);
            for (int i = 0; i < list.tagCount(); i++) {
                final String id = list.getStringTagAt(i);
                final IUltraTerminalType type = registry.getById(id);
                // keep unknown ids, a temporarily missing addon must not lose its data
                ids.add(type == null ? id : type.getId());
            }
        } else if (tag != null && tag.hasKey(MODE)) {
            // the legacy mode only selected the gui, it never limited the terminals of the item
            ids.addAll(TerminalIds.legacyTypes());
        } else {
            // items without terminal data (creative menu, /give) contain every registered type
            for (final IUltraTerminalType type : registry.getTypes()) {
                ids.add(type.getId());
            }
        }
        return ids;
    }

    public static void writeTypes(final NBTTagCompound tag, final Collection<String> ids) {
        final NBTTagList list = new NBTTagList();
        for (final String id : ids) {
            list.appendTag(new NBTTagString(id));
        }
        tag.setTag(TYPES, list);
    }

    /**
     * Reads the stored mode.
     *
     * @return the stored mode, or null if there is no mode, or it does not resolve to a registered type
     */
    @Nullable
    public static IUltraTerminalType readMode(final @Nullable NBTTagCompound tag,
            final IUltraTerminalRegistry registry) {
        final NBTBase mode = tag == null ? null : tag.getTag(MODE);
        if (mode == null) {
            return null;
        }
        if (mode.getId() == Constants.NBT.TAG_INT) {
            // the mode of the removed enum based terminal
            return registry.getById(TerminalIds.fromLegacyOrdinal(tag.getInteger(MODE)));
        }
        if (mode.getId() == Constants.NBT.TAG_STRING) {
            return registry.getById(tag.getString(MODE));
        }
        return null;
    }

    /**
     * Rewrites the stored mode to the canonical id of the type it resolves to, converting the int mode of the removed
     * enum based implementation and aliased ids. Called from the write paths, so that reads stay side effect free.
     *
     * @return true if the tag was changed
     */
    public static boolean normalizeMode(final NBTTagCompound tag, final IUltraTerminalRegistry registry) {
        final NBTBase stored = tag.getTag(MODE);
        if (stored == null) {
            return false;
        }
        final IUltraTerminalType mode = readMode(tag, registry);
        if (mode == null) {
            if (stored.getId() == Constants.NBT.TAG_STRING) {
                AELog.warn("[UltraTerminal] Unknown terminal type %s", tag.getString(MODE));
            }
            return false;
        }
        if (stored.getId() == Constants.NBT.TAG_STRING && mode.getId().equals(tag.getString(MODE))) {
            return false;
        }
        tag.setString(MODE, mode.getId());
        return true;
    }

    public static void writeMode(final NBTTagCompound tag, final String id) {
        tag.setString(MODE, id);
    }

    private UltraTerminalData() {}
}
