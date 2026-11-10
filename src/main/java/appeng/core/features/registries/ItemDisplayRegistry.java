package appeng.core.features.registries;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.Item;

import appeng.api.storage.IItemDisplayRegistry;
import appeng.client.render.AppEngRenderItem;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class ItemDisplayRegistry implements IItemDisplayRegistry {

    private final Set<Class<? extends Item>> blacklistedItemClasses;
    private final Set<Item> blacklistedItems;

    public ItemDisplayRegistry() {
        this.blacklistedItemClasses = new HashSet<>();
        this.blacklistedItems = new HashSet<>();
    }

    @Override
    public void blacklistItemDisplay(Item item) {
        this.blacklistedItems.add(item);
    }

    @Override
    public void blacklistItemDisplay(Class<? extends Item> item) {
        this.blacklistedItemClasses.add(item);
    }

    @Override
    public boolean isBlacklisted(Item item) {
        return blacklistedItems.contains(item);
    }

    @Override
    public boolean isBlacklisted(Class<? extends Item> item) {
        return blacklistedItemClasses.contains(item);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addPostItemRenderHook(ItemRenderHook hook) {
        AppEngRenderItem.POST_HOOKS.add(hook);
    }
}
