package com.glodblock.github.common.terminal;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import org.jspecify.annotations.Nullable;

import appeng.api.features.IUltraTerminalRegistry;
import appeng.api.features.IUltraTerminalType;

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

    private UltraTerminalData() {}
}
