package appeng.client.gui.implementations;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import appeng.client.gui.IGuiSub;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.implementations.ContainerRemoteInterface;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.helpers.IInterfaceHost;

public class GuiRemoteInterface extends GuiInterface implements IGuiSub {

    private final ContainerRemoteInterface container;
    private GuiTabButton originalGuiBtn;

    public GuiRemoteInterface(final InventoryPlayer inventoryPlayer, final IInterfaceHost te) {
        this(new ContainerRemoteInterface(inventoryPlayer, te));
    }

    private GuiRemoteInterface(final ContainerRemoteInterface container) {
        super(container);
        this.container = container;
        this.container.setGuiLink(this);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.initPrimaryGuiButton();
    }

    @Override
    public void initPrimaryGuiButton() {
        this.buttonList.remove(this.originalGuiBtn);
        this.originalGuiBtn = null;

        final ItemStack icon = this.container.getPrimaryGuiIcon();
        if (icon != null) {
            this.originalGuiBtn = new GuiTabButton(
                    this.guiLeft + 110,
                    this.guiTop,
                    icon,
                    icon.getDisplayName(),
                    itemRender);
            this.buttonList.add(this.originalGuiBtn);
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        if (btn == this.originalGuiBtn) {
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis());
            return;
        }

        super.actionPerformed(btn);
    }
}
