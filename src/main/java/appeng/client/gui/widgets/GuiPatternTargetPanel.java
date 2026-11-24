package appeng.client.gui.widgets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

import org.lwjgl.opengl.GL11;

import appeng.api.crafting.PatternMatch;
import appeng.api.util.INameSuffixIcons;
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

        void sendToTarget(PatternTargetList.Click click);

        void extractConflict(PatternTargetList.Click click);

        void highlight(int x, int y, int z, int dim, String name);
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
    private static final int SUFFIX_ICON_SIZE = 12;
    private static final int SUFFIX_ICON_STRIDE = SUFFIX_ICON_SIZE + 2;
    private static final int SUFFIX_CARD_PADDING = 3;
    private static final int CONFLICT_CARD_HEIGHT = 27;
    private static final int CONFLICT_TITLE_TOP_PADDING = 8;
    private static final int CONFLICT_ICON_INSET = 5;
    private static final String CONFLICT_MARKER = "⚠ ";
    private static final int BUTTON_HEIGHT = 11;
    private static final int BUTTON_INSET = 2;

    private static final String FRAME_TEXTURE = "guis/craftAmt.png";
    private static final String LIST_TEXTURE = "guis/cpu_selector.png";

    private final AEBaseGui gui;
    private final Actions actions;
    private final GuiScrollbar scrollbar = new GuiScrollbar();

    private PatternTargetList list = PatternTargetList.EMPTY;
    private final List<PatternTargetList.Entry> entries = new ArrayList<>();
    private final Map<PatternTargetList.Entry, List<ItemStack>> suffixIcons = new IdentityHashMap<>();
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

    private PatternTargetList layoutList;
    private int[] layoutInputs;
    private final List<int[]> layoutBlocked = new ArrayList<>();

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
        this.suffixIcons.clear();
        for (PatternTargetList.Entry entry : this.entries) {
            if (entry.suffix == null || entry.suffix.isEmpty()) continue;
            final List<ItemStack> icons = new ArrayList<>();
            try {
                collectSuffixIcons(IChatComponent.Serializer.func_150699_a(entry.suffix), icons);
                if (!icons.isEmpty()) this.suffixIcons.put(entry, icons);
            } catch (RuntimeException ignored) {
                // The card tooltip can still show the suffix as text.
            }
        }
        // Within a status, interfaces that already hold the same pattern or need a card go last.
        this.entries.sort(
                Comparator.comparingInt((PatternTargetList.Entry e) -> statusOrder(e.match))
                        .thenComparing(e -> e.hasSamePattern)
                        .thenComparingInt(e -> e.cardSource == PatternTargetList.CardSource.NONE ? 0 : 1)
                        .thenComparing(GuiPatternTargetPanel::displayName));
        this.scrollbar.setCurrentScroll(0);
    }

    private static void collectSuffixIcons(IChatComponent component, List<ItemStack> icons) {
        if (component == null) return;
        if (component instanceof INameSuffixIcons source) {
            for (ItemStack icon : source.getSuffixIcons()) {
                if (icon != null) icons.add(icon);
            }
        }
        for (IChatComponent sibling : component.getSiblings()) {
            collectSuffixIcons(sibling, icons);
        }
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
        if (!this.layoutChanged(guiLeft, guiTop, guiHeight, screenHeight, buttonsBottom, blocked)) {
            return;
        }
        this.place(guiLeft, guiTop, guiHeight, screenHeight, buttonsBottom + BUTTON_GAP, 0, blocked);
        int availableHeight = this.height;
        int conflictHeight = this.conflictHeight();
        // A tall button column, as on wireless terminals, can leave no room under it; then the panel goes beside it.
        final int minListHeight = this.maxCardWindowHeight(1) + 2;
        if (availableHeight < HEADER_HEIGHT + minListHeight + PADDING + conflictHeight) {
            this.place(guiLeft, guiTop, guiHeight, screenHeight, 0, BESIDE_BUTTONS_GAP, blocked);
            availableHeight = this.height;
            conflictHeight = this.conflictHeight();
        }
        availableHeight = Math.max(HEADER_HEIGHT + minListHeight + PADDING, availableHeight);

        this.listTop = this.y + HEADER_HEIGHT;
        final int maxListHeight = availableHeight - HEADER_HEIGHT - conflictHeight - PADDING;
        this.visibleCards = Math.max(1, Math.min(MAX_VISIBLE_CARDS, this.entries.size()));
        while (this.visibleCards > 1 && this.maxCardWindowHeight(this.visibleCards) + 2 > maxListHeight) {
            this.visibleCards--;
        }
        this.scrollable = this.entries.size() > this.visibleCards;
        this.listHeight = this.entries.isEmpty() ? EMPTY_LIST_HEIGHT : this.maxCardWindowHeight(this.visibleCards) + 2;

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

    // Remembers the inputs of the last layout and returns whether these differ, so the panel is placed only on change.
    private boolean layoutChanged(int guiLeft, int guiTop, int guiHeight, int screenHeight, int buttonsBottom,
            List<int[]> blocked) {
        final int[] inputs = { guiLeft, guiTop, guiHeight, screenHeight, buttonsBottom };
        if (this.layoutList == this.list && Arrays.equals(inputs, this.layoutInputs)
                && sameRects(blocked, this.layoutBlocked)) {
            return false;
        }
        this.layoutList = this.list;
        this.layoutInputs = inputs;
        this.layoutBlocked.clear();
        for (int[] rect : blocked) {
            this.layoutBlocked.add(rect.clone());
        }
        return true;
    }

    private static boolean sameRects(List<int[]> a, List<int[]> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!Arrays.equals(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
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
                        this.list.conflictOutput == null ? GuiText.PatternTargetsConflictTitle.getLocal()
                                : GuiText.PatternTargetsConflictOutput
                                        .getLocal(this.list.conflictOutput.getDisplayName()),
                        (int) (this.conflictTitleTextWidth(this.gui.getFontRenderer()) / this.textScale()));
        return conflictRows == 0 ? 0
                : CONFLICT_TITLE_TOP_PADDING + this.conflictTitle.size() * LINE_HEIGHT
                        + conflictRows * CONFLICT_CARD_HEIGHT
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
            final PatternTargetList.Entry entry = this.entries.get(first + k);
            final boolean hover = this
                    .isOver(mouseX, mouseY, this.insetX + 1, cardY, this.insetWidth - 2, this.cardHeight(entry));
            this.drawEntryCard(font, entry, cardY, hover);
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
            if (entry.match != PatternMatch.MATCH) {
                lines.add(statusLabel(entry.match));
            }
            lines.add(GuiText.GuiDimensionalCoord.getLocal(entry.x, entry.y, entry.z, entry.dim));
            if (entry.match == PatternMatch.NOT_READY) {
                lines.add(GuiText.HoldShiftClick_HIGHLIGHT_INTERFACE.getLocal());
                return lines;
            }
            if (entry.cardSource == PatternTargetList.CardSource.INVENTORY) {
                lines.add(GuiText.PatternTargetsCardFromInventory.getLocal());
            } else if (entry.cardSource == PatternTargetList.CardSource.NETWORK) {
                lines.add(GuiText.PatternTargetsCardFromNetwork.getLocal());
            } else {
                lines.add(GuiText.PatternTargetsFreeSlots.getLocal(entry.freeSlots));
            }
            if (entry.hasSamePattern) {
                lines.add(GuiText.PatternTargetsHasSameTooltip.getLocal());
            }
            lines.add(GuiText.PatternTargetsClick.getLocal());
            lines.add(GuiText.HoldShiftClick_HIGHLIGHT_INTERFACE.getLocal());
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
            } else {
                lines.add(GuiText.HoldShiftClick_HIGHLIGHT_INTERFACE.getLocal());
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
            if (GuiScreen.isShiftKeyDown()) {
                this.actions.highlight(entry.x, entry.y, entry.z, entry.dim, displayName(entry));
            } else if (entry.match != PatternMatch.NOT_READY) {
                this.actions.sendToTarget(new PatternTargetList.Click(this.list.revision, entry.index));
            }
            return true;
        }
        final int conflict = this.conflictAt(mouseX, mouseY);
        if (conflict >= 0) {
            final PatternTargetList.Conflict c = this.list.conflicts.get(conflict);
            if (this.isOverExtractButton(conflict, mouseX, mouseY)) {
                this.actions.extractConflict(new PatternTargetList.Click(this.list.revision, c.index));
            } else if (GuiScreen.isShiftKeyDown()) {
                this.actions.highlight(c.x, c.y, c.z, c.dim, displayName(c));
            }
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
        final int cardHeight = this.cardHeight(entry);
        this.drawCard(cardX, cardY, cardWidth, cardHeight, hover);
        Gui.drawRect(cardX + 1, cardY + 1, cardX + 3, cardY + cardHeight - 1, stripeColor(entry.match));
        if (entry.icon != null) {
            this.gui.drawItem(cardX + 5, cardY + (cardHeight - 15) / 2, entry.icon);
        }

        final int textX = cardX + 24;
        final int textWidth = cardX + cardWidth - 3 - textX;
        this.drawText(
                font,
                GuiInterfaceTerminal.translateRawName(entry.rawName, null, entry.icon),
                textX,
                cardY + 3,
                textWidth,
                ColorUtils.guiTextColorBlack.getColor());
        final int iconY = suffixIconY(cardY);
        final int statusY = this.suffixIcons.containsKey(entry)
                ? iconY + Math.round((SUFFIX_ICON_SIZE - font.FONT_HEIGHT * this.textScale()) / 2)
                : cardY + 13;
        final String room = roomLabel(entry);
        final int roomColor = entry.hasSamePattern ? ColorUtils.patternTargetTextHasSame.getColor()
                : statusTextColor(entry.match);
        final String status = this.statusPrefix(entry);
        if (!status.isEmpty()) {
            this.drawText(font, status, textX, statusY, textWidth, statusTextColor(entry.match));
        }
        int roomX = textX + this.textWidth(font, status);
        final int textRight = textX + textWidth;
        final List<ItemStack> icons = this.suffixIcons.getOrDefault(entry, Collections.emptyList());
        final int iconsWidth = textRight - roomX - this.textWidth(font, room);
        final int shown = this.visibleSuffixIcons(font, icons.size(), iconsWidth);
        for (int i = 0; i < shown; i++) {
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(roomX, iconY, 0);
                GL11.glScalef(SUFFIX_ICON_SIZE / 16F, SUFFIX_ICON_SIZE / 16F, 1);
                this.gui.drawItem(0, 0, icons.get(i));
            } finally {
                GL11.glPopMatrix();
            }
            roomX += SUFFIX_ICON_STRIDE;
        }
        if (shown < icons.size()) {
            final String more = "+" + (icons.size() - shown) + " ";
            final int moreWidth = this.textWidth(font, more);
            if (roomX + moreWidth + this.textWidth(font, room) <= textRight) {
                this.drawText(font, more, roomX, statusY, moreWidth, ColorUtils.guiTextColorGray.getColor());
                roomX += moreWidth;
            }
        }
        this.drawText(font, room, roomX, statusY, textRight - roomX, roomColor);
    }

    private static String roomLabel(PatternTargetList.Entry entry) {
        if (entry.hasSamePattern) return GuiText.PatternTargetsHasSame.getLocal();
        if (entry.cardSource != PatternTargetList.CardSource.NONE) return GuiText.PatternTargetsNeedsCard.getLocal();
        return (entry.totalSlots - entry.freeSlots) + "/" + entry.totalSlots;
    }

    private String statusPrefix(PatternTargetList.Entry entry) {
        return this.compact || entry.match == PatternMatch.MATCH ? "" : statusLabel(entry.match) + " · ";
    }

    private int visibleSuffixIcons(FontRenderer font, int count, int width) {
        int shown = Math.min(count, Math.max(0, width / SUFFIX_ICON_STRIDE));
        while (shown > 0 && shown < count
                && shown * SUFFIX_ICON_STRIDE + this.textWidth(font, "+" + (count - shown) + " ") > width) {
            shown--;
        }
        return shown;
    }

    public ItemStack hoveredSuffixIcon(int mouseX, int mouseY) {
        final PatternTargetList.Entry entry = this.entryAt(mouseX, mouseY);
        final List<ItemStack> icons = this.suffixIcons.get(entry);
        if (icons == null) return null;
        final FontRenderer font = this.gui.getFontRenderer();
        final int cardY = this.cardY(this.entries.indexOf(entry) - this.scrollbar.getCurrentScroll());
        final int x = this.insetX + 25 + this.textWidth(font, this.statusPrefix(entry));
        final int right = this.insetX + this.insetWidth - 4;
        final int shown = this
                .visibleSuffixIcons(font, icons.size(), right - x - this.textWidth(font, roomLabel(entry)));
        for (int i = 0; i < shown; i++) {
            if (this.isOver(
                    mouseX,
                    mouseY,
                    x + i * SUFFIX_ICON_STRIDE,
                    suffixIconY(cardY),
                    SUFFIX_ICON_SIZE,
                    SUFFIX_ICON_SIZE)) {
                return icons.get(i);
            }
        }
        return null;
    }

    private static int suffixIconY(int cardY) {
        return cardY + 12;
    }

    private void drawConflicts(FontRenderer font, int mouseX, int mouseY) {
        final int count = this.list.conflicts.size();
        if (count == 0) {
            return;
        }
        int lineY = this.conflictTop + CONFLICT_TITLE_TOP_PADDING;
        final int markerX = this.x + PADDING + CONFLICT_ICON_INSET;
        final int markerWidth = this.textWidth(font, CONFLICT_MARKER);
        final int titleColor = ColorUtils.patternTargetTextConflict.getColor();
        this.drawText(font, CONFLICT_MARKER, markerX, lineY, markerWidth, titleColor);
        for (String line : this.conflictTitle) {
            this.drawText(font, line, markerX + markerWidth, lineY, this.conflictTitleTextWidth(font), titleColor);
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
            this.drawNineSlice(LIST_TEXTURE, cardX, cardY, cardWidth, CONFLICT_CARD_HEIGHT, 100, 0, 67, 23, 1);
            ScreenColor.resetGuiColor();
            Gui.drawRect(
                    cardX + 1,
                    cardY + 1,
                    cardX + 3,
                    cardY + CONFLICT_CARD_HEIGHT - 1,
                    conflict.sameRecipe ? ColorUtils.patternTargetStripeConflict.getColor()
                            : ColorUtils.patternTargetStripeConflictOther.getColor());
            if (conflict.icon != null) {
                this.gui.drawItem(cardX + CONFLICT_ICON_INSET, cardY + (CONFLICT_CARD_HEIGHT - 15) / 2, conflict.icon);
            }

            final int textX = cardX + 24;
            final int buttonX = cardX + cardWidth - buttonWidth - BUTTON_INSET;
            final int buttonY = extractButtonY(cardY);
            this.drawText(
                    font,
                    GuiInterfaceTerminal.translateRawName(conflict.rawName, null, conflict.icon),
                    textX,
                    cardY + 3,
                    cardX + cardWidth - 3 - textX,
                    ColorUtils.guiTextColorBlack.getColor());

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
                    ColorUtils.patternTargetTextDim.getColor());
        }
    }

    private int conflictTitleTextWidth(FontRenderer font) {
        return this.width - 2 * PADDING - CONFLICT_ICON_INSET - this.textWidth(font, CONFLICT_MARKER);
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

    private void drawNineSlice(String texture, int drawX, int drawY, int drawW, int drawH, int u, int v, int uw, int vh,
            int edge) {
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
            final PatternTargetList.Entry entry = this.entries.get(first + k);
            if (this.isOver(
                    mouseX,
                    mouseY,
                    this.insetX + 1,
                    this.cardY(k),
                    this.insetWidth - 2,
                    this.cardHeight(entry))) {
                return entry;
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
                    CONFLICT_CARD_HEIGHT)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isOverExtractButton(int conflict, int mouseX, int mouseY) {
        final int buttonWidth = this.extractButtonWidth(this.gui.getFontRenderer());
        final int buttonX = this.x + this.width - PADDING - buttonWidth - BUTTON_INSET;
        final int buttonY = extractButtonY(this.conflictCardY(conflict));
        return this.isOver(mouseX, mouseY, buttonX, buttonY, buttonWidth, BUTTON_HEIGHT);
    }

    private static int extractButtonY(int cardY) {
        return cardY + CONFLICT_CARD_HEIGHT - BUTTON_HEIGHT - BUTTON_INSET;
    }

    private int cardY(int visibleIndex) {
        final int first = this.scrollbar.getCurrentScroll();
        int top = this.listTop + 1;
        for (int i = first; i < first + visibleIndex; i++) {
            top += this.cardHeight(this.entries.get(i));
        }
        return top;
    }

    private int cardHeight(PatternTargetList.Entry entry) {
        return CARD_HEIGHT + (this.suffixIcons.containsKey(entry) ? SUFFIX_CARD_PADDING : 0);
    }

    private int maxCardWindowHeight(int count) {
        int tallest = CARD_HEIGHT;
        int height = 0;
        for (int i = 0; i < this.entries.size(); i++) {
            height += this.cardHeight(this.entries.get(i));
            if (i >= count) height -= this.cardHeight(this.entries.get(i - count));
            tallest = Math.max(tallest, height);
        }
        return tallest;
    }

    private int conflictCardY(int index) {
        return this.conflictTop + CONFLICT_TITLE_TOP_PADDING
                + this.conflictTitle.size() * LINE_HEIGHT
                + index * CONFLICT_CARD_HEIGHT;
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
            case MATCH -> ColorUtils.patternTargetStripeMatch.getColor();
            case UNKNOWN -> ColorUtils.patternTargetStripeUnknown.getColor();
            case NOT_READY, NO_MATCH -> ColorUtils.patternTargetStripeNotReady.getColor();
        };
    }

    private static int statusTextColor(PatternMatch match) {
        return switch (match) {
            case MATCH -> ColorUtils.patternTargetTextMatch.getColor();
            case UNKNOWN -> ColorUtils.patternTargetTextUnknown.getColor();
            case NOT_READY, NO_MATCH -> ColorUtils.patternTargetTextDim.getColor();
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
        return GuiInterfaceTerminal.translateRawName(entry.rawName, entry.suffix, entry.icon);
    }

    private static String displayName(PatternTargetList.Conflict conflict) {
        return GuiInterfaceTerminal.translateRawName(conflict.rawName, conflict.suffix, conflict.icon);
    }
}
