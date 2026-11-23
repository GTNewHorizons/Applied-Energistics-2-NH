package com.projecturanus.betterp2p.client.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import org.jspecify.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.projecturanus.betterp2p.BetterP2P;
import com.projecturanus.betterp2p.client.ClientCache;
import com.projecturanus.betterp2p.client.TextureBound;
import com.projecturanus.betterp2p.client.gui.widget.GuiScale;
import com.projecturanus.betterp2p.client.gui.widget.ITypeReceiver;
import com.projecturanus.betterp2p.client.gui.widget.P2PEntryConstants;
import com.projecturanus.betterp2p.client.gui.widget.WidgetButton;
import com.projecturanus.betterp2p.client.gui.widget.WidgetP2PColumn;
import com.projecturanus.betterp2p.client.gui.widget.WidgetP2PDevice;
import com.projecturanus.betterp2p.client.gui.widget.WidgetScrollBar;
import com.projecturanus.betterp2p.client.gui.widget.WidgetTypeSelector;
import com.projecturanus.betterp2p.item.BetterMemoryCardModes;
import com.projecturanus.betterp2p.network.ModNetwork;
import com.projecturanus.betterp2p.network.data.MemoryInfo;
import com.projecturanus.betterp2p.network.data.P2PInfo;
import com.projecturanus.betterp2p.network.data.P2PLocation;
import com.projecturanus.betterp2p.network.packet.C2SCloseGui;
import com.projecturanus.betterp2p.network.packet.C2SRefreshP2PList;
import com.projecturanus.betterp2p.network.packet.C2SUpdateMemoryInfo;
import com.projecturanus.betterp2p.util.p2p.ClientTunnelInfo;
import com.projecturanus.betterp2p.util.p2p.TunnelInfo;

import appeng.client.gui.widgets.MEGuiTextField;
import appeng.parts.p2p.PartP2PGT5Power;
import appeng.parts.p2p.PartP2PInterface;
import appeng.parts.p2p.PartP2PLiquids;
import appeng.parts.p2p.PartP2PRedstone;
import appeng.parts.p2p.PartP2PTunnelME;

public class GuiAdvancedMemoryCard extends GuiScreen implements TextureBound {

    public static final int GUI_WIDTH = 288;
    public static final int GUI_TEX_HEIGHT = 264;
    public static final ResourceLocation BACKGROUND = new ResourceLocation(
            BetterP2P.MODID,
            "textures/gui/advanced_memory_card.png");

    private static final int TABLE_X = 9;
    private static final int TABLE_Y = 19;

    private int guiLeft;
    private int guiTop;
    private int ySize = 242;

    private GuiScale scale;
    private BetterMemoryCardModes mode;
    private @Nullable ClientTunnelInfo type;

    private final InfoList infos;
    private final WidgetScrollBar scrollBar;
    private final WidgetP2PColumn col;
    private final MEGuiTextField searchBar;
    private final ResizeButton resizeButton;
    private final ModeButton modeButton;
    private final TypeButton typeButton;
    private final RefreshButton refreshButton;
    private final WidgetTypeSelector typeSelector;
    private final List<String> sortRules;

    public GuiAdvancedMemoryCard(List<P2PInfo> p2pInfos, MemoryInfo memoryInfo) {
        scale = memoryInfo.getGuiScale();
        mode = memoryInfo.getMode();
        type = BetterP2P.proxy.getP2PFromIndex(memoryInfo.getType()) instanceof ClientTunnelInfo info ? info : null;
        infos = new InfoList(wrap(p2pInfos), this::getSearchText);
        infos.select(memoryInfo.getSelectedEntry());
        scrollBar = new WidgetScrollBar(0, 0);
        col = new WidgetP2PColumn(this, infos, 0, 0, () -> mode, scrollBar);
        searchBar = new MEGuiTextField(100, 10);
        resizeButton = new ResizeButton();
        modeButton = new ModeButton();
        typeButton = new TypeButton();
        refreshButton = new RefreshButton();
        typeSelector = new WidgetTypeSelector(0, 0, createTypeSelectorList(), typeButton);
        sortRules = Arrays.asList(
                "§b§n" + I18n.format("gui.advanced_memory_card.sortinfo1"),
                "§9@in§7 - " + I18n.format("gui.advanced_memory_card.sortinfo2"),
                "§6@out§7 - " + I18n.format("gui.advanced_memory_card.sortinfo3"),
                "§a@b§7 - " + I18n.format("gui.advanced_memory_card.sortinfo4"),
                "§c@u§7 - " + I18n.format("gui.advanced_memory_card.sortinfo5"),
                "§e@type=<name1>[;<name2>;]...§7 - " + I18n.format("gui.advanced_memory_card.sortinfo6"),
                "§7" + I18n.format("gui.advanced_memory_card.sortinfo7"));
    }

