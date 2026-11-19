package com.projecturanus.betterp2p.network.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.BetterP2P;
import com.projecturanus.betterp2p.util.p2p.TunnelInfo;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.parts.IPartHost;
import appeng.me.GridAccessException;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.parts.p2p.PartP2PTunnelNormal;
import appeng.parts.p2p.PartP2PTunnelStatic;
import appeng.util.Platform;

public class GridServerCache {

    private final IGrid grid;
    private final EntityPlayer player;
    private int type;

    private final Map<P2PLocation, PartP2PTunnel<?>> dirtyP2P = new LinkedHashMap<>();

    public GridServerCache(IGrid grid, EntityPlayer player, int type) {
        this.grid = grid;
        this.player = player;
        this.type = type;
    }

    public void setType(int type) {
        this.type = type;
    }

    private List<PartP2PTunnel<?>> collectTunnels() {
        List<PartP2PTunnel<?>> result = new ArrayList<>();
        for (Class<? extends IGridHost> clazz : grid.getMachinesClasses()) {
            if (!PartP2PTunnel.class.isAssignableFrom(clazz) || !isTargetClass(type, clazz)) {
                continue;
            }
            for (IGridNode node : grid.getMachines(clazz)) {
                PartP2PTunnel<?> p2p = (PartP2PTunnel<?>) node.getMachine();
                result.add(p2p);
            }
        }
        return result;
    }

    private @Nullable PartP2PTunnel<?> findTunnel(P2PLocation loc) {
        World world = DimensionManager.getWorld(loc.dim);
        if (world == null || !world.blockExists(loc.x, loc.y, loc.z)
                || !(world.getTileEntity(loc.x, loc.y, loc.z) instanceof IPartHost host)
                || !(host.getPart(loc.facing) instanceof PartP2PTunnel<?>tunnel)) {
            return null;
        }
        IGridNode node = tunnel.getGridNode();
        return node != null && node.getGrid() == grid ? tunnel : null;
    }

    private static boolean isTargetClass(int type, Class<?> clazz) {
        TunnelInfo info = BetterP2P.proxy.getP2PFromClass(clazz);
        return info != null && (type == MemoryInfo.TUNNEL_ANY || info.index == type);
    }

    private boolean hasPermission() {
        return hasPermission(grid, player);
    }

    public static boolean hasPermission(IGrid grid, EntityPlayer player) {
        ISecurityGrid security = grid.getCache(ISecurityGrid.class);
        return security == null || security.hasPermission(player, SecurityPermissions.BUILD);
    }

    public List<P2PInfo> retrieveP2PList() {
        if (!hasPermission()) {
            return Collections.emptyList();
        }
        dirtyP2P.clear();
        List<P2PInfo> result = new ArrayList<>();
        for (PartP2PTunnel<?> tunnel : collectTunnels()) {
            result.add(P2PInfo.of(tunnel));
        }
        return result;
    }

    public void markDirty(P2PLocation key, PartP2PTunnel<?> p2p) {
        if (isTargetClass(type, p2p.getClass())) {
            dirtyP2P.put(key, p2p);
        }
    }

    /**
     * Returns the P2P tunnels marked dirty since the last call and clears the dirty list.
     */
    public List<P2PInfo> getP2PUpdates() {
        List<P2PInfo> result = new ArrayList<>();
        for (PartP2PTunnel<?> tunnel : dirtyP2P.values()) {
            result.add(P2PInfo.of(tunnel));
        }
        dirtyP2P.clear();
        return result;
    }

    public boolean linkP2P(P2PLocation inputIndex, P2PLocation outputIndex) {
        PartP2PTunnel<?> input = findTunnel(inputIndex);
        PartP2PTunnel<?> output = findTunnel(outputIndex);
        if (input == null || output == null) {
            return false;
        }

        if (!hasPermission()) {
            return false;
        }

        if (input.getClass() != output.getClass()) {
            TunnelInfo inputType = BetterP2P.proxy.getP2PFromClass(input.getClass());
            if (inputType == null) {
                return false;
            }
            output = changeP2PType(output, inputType);
            if (output == null) {
                return false;
            }
        }

        if (input == output) {
            return false;
        }

        PartP2PTunnel<?> inputResult = input.convertToInput(player, null);
        if (inputResult == null) {
            throw new IllegalStateException("Cannot bind");
        }
        NBTTagCompound memoryCardData = inputResult.getMemoryCardData();
        markDirty(inputIndex, inputResult);

        ItemStack newType = ItemStack.loadItemStackFromNBT(memoryCardData);
        long freq = memoryCardData.getLong("freq");
        PartP2PTunnel<?> outputResult = output.convertToOutput(player, newType, freq);
        if (outputResult == null) {
            throw new IllegalStateException("Cannot bind");
        }
        markDirty(outputIndex, outputResult);
        return true;
    }

    public boolean unlinkP2P(P2PLocation p2pIndex) {
        if (!hasPermission()) {
            return false;
        }
        PartP2PTunnel<?> tunnel = findTunnel(p2pIndex);
        if (tunnel == null) {
            return false;
        }
        PartP2PTunnel<?> newTunnel = tunnel.unbind(player);
        if (newTunnel == tunnel) {
            return false;
        }
        markDirty(p2pIndex, newTunnel);
        return true;
    }

