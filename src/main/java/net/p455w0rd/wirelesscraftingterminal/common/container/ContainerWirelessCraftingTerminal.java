package net.p455w0rd.wirelesscraftingterminal.common.container;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.p455w0rd.wirelesscraftingterminal.client.gui.WCTPrimaryGui;
import net.p455w0rd.wirelesscraftingterminal.common.container.slot.SlotArmor;
import net.p455w0rd.wirelesscraftingterminal.common.container.slot.SlotBooster;
import net.p455w0rd.wirelesscraftingterminal.common.container.slot.SlotMagnet;
import net.p455w0rd.wirelesscraftingterminal.common.container.slot.SlotTrash;
import net.p455w0rd.wirelesscraftingterminal.common.inventory.WCTInventoryBooster;
import net.p455w0rd.wirelesscraftingterminal.common.inventory.WCTInventoryMagnet;
import net.p455w0rd.wirelesscraftingterminal.common.inventory.WCTInventoryTrash;
import net.p455w0rd.wirelesscraftingterminal.helpers.WTCGuiObject;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.container.PrimaryGui;
import appeng.container.implementations.ContainerCraftingTerm;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.InventoryAction;

public class ContainerWirelessCraftingTerminal extends ContainerCraftingTerm {

    private final WTCGuiObject host;
    public final WCTInventoryBooster boosterInventory;
    public final WCTInventoryMagnet magnetInventory;
    public final WCTInventoryTrash trashInventory;
    public final SlotTrash trashSlot;

    public ContainerWirelessCraftingTerminal(final InventoryPlayer inventoryPlayer, final WTCGuiObject host) {
        super(inventoryPlayer, host, 80, -76, 174, -58);
        final EntityPlayer player = inventoryPlayer.player;
        this.host = host;
        final ItemStack terminal = host.getItemStack();

        this.boosterInventory = new WCTInventoryBooster(terminal);
        this.magnetInventory = new WCTInventoryMagnet(terminal);
        this.trashInventory = new WCTInventoryTrash(terminal);

        if (Reference.WCT_BOOSTER_ENABLED) {
            this.addSlotToContainer(new SlotBooster(this.boosterInventory, 0, 134, -20));
        }

        for (int i = 0; i < 4; i++) {
            this.addSlotToContainer(
                    new SlotArmor(
                            player,
                            inventoryPlayer,
                            inventoryPlayer.getSizeInventory() - 1 - i,
                            8,
                            i * 18 - 76,
                            i));
        }

        this.addSlotToContainer(new SlotMagnet(this.magnetInventory, 152, -20));

        this.trashSlot = new SlotTrash(this.trashInventory, 80, -20, player, terminal);
        this.addSlotToContainer(this.trashSlot);

        for (final SlotRestrictedInput slot : this.getCellViewSlots()) {
            if (slot != null) {
                // The view cell column of the WCT texture is 1px to the right of the one of AE2 terminals.
                slot.xDisplayPosition += 1;
                slot.setStackLimit(1);
            }
        }
    }

    public WTCGuiObject getHost() {
        return this.host;
    }

    @Override
    public void doAction(final EntityPlayerMP player, final InventoryAction action, final int slot, final long id) {
        // The terminal may have been dropped or moved before the packet arrives.
        this.checkItem(this.host);
        if (!this.isValidContainer()) {
            return;
        }
        super.doAction(player, action, slot, id);
    }

    public static @Nullable WTCGuiObject getGuiObject(final @Nullable ItemStack it, final EntityPlayer player,
            final World w, final int slotIndex) {
        if (it == null) {
            return null;
        }

        final IWirelessTermHandler wh = AEApi.instance().registries().wireless().getWirelessTerminalHandler(it);
        if (wh == null) {
            return null;
        }

        // AE2 reads the terminal settings from the tag in the constructor of the GUI object.
        ItemStackNBT.get(it);
        return new WTCGuiObject(wh, it, player, w, slotIndex);
    }

    @Override
    public @NonNull List<AppEngSlot> getValidDestinationSlots(final boolean isPlayerSideSlot,
            final @NonNull ItemStack stackInSlot) {
        final List<AppEngSlot> list = super.getValidDestinationSlots(isPlayerSideSlot, stackInSlot);
        list.removeIf(slot -> slot instanceof SlotTrash);
        return list;
    }

    @Override
    public PrimaryGui createPrimaryGui() {
        return new WCTPrimaryGui(this.host.getPrimaryGuiIcon(), this.getTargetSlotIndex());
    }
}