    private static List<InfoWrapper> wrap(List<P2PInfo> p2pInfos) {
        List<InfoWrapper> result = new ArrayList<>(p2pInfos.size());
        for (P2PInfo info : p2pInfos) {
            result.add(new InfoWrapper(info));
        }
        return result;
    }

    private static List<ClientTunnelInfo> createTypeSelectorList() {
        List<ClientTunnelInfo> result = new ArrayList<>();
        for (Class<?> clazz : Arrays
                .asList(PartP2PTunnelME.class, PartP2PLiquids.class, PartP2PGT5Power.class, PartP2PInterface.class)) {
            if (BetterP2P.proxy.getP2PFromClass(clazz) instanceof ClientTunnelInfo info) {
                result.add(info);
            }
        }
        for (TunnelInfo info : BetterP2P.proxy.getP2PTypeList()) {
            if (!result.contains(info)) {
                result.add((ClientTunnelInfo) info);
            }
        }
        return result;
    }

    private String getSearchText() {
        return searchBar.getText();
    }

    @Override
    public void initGui() {
        super.initGui();
        checkInfo();
        int h = Math.max(height, 256);
        if (scale.minHeight > h) {
            scale = GuiScale.DYNAMIC;
        }
        resizeButton.setHoverText(Collections.singletonList(I18n.format(scale.unlocalizedName)));
        int numEntries = scale.size(height - 75);
        ySize = numEntries * P2PEntryConstants.HEIGHT + 75 + (numEntries - 1);
        guiTop = (h - ySize) / 2;
        guiLeft = (width - GUI_WIDTH) / 2;

        scrollBar.setPosition(guiLeft + 268, guiTop + 19);
        scrollBar.setHeight(numEntries * P2PEntryConstants.HEIGHT + (numEntries - 1) - 7);
        scrollBar.setRange(0, Math.max(0, infos.size() - numEntries));

        searchBar.setMaxStringLength(40);
        searchBar.x = guiLeft + 163;
        searchBar.y = guiTop + 5;
        searchBar.setText(ClientCache.getSearchText());

        col.resize(scale, h - 75);
        col.setPosition(guiLeft + TABLE_X, guiTop + TABLE_Y);

        resizeButton.setPosition(guiLeft - 32, guiTop + 2);
        resizeButton.setTexCoords(scale.ordinal() * 32.0, 200.0);
        buttonList.add(resizeButton);

        modeButton.setPosition(guiLeft - 32, guiTop + 34);
        modeButton.setTexCoords((mode.ordinal() + 3) * 32.0, 232.0);
        buttonList.add(modeButton);

        typeButton.setPosition(guiLeft - 32, guiTop + 66);
        buttonList.add(typeButton);

        refreshButton.setPosition(guiLeft - 32, guiTop + 98);
        refreshButton.setTexCoords(32 * 5.0, 200.0);
        buttonList.add(refreshButton);

        if (typeSelector.getParent() != typeButton) {
            closeTypeSelector(type);
        } else {
            typeSelector.setPos(typeButton.x(), typeButton.y());
        }
        infos.refresh();
        checkInfo();
        refreshOverlay();
        for (WidgetP2PDevice entry : col.getEntries()) {
            entry.updateButtonVisibility();
        }
    }

    public void openTypeSelector(ITypeReceiver parent, boolean useAny) {
        typeSelector.setParent(parent);
        typeSelector.setPos(parent.x(), parent.y());
        typeSelector.setUseAny(useAny);
        typeSelector.setVisible(true);
    }

    public void closeTypeSelector(@Nullable ClientTunnelInfo type) {
        this.type = type;
        typeSelector.setVisible(false);
    }

