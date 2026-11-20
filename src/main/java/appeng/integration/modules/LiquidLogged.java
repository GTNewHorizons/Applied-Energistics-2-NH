package appeng.integration.modules;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;

import com.gtnewhorizons.liquidlogged.api.FluidHolderPredicate;
import com.gtnewhorizons.liquidlogged.api.LiquidLoggedAPI;

import appeng.api.parts.IFacadeContainer;
import appeng.api.parts.IPartHost;
import appeng.block.AEBaseSlabBlock;
import appeng.block.AEBaseStairBlock;
import appeng.block.networking.BlockCableBus;
import appeng.block.storage.BlockSkyChest;
import appeng.helpers.Reflected;
import appeng.integration.IIntegrationModule;
import appeng.integration.IntegrationHelper;
import appeng.integration.abstraction.ILiquidLogged;
import appeng.tile.networking.TileCableBus;

public class LiquidLogged implements IIntegrationModule, ILiquidLogged {

    @Reflected
    public static LiquidLogged instance;

    /** Water only, as in vanilla. */
    private static final FluidHolderPredicate WATER_ONLY = (world, x, y, z, block, meta, fluid) -> fluid
            == FluidRegistry.WATER;

    /** Cables and the parts on them hold water until a facade is attached. */
    private static final FluidHolderPredicate CABLE_BUS = (world, x, y, z, block, meta, fluid) -> {
        if (fluid != FluidRegistry.WATER) {
            return false;
        }
        return !(world.getTileEntity(x, y, z) instanceof TileCableBus bus) || !hasFacade(bus);
    };

    @Reflected
    public LiquidLogged() {
        IntegrationHelper.testClassExistence(this, LiquidLoggedAPI.class);
    }

    @Override
    public void init() {
        for (final Object o : Block.blockRegistry) {
            final Block block = (Block) o;
            if (block instanceof BlockCableBus) {
                LiquidLoggedAPI.registerPredicate(block, CABLE_BUS);
            } else if (isWaterloggable(block)) {
                LiquidLoggedAPI.registerPredicate(block, WATER_ONLY);
            }
        }
    }

    @Override
    public void postInit() {}

    @Override
    public boolean isWaterSource(final World world, final int x, final int y, final int z) {
        final Block block = world.getBlock(x, y, z);
        return (block == Blocks.water || block == Blocks.flowing_water) && world.getBlockMetadata(x, y, z) == 0;
    }

    @Override
    public void fillWater(final World world, final int x, final int y, final int z) {
        if (!world.isRemote && LiquidLoggedAPI.canHold(world, x, y, z, FluidRegistry.WATER)) {
            LiquidLoggedAPI.setFluid(world, x, y, z, FluidRegistry.WATER);
        }
    }

    @Override
    public void revalidate(final IPartHost host) {
        final TileEntity te = host.getTile();
        final World world = te == null ? null : te.getWorldObj();
        if (world != null && !world.isRemote) {
            LiquidLoggedAPI.revalidate(world, te.xCoord, te.yCoord, te.zCoord);
        }
    }

    private static boolean isWaterloggable(final Block block) {
        if (block instanceof AEBaseSlabBlock) {
            return !block.isOpaqueCube(); // double slabs are opaque
        }
        return block instanceof AEBaseStairBlock || block instanceof BlockSkyChest;
    }

    private static boolean hasFacade(final TileCableBus bus) {
        final IFacadeContainer facades = bus.getFacadeContainer();
        for (final ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (facades.getFacade(side) != null) {
                return true;
            }
        }
        return false;
    }
}
