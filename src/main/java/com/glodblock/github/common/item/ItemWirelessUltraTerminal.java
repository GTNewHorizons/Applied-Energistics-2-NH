package com.glodblock.github.common.item;

import static appeng.util.Platform.baublesSlotsOffset;
import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.p455w0rd.wirelesscraftingterminal.api.IWirelessCraftingTerminalItem;
import net.p455w0rd.wirelesscraftingterminal.items.ItemInfinityBooster;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jspecify.annotations.Nullable;

import com.glodblock.github.FluidCraft;
import com.glodblock.github.client.UltraTerminalButtons;
import com.glodblock.github.common.tabs.FluidCraftingTabs;
import com.glodblock.github.common.terminal.TerminalIds;
import com.glodblock.github.common.terminal.UltraTerminalData;
import com.glodblock.github.inventory.item.WirelessMagnet;
import com.glodblock.github.inventory.item.WirelessMagnetCardFilterInventory;
import com.glodblock.github.loader.IRegister;
import com.glodblock.github.util.NameConst;
import com.glodblock.github.util.Util;

import appeng.api.AEApi;
import appeng.api.features.IUltraTerminalRegistry;
import appeng.api.features.IUltraTerminalType;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.networking.IGridNode;
import appeng.core.features.AEFeature;
import appeng.helpers.ICustomButtonDataObject;
import appeng.helpers.ICustomButtonProvider;
import appeng.helpers.ICustomButtonSource;
import appeng.util.Platform;
import baubles.api.BaublesApi;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Wireless terminal that can open the terminal types it has absorbed. A new terminal only contains the types of the
 * terminals it was crafted from, combining it with another terminal adds that terminal.
 */
