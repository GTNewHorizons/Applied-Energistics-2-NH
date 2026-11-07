package appeng.core.features.registries.entries;

import java.util.Collection;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import appeng.api.features.InscriberProcessType;

/**
 * inscribe recipes do not use up the provided optional upon craft
 *
 * @author thatsIch
 * @version rv2
 * @since rv2
 */
public class InscriberInscribeRecipe extends InscriberRecipe {

    public InscriberInscribeRecipe(@NonNull final Collection<ItemStack> inputs, @NonNull final ItemStack output,
            @Nullable final ItemStack top, @Nullable final ItemStack bot) {
        super(inputs, output, top, bot, InscriberProcessType.Inscribe);
    }
}
