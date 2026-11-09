package appeng.client.render.keytypes;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import appeng.api.stacks.AEItemKey;
import appeng.client.api.AEKeyRenderer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ItemKeyRenderer implements AEKeyRenderer<AEItemKey> {

    private static final IIcon BUTTON_ICON = new StateButtonIcon(112, 48, "ItemIcon");

    @Override
    public void drawInGui(Minecraft minecraft, int x, int y, AEItemKey key) {
        ItemStack itemStack = key.getReadonlyStack();
        RenderItem itemRender = RenderItem.getInstance();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();

        itemRender.renderItemAndEffectIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), itemStack, x, y);

        GL11.glTranslatef(0.0f, 0.0f, 200.0f);
        itemRender.renderItemOverlayIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), itemStack, x, y);
        GL11.glTranslatef(0.0f, 0.0f, -200.0f);
    }

    @Override
    public void drawOnBlockFace(World world, AEItemKey key) {
        ItemStack itemstack = key.getReadonlyStack();

        GL11.glPushMatrix();
        GL11.glTranslatef(0, -0.04F, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        if (itemstack.isItemEnchanted() || itemstack.getItem().requiresMultipleRenderPasses()) {
            final EntityItem entityitem = new EntityItem(world, 0.0D, 0.0D, 0.0D, itemstack);

            entityitem.hoverStart = 0;
            entityitem.age = 0;
            entityitem.rotationYaw = 0;

            GL11.glTranslatef(0.0f, -0.05f, -0.25f);
            GL11.glScalef(1.0f / 1.5f, 1.0f / 1.5f, 1.0f / 1.5f);
            GL11.glScalef(1.0f, -1.0f, 0.005f);

            final Block block = Block.getBlockFromItem(itemstack.getItem());
            if (itemstack.getItemSpriteNumber() == 0 && block != null
                    && RenderBlocks.renderItemIn3d(block.getRenderType())) {
                GL11.glRotatef(25.0f, 1.0f, 0.0f, 0.0f);
                GL11.glRotatef(15.0f, 0.0f, 1.0f, 0.0f);
                GL11.glRotatef(30.0f, 0.0f, 1.0f, 0.0f);
            }

            final IItemRenderer customRenderer = MinecraftForgeClient
                    .getItemRenderer(itemstack, IItemRenderer.ItemRenderType.ENTITY);
            if (customRenderer != null && !(itemstack.getItem() instanceof ItemBlock)) {
                if (customRenderer.shouldUseRenderHelper(
                        IItemRenderer.ItemRenderType.ENTITY,
                        itemstack,
                        IItemRenderer.ItemRendererHelper.BLOCK_3D)) {
                    GL11.glTranslatef(0, -0.04F, 0);
                    GL11.glScalef(0.7f, 0.7f, 0.7f);
                    GL11.glRotatef(35, 1, 0, 0);
                    GL11.glRotatef(45, 0, 1, 0);
                    GL11.glRotatef(-90, 0, 1, 0);
                }
            } else if (itemstack.getItem() instanceof ItemBlock) {
                GL11.glTranslatef(0, -0.04F, 0);
                GL11.glScalef(1.1f, 1.1f, 1.1f);
                GL11.glRotatef(-90, 0, 1, 0);
            } else {
                GL11.glTranslatef(0, -0.14F, 0);
                GL11.glScalef(0.8f, 0.8f, 0.8f);
            }

            RenderItem.renderInFrame = true;
            RenderManager.instance.renderEntityWithPosYaw(entityitem, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
            RenderItem.renderInFrame = false;
        } else {
            GL11.glScalef(1.0f / 42.0f, 1.0f / 42.0f, 1.0f / 42.0f);
            GL11.glTranslated(-8.0, -10.2, -10.4);
            GL11.glScalef(1.0f, 1.0f, 0.005f);

            RenderItem.renderInFrame = false;
            final FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            if (!ForgeHooksClient.renderInventoryItem(
                    RenderBlocks.getInstance(),
                    Minecraft.getMinecraft().renderEngine,
                    itemstack,
                    true,
                    0,
                    0,
                    0)) {
                RenderItem.getInstance()
                        .renderItemIntoGUI(fr, Minecraft.getMinecraft().renderEngine, itemstack, 0, 0, false);
            }
        }

        GL11.glPopMatrix();
    }

    @Override
    public List<String> getTooltip(AEItemKey key) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        return key.getReadonlyStack().getTooltip(minecraft.thePlayer, minecraft.gameSettings.advancedItemTooltips);
    }

    @Override
    public ResourceLocation getButtonTexture() {
        return StateButtonIcon.texture();
    }

    @Override
    public IIcon getButtonIcon() {
        return BUTTON_ICON;
    }
}
