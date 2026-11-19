package appeng.client.render.previewBlocks;

import java.util.List;

import net.minecraftforge.common.util.ForgeDirection;

import appeng.parts.misc.PartToggleBus;

public class RendererToggleBus extends AbstractRendererPreview implements IRenderPreview {

    @Override
    public void renderPreview() {
        renderCommonPreview(() -> {
            renderBase(6.0, 6.0, 11.0, 10.0, 10.0, 14.0);
            renderBase(6.0, 6.0, 14.0, 10.0, 10.0, 16.0, ForgeDirection.NORTH);
        });
    }

    @Override
    public List<Class<?>> validItemClass() {
        return ViewHelper.getValidClasses(PartToggleBus.class);
    }
}
