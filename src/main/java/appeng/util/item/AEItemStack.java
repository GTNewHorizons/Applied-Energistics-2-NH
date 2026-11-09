/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.util.item;

import static appeng.util.item.AEItemStackType.ITEM_STACK_TYPE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.github.bsideup.jabel.Desugar;

import appeng.api.config.FuzzyMode;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IAETagCompound;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public final class AEItemStack extends AEStack<IAEItemStack> implements IAEItemStack, Comparable<AEItemStack> {

    /** Sort position of the bounds for {@link ItemList}'s range searches. */
    private static final AESharedNBT LOW_TAG = new AESharedNBT(Integer.MIN_VALUE);
    private static final AESharedNBT HIGH_TAG = new AESharedNBT(Integer.MAX_VALUE);

    private AEItemKey key;
    /**
     * Only set on the bounds that {@link #getLow} and {@link #getHigh} create for the range searches of
     * {@link ItemList}. It replaces the damage, display damage and tag of the key in {@link #compareTo}.
     */
    private @Nullable Bound bound;

    private AEItemStack(final AEItemStack is) {
        this.key = is.key;
        this.setStackSize(is.getStackSize());
        this.setCraftable(is.isCraftable());
        this.setCountRequestable(is.getCountRequestable());
        this.setCountRequestableCrafts(is.getCountRequestableCrafts());
        this.setUsedPercent(is.getUsedPercent());
    }

    private AEItemStack(final ItemStack is) {
        if (is == null) {
            throw new InvalidParameterException("null is not a valid ItemStack for AEItemStack.");
        }

        if (is.getItem() == null) {
            throw new InvalidParameterException("Contained item is null, thus not a valid ItemStack for AEItemStack.");
        }

        this.key = AEItemKey.of(is);

        this.setStackSize(is.stackSize);
        this.setCraftable(false);
        this.setCountRequestable(0);
        this.setCountRequestableCrafts(0);
        this.setUsedPercent(0);
    }

    public static IAEItemStack loadItemStackFromNBT(final NBTTagCompound i) {
        if (i == null) {
            return null;
        }

        final ItemStack itemstack = ItemStack.loadItemStackFromNBT(i);
        if (itemstack == null) {
            return null;
        }

        final AEItemStack item = AEItemStack.create(itemstack);
        // item.priority = i.getInteger( "Priority" );
        item.setStackSize(i.getLong("Cnt"));
        item.setCountRequestable(i.getLong("Req"));
        item.setCraftable(i.getBoolean("Craft"));
        item.setCountRequestableCrafts(i.getLong("ReqMade"));
        item.setUsedPercent(i.getFloat("UsedPercent"));
        return item;
    }

    @Nullable
    public static AEItemStack create(final ItemStack stack) {
        if (stack == null) {
            return null;
        }

        return new AEItemStack(stack);
    }

    public static IAEItemStack loadItemStackFromPacket(final ByteBuf data) throws IOException {
        final byte mask = data.readByte();
        // byte PriorityType = (byte) (mask & 0x03);
        final byte stackType = (byte) ((mask & 0x0C) >> 2);
        final byte countReqType = (byte) ((mask & 0x30) >> 4);
        final boolean isCraftable = (mask & 0x40) > 0;
        final boolean hasTagCompound = (mask & 0x80) > 0;

        // don't send this...
        final NBTTagCompound d = new NBTTagCompound();

        d.setShort("id", data.readShort());
        d.setShort("Damage", data.readShort());
        d.setByte("Count", (byte) 0);

        if (hasTagCompound) {
            final int len = data.readInt();

            final byte[] bd = new byte[len];
            data.readBytes(bd);

            final ByteArrayInputStream di = new ByteArrayInputStream(bd);
            d.setTag("tag", CompressedStreamTools.read(new DataInputStream(di)));
        }

        // long priority = getPacketValue( PriorityType, data );
        final long stackSize = getPacketValue(stackType, data);
        final long countRequestable = getPacketValue(countReqType, data);

        final byte mask2 = data.readByte();
        final byte countReqMadeType = (byte) ((mask2 & 0x3));
        final byte usedPercentType = (byte) ((mask2 & 0xC) >> 2);
        final long countRequestableCrafts = getPacketValue(countReqMadeType, data);
        final long longUsedPercent = getPacketValue(usedPercentType, data);

        final ItemStack itemstack = ItemStack.loadItemStackFromNBT(d);
        if (itemstack == null) {
            return null;
        }

        final AEItemStack item = AEItemStack.create(itemstack);
        // item.priority = (int) priority;
        item.setStackSize(stackSize);
        item.setCountRequestable(countRequestable);
        item.setCraftable(isCraftable);
        item.setCountRequestableCrafts(countRequestableCrafts);
        item.setUsedPercent(longUsedPercent / 10000f);
        return item;
    }

    @Override
    public void add(final IAEItemStack option) {
        if (option == null) {
            return;
        }

        // if ( priority < ((AEItemStack) option).priority )
        // priority = ((AEItemStack) option).priority;

        this.incStackSize(option.getStackSize());
        this.setCountRequestable(this.getCountRequestable() + option.getCountRequestable());
        this.setCraftable(this.isCraftable() || option.isCraftable());
        this.setCountRequestableCrafts(this.getCountRequestableCrafts() + option.getCountRequestableCrafts());
        this.setUsedPercent(this.getUsedPercent() + option.getUsedPercent());
    }

    @Override
    public void writeToNBT(final NBTTagCompound i) {
        /*
         * Mojang Fucked this over ; GC Optimization - Ugly Yes, but it saves a lot in the memory department.
         */

        /*
         * NBTBase id = i.getTag( "id" ); NBTBase Count = i.getTag( "Count" ); NBTBase Cnt = i.getTag( "Cnt" ); NBTBase
         * Req = i.getTag( "Req" ); NBTBase Craft = i.getTag( "Craft" ); NBTBase Damage = i.getTag( "Damage" );
         */

        /*
         * if ( id != null && id instanceof NBTTagShort ) ((NBTTagShort) id).data = (short) this.def.item.itemID; else
         */
        i.setShort("id", (short) Item.itemRegistry.getIDForObject(this.getItem()));

        /*
         * if ( Count != null && Count instanceof NBTTagByte ) ((NBTTagByte) Count).data = (byte) 0; else
         */
        i.setByte("Count", (byte) 0);

        /*
         * if ( Cnt != null && Cnt instanceof NBTTagLong ) ((NBTTagLong) Cnt).data = this.stackSize; else
         */
        i.setLong("Cnt", this.getStackSize());

        /*
         * if ( Req != null && Req instanceof NBTTagLong ) ((NBTTagLong) Req).data = this.stackSize; else
         */
        i.setLong("Req", this.getCountRequestable());

        /*
         * if ( Craft != null && Craft instanceof NBTTagByte ) ((NBTTagByte) Craft).data = (byte) (this.isCraftable() ?
         * 1 : 0); else
         */
        i.setBoolean("Craft", this.isCraftable());

        /*
         * if ( Damage != null && Damage instanceof NBTTagShort ) ((NBTTagShort) Damage).data = (short)
         * this.def.damageValue; else
         */
        i.setShort("Damage", (short) this.getItemDamage());

        if (this.key.getSharedTag() != null) {
            i.setTag("tag", this.key.getSharedTag());
        } else {
            i.removeTag("tag");
        }

        // Don't break any existing drive swapping automation in the world
        if (this.getCountRequestableCrafts() != 0L) i.setLong("ReqMade", this.getCountRequestableCrafts());

        if (this.getUsedPercent() != 0) i.setFloat("UsedPercent", this.getUsedPercent());
    }

    @Override
    public boolean fuzzyComparison(final Object st, final FuzzyMode mode) {
        if (st instanceof IAEItemStack) {
            final IAEItemStack o = (IAEItemStack) st;

            if (this.sameOre(o)) {
                return true;
            }

            if (o.getItem() == this.getItem()) {
                if (this.getItem().isDamageable()) {
                    final ItemStack a = this.getItemStack();
                    final ItemStack b = o.getItemStack();

                    try {
                        if (mode == FuzzyMode.IGNORE_ALL) {
                            return true;
                        } else if (mode == FuzzyMode.PERCENT_99) {
                            return (a.getItemDamageForDisplay() > 1) == (b.getItemDamageForDisplay() > 1);
                        } else {
                            final float percentDamageOfA = (float) a.getItemDamageForDisplay()
                                    / (float) a.getMaxDamage();
                            final float percentDamageOfB = (float) b.getItemDamageForDisplay()
                                    / (float) b.getMaxDamage();

                            return (percentDamageOfA > mode.breakPoint) == (percentDamageOfB > mode.breakPoint);
                        }
                    } catch (final Throwable e) {
                        if (mode == FuzzyMode.IGNORE_ALL) {
                            return true;
                        } else if (mode == FuzzyMode.PERCENT_99) {
                            return (a.getItemDamage() > 1) == (b.getItemDamage() > 1);
                        } else {
                            final float percentDamageOfA = (float) a.getItemDamage() / (float) a.getMaxDamage();
                            final float percentDamageOfB = (float) b.getItemDamage() / (float) b.getMaxDamage();

                            return (percentDamageOfA > mode.breakPoint) == (percentDamageOfB > mode.breakPoint);
                        }
                    }
                }

                return this.getItemDamage() == o.getItemDamage();
            }
        }

        if (st instanceof ItemStack) {
            final ItemStack o = (ItemStack) st;

            OreHelper.INSTANCE.sameOre(this, o);

            if (o.getItem() == this.getItem()) {
                if (this.getItem().isDamageable()) {
                    final ItemStack a = this.getItemStack();

                    try {
                        if (mode == FuzzyMode.IGNORE_ALL) {
                            return true;
                        } else if (mode == FuzzyMode.PERCENT_99) {
                            return (a.getItemDamageForDisplay() > 1) == (o.getItemDamageForDisplay() > 1);
                        } else {
                            final float percentDamageOfA = (float) a.getItemDamageForDisplay()
                                    / (float) a.getMaxDamage();
                            final float percentDamageOfB = (float) o.getItemDamageForDisplay()
                                    / (float) o.getMaxDamage();

                            return (percentDamageOfA > mode.breakPoint) == (percentDamageOfB > mode.breakPoint);
                        }
                    } catch (final Throwable e) {
                        if (mode == FuzzyMode.IGNORE_ALL) {
                            return true;
                        } else if (mode == FuzzyMode.PERCENT_99) {
                            return (a.getItemDamage() > 1) == (o.getItemDamage() > 1);
                        } else {
                            final float percentDamageOfA = (float) a.getItemDamage() / (float) a.getMaxDamage();
                            final float percentDamageOfB = (float) o.getItemDamage() / (float) o.getMaxDamage();

                            return (percentDamageOfA > mode.breakPoint) == (percentDamageOfB > mode.breakPoint);
                        }
                    }
                }

                return this.getItemDamage() == o.getItemDamage();
            }
        }

        return false;
    }

    @Override
    public IAEItemStack copy() {
        return new AEItemStack(this);
    }

    @Override
    public IAEItemStack empty() {
        final IAEItemStack dup = this.copy();
        dup.reset();
        return dup;
    }

    @Override
    public IAETagCompound getTagCompound() {
        return this.key.getSharedTag();
    }

    @Override
    public boolean isItem() {
        return true;
    }

    @Override
    public boolean isFluid() {
        return false;
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    @Override
    public String getLocalizedName() {
        return this.getDisplayName();
    }

    @Override
    public ItemStack getItemStack() {
        return this.key.toStack(this.getStackSize());
    }

    @Override
    public Item getItem() {
        return this.key.getItem();
    }

    @Override
    public int getItemDamage() {
        return this.key.getItemDamage();
    }

    @Override
    public boolean sameOre(final IAEItemStack is) {
        return OreHelper.INSTANCE.sameOre(this, is);
    }

    @Override
    public boolean isSameType(final IAEItemStack otherStack) {
        if (otherStack == null) {
            return false;
        }

        return this.key.equals(((AEItemStack) otherStack).key);
    }

    @Override
    public boolean isSameType(final Object otherStack) {
        if (otherStack instanceof AEItemStack ais) return isSameType(ais);
        else if (otherStack instanceof ItemStack is) return isSameType(is);

        return false;
    }

    @Override
    public boolean isSameType(final ItemStack otherStack) {
        if (otherStack == null) {
            return false;
        }

        return this.key.matches(otherStack);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public boolean equals(final Object ia) {
        if (ia instanceof AEItemStack) {
            return ((AEItemStack) ia).key.equals(this.key);
        } else if (ia instanceof ItemStack is) {

            if (is.getItem() == this.getItem() && is.getItemDamage() == this.getItemDamage()) {
                final NBTTagCompound ta = this.key.getSharedTag();
                final NBTTagCompound tb = is.getTagCompound();
                if (ta == tb) {
                    return true;
                }

                if ((ta == null && tb == null) || (ta != null && ta.hasNoTags() && tb == null)
                        || (tb != null && tb.hasNoTags() && ta == null)
                        || (ta != null && ta.hasNoTags() && tb != null && tb.hasNoTags())) {
                    return true;
                }

                if ((ta == null && tb != null) || (ta != null && tb == null)) {
                    return false;
                }

                if (AESharedNBT.isShared(tb)) {
                    return ta == tb;
                }

                return Platform.NBTEqualityTest(ta, tb);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return this.getItemStack().toString();
    }

    @Override
    public int compareTo(final AEItemStack b) {
        final int id = Item.getIdFromItem(this.getItem()) - Item.getIdFromItem(b.getItem());
        if (id != 0) {
            return id;
        }

        final int damageValue = Integer.compare(this.getSortDamage(), b.getSortDamage());
        if (damageValue != 0) {
            return damageValue;
        }

        final int displayDamage = Integer.compare(this.getSortDisplayDamage(), b.getSortDisplayDamage());
        if (displayDamage != 0) {
            return displayDamage;
        }

        final AESharedNBT tag = this.getSortTag();
        final AESharedNBT otherTag = b.getSortTag();
        if (tag == otherTag) {
            return 0;
        }

        final int nbt = Integer.compare(tag == null ? 0 : tag.getHash(), otherTag == null ? 0 : otherTag.getHash());
        if (nbt != 0) {
            return nbt;
        }
        return Integer.compare(System.identityHashCode(tag), System.identityHashCode(otherTag));
    }

    private int getSortDamage() {
        return this.bound != null ? this.bound.damage() : this.getItemDamage();
    }

    private int getSortDisplayDamage() {
        return this.bound != null ? this.bound.displayDamage() : this.key.getFuzzySearchValue();
    }

    private @Nullable AESharedNBT getSortTag() {
        return this.bound != null ? this.bound.tag() : this.key.getSharedTag();
    }

    @SideOnly(Side.CLIENT)
    public List<String> getToolTip() {
        return Platform.getTooltip(this.getItemStack());
    }

    @Override
    public String getDisplayName() {
        return Platform.getItemDisplayName(this.getItemStack());
    }

    @Override
    public String getUnlocalizedName() {
        return getItem().getUnlocalizedName();
    }

    @Override
    public IChatComponent getChatComponent() {
        return this.getItemStack().func_151000_E();
    }

    // addon...
    public String getModID() {
        return this.getModId();
    }

    @Override
    public String getModId() {
        return this.key.getModId();
    }

    IAEItemStack getLow(final FuzzyMode fuzzy, final boolean ignoreMeta) {
        if (ignoreMeta) {
            return this.withBound(0, 0, this.key.getSharedTag());
        }

        int damage = this.getItemDamage();
        int displayDamage = this.key.getFuzzySearchValue();
        if (this.getItem().isDamageable()) {
            if (fuzzy == FuzzyMode.IGNORE_ALL) {
                displayDamage = 0;
            } else if (fuzzy == FuzzyMode.PERCENT_99) {
                displayDamage = this.getItemDamage() == 0 ? 0 : 1;
            } else {
                final int breakpoint = fuzzy.calculateBreakPoint(this.key.getFuzzySearchMaxValue());
                displayDamage = breakpoint <= this.key.getFuzzySearchValue() ? breakpoint : 0;
            }

            damage = displayDamage;
        }

        return this.withBound(damage, displayDamage, LOW_TAG);
    }

    IAEItemStack getHigh(final FuzzyMode fuzzy, final boolean ignoreMeta) {
        if (ignoreMeta) {
            return this.withBound(Integer.MAX_VALUE, Integer.MAX_VALUE, this.key.getSharedTag());
        }

        int damage = this.getItemDamage();
        int displayDamage = this.key.getFuzzySearchValue();
        if (this.getItem().isDamageable()) {
            final int maxDamage = this.key.getFuzzySearchMaxValue();
            if (fuzzy == FuzzyMode.IGNORE_ALL) {
                displayDamage = maxDamage + 1;
            } else if (fuzzy == FuzzyMode.PERCENT_99) {
                displayDamage = this.getItemDamage() == 0 ? 0 : maxDamage + 1;
            } else {
                final int breakpoint = fuzzy.calculateBreakPoint(maxDamage);
                displayDamage = this.key.getFuzzySearchValue() < breakpoint ? breakpoint - 1 : maxDamage + 1;
            }

            damage = displayDamage;
        }

        return this.withBound(damage, displayDamage, HIGH_TAG);
    }

    private AEItemStack withBound(final int damage, final int displayDamage, final @Nullable AESharedNBT tag) {
        final AEItemStack result = new AEItemStack(this);
        result.bound = new Bound(damage, displayDamage, tag);
        return result;
    }

    @Desugar
    private record Bound(int damage, int displayDamage, @Nullable AESharedNBT tag) {}

    public boolean isOre() {
        return this.getOreReference() != null;
    }

    @Nullable
    OreReference getOreReference() {
        return OreHelper.INSTANCE.isOre(this.getItemStack());
    }

    @Override
    protected void writeIdentity(final ByteBuf i) throws IOException {
        i.writeShort(Item.itemRegistry.getIDForObject(this.getItem()));
        i.writeShort(this.getItemDamage());
    }

    @Override
    protected void readNBT(final ByteBuf i) throws IOException {
        if (this.hasTagCompound()) {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final DataOutputStream data = new DataOutputStream(bytes);

            CompressedStreamTools.write((NBTTagCompound) this.getTagCompound(), data);

            final byte[] tagBytes = bytes.toByteArray();
            final int size = tagBytes.length;

            i.writeInt(size);
            i.writeBytes(tagBytes);
        }
    }

    @Override
    public boolean hasTagCompound() {
        return this.key.hasTag();
    }

    @Override
    public void setTagCompound(NBTTagCompound tagCompound) {
        if (tagCompound != null) {
            this.key = AEItemKey.of(this.getItem(), this.getItemDamage(), tagCompound);
        }
    }

    @Override
    public ItemStack getItemStackForNEI() {
        return this.getItemStack();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawInGui(Minecraft mc, int x, int y) {
        ItemStack itemStack = this.getItemStack();
        RenderItem itemRender = RenderItem.getInstance();

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();

        itemRender.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), itemStack, x, y);

        GL11.glTranslatef(0.0f, 0.0f, 200.0f);

        int stackSize = itemStack.stackSize;
        itemStack.stackSize = 1;
        itemRender.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), itemStack, x, y);
        itemStack.stackSize = stackSize;

        GL11.glTranslatef(0.0f, 0.0f, -200.0f);
    }

    @Override
    public void drawOnBlockFace(World world) {
        ItemStack itemstack = this.getItemStack();
        itemstack.stackSize = 1;

        final EntityItem entityitem = new EntityItem(world, 0.0D, 0.0D, 0.0D, itemstack);
        entityitem.getEntityItem().stackSize = 1;

        // set all this stuff and then do shit? meh?
        entityitem.hoverStart = 0;
        entityitem.age = 0;
        entityitem.rotationYaw = 0;

        GL11.glPushMatrix();
        GL11.glTranslatef(0, -0.04F, 0);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        if (itemstack.isItemEnchanted() || itemstack.getItem().requiresMultipleRenderPasses()) {
            GL11.glTranslatef(0.0f, -0.05f, -0.25f);
            GL11.glScalef(1.0f / 1.5f, 1.0f / 1.5f, 1.0f / 1.5f);
            GL11.glScalef(1.0f, -1.0f, 0.005f);

            final Block block = Block.getBlockFromItem(itemstack.getItem());
            if ((itemstack.getItemSpriteNumber() == 0 && block != null
                    && RenderBlocks.renderItemIn3d(block.getRenderType()))) {
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
    public int getAmountPerUnit() {
        return 1;
    }

    @Override
    public IAEStackType<IAEItemStack> getStackType() {
        return ITEM_STACK_TYPE;
    }

    @NonNull
    @Override
    public AEItemKey getKey() {
        return this.key;
    }
}