    private void checkInfo() {
        for (InfoWrapper info : infos.getFiltered()) {
            long frequency = info.getFrequency();
            boolean missing = info.output ? col.findInput(frequency) == null : col.findOutput(frequency) == null;
            info.setError(frequency != 0L && missing);
        }
    }

    public void refreshInfo(List<P2PInfo> p2pInfos) {
        infos.rebuild(wrap(p2pInfos), scrollBar, scale.size(height - 75));
        checkInfo();
        refreshOverlay();
    }

    public void updateInfo(List<P2PInfo> p2pInfos) {
        infos.update(wrap(p2pInfos), scrollBar, scale.size(height - 75));
        checkInfo();
        refreshOverlay();
    }

    private void syncMemoryInfo() {
        InfoWrapper selected = infos.getSelectedInfo();
        ModNetwork.CHANNEL.sendToServer(
                new C2SUpdateMemoryInfo(
                        new MemoryInfo(
                                infos.getSelectedEntry(),
                                selected != null ? selected.getFrequency() : 0,
                                mode,
                                scale,
                                getTypeID())));
    }

    @Override
    public void onGuiClosed() {
        ClientCache.setSearchText(searchBar.getText());
        col.onGuiClosed();
        syncMemoryInfo();
        ModNetwork.CHANNEL.sendToServer(new C2SCloseGui());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawGuiBackground();
        // Text rendering messes up the GL state, so it is drawn before the P2P entries
        fontRendererObj.drawString(I18n.format("item.advanced_memory_card.name.gui"), guiLeft + TABLE_X, guiTop + 6, 0);
        searchBar.drawTextBox();
        for (GuiButton button : buttonList) {
            ((WidgetButton) button).draw(mc, mouseX, mouseY, partialTicks);
        }

        GL11.glPushAttrib(GL11.GL_BLEND | GL11.GL_TEXTURE_2D | GL11.GL_COLOR);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor3f(255f, 255f, 255f);
        scrollBar.draw(this);

        col.render(mouseX, mouseY, partialTicks);
        GL11.glPopAttrib();
        if (typeSelector.isVisible()) {
            typeSelector.render(this, mouseX, mouseY, partialTicks);
            return;
        }
        for (GuiButton button : buttonList) {
            WidgetButton widgetButton = (WidgetButton) button;
            if (widgetButton.isHovering(mouseX, mouseY)) {
                drawHoveringText(widgetButton.getHoverText(), mouseX, mouseY + 10, fontRendererObj);
                return;
            }
        }
        if (searchBar.isMouseIn(mouseX, mouseY)) {
            drawHoveringText(sortRules, guiLeft, guiTop + ySize - 40, fontRendererObj);
        } else {
            col.mouseHovered(mouseX, mouseY);
        }
    }

    public void selectInfo(@Nullable P2PLocation which) {
        infos.select(which);
        syncMemoryInfo();
        refreshOverlay();
    }

    private void refreshOverlay() {
        InfoWrapper selected = infos.getSelectedInfo();
        ClientCache.setSelectedPosition(selected != null ? selected.loc : null);
        List<P2PLocation> positions = new ArrayList<>();
        if (selected != null) {
            for (InfoWrapper info : infos.getSorted()) {
                if (info.getFrequency() == selected.getFrequency() && !info.equals(selected)
                        && info.loc.dim == mc.thePlayer.dimension) {
                    positions.add(info.loc);
                }
            }
        }
        ClientCache.setPositions(positions);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (typeSelector.isVisible()) {
            typeSelector.mousePressed(mouseX, mouseY, mouseButton);
            return;
        }
        col.mouseClicked(mouseX, mouseY, mouseButton);
        scrollBar.click(mouseX, mouseY);
        for (GuiButton button : buttonList) {
            if (((WidgetButton) button).mousePressed(mouseX, mouseY, mouseButton)) {
                return;
            }
        }
        searchBar.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 1 && searchBar.isMouseIn(mouseX, mouseY)) {
            searchBar.setText("");
            infos.refilter();
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        super.mouseMovedOrUp(mouseX, mouseY, state);
        if (state != -1) {
            scrollBar.stopMoving();
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        scrollBar.click(mouseX, mouseY);
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scrollBar.wheel(wheel);
            col.finishRename();
        }
    }

    @Override
    public void bindTexture(String modid, String location) {
        bindTexture(new ResourceLocation(modid, location));
    }

