package appeng.util.inv;

import appeng.parts.p2p.PartP2PItems;
import net.minecraftforge.common.util.ForgeDirection;

public class AdaptorP2PItem extends AdaptorIInventory {

    public AdaptorP2PItem(PartP2PItems p2p, ForgeDirection side) {
        super(new WrapperMCISidedInventory(p2p, side), p2p.getInventoryStackLimit());
    }
}
