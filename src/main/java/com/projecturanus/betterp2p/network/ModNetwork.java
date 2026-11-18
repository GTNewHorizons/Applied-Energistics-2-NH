package com.projecturanus.betterp2p.network;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

    private static final long NETWORK_CD = 250L;

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(BetterP2P.MODID);

    private static final Map<UUID, PlayerRequest> playerState = Collections.synchronizedMap(new WeakHashMap<>());

    private static ScheduledThreadPoolExecutor networkWorker;

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
        networkWorker = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "BetterP2P-NetworkWorker");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
    }

    public static @Nullable GridServerCache getGridCache(EntityPlayer player) {
        PlayerRequest request = playerState.get(player.getUniqueID());
        return request != null ? request.gridCache : null;
    }

    public static void requestP2PList(EntityPlayer player, int type) {
        synchronized (playerState) {
            PlayerRequest request = playerState.get(player.getUniqueID());
            if (request == null) {
                return;
            }
            GridServerCache cache = request.gridCache;
            cache.setType(type);
            sendUpdate((EntityPlayerMP) player, request, () -> new S2CUpdateP2P(cache.retrieveP2PList(), true));
        }
    }

    /**
     * Requests within the cooldown are bundled into one update.
     */
    public static void requestP2PUpdate(EntityPlayer player) {
        synchronized (playerState) {
            PlayerRequest request = playerState.get(player.getUniqueID());
            if (request == null) {
                return;
            }
            GridServerCache cache = request.gridCache;
            sendUpdate((EntityPlayerMP) player, request, () -> new S2CUpdateP2P(cache.getP2PUpdates(), false));
        }
    }

    private static void sendUpdate(EntityPlayerMP player, PlayerRequest request, Supplier<S2CUpdateP2P> message) {
        if (request.updateReady + NETWORK_CD < System.currentTimeMillis()) {
            CHANNEL.sendTo(message.get(), player);
            request.updateReady = System.currentTimeMillis() + NETWORK_CD;
        } else if (!request.updatePending) {
            request.updatePending = true;
            networkWorker.schedule(() -> {
                synchronized (playerState) {
                    CHANNEL.sendTo(message.get(), player);
                    request.updatePending = false;
                }
            }, request.updateReady - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
    }

    public static void initConnection(EntityPlayer player, IGrid grid, MemoryInfo info) {
        GridServerCache cache = new GridServerCache(grid, player, info.getType());
        playerState.put(player.getUniqueID(), new PlayerRequest(cache));
        CHANNEL.sendTo(new S2COpenGui(cache.retrieveP2PList(), info), (EntityPlayerMP) player);
    }

    public static void removeConnection(EntityPlayer player) {
        playerState.remove(player.getUniqueID());
    }

    public static void stop() {
        networkWorker.shutdown();
    }

    private static final class PlayerRequest {

        private final GridServerCache gridCache;
        private boolean updatePending;
        private long updateReady = System.currentTimeMillis();

        private PlayerRequest(GridServerCache gridCache) {
            this.gridCache = gridCache;
        }
    }
}