    public boolean renameP2P(P2PLocation loc, String name) {
        if (!hasPermission()) {
            return false;
        }
        PartP2PTunnel<?> tunnel = findTunnel(loc);
        if (tunnel == null) {
            return false;
        }
        tunnel.setCustomName(name);
        PartP2PTunnel<?> input = tunnel.isOutput() ? tunnel.getInput() : tunnel;
        if (input == null) {
            input = tunnel;
        }
        try {
            for (PartP2PTunnel<?> output : input.getOutputs()) {
                markDirty(P2PLocation.of(output), output);
            }
        } catch (GridAccessException ignored) {}
        markDirty(P2PLocation.of(input), input);
        return true;
    }

    private @Nullable PartP2PTunnel<?> changeP2PType(PartP2PTunnel<?> tunnel, TunnelInfo newType) {
        if (BetterP2P.proxy.getP2PFromClass(tunnel.getClass()) == newType) {
            player.addChatMessage(new ChatComponentTranslation("gui.advanced_memory_card.error.same_type"));
            return null;
        }
        boolean converted = false;
        if (PartP2PTunnelStatic.class.isAssignableFrom(newType.clazz)) {
            converted = consumeOneItem(newType.stack);
            if (!converted) {
                sendMissingItems(1, newType);
                return null;
            }
        }
        if (tunnel instanceof PartP2PTunnelStatic<?>) {
            List<TunnelInfo> attunables = BetterP2P.proxy.getP2PTypeList().stream()
                    .filter(e -> PartP2PTunnelNormal.class.isAssignableFrom(e.clazz))
                    // Check the exact type first
                    .sorted((o1, o2) -> o1 == newType ? -1 : o2 == newType ? 1 : 0).collect(Collectors.toList());
            for (TunnelInfo attunable : attunables) {
                if (consumeOneItem(attunable.stack)) {
                    converted = true;
                    break;
                }
            }
            if (!converted) {
                sendMissingItems(1, newType);
                return null;
            }
        }

        if (converted) {
            ItemStack drop = ItemStack.copyItemStack(tunnel.getItemStack());
            drop.stackSize = 1;
            List<ItemStack> drops = new ArrayList<>();
            drops.add(drop);
            tunnel.getDrops(drops, false);
            for (ItemStack stack : drops) {
                player.inventory.addItemStackToInventory(stack);
            }
            Platform.spawnDrops(player.worldObj, (int) player.posX, (int) player.posY, (int) player.posZ, drops);
        }

        IPartHost host = tunnel.getHost();
        host.removePart(tunnel.getSide(), false);
        ForgeDirection dir = host.addPart(newType.stack, tunnel.getSide(), player);
        if (dir != null && host.getPart(dir) instanceof PartP2PTunnel<?>newTunnel) {
            newTunnel.output = tunnel.isOutput();
            try {
                newTunnel.getProxy().getP2P().updateFreq(newTunnel, tunnel.getFrequency());
                return newTunnel;
            } catch (GridAccessException ignored) {}
        }
        return null;
    }

    private boolean consumeOneItem(ItemStack target) {
        ItemStack[] inventory = player.inventory.mainInventory;
        for (int i = 0; i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            if (stack != null && stack.isItemEqual(target)) {
                player.inventory.decrStackSize(i, 1);
                return true;
            }
        }
        return false;
    }

    private void sendMissingItems(int amount, TunnelInfo type) {
        player.addChatMessage(
                new ChatComponentTranslation(
                        "gui.advanced_memory_card.error.missing_items",
                        amount,
                        type.stack.getDisplayName()));
    }

    public boolean changeAllP2Ps(P2PLocation p2p, TunnelInfo newType) {
        if (!hasPermission()) {
            return false;
        }

        PartP2PTunnel<?> tunnel = findTunnel(p2p);
        if (tunnel == null) {
            return false;
        }

        try {
            if (tunnel.isOutput()) {
                PartP2PTunnel<?> input = tunnel.getInput();
                if (input != null) {
                    tunnel = input;
                }
            }

            List<PartP2PTunnel<?>> outputs = new ArrayList<>();
            for (PartP2PTunnel<?> output : tunnel.getOutputs()) {
                outputs.add(output);
            }
            if (PartP2PTunnelStatic.class.isAssignableFrom(newType.clazz)) {
                int amount = outputs.size() + 1;
                int hasItems = 0;
                for (ItemStack stack : player.inventory.mainInventory) {
                    if (stack != null && stack.isItemEqual(newType.stack)) {
                        hasItems += stack.stackSize;
                        if (hasItems >= amount) {
                            break;
                        }
                    }
                }
                if (hasItems < amount) {
                    sendMissingItems(amount, newType);
                    return false;
                }
            }
            changeP2PType(tunnel, newType);
            for (PartP2PTunnel<?> output : outputs) {
                changeP2PType(output, newType);
            }
            return true;
        } catch (GridAccessException e) {
            return false;
        }
    }
}
