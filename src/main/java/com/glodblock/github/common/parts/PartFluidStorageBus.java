package com.glodblock.github.common.parts;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.glodblock.github.util.Util;

import appeng.api.stacks.AEItemKey;
import appeng.api.storage.KeyConfigInventory;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStackType;
import appeng.parts.misc.PartStorageBus;

public class PartFluidStorageBus extends PartStorageBus {

    public PartFluidStorageBus(ItemStack is) {
        super(is);
    }

    @Override
    public IAEStackType<?> getStackType() {
        return FLUID_STACK_TYPE;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);

        // Migrate from old data
        KeyConfigInventory config = this.getConfig();
        for (int i = 0; i < config.getSizeInventory(); i++) {
            if (config.getKey(i) instanceof AEItemKey itemKey) {
                final IAEFluidStack fluid = Util.getAEFluidFromItem(itemKey.toStack());
                config.setKey(i, fluid == null ? null : fluid.getKey());
            }
        }
    }
}
