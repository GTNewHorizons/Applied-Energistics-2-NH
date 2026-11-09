package appeng.client.gui.implementations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.config.TransferDirection;
import appeng.api.config.YesNo;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.parts.ITransferTerminal;
import appeng.api.util.NamedDimensionalCoord;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.slots.VirtualMEPhantomSlot;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.client.render.highlighter.BlockPosHighlighter;
import appeng.container.implementations.ContainerTransferTerminal;
import appeng.container.slot.AppEngSlot;
import appeng.container.sync.handlers.BooleanSyncHandler;
import appeng.core.AEConfig;
import appeng.core.localization.ColorUtils;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.transfer.ConfigStripWindow;
import appeng.helpers.transfer.TransferNameFilter;
import appeng.helpers.transfer.TransferRowWindow;
import appeng.util.Platform;

public class GuiTransferTerminal extends AEBaseGui implements IDropToFillTextField {

    private static final int GUI_WIDTH = 208;
    private static final int STRETCH_TOP = 52;
    private static final int STRETCH_BOTTOM = 124;
    private static final int FIXED_BOTTOM_HEIGHT = 132;
    private static final int EXTRA_BOTTOM_MARGIN = 32;
    private static final int LIST_LEFT = 8;
    private static final int LIST_RIGHT = 168;
    private static final int LIST_TOP = 55;
    private static final int LIST_BOTTOM_MARGIN = 3;
    private static final int ROW_HEIGHT = 29;
    private static final int SMALL_VISIBLE_ROWS = 3;
    private static final int MIN_VISIBLE_ROWS = 2;
    private static final int SCROLLBAR_LEFT = 175;
    private static final int SCROLLBAR_TOP = 18;
    private static final int HIGHLIGHT_X = LIST_LEFT;
    private static final int HIGHLIGHT_Y = 5;
    private static final int HALF_BUTTON_SIZE = 8;
    private static final int ICON_X = LIST_LEFT + 10;
    private static final int TEXT_X = LIST_LEFT + 28;
    private static final int NAME_MIN_WIDTH = 40;
    private static final int STRIP_Y = 11;
    private static final int STRIP_X = TEXT_X + 20;
    private static final int STRIP_VISIBLE = 6;
    private static final int STRIP_CELL = 16;
    private static final int ELLIPSIS_WIDTH = 9;
    private static final int QUERY_SLOT_X = 11;
    private static final int QUERY_SLOT_Y = 21;
    private static final int FILTER_BUTTON_X = 30;
    private static final int FILTER_BUTTON_STEP = 18;
    private static final int FILTER_BUTTON_Y = 21;
    private static final int SEARCH_X = 10;
    private static final int SEARCH_Y = 40;
    private static final int SEARCH_WIDTH = 156;
    private static final int SEARCH_HEIGHT = 12;
    private static final int TITLE_WIDTH = 160;

    // interfaceterminal.png has a search bar in the title row, it is shared with the Interface Terminal.
    private static final int TEXTURE_BAR_X = 104;
    private static final int TEXTURE_BAR_Y = 4;
    private static final int TEXTURE_BAR_WIDTH = 65;
    private static final int TEXTURE_BLANK_X = 8;
    private static final int SEARCH_PIECE_WIDTH = 52;
    private static final int TEXTURE_BAR_MIDDLE_OFFSET = 6;

    private static final int QUERY_MATCH_COLOR = 0x6600FF00;
    private static final int FUZZY_MARK_COLOR = 0xFF4040FF;
    private static final int SELECTED_COLOR = 0xFF00A000;
    private static final int SLOT_BORDER_COLOR = 0xFF373737;
    private static final int SLOT_FILL_COLOR = 0xFF8B8B8B;

    private final ContainerTransferTerminal container;
    private final List<TransferComponentInfo> pending = new ArrayList<>();
    private final List<Row> cachedRows = new ArrayList<>();
    private final List<Row> locallyFilteredRows = new ArrayList<>();
    private final String clientLanguage;
    private final Map<Integer, GuiImgButton> settingButtons = new HashMap<>();
    private final GuiImgButton highlightButton = new GuiImgButton(
            HIGHLIGHT_X,
            0,
            Settings.ACTIONS,
            ActionItems.HIGHLIGHT_INTERFACE);

