package appeng.client.gui.implementations;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.slots.VirtualMESlotSingle;
import appeng.container.implementations.ContainerStackAmount;
import appeng.core.localization.ColorUtils;
import appeng.core.localization.GuiText;
import appeng.helpers.Reflected;

public class GuiStackAmount extends GuiAmount {

    private final ContainerStackAmount container;
    private final VirtualMESlotSingle slot;

    @Reflected
    public GuiStackAmount(final InventoryPlayer inventoryPlayer, final Object te) {
        super(new ContainerStackAmount(inventoryPlayer, te));
        this.container = (ContainerStackAmount) this.inventorySlots;
        this.slot = new VirtualMESlotSingle(34, 53, 0, null);
        this.container.setStackListener(this::update);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.registerVirtualSlots(this.slot);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(GuiText.SelectAmount.getLocal(), 8, 6, ColorUtils.guiTextColorGray.getColor());
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        this.nextBtn.displayString = GuiText.Set.getLocal();

        try {
            long resultI = getAmountLong();
            this.nextBtn.enabled = resultI > 0;
        } catch (final NumberFormatException e) {
            this.nextBtn.enabled = false;
        }

        this.amountTextField.drawTextBox();
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        try {
            if (btn == this.nextBtn && btn.enabled && this.container.getStack() != null) {
                this.container.submitAmount(getAmountLong());
            }
        } catch (final NumberFormatException e) {
            // nope..
            this.amountTextField.setText("1");
        }
    }

    protected String getBackground() {
        return "guis/craftAmt.png";
    }

    private void update() {
        final IAEStack<?> stack = this.container.getStack();
        this.slot.setAEStack(stack);
        if (stack == null) {
            return;
        }

        this.amountTextField.setText(String.valueOf(stack.getStackSize()));
        this.amountTextField.setCursorPositionEnd();
        this.amountTextField.setSelectionPos(0);
    }
}
