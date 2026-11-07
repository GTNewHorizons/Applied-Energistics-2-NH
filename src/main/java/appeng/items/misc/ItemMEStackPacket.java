package appeng.items.misc;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;

import appeng.api.storage.data.IAEStack;
import appeng.core.features.AEFeature;
import appeng.core.localization.GuiText;
import appeng.items.AEBaseItem;

public class ItemMEStackPacket extends AEBaseItem {

    public ItemMEStackPacket() {
        this.setFeature(EnumSet.of(AEFeature.Core));
        this.setMaxStackSize(1);
    }

    @Override
    protected void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
            boolean displayMoreInfo) {
        super.addCheckedInformation(stack, player, lines, displayMoreInfo);

        lines.add(GuiText.itemMEStackPacketDesc1.getLocal());
        lines.add(GuiText.itemMEStackPacketDesc2.getLocal());

        final IAEStack<?> aes = toAEStack(stack);
        if (aes != null) lines.add(aes.getDisplayName() + ": " + aes.getStackSize());
    }

    @Nullable
    public static IAEStack<?> toAEStack(@NonNull final ItemStack is) {
        if (is.getItem() instanceof ItemMEStackPacket) {
            return IAEStack.fromNBTGeneric(ItemStackNBT.get(is));
        }
        return null;
    }

    // hide from creative tab & nei
    @Override
    protected void getCheckedSubItems(Item sameItem, CreativeTabs creativeTab, List<ItemStack> itemStacks) {}
}
