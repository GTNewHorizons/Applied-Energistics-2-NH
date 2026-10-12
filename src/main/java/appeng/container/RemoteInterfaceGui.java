package appeng.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IInterfaceHost;

/** Retains the terminal authorization when visiting an interface's secondary GUIs. */
public class RemoteInterfaceGui extends PrimaryGui {

    private final ContainerInterfaceTerminal terminal;
    private final IInterfaceHost host;

    public RemoteInterfaceGui(final ContainerInterfaceTerminal terminal, final IInterfaceHost host,
            final ItemStack icon, final ForgeDirection side) {
        super(GuiBridge.GUI_REMOTE_INTERFACE, icon, host.getTileEntity(), side);
        this.terminal = terminal;
        this.host = host;
    }

    @Override
    public void open(final EntityPlayer player) {
        this.terminal.openInterface(player, this.host);
    }

    public boolean canInteractWith(final EntityPlayer player) {
        return this.terminal.canAccessInterface(player, this.host);
    }

}
