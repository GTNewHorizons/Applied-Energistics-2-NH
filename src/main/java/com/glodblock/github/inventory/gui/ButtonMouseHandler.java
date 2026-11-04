package com.glodblock.github.inventory.gui;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.resources.I18n;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.client.gui.ITankDump;
import com.glodblock.github.network.CPacketDumpTank;
import com.glodblock.github.util.NameConst;

public class ButtonMouseHandler implements MouseRegionManager.Handler {

    @Nullable
    private final String tooltipKey;

    private final Runnable callback;

    public ButtonMouseHandler(@Nullable String tooltipKey, Runnable callback) {
        this.tooltipKey = tooltipKey;
        this.callback = callback;
    }

    @Nullable
    @Override
    public List<String> getTooltip() {
        return tooltipKey != null ? Collections.singletonList(I18n.format(tooltipKey)) : null;
    }

    @Override
    public boolean onClick(int button) {
        if (button == 0) {
            callback.run();
            return true;
        }
        return false;
    }

    public static ButtonMouseHandler dumpTank(ITankDump host, int index) {
        return new ButtonMouseHandler(NameConst.TT_DUMP_TANK, () -> {
            if (host.canDumpTank(index)) {
                FluidCraft.proxy.netHandler.sendToServer(new CPacketDumpTank(index));
            }
        });
    }
}
