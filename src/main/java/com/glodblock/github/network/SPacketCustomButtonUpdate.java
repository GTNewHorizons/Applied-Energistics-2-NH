package com.glodblock.github.network;

import net.minecraft.client.Minecraft;

import appeng.container.AEBaseContainer;
import appeng.helpers.ICustomButtonDataObject;
import appeng.helpers.ICustomButtonProvider;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Syncs the state of the custom buttons to the client, the payload is empty when the container has no custom buttons.
 */
public class SPacketCustomButtonUpdate implements IMessage {

    private ICustomButtonDataObject dataObject;

    private ByteBuf buf;

    public SPacketCustomButtonUpdate() {}

    public SPacketCustomButtonUpdate(ICustomButtonDataObject dataObject) {
        this.dataObject = dataObject;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.buf = buf.readBoolean() ? buf : null;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.dataObject != null);
        if (this.dataObject != null) {
            this.dataObject.writeByte(buf);
        }
    }

    public static class Handler implements IMessageHandler<SPacketCustomButtonUpdate, IMessage> {

        @Override
        public IMessage onMessage(SPacketCustomButtonUpdate message, MessageContext ctx) {
            if (message.buf == null || !message.buf.isReadable()) {
                return null;
            }
            if (Minecraft.getMinecraft().thePlayer.openContainer instanceof AEBaseContainer abc
                    && abc.getTarget() instanceof ICustomButtonProvider icbp) {
                final ICustomButtonDataObject dataObject = icbp.getDataObject();
                if (dataObject != null) {
                    dataObject.readByte(message.buf);
                }
            }
            return null;
        }
    }
}
