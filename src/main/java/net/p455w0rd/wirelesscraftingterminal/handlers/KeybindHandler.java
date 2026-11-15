package net.p455w0rd.wirelesscraftingterminal.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.p455w0rd.wirelesscraftingterminal.api.IWirelessCraftingTerminalItem;
import net.p455w0rd.wirelesscraftingterminal.common.utils.RandomUtils;
import net.p455w0rd.wirelesscraftingterminal.core.sync.network.NetworkHandler;
import net.p455w0rd.wirelesscraftingterminal.core.sync.packets.PacketOpenGui;
import net.p455w0rd.wirelesscraftingterminal.core.sync.packets.PacketSetMagnet;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import cpw.mods.fml.common.gameevent.InputEvent.MouseInputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class KeybindHandler {

    public static KeyBinding openTerminal;
    public static KeyBinding openMagnetFilter;
    public static KeyBinding changeMagnetMode;

    public static void registerKeybinds() {
        openTerminal = new KeyBinding("ae2wct.OpenTerminal", Keyboard.CHAR_NONE, "key.categories.ae2wct");
        openMagnetFilter = new KeyBinding("ae2wct.OpenMagnetFilter", Keyboard.CHAR_NONE, "key.categories.ae2wct");
        changeMagnetMode = new KeyBinding("ae2wct.SwitchMagnetMode", Keyboard.CHAR_NONE, "key.categories.ae2wct");
        ClientRegistry.registerKeyBinding(openTerminal);
        ClientRegistry.registerKeyBinding(openMagnetFilter);
        ClientRegistry.registerKeyBinding(changeMagnetMode);
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        handleKeybindings();
    }

    @SubscribeEvent
    public void onMouseInput(MouseInputEvent event) {
        handleKeybindings();
    }

    private static void handleKeybindings() {
        // Consume presses even while a screen is open, otherwise they fire after the screen closes.
        final boolean openTerminalPressed = openTerminal.isPressed();
        final boolean openMagnetFilterPressed = openMagnetFilter.isPressed();
        final boolean changeMagnetModePressed = changeMagnetMode.isPressed();

        final Minecraft mc = Minecraft.getMinecraft();
        final EntityClientPlayerMP p = mc.thePlayer;
        if (p == null || mc.currentScreen != null) {
            return;
        }

        if (openTerminalPressed) {
            ItemStack is = RandomUtils.getWirelessTerm(p.inventory);
            if (is != null && ((IWirelessCraftingTerminalItem) is.getItem()).isWirelessCraftingEnabled(is)) {
                NetworkHandler.instance.sendToServer(new PacketOpenGui(Reference.GUI_WCT));
            }
        } else if (openMagnetFilterPressed) {
            ItemStack magnetItem = RandomUtils.getMagnet(p.inventory);
            // ensure player has a Wireless Crafting Terminal (with Magnet Card Installed) or Magnet Card in their
            // inventory
            // and that they have manually right=clicked it to initialize it
            if (RandomUtils.isMagnetInitialized(magnetItem)) {
                NetworkHandler.instance.sendToServer(new PacketOpenGui(Reference.GUI_MAGNET));
            } else {
                p.addChatMessage(new ChatComponentText(LocaleHandler.InitializeMagnet.getLocal()));
            }
        } else if (changeMagnetModePressed) {
            ItemStack magnetItem = RandomUtils.getMagnet(p.inventory);
            if (magnetItem != null) {
                NetworkHandler.instance.sendToServer(new PacketSetMagnet(magnetItem.getItemDamage()));
            }
        }
    }
}
