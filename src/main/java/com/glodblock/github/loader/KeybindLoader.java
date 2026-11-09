package com.glodblock.github.loader;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.lwjgl.input.Keyboard;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.common.item.ItemWirelessUltraTerminal;
import com.glodblock.github.common.terminal.TerminalIds;
import com.glodblock.github.network.CPacketSwitchGuis;
import com.glodblock.github.network.CPacketValueConfig;
import com.glodblock.github.util.Util;

import appeng.api.AEApi;
import appeng.api.features.IUltraTerminalType;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class KeybindLoader implements Runnable {

    public static KeyBinding openTerminal;
    public static KeyBinding openCraftingTerminal;
    public static KeyBinding openLevelTerminal;
    public static KeyBinding openInterfaceTerminal;
    public static KeyBinding openPatternTerminal;
    public static KeyBinding openPatternExTerminal;
    public static KeyBinding restock;

    /**
     * Terminal types that have their own keybinding, kept for compatibility with existing player options.
     */
    private static final Set<String> FIXED_TERMINALS = new HashSet<>(
            Arrays.asList(
                    TerminalIds.AE2FC_CRAFTING,
                    TerminalIds.AE2FC_LEVEL,
                    TerminalIds.AE2FC_INTERFACE,
                    TerminalIds.AE2FC_PATTERN,
                    TerminalIds.AE2FC_PATTERN_EX));

    /**
     * Keybinding per registered terminal type that has no fixed keybinding.
     */
    private static final Map<String, KeyBinding> DYNAMIC_KEYS = new LinkedHashMap<>();

    @Override
    public void run() {
        openTerminal = new KeyBinding(FluidCraft.MODID + ".key.OpenTerminal", Keyboard.CHAR_NONE, "itemGroup.ae2fc");
        openCraftingTerminal = new KeyBinding(
                FluidCraft.MODID + ".key.OpenCraftingTerminal",
                Keyboard.CHAR_NONE,
                "itemGroup.ae2fc");
        openLevelTerminal = new KeyBinding(
                FluidCraft.MODID + ".key.OpenLevelTerminal",
                Keyboard.CHAR_NONE,
                "itemGroup.ae2fc");
        openInterfaceTerminal = new KeyBinding(
                FluidCraft.MODID + ".key.OpenInterfaceTerminal",
                Keyboard.CHAR_NONE,
                "itemGroup.ae2fc");
        openPatternTerminal = new KeyBinding(
                FluidCraft.MODID + ".key.OpenPatternTerminal",
                Keyboard.CHAR_NONE,
                "itemGroup.ae2fc");
        openPatternExTerminal = new KeyBinding(
                FluidCraft.MODID + ".key.OpenPatternExTerminal",
                Keyboard.CHAR_NONE,
                "itemGroup.ae2fc");
        restock = new KeyBinding(FluidCraft.MODID + ".key.Restock", Keyboard.CHAR_NONE, "itemGroup.ae2fc");
        ClientRegistry.registerKeyBinding(openTerminal);
        ClientRegistry.registerKeyBinding(openCraftingTerminal);
        ClientRegistry.registerKeyBinding(openLevelTerminal);
        ClientRegistry.registerKeyBinding(openInterfaceTerminal);
        ClientRegistry.registerKeyBinding(openPatternTerminal);
        ClientRegistry.registerKeyBinding(openPatternExTerminal);
        ClientRegistry.registerKeyBinding(restock);
        this.registerDynamicKeybinds();
        FMLCommonHandler.instance().bus().register(this);
    }

    /**
     * Registers one unbound keybinding for every terminal type that has no fixed keybinding, so terminals from addons
     * are bindable without extra work. The description only depends on the terminal id, so the binding survives
     * language changes. Terminals registered after initialization get no keybinding.
     */
    private void registerDynamicKeybinds() {
        DYNAMIC_KEYS.clear();
        for (final IUltraTerminalType type : AEApi.instance().registries().ultraTerminal().getTypes()) {
            final String id = type.getId();
            if (FIXED_TERMINALS.contains(id) || DYNAMIC_KEYS.containsKey(id)) {
                continue;
            }
            final KeyBinding binding = new KeyBinding(
                    FluidCraft.MODID + ".key.Open." + id.replace(':', '.'),
                    Keyboard.CHAR_NONE,
                    "itemGroup.ae2fc");
            ClientRegistry.registerKeyBinding(binding);
            DYNAMIC_KEYS.put(id, binding);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        handleKeybindings();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        handleKeybindings();
    }

    private void handleKeybindings() {
        if (openTerminal.isPressed()) {
            handleOpenTerminalKey(null);
        } else if (openCraftingTerminal.isPressed()) {
            handleOpenTerminalKey(TerminalIds.AE2FC_CRAFTING);
        } else if (openLevelTerminal.isPressed()) {
            handleOpenTerminalKey(TerminalIds.AE2FC_LEVEL);
        } else if (openInterfaceTerminal.isPressed()) {
            handleOpenTerminalKey(TerminalIds.AE2FC_INTERFACE);
        } else if (openPatternTerminal.isPressed()) {
            handleOpenTerminalKey(TerminalIds.AE2FC_PATTERN);
        } else if (openPatternExTerminal.isPressed()) {
            handleOpenTerminalKey(TerminalIds.AE2FC_PATTERN_EX);
        } else if (restock.isPressed()) {
            handleRestockKey();
        } else {
            for (final Map.Entry<String, KeyBinding> entry : DYNAMIC_KEYS.entrySet()) {
                if (entry.getValue().isPressed()) {
                    handleOpenTerminalKey(entry.getKey());
                    return;
                }
            }
        }
    }

    private void handleOpenTerminalKey(String modeId) {
        if (Minecraft.getMinecraft().currentScreen != null) return;
        final EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        final ImmutablePair<Integer, ItemStack> temp = Util.getUltraWirelessTerm(player);
        if (temp == null) return;
        if (modeId != null) {
            // only the absorbed terminals of the item can be opened
            if (!ItemWirelessUltraTerminal.isAbsorbed(temp.getRight(), modeId)) return;
            ItemWirelessUltraTerminal.setModeId(temp.getRight(), modeId);
        }
        FluidCraft.proxy.netHandler.sendToServer(new CPacketSwitchGuis(modeId, true));
    }

    private void handleRestockKey() {
        if (Minecraft.getMinecraft().currentScreen != null) return;
        EntityClientPlayerMP p = Minecraft.getMinecraft().thePlayer;
        if (p.openContainer == null) {
            return;
        }

        ImmutablePair<Integer, ItemStack> term = Util.getUltraWirelessTerm(p);
        if (term != null) {
            FluidCraft.proxy.netHandler.sendToServer(new CPacketValueConfig(0, 1));
        }
    }
}
