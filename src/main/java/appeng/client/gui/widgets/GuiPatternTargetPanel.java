package appeng.client.gui.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import appeng.api.crafting.PatternMatch;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.ScreenColor;
import appeng.client.gui.implementations.GuiInterfaceTerminal;
import appeng.core.localization.ColorUtils;
import appeng.core.localization.GuiText;
import appeng.crafting.target.PatternTargetList;

/**
 * Side panel under the Pattern Terminal's left button column that lists the interfaces for the encoded pattern, drawn
 * from AE2's own GUI textures. Coordinates are relative to the terminal's top left corner.
 */
public class GuiPatternTargetPanel {

    /**
     * What the panel asks the terminal to do on a click.
     */
    public interface Actions {

        void sendToTarget(int index);

        void extractConflict(int index);
    }

    private static final int MAX_WIDTH = 200;
    private static final int MIN_WIDTH = 100;
    // Narrower panels draw text at the CPU table's 0.8 scale so names keep more of their length.
    private static final int COMPACT_WIDTH = 150;
    private static final float COMPACT_SCALE = 0.8F;
    private static final int BUTTON_GAP = 2;
    // The terminal draws up to two 18 px button columns to its left.
    private static final int BESIDE_BUTTONS_GAP = 40;
    private static final int SCREEN_MARGIN = 2;
    private static final int PADDING = 6;
    private static final int HEADER_HEIGHT = 24;
    private static final int CARD_HEIGHT = 23;
    private static final int MAX_VISIBLE_CARDS = 5;
    private static final int EMPTY_LIST_HEIGHT = 2 * CARD_HEIGHT + 2;
    private static final int TRACK_WIDTH = 14;
    private static final int CONFLICT_ROWS = 3;
    private static final int LINE_HEIGHT = 10;
    private static final int BUTTON_HEIGHT = 11;

    private static final String FRAME_TEXTURE = "guis/craftAmt.png";
    private static final String LIST_TEXTURE = "guis/cpu_selector.png";

    private static final int STRIPE_MATCH = 0xFF2E9E2E;
    private static final int STRIPE_UNKNOWN = 0xFFC8A000;
    private static final int STRIPE_NOT_READY = 0xFF8B8B8B;
    private static final int STRIPE_CONFLICT = 0xFFD08A1E;
    private static final int STRIPE_CONFLICT_OTHER = 0xFFB02020;
    private static final int TEXT_MATCH = 0x2E7D32;
    private static final int TEXT_UNKNOWN = 0x8A6D00;
    private static final int TEXT_DIM = 0x606060;
    private static final int TEXT_HAS_SAME = 0xB07000;
    private static final int TEXT_CONFLICT = 0x9A1010;

    private final AEBaseGui gui;
    private final Actions actions;
    private final GuiScrollbar scrollbar = new GuiScrollbar();

    private PatternTargetList list = PatternTargetList.EMPTY;
    private final List<PatternTargetList.Entry> entries = new ArrayList<>();
    private ItemStack patternIcon;

    private int x;
    private int y;
    private int width;
    private int height;
    private int listTop;
    private int listHeight;
    private int insetX;
    private int insetWidth;
    private int visibleCards;
    private boolean scrollable;
    private boolean compact;
    private int conflictTop;
    private List<String> conflictTitle = Collections.emptyList();
    private boolean dragging;

    public GuiPatternTargetPanel(AEBaseGui gui, Actions actions) {
        this.gui = gui;
        this.actions = actions;
    }

    /**
     * Shows the given list; a new list resets the scroll.
     */
    public void setList(PatternTargetList list) {
        if (list == this.list) {
            return;
        }
        this.list = list;
        this.entries.clear();
        this.entries.addAll(list.entries);
        // Within a status, interfaces that already hold the same pattern or need a card go last.
        this.entries.sort(
                Comparator.comparingInt((PatternTargetList.Entry e) -> statusOrder(e.match))
                        .thenComparing(e -> e.hasSamePattern)
                        .thenComparingInt(e -> e.cardSource == PatternTargetList.Entry.CARD_NONE ? 0 : 1)
                        .thenComparing(GuiPatternTargetPanel::displayName));
        this.scrollbar.setCurrentScroll(0);
    }

