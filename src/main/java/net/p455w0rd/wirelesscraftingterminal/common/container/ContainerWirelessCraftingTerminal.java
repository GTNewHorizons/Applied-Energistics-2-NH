package net.p455w0rd.wirelesscraftingterminal.common.container;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
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
import appeng.container.ContainerNull;
import appeng.container.PrimaryGui;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.container.slot.SlotCraftingTerm;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IContainerCraftingPacket;
import appeng.helpers.InventoryAction;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;

public class ContainerWirelessCraftingTerminal extends ContainerMEMonitorable
        implements IAEAppEngInventory, IContainerCraftingPacket {

    private static final int CRAFTING_SLOT_X_POS = 80;

    private final WTCGuiObject host;
    public final WCTInventoryBooster boosterInventory;
    public final WCTInventoryMagnet magnetInventory;
    public final WCTInventoryTrash trashInventory;
    private final AppEngInternalInventory output = new AppEngInternalInventory(this, 1);
    private final SlotCraftingMatrix[] craftMatrixSlots = new SlotCraftingMatrix[9];
    private final SlotCraftingTerm outputSlot;
    public final SlotTrash trashSlot;

    public ContainerWirelessCraftingTerminal(final InventoryPlayer inventoryPlayer, final WTCGuiObject host) {
        super(inventoryPlayer, host, false);
        final EntityPlayer player = inventoryPlayer.player;
        this.host = host;
        final ItemStack terminal = host.getItemStack();
        final IInventory crafting = host.getInventoryByName("crafting");

        this.boosterInventory = new WCTInventoryBooster(terminal);
        this.magnetInventory = new WCTInventoryMagnet(terminal);
        this.trashInventory = new WCTInventoryTrash(terminal);

        if (Reference.WCT_BOOSTER_ENABLED) {
            this.addSlotToContainer(new SlotBooster(this.boosterInventory, 0, 134, -20));
        }

        this.bindPlayerInventory(inventoryPlayer, 0, 0);

        for (int i = 0; i < 4; i++) {
            this.addSlotToContainer(
                    new SlotArmor(player, inventoryPlayer, inventoryPlayer.getSizeInventory() - 1 - i, 8, i * 18 - 76, i));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int index = j + i * 3;
                this.craftMatrixSlots[index] = new SlotCraftingMatrix(
                        this,
                        crafting,
                        index,
                        CRAFTING_SLOT_X_POS + j * 18,
                        i * 18 - 76);
                this.addSlotToContainer(this.craftMatrixSlots[index]);
            }
        }

        this.outputSlot = new SlotCraftingTerm(
                player,
                this.getActionSource(),
                this.getPowerSource(),
                host,
                crafting,
                crafting,
                this.output,
                174,
                -58,
                this);
        this.addSlotToContainer(this.outputSlot);

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

        this.onCraftMatrixChanged(crafting);
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
    public void onCraftMatrixChanged(final IInventory inventory) {
        final InventoryCrafting ic = new InventoryCrafting(new ContainerNull(), 3, 3);
        for (int x = 0; x < 9; x++) {
            ic.setInventorySlotContents(x, this.craftMatrixSlots[x].getStack());
        }

        this.outputSlot.putStack(
                CraftingManager.getInstance().findMatchingRecipe(ic, this.getPlayerInv().player.worldObj));
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

    @Override
    public void saveChanges() {}

    @Override
    public void onChangeInventory(final IInventory inv, final int slot, final InvOperation mc,
            final ItemStack removedStack, final ItemStack newStack) {}

    @Override
    public @Nullable IInventory getInventoryByName(final String name) {
        if (name.equals("player")) {
            return this.getInventoryPlayer();
        }
        return this.host.getInventoryByName(name);
    }

    @Override
    public boolean useRealItems() {
        return true;
    }
}
