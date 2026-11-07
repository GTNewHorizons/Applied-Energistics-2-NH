package thaumicenergistics.api;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.NonNull;

import appeng.api.features.INetworkEncodable;
import appeng.api.implementations.items.IAEItemPowerStorage;

@Deprecated
public interface IThEWirelessEssentiaTerminal extends INetworkEncodable, IAEItemPowerStorage {

    @Deprecated
    @NonNull
    NBTTagCompound getWETerminalTag(@NonNull ItemStack terminalItemstack);
}