    private MEGuiTextField searchField;
    private GuiImgButton terminalStyleBox;
    private GuiImgButton directionButton;
    private GuiImgButton craftingButton;
    private boolean tallMode;
    private int visibleRows = SMALL_VISIBLE_ROWS;
    private int cacheStart;
    private int serverTotal;
    private int lastMouseX;
    private int lastMouseY;

    public GuiTransferTerminal(final InventoryPlayer inventoryPlayer, final ITransferTerminal host) {
        super(new ContainerTransferTerminal(inventoryPlayer, host));
        this.container = (ContainerTransferTerminal) this.inventorySlots;
        this.clientLanguage = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
        this.container.serverLanguageSync.onClientChange((oldValue, newValue) -> this.updateSearchTooltip());
        this.tallMode = AEConfig.instance.getConfigManager().getSetting(Settings.TERMINAL_STYLE) == TerminalStyle.TALL;
        this.xSize = GUI_WIDTH;
        this.highlightButton.setHalfSize(true);
        this.recalculateScreenSize();
        this.setScrollBar(new GuiScrollbar());
    }

    @Override
    public void initGui() {
        final String previousSearch = this.searchField == null ? "" : this.searchField.getText();
        this.recalculateScreenSize();
        super.initGui();

        this.registerVirtualSlots(
                new VirtualMEPhantomSlot(
                        QUERY_SLOT_X,
                        QUERY_SLOT_Y,
                        this.container.querySync,
                        0,
                        (slot, type, button) -> true));

        this.terminalStyleBox = new GuiImgButton(
                this.guiLeft - 18,
                this.guiTop + 8,
                Settings.TERMINAL_STYLE,
                this.tallMode ? TerminalStyle.TALL : TerminalStyle.SMALL);
        this.buttonList.add(this.terminalStyleBox);

        this.directionButton = this.addFilterButton(0, Settings.TRANSFER_DIRECTION, this.getMode());
        this.craftingButton = this
                .addToggleButton(1, Settings.TRANSFER_CRAFTING_CARD_ONLY, this.container.craftingOnlySync);

        this.searchField = new MEGuiTextField(SEARCH_WIDTH, SEARCH_HEIGHT, this.searchTooltip()) {

            @Override
            public void onTextChange(final String oldText) {
                super.onTextChange(oldText);
                GuiTransferTerminal.this.refreshRows();
            }
        };
        this.searchField.x = this.guiLeft + SEARCH_X;
        this.searchField.y = this.guiTop + SEARCH_Y;
        this.searchField.setText(previousSearch);

        this.repositionSlots();
        this.refreshRows();
    }

    private GuiImgButton addFilterButton(final int index, final Settings setting, final Enum<?> value) {
        final GuiImgButton button = new GuiImgButton(
                this.guiLeft + FILTER_BUTTON_X + index * FILTER_BUTTON_STEP,
                this.guiTop + FILTER_BUTTON_Y,
                setting,
                value);
        this.buttonList.add(button);
        return button;
    }

    private GuiImgButton addToggleButton(final int index, final Settings setting, final BooleanSyncHandler sync) {
        return this.addFilterButton(index, setting, sync.get() ? YesNo.YES : YesNo.NO);
    }

    private String searchTooltip() {
        String tooltip = GuiText.TransferTerminalSearchTooltip.getLocal();
        final String serverLanguage = this.container.serverLanguageSync.get();
        if (!serverLanguage.isEmpty() && !serverLanguage.equals(this.clientLanguage)) {
            tooltip += "\n" + String.format(
                    GuiText.TransferTerminalSearchLanguageWarning.getLocal(),
                    serverLanguage,
                    this.clientLanguage);
        }
        return tooltip.replace("\\n", "\n");
    }