    public void setPatternIcon(ItemStack patternIcon) {
        this.patternIcon = patternIcon;
    }

    public boolean isVisible() {
        return this.list.hasPattern;
    }

    /**
     * Places the panel left of the terminal, under its button column or beside it, as tall as its content and clear of
     * the given screen rectangles {x, y, w, h}, such as NEI's buttons.
     */
    public void layout(int guiLeft, int guiTop, int guiHeight, int screenHeight, int buttonsBottom,
            List<int[]> blocked) {
        this.place(guiLeft, guiTop, guiHeight, screenHeight, buttonsBottom + BUTTON_GAP, 0, blocked);
        int availableHeight = this.height;
        int conflictHeight = this.conflictHeight();
        // A tall button column, as on wireless terminals, can leave no room under it; then the panel goes beside it.
        if (availableHeight < HEADER_HEIGHT + CARD_HEIGHT + 2 + PADDING + conflictHeight) {
            this.place(guiLeft, guiTop, guiHeight, screenHeight, 0, BESIDE_BUTTONS_GAP, blocked);
            availableHeight = this.height;
            conflictHeight = this.conflictHeight();
        }
        availableHeight = Math.max(HEADER_HEIGHT + CARD_HEIGHT + 2 + PADDING, availableHeight);

        this.listTop = this.y + HEADER_HEIGHT;
        final int maxListHeight = availableHeight - HEADER_HEIGHT - conflictHeight - PADDING;
        final int fitByHeight = Math.max(1, (maxListHeight - 2) / CARD_HEIGHT);
        this.visibleCards = Math.min(Math.min(MAX_VISIBLE_CARDS, fitByHeight), Math.max(1, this.entries.size()));
        this.scrollable = this.entries.size() > this.visibleCards;
        this.listHeight = this.entries.isEmpty() ? EMPTY_LIST_HEIGHT : this.visibleCards * CARD_HEIGHT + 2;

        this.insetX = this.x + PADDING;
        this.insetWidth = this.width - 2 * PADDING - (this.scrollable ? TRACK_WIDTH : 0);
        this.conflictTop = this.listTop + this.listHeight;
        this.height = Math.min(availableHeight, this.conflictTop + conflictHeight + PADDING - this.y);

        this.scrollbar.setLeft(this.insetX + this.insetWidth + 1);
        this.scrollbar.setTop(this.listTop + 1);
        this.scrollbar.setWidth(12);
        this.scrollbar.setHeight(this.listHeight - 2);
        this.scrollbar.setRange(0, Math.max(0, this.entries.size() - this.visibleCards), 1);
    }

