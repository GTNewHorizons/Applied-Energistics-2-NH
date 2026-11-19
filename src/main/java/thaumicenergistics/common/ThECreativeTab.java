package thaumicenergistics.common;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import thaumicenergistics.api.ThEApi;

/**
 * Kept out of {@link ThaumicEnergistics} because FML initializes every {@code @SidedProxy} holder class in the jar,
 * even when ThE itself is skipped, and creating the tab registers it in the creative inventory.
 */
public final class ThECreativeTab extends CreativeTabs {

    public static final ThECreativeTab INSTANCE = new ThECreativeTab();

    private ThECreativeTab() {
        super("ThaumicEnergistics");
    }

    @Override
    public ItemStack getIconItemStack() {
        return ThEApi.instance().blocks().InfusionProvider.getStack();
    }

    @Override
    public Item getTabIconItem() {
        return ThEApi.instance().blocks().InfusionProvider.getItem();
    }
}
