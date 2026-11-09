package appeng.helpers;

import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public interface ICustomButtonDataObject {

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
        return false;
    }

    void readData(NBTTagCompound tag);

    void writeData(NBTTagCompound tag);

    void writeByte(ByteBuf buf);

    void readByte(ByteBuf buf);
}
