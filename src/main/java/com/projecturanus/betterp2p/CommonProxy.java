package com.projecturanus.betterp2p;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.common.parts.PartFluidP2PInterface;
import com.glodblock.github.loader.ItemAndBlockHolder;
import com.projecturanus.betterp2p.item.ItemAdvancedMemoryCard;
import com.projecturanus.betterp2p.network.ServerEventHandler;
import com.projecturanus.betterp2p.util.p2p.TunnelInfo;

import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import appeng.api.definitions.IItems;
import appeng.api.definitions.IParts;
import appeng.parts.p2p.PartP2PGT5Power;
import appeng.parts.p2p.PartP2PIC2Power;
import appeng.parts.p2p.PartP2PInterface;
import appeng.parts.p2p.PartP2PItems;
import appeng.parts.p2p.PartP2PLight;
import appeng.parts.p2p.PartP2PLiquids;
import appeng.parts.p2p.PartP2POpenComputers;
import appeng.parts.p2p.PartP2PPressure;
import appeng.parts.p2p.PartP2PRFPower;
import appeng.parts.p2p.PartP2PRedstone;
import appeng.parts.p2p.PartP2PSound;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.parts.p2p.PartP2PTunnelME;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class CommonProxy {

    private final Map<Class<?>, TunnelInfo> tunnelTypes = new HashMap<>();
    private final Int2ObjectMap<TunnelInfo> tunnelIndices = new Int2ObjectLinkedOpenHashMap<>();

    public void postInit() {
        ServerEventHandler.register();
        IItems items = AEApi.instance().definitions().items();
        GameRegistry.addRecipe(
                new ShapelessRecipes(
                        new ItemStack(ItemAdvancedMemoryCard.INSTANCE),
                        Arrays.asList(
                                items.networkTool().maybeStack(1).get(),
                                items.memoryCard().maybeStack(1).get())));
        initTunnels();
    }

    // Lambdas, not constructor references: those are linked eagerly and fail without OpenComputers
    private void initTunnels() {
        IParts parts = AEApi.instance().definitions().parts();
        int typeId = 0;
        registerTunnel(parts.p2PTunnelME(), typeId++, PartP2PTunnelME.class, stack -> new PartP2PTunnelME(stack));
        registerTunnel(parts.p2PTunnelEU(), typeId++, PartP2PIC2Power.class, stack -> new PartP2PIC2Power(stack));
        registerTunnel(parts.p2PTunnelRF(), typeId++, PartP2PRFPower.class, stack -> new PartP2PRFPower(stack));
        registerTunnel(parts.p2PTunnelRedstone(), typeId++, PartP2PRedstone.class, stack -> new PartP2PRedstone(stack));
        registerTunnel(parts.p2PTunnelLiquids(), typeId++, PartP2PLiquids.class, stack -> new PartP2PLiquids(stack));
        registerTunnel(parts.p2PTunnelItems(), typeId++, PartP2PItems.class, stack -> new PartP2PItems(stack));
        registerTunnel(parts.p2PTunnelLight(), typeId++, PartP2PLight.class, stack -> new PartP2PLight(stack));
        registerTunnel(
                parts.p2PTunnelOpenComputers(),
                typeId++,
                PartP2POpenComputers.class,
                stack -> new PartP2POpenComputers(stack));
        registerTunnel(
                parts.p2PTunnelPneumaticCraft(),
                typeId++,
                PartP2PPressure.class,
                stack -> new PartP2PPressure(stack));
        registerTunnel(parts.p2PTunnelGregtech(), typeId++, PartP2PGT5Power.class, stack -> new PartP2PGT5Power(stack));
        registerTunnel(
                parts.p2PTunnelMEInterface(),
                typeId++,
                PartP2PInterface.class,
                stack -> new PartP2PInterface(stack));
        registerTunnel(parts.p2PTunnelSound(), typeId++, PartP2PSound.class, stack -> new PartP2PSound(stack));
        if (Loader.isModLoaded("ae2fc") && ItemAndBlockHolder.FLUID_INTERFACE_P2P != null) {
            registerTunnel(
                    typeId,
                    ItemAndBlockHolder.FLUID_INTERFACE_P2P.stack(),
                    PartFluidP2PInterface.class,
                    stack -> new PartFluidP2PInterface(stack));
        }
    }

    private void registerTunnel(IItemDefinition def, int type, Class<? extends PartP2PTunnel<?>> clazz,
            Function<ItemStack, ? extends PartP2PTunnel<?>> factory) {
        if (def.isEnabled()) {
            registerTunnel(type, def.maybeStack(1).get(), clazz, factory);
        }
    }

    private void registerTunnel(int type, ItemStack stack, Class<? extends PartP2PTunnel<?>> clazz,
            Function<ItemStack, ? extends PartP2PTunnel<?>> factory) {
        TunnelInfo info = createTunnelInfo(type, stack, clazz, factory);
        tunnelTypes.put(clazz, info);
        tunnelIndices.put(type, info);
    }

    protected TunnelInfo createTunnelInfo(int type, ItemStack stack, Class<? extends PartP2PTunnel<?>> clazz,
            Function<ItemStack, ? extends PartP2PTunnel<?>> factory) {
        return new TunnelInfo(type, stack, clazz);
    }

    public @Nullable TunnelInfo getP2PFromIndex(int index) {
        return tunnelIndices.get(index);
    }

    public @Nullable TunnelInfo getP2PFromClass(Class<?> clazz) {
        return tunnelTypes.get(clazz);
    }

    public List<TunnelInfo> getP2PTypeList() {
        return new ArrayList<>(tunnelIndices.values());
    }
}