    public void bindTexture(ResourceLocation loc) {
        mc.getTextureManager().bindTexture(loc);
    }

    private void drawGuiBackground() {
        bindTexture(BACKGROUND);
        Tessellator tessellator = Tessellator.instance;
        GuiHelper.drawTexturedQuad(
                tessellator,
                guiLeft,
                guiTop,
                guiLeft + GUI_WIDTH,
                guiTop + 60.0,
                0.0,
                0.0,
                1.0,
                60.0 / GUI_TEX_HEIGHT);
        double p2pHeight = P2PEntryConstants.HEIGHT + 1.0;
        for (int i = 0; i < scale.size(ySize - 75) - 2; i++) {
            GuiHelper.drawTexturedQuad(
                    tessellator,
                    guiLeft,
                    guiTop + 60.0 + p2pHeight * i,
                    guiLeft + GUI_WIDTH,
                    guiTop + 60.0 + p2pHeight * (i + 1),
                    0.0,
                    60.0 / GUI_TEX_HEIGHT,
                    1.0,
                    102.0 / GUI_TEX_HEIGHT);
        }
        GuiHelper.drawTexturedQuad(
                tessellator,
                guiLeft,
                guiTop + ySize - 98.0,
                guiLeft + GUI_WIDTH,
                guiTop + ySize,
                0.0,
                102.0 / GUI_TEX_HEIGHT,
                1.0,
                200.0 / GUI_TEX_HEIGHT);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void keyTyped(char c, int key) {
        if (key == Keyboard.KEY_ESCAPE && typeSelector.isVisible()) {
            closeTypeSelector(type);
            return;
        }
        if (key == Keyboard.KEY_LSHIFT || col.keyTyped(c, key)) return;
        if (!(Character.isWhitespace(c) && searchBar.getText().isEmpty()) && searchBar.textboxKeyTyped(c, key)) {
            infos.refilter();
        } else if (c == 'e') {
            mc.displayGuiScreen(null);
            mc.setIngameFocus();
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    public void drawHoveringText(List<String> textLines, int x, int y, FontRenderer font) {
        super.drawHoveringText(textLines, x, y, font);
    }

    public int getTypeID() {
        return type != null ? type.index : MemoryInfo.TUNNEL_ANY;
    }

    public static List<String> fmtTooltips(String title, int maxChars, String... keys) {
        List<String> result = new ArrayList<>();
        result.add(I18n.format(title));
        for (String key : keys) {
            String[] words = I18n.format(key).split(" ", -1);
            int i = 0;
            if (key.length() < maxChars) {
                result.add(key);
            }
            while (i < words.length) {
                StringBuilder s = new StringBuilder();
                while (s.length() < maxChars) {
                    s.append(words[i]);
                    i++;
                    if (i >= words.length) break;
                    s.append(' ');
                }
                if (s.length() == 0 || s.charAt(0) != '§') {
                    s.insert(0, "§7");
                }
                result.add(s.toString());
            }
        }
        return result;
    }

    private final class ResizeButton extends WidgetButton {

        private ResizeButton() {
            super(GuiAdvancedMemoryCard.this, 0, 0, 32, 32);
        }

        @Override
        public boolean mousePressed(int mouseX, int mouseY, int button) {
            if (!super.mousePressed(mc, mouseX, mouseY)) {
                return false;
            }
            if (button == 0) {
                scale = scale.next(false);
            } else if (button == 1) {
                scale = scale.next(true);
            }
            initGui();
            func_146113_a(mc.getSoundHandler());
            return true;
        }
    }

    private final class ModeButton extends WidgetButton {

        private final List<List<String>> modeDescriptions = new ArrayList<>();

        private ModeButton() {
            super(GuiAdvancedMemoryCard.this, 0, 0, 32, 32);
            for (BetterMemoryCardModes m : BetterMemoryCardModes.values()) {
                modeDescriptions.add(
                        fmtTooltips(m.unlocalizedName, BetterMemoryCardModes.MAX_TOOLTIP_LENGTH, m.unlocalizedDesc));
            }
            setHoverText(modeDescriptions.get(mode.ordinal()));
        }

        @Override
        public boolean mousePressed(int mouseX, int mouseY, int button) {
            if (!super.mousePressed(mc, mouseX, mouseY)) {
                return false;
            }
            if (button == 0) {
                mode = mode.next(false);
            } else if (button == 1) {
                mode = mode.next(true);
            } else {
                return false;
            }
            setHoverText(modeDescriptions.get(mode.ordinal()));
            setTexCoords((mode.ordinal() + 3) * 32.0, 232.0);
            syncMemoryInfo();
            func_146113_a(mc.getSoundHandler());
            return true;
        }
    }

    private final class TypeButton extends WidgetButton implements ITypeReceiver {

        private final List<TunnelInfo> types = BetterP2P.proxy.getP2PTypeList();
        private int index;
        private final @Nullable ClientTunnelInfo me = (ClientTunnelInfo) BetterP2P.proxy
                .getP2PFromClass(PartP2PTunnelME.class);
        private final @Nullable ClientTunnelInfo fluid = (ClientTunnelInfo) BetterP2P.proxy
                .getP2PFromClass(PartP2PLiquids.class);
        private final @Nullable ClientTunnelInfo redstone = (ClientTunnelInfo) BetterP2P.proxy
                .getP2PFromClass(PartP2PRedstone.class);

        private TypeButton() {
            super(GuiAdvancedMemoryCard.this, 0, 0, 32, 32);
            index = indexOf(type);
            updateHoverText();
        }

        private int indexOf(@Nullable ClientTunnelInfo info) {
            int i = info != null ? types.indexOf(info) : -1;
            return i >= 0 ? i : types.size();
        }

        private void updateHoverText() {
            String typeName = type != null ? "§a" + type.stack.getDisplayName()
                    : I18n.format("gui.advanced_memory_card.types.any");
            setHoverText(Collections.singletonList(I18n.format("gui.advanced_memory_card.types.filtered", typeName)));
        }

        private @Nullable ClientTunnelInfo nextType() {
            index = Math.floorMod(index + 1, types.size() + 1);
            return index < types.size() && types.get(index) instanceof ClientTunnelInfo info ? info : null;
        }

        @Override
        public boolean mousePressed(int mouseX, int mouseY, int button) {
            if (!super.mousePressed(mc, mouseX, mouseY)) {
                return false;
            }
            if (button == 1) {
                openTypeSelector(this, true);
                return true;
            }
            type = nextType();
            commitType();
            return true;
        }

        @Override
        public void draw(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            drawBG(Tessellator.instance, mouseX, mouseY, partialTicks);
            if (type != null) {
                GuiHelper.drawBlockIcon(
                        mc,
                        type.getIcon(),
                        type.stack.getIconIndex(),
                        xPosition + 2,
                        yPosition + 2,
                        28.0,
                        28.0);
            } else if (!types.isEmpty()) {
                IIcon overlay = types.get(0).stack.getIconIndex();
                drawAnyTypeIcon(redstone, overlay, 12);
                drawAnyTypeIcon(fluid, overlay, 7);
                drawAnyTypeIcon(me, overlay, 2);
            }
        }

        private void drawAnyTypeIcon(@Nullable ClientTunnelInfo info, IIcon overlay, int offset) {
            if (info != null) {
                GuiHelper
                        .drawBlockIcon(mc, info.getIcon(), overlay, xPosition + offset, yPosition + offset, 18.0, 18.0);
            }
        }

        private void commitType() {
            updateHoverText();
            ModNetwork.CHANNEL.sendToServer(new C2SRefreshP2PList(getTypeID()));
            func_146113_a(mc.getSoundHandler());
        }

        @Override
        public void accept(@Nullable ClientTunnelInfo newType) {
            index = indexOf(newType);
            type = newType;
            commitType();
            closeTypeSelector(newType);
        }

        @Override
        public int x() {
            return guiLeft;
        }

        @Override
        public int y() {
            return yPosition;
        }
    }

    private final class RefreshButton extends WidgetButton {

        private RefreshButton() {
            super(GuiAdvancedMemoryCard.this, 0, 0, 32, 32);
        }

        @Override
        public boolean mousePressed(int mouseX, int mouseY, int button) {
            if (!super.mousePressed(mc, mouseX, mouseY)) {
                return false;
            }
            ModNetwork.CHANNEL.sendToServer(new C2SRefreshP2PList(getTypeID()));
            return true;
        }
    }
}
