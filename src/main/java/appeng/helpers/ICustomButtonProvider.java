package appeng.helpers;

import java.util.List;

import net.minecraft.client.gui.GuiButton;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface ICustomButtonProvider {

    @SideOnly(Side.CLIENT)
    void initCustomButtons(int guiLeft, int guiTop, int xSize, int ySize, int xOffset, int yOffset,
            List<GuiButton> buttonList);

    @SideOnly(Side.CLIENT)
    boolean actionPerformedCustomButtons(final GuiButton btn);

    /**
     * Called before the gui handles a mouse click, left clicks are reported again after the gui handled them.
     *
     * @return true if the click is consumed
     */
    @SideOnly(Side.CLIENT)
    default boolean mouseClickedCustomButtons(final int mouseX, final int mouseY, final int mouseButton) {
        final ICustomButtonDataObject dataObject = this.getDataObject();
        return dataObject != null && dataObject.mouseClickedCustomButtons(mouseX, mouseY, mouseButton);
    }

    void writeCustomButtonData();

    void readCustomButtonData();

    ICustomButtonDataObject getDataObject();

    void setDataObject(ICustomButtonDataObject dataObject);
}
