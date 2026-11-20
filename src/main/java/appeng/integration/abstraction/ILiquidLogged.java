package appeng.integration.abstraction;

import net.minecraft.world.World;

import appeng.api.parts.IPartHost;

public interface ILiquidLogged {

    /** True if the cell holds a water source; call before a block replaces it. */
    boolean isWaterSource(World world, int x, int y, int z);

    /** Fills the cell with water if the block now in it can hold water. */
    void fillWater(World world, int x, int y, int z);

    /** Drops the stored fluid if the cable bus can no longer hold it; call after a facade is added. */
    void revalidate(IPartHost host);
}
