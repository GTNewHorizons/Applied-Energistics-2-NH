package thaumicenergistics.common.storage;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import appeng.api.behaviors.DisplayItemConverter;
import appeng.api.stacks.GenericStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumicenergistics.common.integration.ModsList;

/**
 * Converts aspect items of Aspect Recipe Index and Thaumcraft NEI Plugin into essentia. Aspect Recipe Index items carry
 * the amount as stack size, Thaumcraft NEI Plugin items in their aspect list.
 */
@NullMarked
public final class EssentiaDisplayItemConverter implements DisplayItemConverter {

    @Override
    public @Nullable GenericStack convert(ItemStack stack) {
        if (ModsList.ASPECT_RECIPE_INDEX.isLoaded()
                && stack.getItem() instanceof com.gtnewhorizons.aspectrecipeindex.common.items.ItemAspect) {
            final Aspect aspect = com.gtnewhorizons.aspectrecipeindex.common.items.ItemAspect.getAspect(stack);
            return aspect == null ? null : new GenericStack(AEEssentiaKey.of(aspect), stack.stackSize);
        }
        if (ModsList.THAUMCRAFT_NEI_PLUGIN.isLoaded()
                && stack.getItem() instanceof com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect) {
            final AspectList aspects = com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect.getAspects(stack);
            if (aspects == null || aspects.getAspects().length == 0) {
                return null;
            }
            final Aspect aspect = aspects.getAspects()[0];
            return aspect == null ? null : new GenericStack(AEEssentiaKey.of(aspect), aspects.getAmount(aspect));
        }
        return null;
    }
}
