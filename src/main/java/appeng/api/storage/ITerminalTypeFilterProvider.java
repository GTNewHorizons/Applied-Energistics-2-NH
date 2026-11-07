package appeng.api.storage;

import net.minecraft.entity.player.EntityPlayer;

import org.jspecify.annotations.NonNull;

import appeng.api.storage.data.IAEStackType;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;

public interface ITerminalTypeFilterProvider {

    @NonNull
    Reference2BooleanMap<IAEStackType<?>> getTypeFilter(EntityPlayer player);

    void saveTypeFilter();
}
