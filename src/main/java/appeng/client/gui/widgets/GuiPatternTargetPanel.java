package appeng.client.gui.widgets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.StatCollector;

import appeng.api.crafting.PatternMatch;
import appeng.client.gui.AEBaseGui;
import appeng.core.localization.GuiText;
import appeng.crafting.target.PatternTargetList;

/**
 * Side panel of the Pattern Terminal that lists the interfaces for the encoded pattern. Coordinates are relative to
 * the terminal's top left corner.
 */
public class GuiPatternTargetPanel {

    public static final int WIDTH = 130;
    private static final int PADDING = 6;
    private static final int TITLE_HEIGHT = 14;
    private static final int HEADER_HEIGHT = 13;
    private static final int ENTRY_HEIGHT = 20;
    private static final int SCROLLBAR_WIDTH = 4;

    // Vanilla container frame colors, so the panel looks like part of the terminal.
    private static final int COLOR_OUTLINE = 0xFF000000;
    private static final int COLOR_LIGHT = 0xFFFFFFFF;
    private static final int COLOR_SHADOW = 0xFF555555;
    private static final int COLOR_BACKGROUND = 0xFFC6C6C6;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_DARK = 0xFF373737;
    private static final int COLOR_HOVER = 0x80FFFFFF;
    private static final int COLOR_SEPARATOR = 0xFF8B8B8B;
    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_COUNT = 0x707070;
    private static final int COLOR_MATCH = 0x2E7D32;
    private static final int COLOR_UNKNOWN = 0x8A6D00;

    private final AEBaseGui gui;
    private int x;
    private int y;
    private int height;
    private int scroll;

    private PatternTargetList list = PatternTargetList.EMPTY;
    private final List<Row> rows = new ArrayList<>();
    private final List<DrawnRow> drawnRows = new ArrayList<>();

    public GuiPatternTargetPanel(AEBaseGui gui) {
        this.gui = gui;
    }

    public void setBounds(int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;
    }

    /**
     * Shows the given list; a new list resets the scroll.
     */
    public void setList(PatternTargetList list) {
        if (list == this.list) {
            return;
        }
        this.list = list;
        this.scroll = 0;
        this.rows.clear();
        this.addGroup(PatternMatch.MATCH, GuiText.PatternTargetsMatch, COLOR_MATCH);
        this.addGroup(PatternMatch.UNKNOWN, GuiText.PatternTargetsUnknown, COLOR_UNKNOWN);
    }

    public boolean isVisible() {
        return this.list.hasPattern;
    }

    public void draw(int mouseX, int mouseY) {
        this.drawnRows.clear();
        if (!this.isVisible()) {
            return;
        }
        final FontRenderer font = this.gui.getFontRenderer();
        this.drawFrame();
        font.drawString(GuiText.PatternTargetsTitle.getLocal(), this.x + PADDING, this.y + PADDING, COLOR_TEXT);
        Gui.drawRect(
                this.x + PADDING,
                this.y + PADDING + TITLE_HEIGHT - 4,
                this.x + WIDTH - PADDING,
                this.y + PADDING + TITLE_HEIGHT - 3,
                COLOR_SEPARATOR);

        final int listTop = this.listTop();
        if (this.rows.isEmpty()) {
            int textY = listTop + 2;
            for (Object line : font
                    .listFormattedStringToWidth(GuiText.PatternTargetsNone.getLocal(), WIDTH - 2 * PADDING)) {
                font.drawString((String) line, this.x + PADDING, textY, COLOR_TEXT);
                textY += font.FONT_HEIGHT + 2;
            }
            return;
        }

        final boolean scrollable = this.maxScroll() > 0;
        final int contentRight = this.x + WIDTH - PADDING - (scrollable ? SCROLLBAR_WIDTH + 2 : 0);
        int rowY = listTop;
        for (int i = this.scroll; i < this.rows.size(); i++) {
            final Row row = this.rows.get(i);
            if (rowY + row.height() > this.listBottom()) {
                break;
            }
            if (row.entry == null) {
                font.drawString(row.text, this.x + PADDING, rowY + 3, row.color);
            } else {
                this.drawEntry(font, row, rowY, contentRight, mouseX, mouseY);
                this.drawnRows.add(new DrawnRow(row.entry, rowY));
            }
            rowY += row.height();
        }

        if (scrollable) {
            this.drawScrollbar(listTop);
        }
    }

    /**
     * Returns the interface under the mouse, or null.
     */
    public PatternTargetList.Entry getEntryAt(int mouseX, int mouseY) {
        if (!this.isVisible() || mouseX < this.x || mouseX >= this.x + WIDTH) {
            return null;
        }
        for (DrawnRow row : this.drawnRows) {
            if (mouseY >= row.y && mouseY < row.y + ENTRY_HEIGHT) {
                return row.entry;
            }
        }
        return null;
    }

    /**
     * Returns the tooltip lines for an interface.
     */
    public List<String> getTooltip(PatternTargetList.Entry entry) {
        final List<String> lines = new ArrayList<>();
        lines.add(displayName(entry));
        lines.add(GuiText.GuiDimensionalCoord.getLocal(entry.x, entry.y, entry.z, entry.dim));
        lines.add(GuiText.PatternTargetsFreeSlots.getLocal(entry.freeSlots));
        lines.add(GuiText.PatternTargetsClick.getLocal());
        return lines;
    }

    /**
     * Scrolls the list when the mouse is over the panel; returns whether it used the wheel.
     */
    public boolean mouseWheel(int mouseX, int mouseY, int wheel) {
        if (!this.contains(mouseX, mouseY)) {
            return false;
        }
        this.scroll = Math.max(0, Math.min(this.maxScroll(), this.scroll - Integer.signum(wheel)));
        return true;
    }

