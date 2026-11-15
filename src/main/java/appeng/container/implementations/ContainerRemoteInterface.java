package appeng.container.implementations;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import appeng.client.gui.IGuiSub;
import appeng.container.PrimaryGui;
import appeng.container.RemoteInterfaceGui;
import appeng.container.interfaces.IContainerSubGui;
import appeng.container.slot.SlotInaccessible;
import appeng.helpers.IInterfaceHost;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ContainerRemoteInterface extends ContainerInterface implements IContainerSubGui {

    private final Slot primaryGuiButtonIcon;
    private RemoteInterfaceGui remoteGui;

    @SideOnly(Side.CLIENT)
    private IGuiSub guiLink;

    public ContainerRemoteInterface(final InventoryPlayer ip, final IInterfaceHost host) {
        super(ip, host);
        this.primaryGuiButtonIcon = new SlotInaccessible(new AppEngInternalInventory(null, 1), 0, 0, -9000);
        this.addSlotToContainer(this.primaryGuiButtonIcon);
        // Only the validated terminal-opening path may activate this container on the server.
        this.setValidContainer(Platform.isClient());
    }

    public void setRemoteGui(final RemoteInterfaceGui remoteGui, final PrimaryGui terminalGui) {
        this.remoteGui = remoteGui;

        this.setPrimaryGui(terminalGui);
        this.setValidContainer(remoteGui.canInteractWith(this.getInventoryPlayer().player));
    }

    @Override
    public PrimaryGui createPrimaryGui() {
        return this.remoteGui != null ? this.remoteGui : super.createPrimaryGui();
    }

    @Override
    public boolean hasRemoteInterfaceOrigin() {
        return true;
    }

    @Override
    public boolean canInteractWith(final EntityPlayer player) {
        if (Platform.isServer() && (this.remoteGui == null || !this.remoteGui.canInteractWith(player))) {
            return false;
        }
        return super.canInteractWith(player);
    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            if (this.remoteGui == null) return;

            if (!this.canInteractWith(this.getInventoryPlayer().player)) {
                this.setValidContainer(false);
                return;
            }
        }
        super.detectAndSendChanges();
    }

    @Override
    public void setPrimaryGui(final PrimaryGui primaryGui) {
        super.setPrimaryGui(primaryGui);
        this.primaryGuiButtonIcon.putStack(primaryGui.getIcon());
    }

    @Override
    public void onSlotChange(final Slot slot) {
        super.onSlotChange(slot);
        if (Platform.isClient() && slot == this.primaryGuiButtonIcon && this.guiLink != null) {
            this.guiLink.initPrimaryGuiButton();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack getPrimaryGuiIcon() {
        return this.primaryGuiButtonIcon.getStack();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void setGuiLink(final IGuiSub guiLink) {
        this.guiLink = guiLink;
    }
}
