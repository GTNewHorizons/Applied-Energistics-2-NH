package com.glodblock.github.common.terminal;

import static appeng.util.Platform.nextEnum;
import static com.glodblock.github.inventory.item.WirelessMagnet.modeKey;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.glodblock.github.common.item.ItemWirelessUltraTerminal;
import com.glodblock.github.inventory.item.WirelessMagnet;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

/**
 * Persisted and synchronized state of the ultra terminal buttons. The button widgets on the client mirror this state.
 */
public final class UltraTerminalButtonState implements IUltraTerminalButtonState {

    public static final String RESTOCK = "restock";

    private final ItemStack terminal;
    private WirelessMagnet.Mode magnetMode = WirelessMagnet.Mode.Off;
    private boolean reStock;

    public UltraTerminalButtonState(final ItemStack terminal) {
        this.terminal = terminal;
    }

    public WirelessMagnet.Mode getMagnetMode() {
        return this.magnetMode;
    }

    public boolean isRestockEnabled() {
        return this.reStock;
    }

    @Override
    public void toggleRestock() {
        this.reStock = !this.reStock;
    }

    @Override
    public void toggleMagnetMode() {
        this.magnetMode = nextEnum(this.magnetMode);
    }

    public void readData(final NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        this.reStock = tag.getBoolean(RESTOCK);
        if (tag.hasKey(modeKey)) {
            final int ordinal = tag.getInteger(modeKey);
            if (ordinal >= 0 && ordinal < WirelessMagnet.Mode.values().length) {
                this.magnetMode = WirelessMagnet.Mode.values()[ordinal];
            }
        }
    }

    public void writeData(final NBTTagCompound tag) {
        if (tag == null) {
            return;
        }
        tag.setBoolean(RESTOCK, this.reStock);
        tag.setInteger(modeKey, this.magnetMode.ordinal());
    }

    public void readByte(final ByteBuf buf) {
        this.reStock = buf.readBoolean();
        this.magnetMode = WirelessMagnet.Mode.values()[buf.readInt()];
        ItemWirelessUltraTerminal.setModeId(this.terminal, ByteBufUtils.readUTF8String(buf));
    }

    public void writeByte(final ByteBuf buf) {
        buf.writeBoolean(this.reStock);
        buf.writeInt(this.magnetMode.ordinal());
        ByteBufUtils.writeUTF8String(buf, ItemWirelessUltraTerminal.getModeId(this.terminal));
    }
}