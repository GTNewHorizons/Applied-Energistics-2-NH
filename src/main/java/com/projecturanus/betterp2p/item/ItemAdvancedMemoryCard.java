package com.projecturanus.betterp2p.item;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import com.projecturanus.betterp2p.BetterP2P;
import com.projecturanus.betterp2p.client.ClientCache;
import com.projecturanus.betterp2p.client.gui.widget.GuiScale;
import com.projecturanus.betterp2p.network.ModNetwork;
import com.projecturanus.betterp2p.network.data.MemoryInfo;
import com.projecturanus.betterp2p.network.data.P2PLocation;
import com.projecturanus.betterp2p.util.p2p.P2PUtil;

import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.SelectedPart;
import appeng.core.CreativeTab;
import appeng.parts.p2p.PartP2PTunnel;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemAdvancedMemoryCard extends Item {

    public static final ItemAdvancedMemoryCard INSTANCE = new ItemAdvancedMemoryCard();

    private ItemAdvancedMemoryCard() {
        setMaxStackSize(1);
        setUnlocalizedName("advanced_memory_card");
        setCreativeTab(CreativeTab.instance);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        MemoryInfo info = getInfo(stack);
        tooltip.add(
                StatCollector.translateToLocal(
                        "gui.advanced_memory_card.mode." + info.getMode().name().toLowerCase(Locale.ROOT)));
    }

    @SideOnly(Side.CLIENT)
    private void clearClientCache() {
        ClientCache.clear();
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player) {
        if (player.isSneaking() && world.isRemote) {
            clearClientCache();
        }
        return super.onItemRightClick(itemstack, world, player);
    }

    @Override
    public boolean onItemUse(ItemStack itemstack, EntityPlayer player, World w, int x, int y, int z, int side, float hx,
            float hy, float hz) {
        if (w.isRemote) {
            return false;
        }
        TileEntity te = w.getTileEntity(x, y, z);
        if (!(te instanceof IGridHost gridHost) || gridHost.getGridNode(ForgeDirection.getOrientation(side)) == null) {
            return false;
        }
        SelectedPart selected = Platform.selectPartFromTE(te, Vec3.createVectorHelper(hx, hy, hz));
        IPart part = selected != null ? selected.part : null;
        IGridNode node = part != null ? part.getGridNode() : null;
        IGrid grid = node != null ? node.getGrid() : null;
        if (grid == null) {
            return false;
        }
        if (grid instanceof ISecurityGrid securityGrid
                && !securityGrid.hasPermission(player, SecurityPermissions.BUILD)) {
            return false;
        }

        ItemStack stack = player.getHeldItem();
        MemoryInfo info = getInfo(stack);
        if (part instanceof PartP2PTunnel<?>tunnel) {
            info.setType(P2PUtil.getTypeIndex(tunnel));
            info.setSelectedEntry(P2PLocation.of(tunnel));
        } else {
            info.setType(MemoryInfo.TUNNEL_ANY);
            info.setSelectedEntry(null);
        }
        writeInfo(stack, info);
        ModNetwork.initConnection(player, grid, info);
        return true;
    }

    @Override
    public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister ri) {
        itemIcon = ri.registerIcon(BetterP2P.MODID + ":advanced_memory_card");
    }

    public MemoryInfo getInfo(ItemStack stack) {
        checkItem(stack);
        NBTTagCompound compound = stack.getTagCompound();
        if (compound == null) {
            compound = new NBTTagCompound();
            stack.setTagCompound(compound);
        }
        if (!compound.hasKey("gui")) {
            compound.setByte("gui", (byte) GuiScale.DYNAMIC.ordinal());
        }
        if (!compound.hasKey("selectedIndex", Constants.NBT.TAG_COMPOUND)) {
            compound.setTag("selectedIndex", new NBTTagCompound());
        }
        return new MemoryInfo(
                P2PLocation.readFromNBT(compound.getCompoundTag("selectedIndex")),
                compound.getLong("frequency"),
                BetterMemoryCardModes.fromOrdinal(compound.getInteger("mode")),
                GuiScale.fromOrdinal(compound.getByte("gui")),
                MemoryInfo.TUNNEL_ANY);
    }

    public void writeInfo(ItemStack stack, MemoryInfo info) {
        checkItem(stack);
        NBTTagCompound compound = stack.getTagCompound();
        if (compound == null) {
            compound = new NBTTagCompound();
            stack.setTagCompound(compound);
        }
        compound.setTag("selectedIndex", P2PLocation.writeToNBT(info.getSelectedEntry()));
        compound.setLong("frequency", info.getFrequency());
        compound.setInteger("mode", info.getMode().ordinal());
        compound.setByte("gui", (byte) info.getGuiScale().ordinal());
    }

    private void checkItem(ItemStack stack) {
        if (stack.getItem() != this) {
            throw new ClassCastException(
                    "Cannot cast " + stack.getItem().getClass().getName() + " to " + getClass().getName());
        }
    }
}
