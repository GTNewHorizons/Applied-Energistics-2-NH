package com.projecturanus.betterp2p.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IIcon;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.BetterP2P;
import com.projecturanus.betterp2p.network.data.P2PInfo;
import com.projecturanus.betterp2p.network.data.P2PLocation;
import com.projecturanus.betterp2p.util.p2p.ClientTunnelInfo;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class InfoWrapper {

    private static final int STATUS_LINE = 4;

    private long frequency;
    private boolean error;
    public final boolean hasChannel;
    public final P2PLocation loc;
    public final boolean output;
    public final int type;
    public final String name;

    /** The backing icon (quartz, iron, etc.) */
    public final IIcon icon;
    /** The P2P overlay texture (frame) */
    public final IIcon overlay;

    public final String description;
    public final List<String> hoverInfo;
    public final @Nullable String channels;
    private @Nullable String freqDisplay;

    public final GuiButton bindButton = new GuiButton(0, 0, 0, 34, 20, I18n.format("gui.advanced_memory_card.bind"));
    public final GuiButton unbindButton = new GuiButton(
            0,
            0,
            0,
            34,
            20,
            I18n.format("gui.advanced_memory_card.unbind"));

    public InfoWrapper(P2PInfo info) {
        frequency = info.frequency;
        hasChannel = info.hasChannel;
        loc = new P2PLocation(info.posX, info.posY, info.posZ, info.facing, info.dim);
        output = info.output;
        type = info.type;
        name = info.name;

        ClientTunnelInfo p2pType = (ClientTunnelInfo) BetterP2P.proxy.getP2PFromIndex(info.type);
        icon = p2pType.getIcon();
        overlay = p2pType.stack.getIconIndex();
        description = I18n.format(
                "gui.advanced_memory_card.p2p_status.type",
                p2pType.dispName,
                output ? I18n.format("gui.advanced_memory_card.p2p_status.output")
                        : I18n.format("gui.advanced_memory_card.p2p_status.input"));
        channels = info.channels >= 0 ? I18n.format("gui.advanced_memory_card.extra.channel", info.channels) : null;

        hoverInfo = new ArrayList<>();
        hoverInfo.add("§bP2P - " + p2pType.dispName);
        hoverInfo.add("§e" + I18n.format("gui.advanced_memory_card.pos", info.posX, info.posY, info.posZ));
        hoverInfo.add("§e" + I18n.format("gui.advanced_memory_card.side", info.facing.name()));
        hoverInfo.add("§e" + I18n.format("gui.advanced_memory_card.dim", info.dim));
        hoverInfo.add(getStatusLine());
        if (!info.hasChannel) {
            hoverInfo.add("§c" + I18n.format("gui.advanced_memory_card.p2p_status.offline"));
        }
    }

    public long getFrequency() {
        return frequency;
    }

    public void setFrequency(long frequency) {
        this.frequency = frequency;
        hoverInfo.set(STATUS_LINE, getStatusLine());
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    private String getStatusLine() {
        if (error || frequency == 0L) {
            return "§c" + I18n.format("gui.advanced_memory_card.p2p_status.unbound");
        }
        return "§a" + I18n.format("gui.advanced_memory_card.p2p_status.bound");
    }

    public String getFreqDisplay() {
        if (freqDisplay == null) {
            StringBuilder builder = new StringBuilder();
            builder.append(I18n.format("item.advanced_memory_card.selected")).append(' ');
            if (frequency != 0L) {
                String hex = Integer.toHexString((int) (frequency >> 32)).toUpperCase(Locale.ROOT)
                        + Integer.toHexString((int) frequency).toUpperCase(Locale.ROOT);
                builder.append(groupByFour(hex));
            } else {
                builder.append(I18n.format("gui.advanced_memory_card.desc.not_set"));
            }
            freqDisplay = builder.toString();
        }
        return freqDisplay;
    }

    private static String groupByFour(String s) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i % 4 == 0 && i != 0) {
                result.append(' ');
            }
            result.append(s.charAt(i));
        }
        return result.toString();
    }

    @Override
    public int hashCode() {
        return loc.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return loc.equals(((InfoWrapper) o).loc);
    }
}