public class ItemWirelessUltraTerminal extends ItemBaseWirelessTerminal
        implements IRegister<ItemWirelessUltraTerminal>, IGuiItem, ICustomButtonSource, IWirelessCraftingTerminalItem {

    public final static String TYPES = UltraTerminalData.TYPES;

    public ItemWirelessUltraTerminal() {
        AEApi.instance().registries().wireless().registerWirelessHandler(this);
        this.setFeature(EnumSet.of(AEFeature.WirelessAccessTerminal, AEFeature.PoweredTools));
        setUnlocalizedName(NameConst.ITEM_WIRELESS_ULTRA_TERMINAL);
        setTextureName(FluidCraft.resource(NameConst.ITEM_WIRELESS_ULTRA_TERMINAL).toString());
    }

    @Override
    public ItemWirelessUltraTerminal register() {
        GameRegistry.registerItem(this, NameConst.ITEM_WIRELESS_ULTRA_TERMINAL, FluidCraft.MODID);
        setCreativeTab(FluidCraftingTabs.INSTANCE);
        return this;
    }

    public static boolean isUltraTerminal(final @Nullable ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemWirelessUltraTerminal;
    }

    @Override
    public boolean isWirelessCraftingEnabled(final ItemStack terminal) {
        return isAbsorbed(terminal, TerminalIds.WCT_CRAFTING);
    }

    @Override
    public boolean checkForBooster(final ItemStack terminal) {
        if (!Reference.WCT_BOOSTER_ENABLED || !terminal.hasTagCompound()) {
            return false;
        }
        final NBTTagList slots = terminal.getTagCompound().getTagList("BoosterSlot", Constants.NBT.TAG_COMPOUND);
        final ItemStack booster = ItemStack.loadItemStackFromNBT(slots.getCompoundTagAt(0));
        return booster != null && booster.getItem() instanceof ItemInfinityBooster;
    }

    @Override
    public boolean hasInfinityRange(final ItemStack terminal) {
        return super.hasInfinityRange(terminal) || checkForBooster(terminal);
    }

    /**
     * @return the terminal types of this item in registration order. Legacy items contain all five original modes;
     *         items without any terminal data contain every registered one.
     */
    public static List<IUltraTerminalType> getTypes(final ItemStack is) {
        final Set<String> ids = absorbedIds(is);
        final List<IUltraTerminalType> types = new ArrayList<>(ids.size());
        for (final IUltraTerminalType type : AEApi.instance().registries().ultraTerminal().getTypes()) {
            if (isAlwaysAbsorbed(type) || ids.contains(type.getId())) {
                types.add(type);
            }
        }
        return types;
    }

    public static boolean isAbsorbed(final ItemStack is, final @Nullable String id) {
        if (id == null) {
            return false;
        }
        final IUltraTerminalType type = AEApi.instance().registries().ultraTerminal().getById(id);
        return type != null && (isAlwaysAbsorbed(type) || absorbedIds(is).contains(type.getId()));
    }

    /**
     * @return true for terminal types without an ingredient, they are part of every ultra terminal.
     */
    private static boolean isAlwaysAbsorbed(final IUltraTerminalType type) {
        return type.getIngredient() == null;
    }

    /**
     * Adds the terminals of the given stacks to the ultra terminal and keeps the stored mode valid.
     *
     * @param keepAbsorbed true to keep the terminals the item already contains, false to start from an empty list
     */
    public static void absorb(final ItemStack ultraTerminal, final boolean keepAbsorbed, final ItemStack... terminals) {
        final IUltraTerminalRegistry registry = AEApi.instance().registries().ultraTerminal();
        final Set<String> ids = new LinkedHashSet<>();
        if (keepAbsorbed) {
            addAbsorbed(ids, absorbedIds(ultraTerminal));
        }
        for (final ItemStack terminal : terminals) {
            if (terminal == null) {
                continue;
            }
            if (terminal.getItem() instanceof ItemWirelessUltraTerminal) {
                addAbsorbed(ids, absorbedIds(terminal));
            }
            for (final IUltraTerminalType type : registry.getTypes()) {
                if (!isAlwaysAbsorbed(type) && type.isBackedBy(terminal)) {
                    ids.add(type.getId());
                }
            }
        }
        final NBTTagCompound tag = Platform.openNbtData(ultraTerminal);
        UltraTerminalData.writeTypes(tag, ids);
        normalizeStoredMode(ultraTerminal);
        final IUltraTerminalType mode = readStoredMode(ultraTerminal);
        if (mode == null || !isAbsorbed(ultraTerminal, mode.getId())) {
            for (final IUltraTerminalType type : getTypes(ultraTerminal)) {
                UltraTerminalData.writeMode(tag, type.getId());
                break;
            }
        }
    }

    /**
     * Adds the given terminal ids, the ones that are part of every ultra terminal are skipped.
     */
    private static void addAbsorbed(final Set<String> ids, final Collection<String> source) {
        final IUltraTerminalRegistry registry = AEApi.instance().registries().ultraTerminal();
        for (final String id : source) {
            final IUltraTerminalType type = registry.getById(id);
            if (type == null || !isAlwaysAbsorbed(type)) {
                ids.add(type == null ? id : type.getId());
            }
        }
    }

    /**
     * @return the terminal that is opened, or the first absorbed terminal if the stored one is not absorbed anymore.
     */
    @Nullable
    public static IUltraTerminalType getCurrentType(final ItemStack is) {
        final List<IUltraTerminalType> types = getTypes(is);
        final IUltraTerminalType mode = readStoredMode(is);
        if (mode != null && types.contains(mode)) {
            return mode;
        }
        return types.isEmpty() ? null : types.get(0);
    }

    public static String getModeId(final ItemStack is) {
        final IUltraTerminalType type = getCurrentType(is);
        return type == null ? "" : type.getId();
    }

    public static void setModeId(final ItemStack is, final @Nullable String id) {
        if (is == null || !isAbsorbed(is, id)) {
            return;
        }
        final IUltraTerminalType type = AEApi.instance().registries().ultraTerminal().getById(id);
        if (type == null) {
            return;
        }
        final NBTTagCompound tag = Platform.openNbtData(is);
        if (!tag.hasKey(TYPES, Constants.NBT.TAG_LIST)) {
            // pin the type list first, the item may only have data for its current mode
            final Set<String> ids = new LinkedHashSet<>();
            addAbsorbed(ids, absorbedIds(is));
            UltraTerminalData.writeTypes(tag, ids);
        }
        UltraTerminalData.writeMode(tag, type.getId());
    }

    /**
     * Converts a mode stored by an older implementation to the id based format.
     */
    private static void normalizeStoredMode(final ItemStack is) {
        final NBTTagCompound tag = is == null ? null : is.getTagCompound();
        if (tag != null) {
            UltraTerminalData.normalizeMode(tag, AEApi.instance().registries().ultraTerminal());
        }
    }

    @Nullable
    private static IUltraTerminalType readStoredMode(final ItemStack is) {
        return UltraTerminalData
                .readMode(is == null ? null : is.getTagCompound(), AEApi.instance().registries().ultraTerminal());
    }

    /**
     * @return the id of the next usable absorbed terminal, wrapping around.
     */
    public static String getNextModeId(final EntityPlayer player, final ItemStack is) {
        final List<IUltraTerminalType> types = getTypes(is);
        if (types.isEmpty()) {
            return getModeId(is);
        }
        final IUltraTerminalType current = getCurrentType(is);
        final int start = current == null ? 0 : types.indexOf(current);
        for (int i = 1; i <= types.size(); i++) {
            final IUltraTerminalType candidate = types.get(Math.floorMod(start + i, types.size()));
            if (candidate == current) {
                continue;
            }
            if (candidate.isAvailable(player, is)) {
                return candidate.getId();
            }
        }
        return getModeId(is);
    }

    /**
     * @return the requested terminal if it is absorbed and usable, otherwise the first usable absorbed terminal.
     */
    @Nullable
    public static IUltraTerminalType resolveAvailableType(final EntityPlayer player, final ItemStack terminal,
            final @Nullable IUltraTerminalType requested) {
        if (requested != null && isAbsorbed(terminal, requested.getId()) && requested.isAvailable(player, terminal)) {
            return requested;
        }
        for (final IUltraTerminalType type : getTypes(terminal)) {
            if (type.isAvailable(player, terminal)) {
                return type;
            }
        }
        return null;
    }

    /**
     * @return the terminal types of the item: its type list, all original modes for legacy items, or every registered
     *         terminal when it has no terminal data at all.
     */
    private static Set<String> absorbedIds(final ItemStack is) {
        return UltraTerminalData
                .readTypes(is == null ? null : is.getTagCompound(), AEApi.instance().registries().ultraTerminal());
    }

    @Override
    protected IUltraTerminalType getTerminalType(final ItemStack is) {
        return getCurrentType(is);
    }

    @Override
    protected boolean canOpen(final ItemStack is, final IUltraTerminalType type) {
        return isAbsorbed(is, type.getId());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
            boolean displayMoreInfo) {
        super.addCheckedInformation(stack, player, lines, displayMoreInfo);
        if (GuiScreen.isCtrlKeyDown()) {
            this.addInstalledTerminals(stack, player, lines);
            return;
        }
        final IUltraTerminalType type = getCurrentType(stack);
        if (isShiftKeyDown()) {
            lines.add(StatCollector.translateToLocal(NameConst.TT_ULTRA_TERMINAL));
            if (type != null) {
                lines.add(type.getDisplayName());
            }
            lines.add(NameConst.i18n(NameConst.TT_ULTRA_TERMINAL_TIPS));
            lines.addAll(Arrays.asList(NameConst.i18n(NameConst.TT_ULTRA_TERMINAL_TIPS_DESC).split("\\\\n")));
        } else {
            lines.add(NameConst.i18n(NameConst.TT_SHIFT_FOR_MORE));
        }
    }

    /**
     * Lists the absorbed terminals of the stack.
     */
    @SideOnly(Side.CLIENT)
    private void addInstalledTerminals(final ItemStack stack, final EntityPlayer player, final List<String> lines) {
        lines.add(NameConst.i18n(NameConst.TT_ULTRA_TERMINAL_INSTALLED));
        final IUltraTerminalType current = getCurrentType(stack);
        for (final IUltraTerminalType type : getTypes(stack)) {
            if (!type.isAvailable(player, stack)) {
                continue;
            }
            final boolean selected = current != null && current.getId().equals(type.getId());
            lines.add((selected ? EnumChatFormatting.GREEN : EnumChatFormatting.GRAY) + "  " + type.getDisplayName());
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        final IUltraTerminalType type = getCurrentType(stack);
        if (type == null) {
            return super.getItemStackDisplayName(stack);
        }
        return StatCollector.translateToLocalFormatted("item.wireless_ultra_terminal.with", type.getDisplayName());
    }

    @Override
    public Object getInventory(ItemStack stack, World world, int x, int y, int z, EntityPlayer player) {
        final IGridNode gridNode = Util.getWirelessGrid(stack);

        if (Util.GuiHelper.decodeType(y).getLeft() == Util.GuiHelper.GuiType.ITEM && z == -1) {
            return new WirelessMagnetCardFilterInventory(stack, x, gridNode, player);
        }

        return createItemInventory(stack, x, player, z != 0 ? getGuiType(stack, z) : getTerminalType(stack));
    }

    @Override
    public void openGui(final ItemStack is, final World w, final EntityPlayer player, final Object mode) {
        normalizeStoredMode(is);
        final IUltraTerminalType type = resolveAvailableType(player, is, getCurrentType(is));
        if (type == null) {
            return;
        }
        if (!type.getId().equals(getModeId(is))) {
            setModeId(is, type.getId());
        }
        type.openGui(player, is, player.inventory.currentItem);
    }

    @Override
    public ItemStack onItemRightClick(final ItemStack is, final World w, final EntityPlayer player) {
        if (player.isSneaking()) {
            setModeId(is, getNextModeId(player, is));
            if (w.isRemote) {
                FluidCraft.proxy.showItemName(is);
            }
            return is;
        }
        return super.onItemRightClick(is, w, player);
    }

    /**
     * Switches the given terminal item to the provided terminal type and opens it.
     *
     * @param modeId the terminal type to open, or null to open the terminal stored in the item
     */
    public void switchTerminal(final EntityPlayer player, final ImmutablePair<Integer, ItemStack> term,
            final @Nullable String modeId) {
        if (term == null || term.getRight() == null) {
            return;
        }
        final ItemStack terminal = term.getRight();
        if (modeId != null && !isAbsorbed(terminal, modeId)) {
            return;
        }
        final IUltraTerminalType requested = modeId != null
                ? AEApi.instance().registries().ultraTerminal().getById(modeId)
                : getCurrentType(terminal);
        final IUltraTerminalType type = resolveAvailableType(player, terminal, requested);
        if (type == null) {
            return;
        }
        if (!AEApi.instance().registries().wireless().performCheck(terminal, player)) {
            return;
        }
        if (!type.getId().equals(getModeId(terminal))) {
            setModeId(terminal, type.getId());
        }

        // the opener resolves the registry index of the type and carries it to the item gui
        type.openGui(player, terminal, term.getLeft());
    }

    @Override
    public void onWornTick(ItemStack itemStack, EntityLivingBase entityLivingBase) {
        if (Platform.isServer() && entityLivingBase instanceof EntityPlayer player) {
            IInventory handler = BaublesApi.getBaubles(player);
            if (handler != null) {
                for (int i = 0; i < handler.getSizeInventory(); ++i) {
                    if (handler.getStackInSlot(i) == itemStack) {
                        onUpdate(itemStack, null, player, i + baublesSlotsOffset, false);
                    }
                }
            }
        }
    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int slot, boolean p_77663_5_) {
        if (entityIn instanceof EntityPlayer player) {
            if (WirelessMagnet.getMode(stack) != WirelessMagnet.Mode.Off) {
                WirelessMagnet.doMagnet(stack, player);
            }
        }
    }

    @Override
    public ICustomButtonDataObject getCustomDataObject(ICustomButtonProvider provider) {
        if (provider instanceof IGuiItemObject guiObject) {
            return new UltraTerminalButtons(guiObject.getItemStack());
        }
        return null;
    }
}
