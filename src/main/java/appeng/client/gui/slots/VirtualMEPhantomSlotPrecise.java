package appeng.client.gui.slots;

import static appeng.client.gui.implementations.GuiMEMonitorable.keyBindPickBlockAction;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import appeng.container.stackedit.StackEditMode;
import appeng.container.stackedit.StackEditor;
import appeng.container.sync.handlers.AEStackInventorySyncHandler;

public class VirtualMEPhantomSlotPrecise extends VirtualMEPhantomSlot {

    private final StackEditor editor;

    public VirtualMEPhantomSlotPrecise(int x, int y, AEStackInventorySyncHandler syncHandler, StackEditor editor,
            int slotIndex, TypeAcceptPredicate acceptType) {
        super(x, y, syncHandler, slotIndex, acceptType);
        this.editor = editor;
        this.showAmount = true;
    }

    @Override
    public void handleMouseClicked(@Nullable ItemStack itemStack, boolean isExtraAction, int mouseButton) {
        if (mouseButton == keyBindPickBlockAction && itemStack == null && !isExtraAction && this.getAEStack() != null) {
            this.editor.request(StackEditMode.AMOUNT, this.getSlotIndex());
            return;
        }

        super.handleMouseClicked(itemStack, isExtraAction, mouseButton);
    }
}