    // Sets x, width and y, and the room down to the screen's bottom or the first blocked rectangle in height.
    private void place(int guiLeft, int guiTop, int guiHeight, int screenHeight, int startTop, int gap,
            List<int[]> blocked) {
        this.width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, guiLeft - gap - SCREEN_MARGIN));
        this.x = -gap - this.width;
        this.compact = this.width < COMPACT_WIDTH;

        int top = startTop;
        int bottom = screenHeight - guiTop - SCREEN_MARGIN;
        final int screenLeft = guiLeft + this.x;
        for (int[] rect : blocked) {
            if (rect[0] >= screenLeft + this.width || rect[0] + rect[2] <= screenLeft) {
                continue;
            }
            final int rectTop = rect[1] - guiTop;
            final int rectBottom = rectTop + rect[3];
            if (rectTop + rect[3] / 2 < guiHeight / 2) {
                top = Math.max(top, rectBottom + 2);
            } else {
                bottom = Math.min(bottom, rectTop - 2);
            }
        }
        this.y = top;
        this.height = bottom - top;
    }

    // Wraps the conflict title to the current width and returns the height of the whole conflict block.
    private int conflictHeight() {
        final int conflictCount = this.list.conflicts.size();
        final int conflictRows = Math.min(CONFLICT_ROWS, conflictCount);
        this.conflictTitle = conflictRows == 0 ? Collections.emptyList()
                : listLines(
                        this.gui.getFontRenderer(),
                        GuiText.PatternTargetsConflictTitle.getLocal(),
                        (int) ((this.width - 2 * PADDING - 10) / this.textScale()));
        return conflictRows == 0 ? 0
                : 4 + this.conflictTitle.size() * LINE_HEIGHT
                        + conflictRows * CARD_HEIGHT
                        + (conflictCount > CONFLICT_ROWS ? LINE_HEIGHT : 0);
    }

    public void draw(int mouseX, int mouseY) {
        if (!this.isVisible()) {
            return;
        }
        final FontRenderer font = this.gui.getFontRenderer();

        ScreenColor.setGuiColor();
        this.drawFrame(this.x, this.y, this.width, this.height);
        this.drawInset(this.insetX, this.listTop, this.insetWidth, this.listHeight);
        if (this.scrollable) {
            this.drawTrack(this.insetX + this.insetWidth, this.listTop, this.listHeight);
        }
        ScreenColor.resetGuiColor();
        if (this.scrollable) {
            this.scrollbar.draw(this.gui);
        }

        this.drawHeader(font);

        if (this.entries.isEmpty()) {
            int textY = this.listTop + 4;
            final int lineWidth = this.insetWidth - 6;
            for (String line : listLines(
                    font,
                    GuiText.PatternTargetsNone.getLocal(),
                    (int) (lineWidth / this.textScale()))) {
                this.drawText(font, line, this.insetX + 4, textY, lineWidth, ColorUtils.guiTextColorGray.getColor());
                textY += LINE_HEIGHT;
            }
        }

        final int first = this.scrollbar.getCurrentScroll();
        for (int k = 0; k < this.visibleCards && first + k < this.entries.size(); k++) {
            final int cardY = this.cardY(k);
            final boolean hover = this.isOver(mouseX, mouseY, this.insetX + 1, cardY, this.insetWidth - 2, CARD_HEIGHT);
            this.drawEntryCard(font, this.entries.get(first + k), cardY, hover);
        }

        this.drawConflicts(font, mouseX, mouseY);
    }

    /**
     * Returns the tooltip for the card under the mouse, or null.
     */
    public List<String> getTooltip(int mouseX, int mouseY) {
        if (!this.isVisible()) {
            return null;
        }
        final PatternTargetList.Entry entry = this.entryAt(mouseX, mouseY);
        if (entry != null) {
            final List<String> lines = new ArrayList<>();
            lines.add(displayName(entry));
            lines.add(statusLabel(entry.match));
            lines.add(GuiText.GuiDimensionalCoord.getLocal(entry.x, entry.y, entry.z, entry.dim));
            if (entry.cardSource == PatternTargetList.Entry.CARD_FROM_INVENTORY) {
                lines.add(GuiText.PatternTargetsCardFromInventory.getLocal());
            } else if (entry.cardSource == PatternTargetList.Entry.CARD_FROM_NETWORK) {
                lines.add(GuiText.PatternTargetsCardFromNetwork.getLocal());
            } else {
                lines.add(GuiText.PatternTargetsFreeSlots.getLocal(entry.freeSlots));
            }
            if (entry.hasSamePattern) {
                lines.add(GuiText.PatternTargetsHasSameTooltip.getLocal());
            }
            lines.add(GuiText.PatternTargetsClick.getLocal());
            return lines;
        }
        final int conflict = this.conflictAt(mouseX, mouseY);
        if (conflict >= 0) {
            final PatternTargetList.Conflict c = this.list.conflicts.get(conflict);
            final List<String> lines = new ArrayList<>();
            lines.add(displayName(c));
            lines.add(GuiText.GuiDimensionalCoord.getLocal(c.x, c.y, c.z, c.dim));
            lines.add(
                    c.sameRecipe ? GuiText.PatternTargetsSameRecipe.getLocal()
                            : GuiText.PatternTargetsOtherRecipe.getLocal());
            if (this.isOverExtractButton(conflict, mouseX, mouseY)) {
                lines.add(GuiText.PatternTargetsExtractTooltip.getLocal());
            }
            return lines;
        }
        return null;
    }

    /**
     * Handles a click; returns whether it landed on the panel.
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!this.contains(mouseX, mouseY)) {
            return false;
        }
        if (button != 0) {
            return true;
        }
        if (this.scrollable && this.scrollbar.contains(mouseX, mouseY)) {
            this.scrollbar.click(this.gui, mouseX, mouseY);
            this.dragging = true;
            return true;
        }
        final PatternTargetList.Entry entry = this.entryAt(mouseX, mouseY);
        if (entry != null) {
            this.actions.sendToTarget(entry.index);
            return true;
        }
        final int conflict = this.conflictAt(mouseX, mouseY);
        if (conflict >= 0 && this.isOverExtractButton(conflict, mouseX, mouseY)) {
            this.actions.extractConflict(this.list.conflicts.get(conflict).index);
        }
        return true;
    }

    /**
     * Moves the scrollbar while it is dragged; returns whether the panel took the drag.
     */
    public boolean mouseDragged(int mouseY) {
        if (!this.dragging) {
            return false;
        }
        this.scrollbar.clickMove(mouseY);
        return true;
    }

    public void mouseReleased() {
        this.dragging = false;
        this.scrollbar.release();
    }

    /**
     * Scrolls the list when the mouse is over the panel; returns whether it used the wheel.
     */
    public boolean mouseWheel(int mouseX, int mouseY, int wheel) {
        if (!this.contains(mouseX, mouseY)) {
            return false;
        }
        if (this.scrollable) {
            this.scrollbar.wheel(wheel);
        }
        return true;
    }

    public boolean contains(int mouseX, int mouseY) {
        return this.isVisible() && this.isOver(mouseX, mouseY, this.x, this.y, this.width, this.height);
    }

    /**
     * Returns whether a rectangle overlaps the panel, for hiding NEI slots under it.
     */
    public boolean intersects(int rx, int ry, int rw, int rh) {
        return this.isVisible() && rx < this.x + this.width
                && rx + rw > this.x
                && ry < this.y + this.height
                && ry + rh > this.y;
    }

    private void drawHeader(FontRenderer font) {
        int titleX = this.x + PADDING;
        if (this.patternIcon != null) {
            this.gui.drawItem(titleX, this.y + 5, this.patternIcon);
            titleX += 20;
        }
        String counter = "";
        if (this.scrollable) {
            final int first = this.scrollbar.getCurrentScroll() + 1;
            final int last = Math.min(this.entries.size(), first + this.visibleCards - 1);
            counter = first + "-" + last + "/" + this.entries.size();
        }
        final int counterWidth = this.textWidth(font, counter);
        final int titleWidth = this.x + this.width - PADDING - counterWidth - 4 - titleX;
        this.drawText(
                font,
                GuiText.PatternTargetsTitle.getLocal(),
                titleX,
                this.y + 9,
                titleWidth,
                ColorUtils.guiTextColorGray.getColor());
        this.drawText(
                font,
                counter,
                this.x + this.width - PADDING - counterWidth,
                this.y + 9,
                counterWidth,
                ColorUtils.guiTextColorGray.getColor());
    }

    private float textScale() {
        return this.compact ? COMPACT_SCALE : 1.0F;
    }

    private int textWidth(FontRenderer font, String text) {
        return (int) Math.ceil(font.getStringWidth(text) * this.textScale());
    }

    private void drawText(FontRenderer font, String text, int textX, int textY, int maxWidth, int color) {
        final float scale = this.textScale();
        final String trimmed = ellipsize(font, text, (int) (maxWidth / scale));
        if (scale == 1.0F) {
            font.drawString(trimmed, textX, textY, color);
            return;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(textX, textY, 0);
        GL11.glScalef(scale, scale, 1.0F);
        font.drawString(trimmed, 0, 0, color);
        GL11.glPopMatrix();
    }

    /**
     * Cuts text to the width in unscaled font pixels, ending it with "..." when something was cut.
     */
    private static String ellipsize(FontRenderer font, String text, int maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) {
            return text;
        }
        // The vanilla ascii font has no glyph for U+2026, so three dots keep the same look.
        final String dots = "...";
        final int room = maxWidth - font.getStringWidth(dots);
        return room > 0 ? font.trimStringToWidth(text, room) + dots : "";
    }

    private void drawEntryCard(FontRenderer font, PatternTargetList.Entry entry, int cardY, boolean hover) {
        final int cardX = this.insetX + 1;
        final int cardWidth = this.insetWidth - 2;
        this.drawCard(cardX, cardY, cardWidth, CARD_HEIGHT, hover);
        Gui.drawRect(cardX + 1, cardY + 1, cardX + 3, cardY + CARD_HEIGHT - 1, stripeColor(entry.match));
        if (entry.icon != null) {
            this.gui.drawItem(cardX + 5, cardY + 3, entry.icon);
        }

        final int textX = cardX + 24;
        final int textWidth = cardX + cardWidth - 3 - textX;
        this.drawText(font, displayName(entry), textX, cardY + 3, textWidth, ColorUtils.guiTextColorBlack.getColor());
        final String room;
        int roomColor = statusTextColor(entry.match);
        if (entry.hasSamePattern) {
            room = GuiText.PatternTargetsHasSame.getLocal();
            roomColor = TEXT_HAS_SAME;
        } else if (entry.cardSource != PatternTargetList.Entry.CARD_NONE) {
            room = GuiText.PatternTargetsNeedsCard.getLocal();
        } else {
            room = GuiText.PatternTargetsFree.getLocal(entry.freeSlots);
        }
        // A narrow panel leaves the status to the stripe colour and the tooltip.
        int roomX = textX;
        if (!this.compact) {
            final String status = statusLabel(entry.match) + " · ";
            this.drawText(font, status, textX, cardY + 13, textWidth, statusTextColor(entry.match));
            roomX += this.textWidth(font, status);
        }
        this.drawText(font, room, roomX, cardY + 13, textX + textWidth - roomX, roomColor);
    }

    private void drawConflicts(FontRenderer font, int mouseX, int mouseY) {
        final int count = this.list.conflicts.size();
        if (count == 0) {
            return;
        }
        int lineY = this.conflictTop + 4;
        for (int i = 0; i < this.conflictTitle.size(); i++) {
            final String prefix = i == 0 ? "⚠ " : "  ";
            this.drawText(
                    font,
                    prefix + this.conflictTitle.get(i),
                    this.x + PADDING,
                    lineY,
                    this.width - 2 * PADDING,
                    TEXT_CONFLICT);
            lineY += LINE_HEIGHT;
        }

        final int cardX = this.x + PADDING;
        final int cardWidth = this.width - 2 * PADDING;
        final String extract = GuiText.PatternTargetsExtract.getLocal();
        final int buttonWidth = this.extractButtonWidth(font);
        for (int i = 0; i < Math.min(CONFLICT_ROWS, count); i++) {
            final PatternTargetList.Conflict conflict = this.list.conflicts.get(i);
            final int cardY = this.conflictCardY(i);
            GL11.glColor4f(1.0F, 0.85F, 0.75F, 1.0F);
            this.drawNineSlice(LIST_TEXTURE, cardX, cardY, cardWidth, CARD_HEIGHT, 100, 0, 67, 23, 1);
            ScreenColor.resetGuiColor();
            Gui.drawRect(
                    cardX + 1,
                    cardY + 1,
                    cardX + 3,
                    cardY + CARD_HEIGHT - 1,
                    conflict.sameRecipe ? STRIPE_CONFLICT : STRIPE_CONFLICT_OTHER);
            if (conflict.icon != null) {
                this.gui.drawItem(cardX + 5, cardY + 3, conflict.icon);
            }

            // The name gets the whole first line; the button shares the second line with the recipe note.
            final int textX = cardX + 24;
            final int buttonX = cardX + cardWidth - buttonWidth - 3;
            final int buttonY = cardY + CARD_HEIGHT - BUTTON_HEIGHT - 1;
            this.drawText(
                    font,
                    displayName(conflict),
                    textX,
                    cardY + 2,
                    cardX + cardWidth - 3 - textX,
                    ColorUtils.guiTextColorBlack.getColor());
            if (!this.compact) {
                // A narrow panel leaves the recipe note to the stripe colour and the tooltip.
                final String recipe = conflict.sameRecipe ? GuiText.PatternTargetsSameRecipe.getLocal()
                        : GuiText.PatternTargetsOtherRecipe.getLocal();
                this.drawText(font, recipe, textX, cardY + 13, buttonX - 2 - textX, TEXT_DIM);
            }

            final boolean buttonHover = this.isOver(mouseX, mouseY, buttonX, buttonY, buttonWidth, BUTTON_HEIGHT);
            this.drawCard(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, buttonHover);
            this.drawText(
                    font,
                    extract,
                    buttonX + 4,
                    buttonY + 2,
                    buttonWidth - 4,
                    ColorUtils.guiTextColorBlack.getColor());
        }
        if (count > CONFLICT_ROWS) {
            this.drawText(
                    font,
                    GuiText.PatternTargetsMore.getLocal(count - CONFLICT_ROWS),
                    cardX,
                    this.conflictCardY(CONFLICT_ROWS) + 1,
                    cardWidth,
                    TEXT_DIM);
        }
    }

    private int extractButtonWidth(FontRenderer font) {
        return this.textWidth(font, GuiText.PatternTargetsExtract.getLocal()) + 8;
    }

    // Raised card from the CPU table texture; hovered cards get AE2's light blue tint.
    private void drawCard(int cardX, int cardY, int cardWidth, int cardHeight, boolean hover) {
        if (hover) {
            GL11.glColor4f(0.65F, 0.9F, 1.0F, 1.0F);
        } else {
            ScreenColor.setGuiColor();
        }
        this.drawNineSlice(LIST_TEXTURE, cardX, cardY, cardWidth, cardHeight, 100, 0, 67, 23, 1);
        ScreenColor.resetGuiColor();
    }

    // The CPU table texture has no right edge (it joins the crafting window), so the frame comes from a full window.
    private void drawFrame(int frameX, int frameY, int frameWidth, int frameHeight) {
        this.gui.bindTexture(FRAME_TEXTURE);
        // The rounded corners are transparent pixels; without the alpha test they would draw black.
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        final int right = frameX + frameWidth - 4;
        final int bottom = frameY + frameHeight - 4;
        this.stretch(frameX, frameY, 4, 4, 0, 0, 4, 4);
        this.stretch(right, frameY, 4, 4, 172, 0, 4, 4);
        this.stretch(frameX, bottom, 4, 4, 0, 103, 4, 4);
        this.stretch(right, bottom, 4, 4, 172, 103, 4, 4);
        this.stretch(frameX + 4, frameY, frameWidth - 8, 4, 10, 0, 1, 4);
        this.stretch(frameX + 4, bottom, frameWidth - 8, 4, 10, 103, 1, 4);
        this.stretch(frameX, frameY + 4, 4, frameHeight - 8, 0, 10, 4, 1);
        this.stretch(right, frameY + 4, 4, frameHeight - 8, 172, 10, 4, 1);
        this.stretch(frameX + 4, frameY + 4, frameWidth - 8, frameHeight - 8, 10, 10, 1, 1);
    }

    private void drawInset(int insetLeft, int insetTop, int insetW, int insetH) {
        this.gui.bindTexture(LIST_TEXTURE);
        final int right = insetLeft + insetW - 1;
        final int bottom = insetTop + insetH - 1;
        this.stretch(insetLeft, insetTop, insetW - 1, 1, 20, 18, 1, 1);
        this.stretch(insetLeft, insetTop, 1, insetH - 1, 8, 30, 1, 1);
        this.stretch(insetLeft + 1, bottom, insetW - 1, 1, 20, 157, 1, 1);
        this.stretch(right, insetTop + 1, 1, insetH - 1, 76, 30, 1, 1);
        this.stretch(insetLeft + 1, insetTop + 1, insetW - 2, insetH - 2, 20, 30, 1, 1);
        this.stretch(right, insetTop, 1, 1, 76, 18, 1, 1);
        this.stretch(insetLeft, bottom, 1, 1, 8, 157, 1, 1);
    }

    private void drawTrack(int trackX, int trackY, int trackHeight) {
        this.gui.bindTexture(LIST_TEXTURE);
        this.stretch(trackX, trackY, TRACK_WIDTH, 1, 77, 18, TRACK_WIDTH, 1);
        this.stretch(trackX, trackY + 1, TRACK_WIDTH, trackHeight - 2, 77, 30, TRACK_WIDTH, 1);
        this.stretch(trackX, trackY + trackHeight - 1, TRACK_WIDTH, 1, 77, 157, TRACK_WIDTH, 1);
    }

    private void drawNineSlice(String texture, int drawX, int drawY, int drawW, int drawH, int u, int v, int uw,
            int vh, int edge) {
        this.gui.bindTexture(texture);
        final int innerW = drawW - 2 * edge;
        final int innerH = drawH - 2 * edge;
        final int innerU = uw - 2 * edge;
        final int innerV = vh - 2 * edge;
        final int right = drawX + drawW - edge;
        final int bottom = drawY + drawH - edge;
        this.stretch(drawX, drawY, edge, edge, u, v, edge, edge);
        this.stretch(right, drawY, edge, edge, u + uw - edge, v, edge, edge);
        this.stretch(drawX, bottom, edge, edge, u, v + vh - edge, edge, edge);
        this.stretch(right, bottom, edge, edge, u + uw - edge, v + vh - edge, edge, edge);
        this.stretch(drawX + edge, drawY, innerW, edge, u + edge, v, innerU, edge);
        this.stretch(drawX + edge, bottom, innerW, edge, u + edge, v + vh - edge, innerU, edge);
        this.stretch(drawX, drawY + edge, edge, innerH, u, v + edge, edge, innerV);
        this.stretch(right, drawY + edge, edge, innerH, u + uw - edge, v + edge, edge, innerV);
        this.stretch(drawX + edge, drawY + edge, innerW, innerH, u + edge, v + edge, innerU, innerV);
    }

    // drawTexturedModalRect copies pixels 1:1; this scales a texture region of a 256x256 sheet to any size.
    private void stretch(int drawX, int drawY, int drawW, int drawH, int u, int v, int uw, int vh) {
        if (drawW <= 0 || drawH <= 0) {
            return;
        }
        final float scale = 1.0F / 256.0F;
        final Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(drawX, drawY + drawH, 0, u * scale, (v + vh) * scale);
        tessellator.addVertexWithUV(drawX + drawW, drawY + drawH, 0, (u + uw) * scale, (v + vh) * scale);
        tessellator.addVertexWithUV(drawX + drawW, drawY, 0, (u + uw) * scale, v * scale);
        tessellator.addVertexWithUV(drawX, drawY, 0, u * scale, v * scale);
        tessellator.draw();
    }

    private PatternTargetList.Entry entryAt(int mouseX, int mouseY) {
        if (!this.isVisible()) {
            return null;
        }
        final int first = this.scrollbar.getCurrentScroll();
        for (int k = 0; k < this.visibleCards && first + k < this.entries.size(); k++) {
            if (this.isOver(mouseX, mouseY, this.insetX + 1, this.cardY(k), this.insetWidth - 2, CARD_HEIGHT)) {
                return this.entries.get(first + k);
            }
        }
        return null;
    }

    private int conflictAt(int mouseX, int mouseY) {
        for (int i = 0; i < Math.min(CONFLICT_ROWS, this.list.conflicts.size()); i++) {
            if (this.isOver(
                    mouseX,
                    mouseY,
                    this.x + PADDING,
                    this.conflictCardY(i),
                    this.width - 2 * PADDING,
                    CARD_HEIGHT)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isOverExtractButton(int conflict, int mouseX, int mouseY) {
        final int buttonWidth = this.extractButtonWidth(this.gui.getFontRenderer());
        final int buttonX = this.x + this.width - PADDING - buttonWidth - 3;
        final int buttonY = this.conflictCardY(conflict) + CARD_HEIGHT - BUTTON_HEIGHT - 1;
        return this.isOver(mouseX, mouseY, buttonX, buttonY, buttonWidth, BUTTON_HEIGHT);
    }

    private int cardY(int visibleIndex) {
        return this.listTop + 1 + visibleIndex * CARD_HEIGHT;
    }

    private int conflictCardY(int index) {
        return this.conflictTop + 4 + this.conflictTitle.size() * LINE_HEIGHT + index * CARD_HEIGHT;
    }

    private boolean isOver(int mouseX, int mouseY, int left, int top, int w, int h) {
        return mouseX >= left && mouseX < left + w && mouseY >= top && mouseY < top + h;
    }

    private static List<String> listLines(FontRenderer font, String text, int maxWidth) {
        final List<String> lines = new ArrayList<>();
        for (Object line : font.listFormattedStringToWidth(text, maxWidth)) {
            lines.add((String) line);
        }
        return lines;
    }

    private static int statusOrder(PatternMatch match) {
        return switch (match) {
            case MATCH -> 0;
            case UNKNOWN -> 1;
            case NOT_READY -> 2;
            case NO_MATCH -> 3;
        };
    }

    private static int stripeColor(PatternMatch match) {
        return switch (match) {
            case MATCH -> STRIPE_MATCH;
            case UNKNOWN -> STRIPE_UNKNOWN;
            case NOT_READY, NO_MATCH -> STRIPE_NOT_READY;
        };
    }

    private static int statusTextColor(PatternMatch match) {
        return switch (match) {
            case MATCH -> TEXT_MATCH;
            case UNKNOWN -> TEXT_UNKNOWN;
            case NOT_READY, NO_MATCH -> TEXT_DIM;
        };
    }

    private static String statusLabel(PatternMatch match) {
        return switch (match) {
            case MATCH -> GuiText.PatternTargetsMatch.getLocal();
            case UNKNOWN -> GuiText.PatternTargetsUnknown.getLocal();
            case NOT_READY, NO_MATCH -> GuiText.PatternTargetsNotReady.getLocal();
        };
    }

    private static String displayName(PatternTargetList.Entry entry) {
        return GuiInterfaceTerminal.translateRawName(entry.rawName, null, entry.icon);
    }

    private static String displayName(PatternTargetList.Conflict conflict) {
        return GuiInterfaceTerminal.translateRawName(conflict.rawName, null, conflict.icon);
    }
}
