package com.projecturanus.betterp2p.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import org.jspecify.annotations.Nullable;

import com.projecturanus.betterp2p.BetterP2P;
import com.projecturanus.betterp2p.network.data.GridServerCache;
import com.projecturanus.betterp2p.network.data.MemoryInfo;
import com.projecturanus.betterp2p.network.packet.C2SCloseGui;
import com.projecturanus.betterp2p.network.packet.C2SLinkP2P;
import com.projecturanus.betterp2p.network.packet.C2SRefreshP2PList;
import com.projecturanus.betterp2p.network.packet.C2SRenameP2P;
import com.projecturanus.betterp2p.network.packet.C2STypeChange;
import com.projecturanus.betterp2p.network.packet.C2SUnlinkP2P;
import com.projecturanus.betterp2p.network.packet.C2SUpdateMemoryInfo;
import com.projecturanus.betterp2p.network.packet.S2COpenGui;
import com.projecturanus.betterp2p.network.packet.S2CUpdateP2P;

import appeng.api.networking.IGrid;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class ModNetwork {

    private static final long LIST_COOLDOWN = 250L;

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(BetterP2P.MODID);

    private static final Map<UUID, PlayerRequest> playerState = new HashMap<>();

    private ModNetwork() {}

    public static void registerNetwork() {
        int id = 0;
        CHANNEL.registerMessage(S2COpenGui.Handler.class, S2COpenGui.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(S2CUpdateP2P.Handler.class, S2CUpdateP2P.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(C2SLinkP2P.Handler.class, C2SLinkP2P.class, id++, Side.SERVER);
        CHANNEL.registerMessage(C2SCloseGui.Handler.class, C2SCloseGui.class, id++, Side.SERVER);
        CHANNEL.registerMessage(C2SUpdateMemoryInfo.Handler.class, C2SUpdateMemoryInfo.class, id++, Side.SERVER);
        CHANNEL.registerMessage(C2SRenameP2P.Handler.class, C2SRenameP2P.class, id++, Side.SERVER);
        CHANNEL.registerMessage(C2SRefreshP2PList.Handler.class, C2SRefreshP2PList.class, id++, Side.SERVER);
        CHANNEL.registerMessage(C2SUnlinkP2P.Handler.class, C2SUnlinkP2P.class, id++, Side.SERVER);
        CHANNEL.registerMessage(C2STypeChange.Handler.class, C2STypeChange.class, id, Side.SERVER);
    }

    public static @Nullable GridServerCache getGridCache(EntityPlayer player) {
        PlayerRequest request = playerState.get(player.getUniqueID());
        return request != null ? request.gridCache : null;
    }

    public static void requestP2PList(EntityPlayer player, int type) {
        PlayerRequest request = playerState.get(player.getUniqueID());
        if (request == null) {
            return;
        }
        request.gridCache.setType(type);
        request.listPending = true;
        flushP2PList(request, System.currentTimeMillis());
    }

    public static void requestP2PUpdate(EntityPlayer player) {
        PlayerRequest request = playerState.get(player.getUniqueID());
        if (request == null) {
            return;
        }
        CHANNEL.sendTo(new S2CUpdateP2P(request.gridCache.getP2PUpdates(), false), request.player);
    }

    static void flushPendingLists() {
        if (playerState.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (PlayerRequest request : playerState.values()) {
            flushP2PList(request, now);
        }
    }

    private static void flushP2PList(PlayerRequest request, long now) {
        if (!request.listPending || now < request.nextListTime) {
            return;
        }
        request.listPending = false;
        request.nextListTime = now + LIST_COOLDOWN;
        CHANNEL.sendTo(new S2CUpdateP2P(request.gridCache.retrieveP2PList(), true), request.player);
    }

    public static void initConnection(EntityPlayer player, IGrid grid, MemoryInfo info) {
        GridServerCache cache = new GridServerCache(grid, player, info.getType());
        playerState.put(player.getUniqueID(), new PlayerRequest((EntityPlayerMP) player, cache));
        CHANNEL.sendTo(new S2COpenGui(cache.retrieveP2PList(), info), (EntityPlayerMP) player);
    }

    public static void removeConnection(EntityPlayer player) {
        playerState.remove(player.getUniqueID());
    }

    public static void stop() {
        playerState.clear();
    }

    private static final class PlayerRequest {

        private final EntityPlayerMP player;
        private final GridServerCache gridCache;
        private boolean listPending;
        private long nextListTime;

        private PlayerRequest(EntityPlayerMP player, GridServerCache gridCache) {
            this.player = player;
            this.gridCache = gridCache;
        }
    }
}
