package com.projecturanus.betterp2p.network.data;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.client.gui.widget.GuiScale;
import com.projecturanus.betterp2p.item.BetterMemoryCardModes;

import io.netty.buffer.ByteBuf;

public final class MemoryInfo {

    public static final int TUNNEL_ANY = -1;

    private @Nullable P2PLocation selectedEntry;
    /** 0 means no frequency is selected. */
    private final long frequency;
    private final BetterMemoryCardModes mode;
    private final GuiScale guiScale;
    /** Index of {@link com.projecturanus.betterp2p.util.p2p.TunnelInfo}, or {@link #TUNNEL_ANY}. */
    private int type;

    public MemoryInfo() {
        this(null, 0, BetterMemoryCardModes.OUTPUT, GuiScale.DYNAMIC, TUNNEL_ANY);
    }

    public MemoryInfo(@Nullable P2PLocation selectedEntry, long frequency, BetterMemoryCardModes mode,
            GuiScale guiScale, int type) {
        this.selectedEntry = selectedEntry;
        this.frequency = frequency;
        this.mode = mode;
        this.guiScale = guiScale;
        this.type = type;
    }

    public @Nullable P2PLocation getSelectedEntry() {
        return selectedEntry;
    }

    public void setSelectedEntry(@Nullable P2PLocation selectedEntry) {
        this.selectedEntry = selectedEntry;
    }

    public long getFrequency() {
        return frequency;
    }

    public BetterMemoryCardModes getMode() {
        return mode;
    }

    public GuiScale getGuiScale() {
        return guiScale;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public static MemoryInfo read(ByteBuf buf) {
        P2PLocation selectedEntry = null;
        if (buf.readBoolean()) {
            selectedEntry = P2PLocation.read(buf);
        }
        long frequency = buf.readLong();
        BetterMemoryCardModes mode;
        try {
            mode = BetterMemoryCardModes.values()[buf.readInt()];
        } catch (Exception e) {
            mode = BetterMemoryCardModes.OUTPUT;
        }
        GuiScale guiScale;
        try {
            guiScale = GuiScale.values()[buf.readByte()];
        } catch (ArrayIndexOutOfBoundsException e) {
            guiScale = GuiScale.DYNAMIC;
        }
        int type = buf.readByte();
        return new MemoryInfo(selectedEntry, frequency, mode, guiScale, type);
    }

    public void write(ByteBuf buf) {
        buf.writeBoolean(selectedEntry != null);
        if (selectedEntry != null) {
            selectedEntry.write(buf);
        }
        buf.writeLong(frequency);
        buf.writeInt(mode.ordinal());
        buf.writeByte(guiScale.ordinal());
        buf.writeByte(type);
    }
}
