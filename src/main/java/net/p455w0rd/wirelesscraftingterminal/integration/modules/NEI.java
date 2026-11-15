package net.p455w0rd.wirelesscraftingterminal.integration.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal;
import net.p455w0rd.wirelesscraftingterminal.integration.IIntegrationModule;
import net.p455w0rd.wirelesscraftingterminal.integration.IntegrationHelper;
import net.p455w0rd.wirelesscraftingterminal.integration.abstraction.INEI;

import appeng.helpers.Reflected;
import appeng.integration.modules.NEIHelpers.NEICraftingHandler;
import appeng.integration.modules.NEIHelpers.TerminalCraftingSlotFinder;
import codechicken.nei.api.API;
import codechicken.nei.guihook.GuiContainerManager;

public class NEI implements INEI, IIntegrationModule {

    @Reflected
    public static NEI instance;

    @Reflected
    public NEI() {
        IntegrationHelper.testClassExistence(this, codechicken.nei.api.API.class);
        IntegrationHelper.testClassExistence(this, codechicken.nei.api.IStackPositioner.class);
        IntegrationHelper.testClassExistence(this, codechicken.nei.guihook.GuiContainerManager.class);
        IntegrationHelper.testClassExistence(this, codechicken.nei.PositionedStack.class);
        IntegrationHelper.testClassExistence(this, codechicken.nei.recipe.IRecipeHandler.class);
    }

    @Override
    public void init() {
        API.registerGuiOverlay(GuiWirelessCraftingTerminal.class, "crafting", new TerminalCraftingSlotFinder());
        API.registerGuiOverlayHandler(GuiWirelessCraftingTerminal.class, new NEICraftingHandler(6, 75), "crafting");
    }

    @Override
    public void postInit() {}

    @Override
    public void drawSlot(final Slot s) {
        if (s == null) {
            return;
        }

        final ItemStack stack = s.getStack();

        if (stack == null) {
            return;
        }

        final Minecraft mc = Minecraft.getMinecraft();
        final FontRenderer fontRenderer = mc.fontRenderer;
        final int x = s.xDisplayPosition;
        final int y = s.yDisplayPosition;

        GuiContainerManager.drawItems.renderItemAndEffectIntoGUI(fontRenderer, mc.getTextureManager(), stack, x, y);
        GuiContainerManager.drawItems.renderItemOverlayIntoGUI(
                fontRenderer,
                mc.getTextureManager(),
                stack,
                x,
                y,
                String.valueOf(stack.stackSize));
    }

    @Override
    public RenderItem setItemRender(final RenderItem renderItem) {
        try {
            final RenderItem ri = GuiContainerManager.drawItems;
            GuiContainerManager.drawItems = renderItem;
            return ri;
        } catch (final Throwable t) {
            throw new IllegalStateException("Invalid version of NEI, please update", t);
        }
    }
}
