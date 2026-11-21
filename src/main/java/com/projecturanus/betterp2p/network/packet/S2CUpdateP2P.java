package com.projecturanus.betterp2p.network.packet;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.client.gui.GuiAdvancedMemoryCard;
import com.projecturanus.betterp2p.network.data.P2PInfo;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * Sends changed P2P tunnels, or the whole list when {@code clear} is set.
 */
public class S2CUpdateP2P implements IMessage {

    private List<P2PInfo> infos = Collections.emptyList();
    private boolean clear;

    public S2CUpdateP2P() {}

    public S2CUpdateP2P(List<P2PInfo> infos, boolean clear) {
        this.infos = infos;
        this.clear = clear;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        infos = P2PInfo.readList(buf);
        clear = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        P2PInfo.writeList(buf, infos);
        buf.writeBoolean(clear);
    }

    public static class Handler implements IMessageHandler<S2CUpdateP2P, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public @Nullable IMessage onMessage(S2CUpdateP2P message, MessageContext ctx) {
            if (Minecraft.getMinecraft().currentScreen instanceof GuiAdvancedMemoryCard gui) {
                if (message.clear) {
                    gui.refreshInfo(message.infos);
                } else {
                    gui.updateInfo(message.infos);
                }
            }
            return null;
        }
    }
}