    public boolean contains(int mouseX, int mouseY) {
        return this.isVisible() && mouseX >= this.x
                && mouseX < this.x + WIDTH
                && mouseY >= this.y
                && mouseY < this.y + this.height;
    }

    /**
     * Returns whether a rectangle overlaps the panel, for hiding NEI slots under it.
     */
    public boolean intersects(int rx, int ry, int rw, int rh) {
        return this.isVisible() && rx < this.x + WIDTH
                && rx + rw > this.x
                && ry < this.y + this.height
                && ry + rh > this.y;
    }

    private void drawFrame() {
        final int right = this.x + WIDTH;
        final int bottom = this.y + this.height;
        Gui.drawRect(this.x, this.y, right, bottom, COLOR_OUTLINE);
        Gui.drawRect(this.x + 1, this.y + 1, right - 1, bottom - 1, COLOR_SHADOW);
        Gui.drawRect(this.x + 1, this.y + 1, right - 2, bottom - 2, COLOR_LIGHT);
        Gui.drawRect(this.x + 2, this.y + 2, right - 2, bottom - 2, COLOR_BACKGROUND);
    }

    private void drawEntry(FontRenderer font, Row row, int rowY, int contentRight, int mouseX, int mouseY) {
        final int slotX = this.x + PADDING;
        final int slotY = rowY + 1;
        if (mouseX >= this.x && mouseX < this.x + WIDTH && mouseY >= rowY && mouseY < rowY + ENTRY_HEIGHT) {
            Gui.drawRect(this.x + 2, rowY, this.x + WIDTH - 2, rowY + ENTRY_HEIGHT, COLOR_HOVER);
        }

        Gui.drawRect(slotX, slotY, slotX + 18, slotY + 18, COLOR_LIGHT);
        Gui.drawRect(slotX, slotY, slotX + 17, slotY + 17, COLOR_SLOT_DARK);
        Gui.drawRect(slotX + 1, slotY + 1, slotX + 17, slotY + 17, COLOR_SLOT);
        if (row.entry.icon != null) {
            this.gui.drawItem(slotX + 1, slotY + 1, row.entry.icon);
        }

        final String count = String.valueOf(row.entry.freeSlots);
        final int countWidth = font.getStringWidth(count);
        final int textX = slotX + 22;
        final int nameWidth = contentRight - textX - countWidth - 4;
        font.drawString(font.trimStringToWidth(row.text, nameWidth), textX, rowY + 6, COLOR_TEXT);
        font.drawString(count, contentRight - countWidth, rowY + 6, COLOR_COUNT);
    }

    private void drawScrollbar(int listTop) {
        final int trackX = this.x + WIDTH - PADDING - SCROLLBAR_WIDTH;
        final int trackHeight = this.listBottom() - listTop;
        Gui.drawRect(trackX, listTop, trackX + SCROLLBAR_WIDTH, listTop + trackHeight, COLOR_SLOT_DARK);

        final int total = this.rows.size();
        final int thumbHeight = Math.max(8, trackHeight * this.visibleCount() / total);
        final int thumbY = listTop + (trackHeight - thumbHeight) * this.scroll / Math.max(1, this.maxScroll());
        Gui.drawRect(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, COLOR_SLOT);
    }

    private int listTop() {
        return this.y + PADDING + TITLE_HEIGHT;
    }

    private int listBottom() {
        return this.y + this.height - PADDING;
    }

    private int visibleCount() {
        int used = 0;
        int count = 0;
        for (int i = this.scroll; i < this.rows.size(); i++) {
            used += this.rows.get(i).height();
            if (used > this.listBottom() - this.listTop()) {
                break;
            }
            count++;
        }
        return Math.max(1, count);
    }

    // The last scroll position that still fills the panel, found by stacking rows from the end.
    private int maxScroll() {
        final int available = this.listBottom() - this.listTop();
        int used = 0;
        for (int i = this.rows.size() - 1; i >= 0; i--) {
            used += this.rows.get(i).height();
            if (used > available) {
                return i + 1;
            }
        }
        return 0;
    }

    private void addGroup(PatternMatch match, GuiText header, int color) {
        final List<PatternTargetList.Entry> group = new ArrayList<>();
        for (PatternTargetList.Entry entry : this.list.entries) {
            if (entry.match == match) {
                group.add(entry);
            }
        }
        if (group.isEmpty()) {
            return;
        }
        group.sort(Comparator.comparing(GuiPatternTargetPanel::displayName));
        this.rows.add(new Row(header.getLocal(), null, color));
        for (PatternTargetList.Entry entry : group) {
            this.rows.add(new Row(displayName(entry), entry, COLOR_TEXT));
        }
    }

    private static String displayName(PatternTargetList.Entry entry) {
        return entry.icon != null ? entry.icon.getDisplayName() : StatCollector.translateToLocal(entry.rawName);
    }

    private static final class Row {

        private final String text;
        private final PatternTargetList.Entry entry;
        private final int color;

        private Row(String text, PatternTargetList.Entry entry, int color) {
            this.text = text;
            this.entry = entry;
            this.color = color;
        }

        private int height() {
            return this.entry == null ? HEADER_HEIGHT : ENTRY_HEIGHT;
        }
    }

    private static final class DrawnRow {

        private final PatternTargetList.Entry entry;
        private final int y;

        private DrawnRow(PatternTargetList.Entry entry, int y) {
            this.entry = entry;
            this.y = y;
        }
    }
}
