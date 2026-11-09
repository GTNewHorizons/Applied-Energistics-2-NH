package appeng.client.gui.implementations;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.parts.ITransferTerminal;
import appeng.client.gui.AEBaseGui;
import appeng.container.implementations.ContainerTransferTerminal;

public class GuiTransferTerminal extends AEBaseGui {

    private final List<TransferComponentInfo> rows = new ArrayList<>();

    public GuiTransferTerminal(final InventoryPlayer inventoryPlayer, final ITransferTerminal host) {
        super(new ContainerTransferTerminal(inventoryPlayer, host));
    }

    public void receiveRows(final List<TransferComponentInfo> received, final boolean first, final boolean last) {
        if (first) {
            this.rows.clear();
        }
        this.rows.addAll(received);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {}

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {}
}
