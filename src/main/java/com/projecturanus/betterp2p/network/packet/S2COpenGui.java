package com.projecturanus.betterp2p.network.packet;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.client.gui.GuiAdvancedMemoryCard;
import com.projecturanus.betterp2p.network.data.MemoryInfo;
import com.projecturanus.betterp2p.network.data.P2PInfo;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class S2COpenGui implements IMessage {

    private List<P2PInfo> infos = Collections.emptyList();
    private MemoryInfo memoryInfo = new MemoryInfo();

    public S2COpenGui() {}

    public S2COpenGui(List<P2PInfo> infos, MemoryInfo memoryInfo) {
        this.infos = infos;
        this.memoryInfo = memoryInfo;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        infos = P2PInfo.readList(buf);
        memoryInfo = MemoryInfo.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        P2PInfo.writeList(buf, infos);
        memoryInfo.write(buf);
    }

    public static class Handler implements IMessageHandler<S2COpenGui, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public @Nullable IMessage onMessage(S2COpenGui message, MessageContext ctx) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen instanceof GuiAdvancedMemoryCard gui) {
                gui.refreshInfo(message.infos);
            } else {
                mc.displayGuiScreen(new GuiAdvancedMemoryCard(message.infos, message.memoryInfo));
            }
            return null;
        }
    }
}
