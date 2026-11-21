package com.glodblock.github.network;

import java.util.Objects;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jspecify.annotations.Nullable;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.common.item.ItemWirelessUltraTerminal;
import com.glodblock.github.common.parts.PartFluidInterface;
import com.glodblock.github.common.parts.PartFluidP2PInterface;
import com.glodblock.github.common.tile.TileFluidInterface;
import com.glodblock.github.common.tile.TileSuperStockReplenisher;
import com.glodblock.github.inventory.InventoryHandler;
import com.glodblock.github.inventory.gui.GuiType;
import com.glodblock.github.util.BlockPos;
import com.glodblock.github.util.Util;

import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.PrimaryGui;
import appeng.container.interfaces.IContainerSubGui;
import appeng.helpers.ICustomButtonProvider;
import appeng.util.Platform;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class CPacketSwitchGuis implements IMessage {

    private String modeId;
    private boolean switchTerminal;

    public CPacketSwitchGuis(String modeId) {
        this(modeId, false);
    }

    public CPacketSwitchGuis(String modeId, boolean switchTerminal) {
        this.modeId = modeId;
        this.switchTerminal = switchTerminal;
    }

    public CPacketSwitchGuis() {
        // NO-OP
    }

    @Override
    public void fromBytes(ByteBuf byteBuf) {
        modeId = ByteBufUtils.readUTF8String(byteBuf);
        switchTerminal = byteBuf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf byteBuf) {
        ByteBufUtils.writeUTF8String(byteBuf, modeId != null ? modeId : "");
        byteBuf.writeBoolean(this.switchTerminal);
    }

    public static class Handler implements IMessageHandler<CPacketSwitchGuis, IMessage> {

        @Nullable
        @Override
        public IMessage onMessage(CPacketSwitchGuis message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            Container cont = player.openContainer;

            // switch terminal

            if (message.switchTerminal) {
                ImmutablePair<Integer, ItemStack> temp = getTerminalToSwitch(player, cont);
                if (temp != null && temp.getRight().getItem() instanceof ItemWirelessUltraTerminal iwut) {
                    final String modeId = message.modeId == null || message.modeId.isEmpty() ? null : message.modeId;
                    iwut.switchTerminal(player, temp, modeId);
                    if (player.openContainer instanceof AEBaseContainer abc) {
                        if (abc.getTarget() instanceof ICustomButtonProvider icbp && icbp.getDataObject() != null) {
                            FluidCraft.proxy.netHandler
                                    .sendTo(new SPacketCustomButtonUpdate(icbp.getDataObject()), player);
                        }
                    }
                }
            } else {
                if (cont instanceof AEBaseContainer aeBaseContainer) {
                    final PrimaryGui pGui = aeBaseContainer.createPrimaryGui();
                    ContainerOpenContext context = aeBaseContainer.getOpenContext();
                    if (context == null) return null;
                    TileEntity te = context.getTile();
                    Object target = aeBaseContainer.getTarget();
                    GuiType guiType;

                    if (te instanceof TileSuperStockReplenisher) guiType = GuiType.GUI_SUPER_STOCK_REPLENISHER;
                    else if (te instanceof TileFluidInterface || target instanceof PartFluidInterface
                            || target instanceof PartFluidP2PInterface)
                        guiType = GuiType.DUAL_INTERFACE_FLUID;
                    else return null;

                    InventoryHandler.openGui(
                            player,
                            player.worldObj,
                            new BlockPos(te),
                            Objects.requireNonNull(context.getSide()),
                            guiType);

                    if (player.openContainer instanceof IContainerSubGui sg) {
                        sg.setPrimaryGui(pGui);
                    }
                }
            }
            return null;
        }

        private static ImmutablePair<Integer, ItemStack> getTerminalToSwitch(EntityPlayerMP player, Container cont) {
            if (cont instanceof AEBaseContainer aeBaseContainer) {
                final int slot = aeBaseContainer.getTargetSlotIndex();
                if (slot != Integer.MIN_VALUE) {
                    final ItemStack terminal = Platform.getItemFromPlayerInventoryBySlotIndex(player, slot);
                    if (terminal != null && terminal.getItem() instanceof ItemWirelessUltraTerminal) {
                        return new ImmutablePair<>(slot, terminal);
                    }
                }
            }

            // hotkeys and the gui of another terminal have no open ultra terminal to take the slot from
            return Util.getUltraWirelessTerm(player);
        }
    }

}
