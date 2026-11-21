package com.projecturanus.betterp2p.network.packet;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.item.ItemAdvancedMemoryCard;
import com.projecturanus.betterp2p.network.data.MemoryInfo;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class C2SUpdateMemoryInfo implements IMessage {

    private MemoryInfo info = new MemoryInfo();

    public C2SUpdateMemoryInfo() {}

    public C2SUpdateMemoryInfo(MemoryInfo info) {
        this.info = info;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        info = MemoryInfo.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        info.write(buf);
    }

    public static class Handler implements IMessageHandler<C2SUpdateMemoryInfo, IMessage> {

        @Override
        public @Nullable IMessage onMessage(C2SUpdateMemoryInfo message, MessageContext ctx) {
            ItemStack stack = ctx.getServerHandler().playerEntity.getHeldItem();
            if (stack != null && stack.getItem() == ItemAdvancedMemoryCard.INSTANCE) {
                ItemAdvancedMemoryCard.INSTANCE.writeInfo(stack, message.info);
            }
            return null;
        }
    }
}
