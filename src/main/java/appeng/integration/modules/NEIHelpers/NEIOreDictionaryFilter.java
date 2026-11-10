package appeng.integration.modules.NEIHelpers;

import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEItemKey;
import appeng.client.gui.implementations.GuiOreFilter;
import appeng.util.partitionlist.OreFilterPartitionList;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.api.ItemFilter.ItemFilterProvider;

public class NEIOreDictionaryFilter implements ItemFilterProvider {

    @Override
    public ItemFilter getFilter() {
        GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;

        if (currentScreen instanceof GuiOreFilter) {
            GuiOreFilter oreScreen = (GuiOreFilter) currentScreen;

            if (oreScreen.useNEIFilter() && !oreScreen.getText().isEmpty()) {
                return new Filter(oreScreen.getText());
            }
        }

        return null;
    }

    public static class Filter implements ItemFilter {

        private final @Nullable Predicate<AEItemKey> list;

        public Filter(String pattern) {
            this.list = OreFilterPartitionList.makeFilter(pattern);
        }

        @Override
        public boolean matches(ItemStack itemStack) {
            return this.list == null || this.list.test(AEItemKey.of(itemStack));
        }
    }
}
