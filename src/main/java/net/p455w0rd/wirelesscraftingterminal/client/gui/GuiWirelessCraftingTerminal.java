package net.p455w0rd.wirelesscraftingterminal.client.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.p455w0rd.wirelesscraftingterminal.client.gui.widgets.GuiTrashButton;
import net.p455w0rd.wirelesscraftingterminal.common.container.ContainerWirelessCraftingTerminal;
import net.p455w0rd.wirelesscraftingterminal.core.sync.network.NetworkHandler;
import net.p455w0rd.wirelesscraftingterminal.core.sync.packets.PacketEmptyTrash;
import net.p455w0rd.wirelesscraftingterminal.handlers.LocaleHandler;
import net.p455w0rd.wirelesscraftingterminal.helpers.WTCGuiObject;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import org.jspecify.annotations.Nullable;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.slots.VirtualMEPinSlot;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.slot.SlotCraftingMatrix;
import appeng.core.localization.ColorUtils;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;

public class GuiWirelessCraftingTerminal extends GuiMEMonitorable {

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation(
            Reference.MODID,
            "textures/gui/crafting_viewcell.png");
    /** Width of the terminal part of the texture. The view cell column starts at this x. */
    private static final int BACKGROUND_WIDTH = 199;
    private static final int VIEW_CELL_COLUMN_WIDTH = 32;
    private static final int VIEW_CELL_COLUMN_HEIGHT = 104;

    private final ContainerWirelessCraftingTerminal containerWCT;
    private @Nullable GuiImgButton clearBtn;
    private @Nullable GuiTrashButton trashBtn;

    public GuiWirelessCraftingTerminal(final InventoryPlayer inventoryPlayer, final WTCGuiObject host) {
        super(inventoryPlayer, host, new ContainerWirelessCraftingTerminal(inventoryPlayer, host));
        this.setReservedSpace(73);
        this.offsetRepoX -= 1;
        this.containerWCT = (ContainerWirelessCraftingTerminal) this.inventorySlots;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.clearBtn = new GuiImgButton(
                this.guiLeft + 134,
                this.guiTop + this.ySize - 160,
                Settings.ACTIONS,
                ActionItems.STASH);
        this.clearBtn.setHalfSize(true);
        this.buttonList.add(this.clearBtn);

        this.trashBtn = new GuiTrashButton(this.guiLeft + 98, this.guiTop + this.ySize - 104);
        this.buttonList.add(this.trashBtn);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        if (btn == this.clearBtn) {
            final Slot slot = this.findSlot(SlotCraftingMatrix.class);
            if (slot != null) {
                appeng.core.sync.network.NetworkHandler.instance
                        .sendToServer(new PacketInventoryAction(InventoryAction.MOVE_REGION, slot.slotNumber, 0));
            }
        } else if (btn == this.trashBtn) {
            if (this.containerWCT.trashSlot.getHasStack()) {
                this.containerWCT.trashSlot.clearStack();
                NetworkHandler.instance.sendToServer(new PacketEmptyTrash());
            }
        } else {
            super.actionPerformed(btn);
        }
    }

    private @Nullable Slot findSlot(final Class<? extends Slot> slotClass) {
        Slot found = null;
        for (final Object slot : this.inventorySlots.inventorySlots) {
            if (slotClass.isInstance(slot)) {
                found = (Slot) slot;
            }
        }
        return found;
    }

    @Override
    protected int getPinButtonY() {
        return super.getPinButtonY() + 31;
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(
                this.getGuiDisplayName(LocaleHandler.WirelessTermLabel.getLocal()),
                8,
                6,
                ColorUtils.guiTextColorGray.getColor());

        VirtualMEPinSlot.drawSlotsBackground(this.pinSlots, this.mc, this.zLevel);

        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.mc.getTextureManager().bindTexture(BACKGROUND_TEXTURE);

        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, BACKGROUND_WIDTH, 18);
        for (int x = 0; x < this.rows; x++) {
            this.drawTexturedModalRect(offsetX, offsetY + 18 + x * 18, 0, 18, BACKGROUND_WIDTH, 18);
        }
        this.drawTexturedModalRect(
                offsetX,
                offsetY + 16 + this.rows * 18,
                0,
                106 - 18 - 18,
                BACKGROUND_WIDTH,
                99 + this.getReservedSpace());

        this.drawTexturedModalRect(
                offsetX + BACKGROUND_WIDTH,
                offsetY,
                BACKGROUND_WIDTH,
                0,
                VIEW_CELL_COLUMN_WIDTH,
                VIEW_CELL_COLUMN_HEIGHT);

        if (Reference.WCT_BOOSTER_ENABLED) {
            this.drawTexturedModalRect(offsetX + 132, offsetY + this.rows * 18 + 83, 237, 237, 19, 19);
        }

        this.searchField.drawTextBox();

        GuiInventory.func_147046_a(
                offsetX + 51,
                offsetY + this.rows * 18 + 94,
                32,
                offsetX + 51 - mouseX,
                offsetY + this.rows * 18 + 50 - mouseY,
                this.mc.thePlayer);

        this.updateViewCells();
    }
}
