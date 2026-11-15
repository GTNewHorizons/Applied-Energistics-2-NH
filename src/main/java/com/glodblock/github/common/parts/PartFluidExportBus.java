package com.glodblock.github.common.parts;

import static appeng.util.item.AEFluidStackType.FLUID_STACK_TYPE;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;

import com.glodblock.github.client.textures.FCPartsTexture;
import com.glodblock.github.util.Util;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Upgrades;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.KeyConfigInventory;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStackType;
import appeng.me.GridAccessException;
import appeng.parts.automation.PartBaseExportBus;
import appeng.util.InventoryAdaptor;

public class PartFluidExportBus extends PartBaseExportBus<IAEFluidStack> {

    public PartFluidExportBus(ItemStack is) {
        super(is);
    }

    @Override
    public IIcon getFaceIcon() {
        return FCPartsTexture.PartFluidExportBus.getIcon();
    }

    @Override
    protected int getAdaptorFlags() {
        return InventoryAdaptor.ALLOW_FLUIDS | InventoryAdaptor.FOR_INSERTS;
    }

    @Override
    public int calculateAmountToSend() {
        double amount = 1000D;
        switch (this.getInstalledUpgrades(Upgrades.SPEED)) {
            case 4:
                amount = amount * 1.5;
            case 3:
                amount = amount * 2;
            case 2:
                amount = amount * 4;
            case 1:
                amount = amount * 8;
        }
        switch (this.getInstalledUpgrades(Upgrades.SUPERSPEED)) {
            case 4:
                amount = amount * 8;
            case 3:
                amount = amount * 12;
            case 2:
                amount = amount * 16;
            case 1:
                amount = amount * 32;
        }
        return (int) Math.floor(amount);
    }

    @Override
    protected IMEMonitor<IAEFluidStack> getMonitor() {
        try {
            return this.getProxy().getStorage().getFluidInventory();
        } catch (final GridAccessException e) {
            return null;
        }
    }

    @Override
    protected void doFuzzy(IAEFluidStack aes, FuzzyMode fzMode, IEnergyGrid energy,
            IMEMonitor<IAEFluidStack> gridInv) {}

    @Override
    protected void doOreDict(IEnergyGrid energy, IMEMonitor<IAEFluidStack> gridInv) {}

    // legacy
    @Override
    public void readFromNBT(NBTTagCompound extra) {
        super.readFromNBT(extra);

        final KeyConfigInventory config = this.getConfig();
        for (int i = 0; i < config.getSizeInventory(); i++) {
            if (config.getKey(i) instanceof AEItemKey itemKey) {
                final IAEFluidStack fluid = Util.getAEFluidFromItem(itemKey.toStack());
                config.setKey(i, fluid == null ? null : fluid.getKey());
            }
        }
    }

    @Override
    public IAEStackType<IAEFluidStack> getStackType() {
        return FLUID_STACK_TYPE;
    }
}
