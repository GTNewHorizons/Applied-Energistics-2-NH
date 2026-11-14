package thaumicenergistics.api.gui;

import net.minecraft.client.gui.FontRenderer;

import org.jspecify.annotations.NonNull;

/**
 * GUI that displays widgets.
 *
 * @author Nividica
 *
 */
public interface IWidgetHost {

    /**
     * Gets the font renderer for the GUI.
     *
     * @return
     */
    @NonNull
    FontRenderer getFontRenderer();

    /**
     * Return the left of the GUI.
     *
     * @return
     */
    int guiLeft();

    /**
     * Return the top of the GUI.
     *
     * @return
     */
    int guiTop();
}
