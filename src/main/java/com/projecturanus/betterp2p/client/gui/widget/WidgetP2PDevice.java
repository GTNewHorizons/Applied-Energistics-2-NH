package com.projecturanus.betterp2p.client.gui.widget;

import java.util.function.Supplier;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;

import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.projecturanus.betterp2p.client.gui.GuiAdvancedMemoryCard;
import com.projecturanus.betterp2p.client.gui.GuiHelper;
import com.projecturanus.betterp2p.client.gui.InfoWrapper;
import com.projecturanus.betterp2p.item.BetterMemoryCardModes;
import com.projecturanus.betterp2p.network.ModNetwork;
import com.projecturanus.betterp2p.network.data.MemoryInfo;
import com.projecturanus.betterp2p.network.packet.C2STypeChange;
import com.projecturanus.betterp2p.util.p2p.ClientTunnelInfo;

public class WidgetP2PDevice implements ITypeReceiver {

    private static final double TEX_W = GuiAdvancedMemoryCard.GUI_WIDTH;
    private static final double TEX_H = GuiAdvancedMemoryCard.GUI_TEX_HEIGHT;

    private final Supplier<@Nullable InfoWrapper> selectedInfo;
    private final Supplier<BetterMemoryCardModes> modeSupplier;
    private final Supplier<@Nullable InfoWrapper> infoSupplier;
    private final GuiAdvancedMemoryCard gui;
    int x;
    int y;
    boolean renderNameTextField = true;

    public WidgetP2PDevice(Supplier<@Nullable InfoWrapper> selectedInfo, Supplier<BetterMemoryCardModes> modeSupplier,
            Supplier<@Nullable InfoWrapper> infoSupplier, GuiAdvancedMemoryCard gui, int x, int y) {
        this.selectedInfo = selectedInfo;
        this.modeSupplier = modeSupplier;
        this.infoSupplier = infoSupplier;
        this.gui = gui;
        this.x = x;
        this.y = y;
    }

    public @Nullable InfoWrapper getInfo() {
        return infoSupplier.get();
    }

    public void updateButtonVisibility() {
        InfoWrapper info = infoSupplier.get();
        if (info == null) {
            return;
        }
        BetterMemoryCardModes mode = modeSupplier.get();
        InfoWrapper selected = selectedInfo.get();
        if (selected == null) {
            info.bindButton.enabled = false;
            info.unbindButton.enabled = false;
        } else if (mode == BetterMemoryCardModes.UNBIND) {
            info.bindButton.enabled = false;
            info.unbindButton.enabled = info.getFrequency() != 0L;
        } else if (mode == BetterMemoryCardModes.COPY
                && ((!info.output && info.getFrequency() != 0L) || selected.output)) {
                    info.bindButton.enabled = false;
                    info.unbindButton.enabled = false;
                } else {
                    info.bindButton.enabled = !info.loc.equals(selected.loc)
                            && (selected.getFrequency() == 0L || info.getFrequency() != selected.getFrequency());
                    info.unbindButton.enabled = false;
                }
    }

    public void render(int mouseX, int mouseY, float partialTicks) {
        InfoWrapper info = infoSupplier.get();
        if (info == null) {
            return;
        }
        InfoWrapper selected = selectedInfo.get();
        if (selected != null && selected.loc.equals(info.loc)) {
            drawBackground(P2PEntryConstants.SELECTED_COLOR);
        } else if (info.isError()) {
            drawBackground(P2PEntryConstants.ERROR_COLOR);
        } else if (!info.hasChannel && info.getFrequency() != 0L) {
            drawBackground(P2PEntryConstants.INACTIVE_COLOR);
        } else if (selected != null && selected.getFrequency() == info.getFrequency() && info.getFrequency() != 0L) {
            drawBackground(P2PEntryConstants.OUTPUT_COLOR);
        }
        GL11.glColor3f(255f, 255f, 255f);
        GuiHelper.drawBlockIcon(gui.mc, info.icon, info.overlay, x + 3, y + 3);
        gui.bindTexture(GuiAdvancedMemoryCard.BACKGROUND);
        Tessellator tessellator = Tessellator.instance;
        double ioU = info.output ? 144.0 : 128.0;
        GuiHelper.drawTexturedQuad(
                tessellator,
                x,
                y + 4.0,
                x + 16.0,
                y + 20.0,
                ioU / TEX_W,
                200.0 / TEX_H,
                (ioU + 16) / TEX_W,
                216.0 / TEX_H);
        double statusU = info.isError() || info.getFrequency() == 0L || !info.hasChannel ? 144.0 : 128.0;
        GuiHelper.drawTexturedQuad(
                tessellator,
                x + 3.0,
                y + 20.0,
                x + 19.0,
                y + 36.0,
                statusU / TEX_W,
                216.0 / TEX_H,
                (statusU + 16) / TEX_W,
                232.0 / TEX_H);

        // Text rendering messes up the GL state, so it is drawn after the textures
        FontRenderer fontRenderer = gui.mc.fontRenderer;
        int leftAlign = x + P2PEntryConstants.LEFT_ALIGN;
        String name = renderNameTextField ? info.name : "";
        fontRenderer.drawString(I18n.format("gui.advanced_memory_card.name", name), leftAlign, y + 3, 0);
        fontRenderer.drawString(info.description, leftAlign, y + 13, 0);
        fontRenderer.drawString(info.getFreqDisplay(), leftAlign, y + 23, 0);
        if (info.channels != null) {
            fontRenderer.drawString(info.channels, leftAlign, y + 33, 0);
        }
        updateButtonVisibility();
        drawButtons(info, mouseX, mouseY);
    }

    private void drawBackground(int color) {
        GuiScreen.drawRect(x, y, x + P2PEntryConstants.WIDTH, y + P2PEntryConstants.HEIGHT, color);
    }

    private void drawButtons(InfoWrapper info, int mouseX, int mouseY) {
        GuiButton button;
        if (info.bindButton.enabled) {
            button = info.bindButton;
        } else if (info.unbindButton.enabled) {
            button = info.unbindButton;
        } else {
            return;
        }
        button.xPosition = x + 190;
        button.width = 56;
        button.yPosition = y + 14;
        button.drawButton(gui.mc, mouseX, mouseY);
    }

    @Override
    public void accept(@Nullable ClientTunnelInfo type) {
        InfoWrapper info = infoSupplier.get();
        if (info != null) {
            ModNetwork.CHANNEL
                    .sendToServer(new C2STypeChange(type != null ? type.index : MemoryInfo.TUNNEL_ANY, info.loc));
        }
        gui.closeTypeSelector(type);
    }

    @Override
    public int x() {
        return x + 40;
    }

    @Override
    public int y() {
        return y;
    }
}