    private void updateSearchTooltip() {
        if (this.searchField != null) {
            this.searchField.setMessage(this.searchTooltip());
        }
    }

    private void updateScrollBar() {
        this.getScrollBar().setTop(SCROLLBAR_TOP).setLeft(SCROLLBAR_LEFT)
                .setHeight(LIST_TOP - SCROLLBAR_TOP + this.visibleRows * ROW_HEIGHT);
        this.getScrollBar().setRange(0, Math.max(0, this.rowCount() - this.visibleRows), 1);
    }

    private String searchText() {
        return this.searchField == null ? "" : this.searchField.getText().trim();
    }

    private boolean languagesMatch() {
        return this.clientLanguage.equals(this.container.serverLanguageSync.get());
    }

    /**
     * Names on the server are in its language, so a client in another language gets the whole list and searches it.
     */
    private boolean isFilteringLocally() {
        return !this.searchText().isEmpty() && !this.languagesMatch();
    }

    private void refreshRows() {
        this.locallyFilteredRows.clear();
        if (this.isFilteringLocally()) {
            final String text = this.searchText();
            for (final Row row : this.cachedRows) {
                if (TransferNameFilter.matches(row.configNames, text)) {
                    this.locallyFilteredRows.add(row);
                }
            }
        }
        this.updateScrollBar();
    }

    private int rowCount() {
        return this.isFilteringLocally() ? this.locallyFilteredRows.size() : this.serverTotal;
    }

    /**
     * @return the row at a position in the list, or null if the server has not sent it yet
     */
    private Row rowAt(final int index) {
        if (this.isFilteringLocally()) {
            return index >= 0 && index < this.locallyFilteredRows.size() ? this.locallyFilteredRows.get(index) : null;
        }

        final int offset = index - this.cacheStart;
        return offset >= 0 && offset < this.cachedRows.size() ? this.cachedRows.get(offset) : null;
    }

    /**
     * Rows arrive in display order, a large window spans several packets and replaces the current one once complete.
     */
    public void receiveRows(final List<TransferComponentInfo> received, final int total, final int start,
            final boolean first, final boolean last) {
        if (first) {
            this.pending.clear();
        }
        this.pending.addAll(received);

        if (last) {
            this.cachedRows.clear();
            for (final TransferComponentInfo info : this.pending) {
                this.cachedRows.add(new Row(info));
            }
            this.pending.clear();
            this.serverTotal = total;
            this.cacheStart = start;
            this.refreshRows();
        }
    }

    @Override
    public void updateScreen() {
        this.requestSearchAndWindow();
        super.updateScreen();
    }

    private void requestSearchAndWindow() {
        final String serverText = this.languagesMatch() ? this.searchText() : "";
        if (!serverText.equals(this.container.searchTextSync.get())) {
            this.container.searchTextSync.set(serverText);
        }

        final boolean wholeList = this.isFilteringLocally();
        final int size = wholeList ? TransferRowWindow.ALL : TransferRowWindow.requestSize(this.visibleRows);
        final int first = this.getScrollBar().getCurrentScroll();
        final boolean sameSize = size == this.container.windowSizeSync.get();
        if (sameSize && (wholeList || TransferRowWindow
                .covers(this.cacheStart, this.cachedRows.size(), first, this.visibleRows, this.serverTotal))) {
            return;
        }

        this.container.windowStartSync.set(wholeList ? 0 : TransferRowWindow.requestStart(first, this.visibleRows));
        this.container.windowSizeSync.set(size);
    }

    @Override
    protected void actionPerformed(final GuiButton button) {
        super.actionPerformed(button);

        if (button == this.directionButton) {
            final TransferDirection next = Platform
                    .rotateEnum(this.getMode(), Mouse.isButtonDown(1), Settings.TRANSFER_DIRECTION.getPossibleValues());
            this.directionButton.set(next);
            this.container.modeSync.set(next.ordinal());
        } else if (button == this.craftingButton) {
            this.toggle(this.craftingButton, this.container.craftingOnlySync);
        } else if (button == this.terminalStyleBox) {
            this.tallMode = !this.tallMode;
            final TerminalStyle style = this.tallMode ? TerminalStyle.TALL : TerminalStyle.SMALL;
            AEConfig.instance.getConfigManager().putSetting(Settings.TERMINAL_STYLE, style);
            this.setWorldAndResolution(this.mc, this.width, this.height);
        }
    }

