package appeng.block.networking;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import appeng.block.AEBaseItemBlock;
import appeng.core.AEConfig;
import appeng.helpers.ControllerPlacementChecker;

public class ItemBlockController extends AEBaseItemBlock {

    public ItemBlockController(final Block id) {
        super(id);
    }

    @Override
    public boolean placeBlockAt(final ItemStack stack, final EntityPlayer player, final World w, final int x,
            final int y, final int z, final int side, final float hitX, final float hitY, final float hitZ,
            final int metadata) {
        if (!w.isRemote && AEConfig.instance.preventConflictingControllerPlacement
                && ControllerPlacementChecker.wouldControllerConflict(w, x, y, z)) {
            final ChatComponentTranslation msg = new ChatComponentTranslation(
                    "chat.appliedenergistics2.ControllerPlacementConflict");
            msg.getChatStyle().setColor(EnumChatFormatting.RED);
            player.addChatMessage(msg);
            // the client already consumed the item locally; resync so the stack count is correct
            if (player instanceof EntityPlayerMP playerMP) {
                playerMP.sendContainerToPlayer(playerMP.inventoryContainer);
            }
            return false;
        }
        return super.placeBlockAt(stack, player, w, x, y, z, side, hitX, hitY, hitZ, metadata);
    }
}