    private void toggle(final GuiImgButton button, final BooleanSyncHandler sync) {
        final boolean enabled = !sync.get();
        sync.set(enabled);
        button.set(enabled ? YesNo.YES : YesNo.NO);
    }

    private TransferDirection getMode() {
        final int mode = this.container.modeSync.get();
        final TransferDirection[] directions = TransferDirection.values();
        return mode >= 0 && mode < directions.length ? directions[mode] : TransferDirection.BOTH;
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.handleTooltip(mouseX, mouseY, this.searchField);
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (!this.searchField.textboxKeyTyped(character, key)) {
            super.keyTyped(character, key);
        }
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) {
        super.mouseClicked(mouseX, mouseY, button);
        this.searchField.mouseClicked(mouseX, mouseY, button);
        if (button == 0) {
            this.handleHighlightClick(mouseX - this.guiLeft, mouseY - this.guiTop);
        }
    }

    private void handleHighlightClick(final int relX, final int relY) {
        final int index = this.rowIndexAt(relY);
        if (index < 0 || relX < HIGHLIGHT_X || relX >= HIGHLIGHT_X + HALF_BUTTON_SIZE) {
            return;
        }

        final int rowTop = this.rowTop(index);
        if (relY < rowTop + HIGHLIGHT_Y || relY >= rowTop + HIGHLIGHT_Y + HALF_BUTTON_SIZE) {
            return;
        }

        final Row row = this.rowAt(index);
        if (row == null) {
            return;
        }

        final String customName = row.info.customName;
        this.highlightButton.func_146113_a(this.mc.getSoundHandler());
        BlockPosHighlighter.highlightNamedBlocks(
                this.mc.thePlayer,
                Collections.singletonMap(
                        new NamedDimensionalCoord(row.info.x, row.info.y, row.info.z, row.info.dim, customName),
                        customName.isEmpty()
                                ? new String[] { PlayerMessages.MachineHighlighted.getUnlocalized(),
                                        PlayerMessages.MachineInOtherDim.getUnlocalized() }
                                : new String[] { PlayerMessages.MachineHighlightedNamed.getUnlocalized(),
                                        PlayerMessages.MachineInOtherDimNamed.getUnlocalized() }),
                row.baseName);
        this.closeGui();
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        final int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            this.getScrollBar().wheel(wheel);
        }
    }

    @Override
    public boolean isOverTextField(final int mouseX, final int mouseY) {
        return this.searchField.isMouseIn(mouseX, mouseY);
    }

    @Override
    public void setTextFieldValue(final String displayName, final int mouseX, final int mouseY, final ItemStack stack) {
        this.searchField.setText(displayName);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture("guis/interfaceterminal.png");
        final int middleHeight = this.getMiddleHeight();
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, STRETCH_TOP);
        this.drawSearchBar(offsetX, offsetY);

        final Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        this.addTexturedRectToTesselator(
                offsetX,
                offsetY + STRETCH_TOP,
                offsetX + this.xSize,
                offsetY + STRETCH_TOP + middleHeight + 1,
                0.0f,
                0.0f,
                STRETCH_TOP / 256.0f,
                this.xSize / 256.0f,
                STRETCH_BOTTOM / 256.0f);
        tessellator.draw();

        this.drawTexturedModalRect(
                offsetX,
                offsetY + STRETCH_TOP + middleHeight,
                0,
                STRETCH_BOTTOM,
                this.xSize,
                FIXED_BOTTOM_HEIGHT);

        drawRect(
                offsetX + QUERY_SLOT_X - 1,
                offsetY + QUERY_SLOT_Y - 1,
                offsetX + QUERY_SLOT_X + 17,
                offsetY + QUERY_SLOT_Y + 17,
                SLOT_BORDER_COLOR);
        drawRect(
                offsetX + QUERY_SLOT_X,
                offsetY + QUERY_SLOT_Y,
                offsetX + QUERY_SLOT_X + 16,
                offsetY + QUERY_SLOT_Y + 16,
                SLOT_FILL_COLOR);

        this.drawSelectionIf(this.container.craftingOnlySync.get(), this.craftingButton);

        final int lineColor = ColorUtils.craftingDiagnosticTerminalLine.getColor();
        for (int line = 0; line <= this.visibleRows; line++) {
            final int y = offsetY + LIST_TOP + line * ROW_HEIGHT - 1;
            drawRect(offsetX + LIST_LEFT, y, offsetX + LIST_RIGHT, y + 1, lineColor);
        }

        GL11.glColor4f(1, 1, 1, 1);
        this.searchField.drawTextBox();
    }

    /**
     * Moves the texture's search bar out of the title row and stretches it, copying the texture rather than drawing
     * colours so that resource packs still apply.
     */
    private void drawSearchBar(final int offsetX, final int offsetY) {
        this.drawTexturedModalRect(
                offsetX + TEXTURE_BAR_X,
                offsetY + TEXTURE_BAR_Y,
                TEXTURE_BLANK_X,
                TEXTURE_BAR_Y,
                TEXTURE_BAR_WIDTH,
                SEARCH_HEIGHT);

        final int[] sources = { TEXTURE_BAR_X, TEXTURE_BAR_X + TEXTURE_BAR_MIDDLE_OFFSET,
                TEXTURE_BAR_X + TEXTURE_BAR_WIDTH - SEARCH_PIECE_WIDTH };
        for (int piece = 0; piece < sources.length; piece++) {
            this.drawTexturedModalRect(
                    offsetX + SEARCH_X + piece * SEARCH_PIECE_WIDTH,
                    offsetY + SEARCH_Y,
                    sources[piece],
                    TEXTURE_BAR_Y,
                    SEARCH_PIECE_WIDTH,
                    SEARCH_HEIGHT);
        }
    }

    private void drawSelectionIf(final boolean active, final GuiImgButton button) {
        if (!active) {
            return;
        }

        drawRect(
                button.xPosition - 1,
                button.yPosition - 1,
                button.xPosition + button.getWidth() + 1,
                button.yPosition + button.getHeight() + 1,
                SELECTED_COLOR);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        final int relX = this.lastMouseX - offsetX;
        final int relY = this.lastMouseY - offsetY;
        final String title = GuiText.TransferTerminal.getLocal();

        this.fontRendererObj.drawString(
                this.fontRendererObj.trimStringToWidth(title, TITLE_WIDTH),
                8,
                7,
                ColorUtils.guiTextColorGray.getColor());

        final List<String> tooltip = new ArrayList<>();
        final int first = this.getScrollBar().getCurrentScroll();
        final int end = Math.min(this.rowCount(), first + this.visibleRows);
        for (int index = first; index < end; index++) {
            final Row row = this.rowAt(index);
            if (row != null) {
                this.drawRow(row, this.rowTop(index), relX, relY, tooltip);
            }
        }

        if (relX >= 8 && relX < 8 + TITLE_WIDTH && relY >= 5 && relY < 16) {
            tooltip.clear();
            tooltip.add(title);
            tooltip.add(GuiText.TransferTerminalOrderTooltip.getLocal());
            tooltip.add(GuiText.TransferTerminalNetworkTooltip.getLocal());
            tooltip.add(GuiText.TransferTerminalMoveTooltip.getLocal());
            tooltip.add(GuiText.TransferTerminalTieTooltip.getLocal());
        } else if (this.isOverEmptyQuerySlot(relX, relY)) {
            tooltip.clear();
            tooltip.add(GuiText.TransferTerminalQueryTooltip.getLocal());
        }

        GL11.glColor4f(1, 1, 1, 1);
        this.getScrollBar().draw(this);

        if (!tooltip.isEmpty()) {
            this.drawTooltip(relX + 12, relY, tooltip.toArray(new String[0]));
        }
    }

    private boolean isOverEmptyQuerySlot(final int relX, final int relY) {
        return relX >= QUERY_SLOT_X && relX < QUERY_SLOT_X + 16
                && relY >= QUERY_SLOT_Y
                && relY < QUERY_SLOT_Y + 16
                && this.container.getQuery() == null;
    }

    private void drawRow(final Row row, final int rowTop, final int relX, final int relY, final List<String> tooltip) {
        final boolean hovered = relX >= LIST_LEFT && relX <= LIST_RIGHT && relY >= rowTop && relY < rowTop + ROW_HEIGHT;
        if (hovered) {
            drawRect(
                    LIST_LEFT,
                    rowTop,
                    LIST_RIGHT,
                    rowTop + ROW_HEIGHT - 1,
                    ColorUtils.craftingDiagnosticTerminalRowHover.getColor());
            tooltip.addAll(row.getTooltip());
        }

        this.highlightButton.yPosition = rowTop + HIGHLIGHT_Y;
        GL11.glColor4f(1, 1, 1, 1);
        this.highlightButton.drawButton(this.mc, relX, relY);
        if (hovered && relX >= HIGHLIGHT_X
                && relX < HIGHLIGHT_X + HALF_BUTTON_SIZE
                && relY >= this.highlightButton.yPosition
                && relY < this.highlightButton.yPosition + HALF_BUTTON_SIZE) {
            tooltip.clear();
            tooltip.add(this.highlightButton.getMessage());
        }

        if (row.info.selfRep != null) {
            this.drawItem(ICON_X, rowTop + 6, row.info.selfRep);
        }

        int rightEdge = LIST_RIGHT;
        rightEdge = this.drawUpgrades(row, rowTop, rightEdge, relX, relY, tooltip);
        rightEdge = this.drawSettings(row, rowTop, rightEdge, relX, relY, tooltip);

        if (row.description.hasPriority) {
            final String priority = "P:" + row.description.priority;
            rightEdge -= this.fontRendererObj.getStringWidth(priority) + 2;
            this.fontRendererObj.drawString(priority, rightEdge, rowTop + 2, ColorUtils.guiTextColorGray.getColor());
        }

        final String name = this.fontRendererObj
                .trimStringToWidth(row.getDisplayLine(), Math.max(NAME_MIN_WIDTH, rightEdge - TEXT_X - 2));
        this.fontRendererObj.drawString(name, TEXT_X, rowTop + 2, ColorUtils.guiTextColorBlack.getColor());

        this.fontRendererObj
                .drawString(row.directionLabel(), TEXT_X, rowTop + STRIP_Y + 4, ColorUtils.guiTextColorGray.getColor());
        this.drawFilter(row, rowTop, relX, relY, tooltip);
    }

    private int drawUpgrades(final Row row, final int rowTop, int rightEdge, final int relX, final int relY,
            final List<String> tooltip) {
        for (final ItemStack upgrade : row.description.upgrades) {
            if (rightEdge - HALF_BUTTON_SIZE - TEXT_X < NAME_MIN_WIDTH) {
                break;
            }
            rightEdge -= HALF_BUTTON_SIZE + 1;
            GL11.glPushMatrix();
            GL11.glTranslatef(rightEdge, rowTop + 1, 0);
            GL11.glScalef(0.5f, 0.5f, 1f);
            this.drawItem(0, 0, upgrade);
            GL11.glPopMatrix();

            if (this.isOverCell(relX, relY, rightEdge, rowTop + 1)) {
                tooltip.clear();
                tooltip.add(upgrade.stackSize + "x " + upgrade.getDisplayName());
            }
        }
        return rightEdge;
    }

    private int drawSettings(final Row row, final int rowTop, int rightEdge, final int relX, final int relY,
            final List<String> tooltip) {
        for (final TransferDescription.SettingEntry entry : row.description.settings) {
            if (rightEdge - HALF_BUTTON_SIZE - TEXT_X < NAME_MIN_WIDTH) {
                break;
            }
            rightEdge -= HALF_BUTTON_SIZE + 1;
            final GuiImgButton button = this.settingButton(entry);
            button.xPosition = rightEdge;
            button.yPosition = rowTop + 1;
            GL11.glColor4f(1, 1, 1, 1);
            button.drawButton(this.mc, relX, relY);

            if (this.isOverCell(relX, relY, rightEdge, rowTop + 1)) {
                tooltip.clear();
                tooltip.add(button.getMessage());
            }
        }
        return rightEdge;
    }

    private boolean isOverCell(final int relX, final int relY, final int x, final int y) {
        return relX >= x && relX < x + HALF_BUTTON_SIZE && relY >= y && relY < y + HALF_BUTTON_SIZE;
    }

    private GuiImgButton settingButton(final TransferDescription.SettingEntry entry) {
        final int key = entry.setting.ordinal() << 8 | entry.value.ordinal();
        return this.settingButtons.computeIfAbsent(key, ignored -> {
            final GuiImgButton button = new GuiImgButton(0, 0, entry.setting, entry.value);
            button.setHalfSize(true);
            return button;
        });
    }

    private void drawFilter(final Row row, final int rowTop, final int relX, final int relY,
            final List<String> tooltip) {
        final int y = rowTop + STRIP_Y;

        if (row.description.oreFilter != null) {
            final String text = this.fontRendererObj.trimStringToWidth(
                    String.format(GuiText.TransferTermOreFilter.getLocal(), row.description.oreFilter),
                    LIST_RIGHT - STRIP_X);
            this.fontRendererObj.drawString(text, STRIP_X, y + 4, ColorUtils.guiTextColorGray.getColor());
            return;
        }

        final List<TransferDescription.ConfigEntry> config = row.description.config;
        final ConfigStripWindow window = ConfigStripWindow.compute(config.size(), STRIP_VISIBLE, row.focusIndex());
        int x = STRIP_X;

        if (window.leftEllipsis) {
            this.fontRendererObj.drawString("...", x, y + 4, ColorUtils.guiTextColorGray.getColor());
            x += ELLIPSIS_WIDTH;
        }

        for (int index = window.start; index < window.end; index++) {
            final TransferDescription.ConfigEntry entry = config.get(index);
            if (entry.matchesQuery) {
                drawRect(x, y, x + STRIP_CELL, y + STRIP_CELL, QUERY_MATCH_COLOR);
            }

            final ItemStack icon = entry.stack.getItemStackForNEI();
            if (icon != null) {
                this.drawItem(x, y, icon);
            }
            if (entry.fuzzy) {
                drawRect(x, y + STRIP_CELL - 2, x + 2, y + STRIP_CELL, FUZZY_MARK_COLOR);
            }

            if (relX >= x && relX < x + STRIP_CELL && relY >= y && relY < y + STRIP_CELL) {
                tooltip.clear();
                tooltip.add(entry.stack.getDisplayName());
                if (entry.fuzzy) {
                    tooltip.add(GuiText.TransferTermFuzzySlot.getLocal());
                }
                if (config.size() > STRIP_VISIBLE) {
                    tooltip.add(
                            String.format(
                                    GuiText.TransferTermShowingSlots.getLocal(),
                                    window.end - window.start,
                                    config.size()));
                }
            }
            x += STRIP_CELL;
        }

        if (window.rightEllipsis) {
            this.fontRendererObj.drawString("...", x, y + 4, ColorUtils.guiTextColorGray.getColor());
        }
    }

    private int rowTop(final int index) {
        return LIST_TOP + (index - this.getScrollBar().getCurrentScroll()) * ROW_HEIGHT;
    }

    /**
     * @return the row under a position relative to the gui, or -1
     */
    private int rowIndexAt(final int relY) {
        if (relY < LIST_TOP || relY >= LIST_TOP + this.visibleRows * ROW_HEIGHT) {
            return -1;
        }

        final int index = this.getScrollBar().getCurrentScroll() + (relY - LIST_TOP) / ROW_HEIGHT;
        return index < this.rowCount() ? index : -1;
    }

    private int getMiddleHeight() {
        return LIST_TOP + this.visibleRows * ROW_HEIGHT + LIST_BOTTOM_MARGIN - STRETCH_TOP;
    }

    private void recalculateScreenSize() {
        this.visibleRows = SMALL_VISIBLE_ROWS;
        if (this.height > 0) {
            final int baseHeight = this.getWindowHeight();
            if (baseHeight > this.height) {
                this.visibleRows = MIN_VISIBLE_ROWS;
            } else if (this.tallMode) {
                this.visibleRows += Math.max(0, this.height - baseHeight - EXTRA_BOTTOM_MARGIN) / ROW_HEIGHT;
            }
        }

        this.ySize = this.getWindowHeight();
    }

    private int getWindowHeight() {
        return STRETCH_TOP + this.getMiddleHeight() + FIXED_BOTTOM_HEIGHT;
    }

    private void repositionSlots() {
        for (final Object object : this.inventorySlots.inventorySlots) {
            if (object instanceof AppEngSlot slot) {
                slot.yDisplayPosition = this.ySize + slot.getY() - 78 - 7;
            }
        }
    }

    private static String resolveSuffix(final String json) {
        try {
            final IChatComponent component = IChatComponent.Serializer.func_150699_a(json);
            return component == null ? "" : component.getUnformattedText();
        } catch (final RuntimeException e) {
            return "";
        }
    }

    private static final class Row {

        private final TransferComponentInfo info;
        private final TransferDescription description;
        private final String baseName;
        private final String displayName;
        private final List<String> configNames = new ArrayList<>();

        private Row(final TransferComponentInfo info) {
            this.info = info;
            this.description = info.description;

            final String representation = info.selfRep == null ? "" : info.selfRep.getDisplayName();
            this.baseName = info.customName.isEmpty() ? representation : info.customName;
            this.displayName = info.nameSuffixJson == null ? this.baseName
                    : this.baseName + " " + resolveSuffix(info.nameSuffixJson);

            for (final TransferDescription.ConfigEntry entry : info.description.config) {
                this.configNames.add(entry.stack.getDisplayName());
            }
        }

        private String getDisplayLine() {
            return this.info.networkTier == 0 ? this.displayName
                    : GuiText.TransferTermSubNetwork.getLocal() + " " + this.displayName;
        }

        private String directionLabel() {
            if (this.description.canImport && this.description.canExport) {
                return GuiText.TransferTermBoth.getLocal();
            }
            if (this.description.canImport) {
                return GuiText.TransferTermIn.getLocal();
            }
            return this.description.canExport ? GuiText.TransferTermOut.getLocal()
                    : GuiText.TransferTermNone.getLocal();
        }

        private int focusIndex() {
            for (int index = 0; index < this.description.config.size(); index++) {
                if (this.description.config.get(index).matchesQuery) {
                    return index;
                }
            }
            return -1;
        }

        private List<String> getTooltip() {
            final List<String> lines = new ArrayList<>();
            lines.add(this.displayName);
            lines.add(
                    String.format(
                            GuiText.TransferTermLocation.getLocal(),
                            this.info.x,
                            this.info.y,
                            this.info.z,
                            this.info.dim));
            if (this.description.hasPriority) {
                lines.add(String.format(GuiText.TransferTermPriority.getLocal(), this.description.priority));
            }
            if (this.description.oreFilter != null) {
                lines.add(String.format(GuiText.TransferTermOreFilter.getLocal(), this.description.oreFilter));
            }
            if (this.info.networkTier > 0) {
                lines.add(GuiText.TransferTermSubNetworkTooltip.getLocal());
            }
            return lines;
        }
    }
}
