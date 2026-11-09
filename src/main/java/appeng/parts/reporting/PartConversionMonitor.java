/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.parts.reporting;

import static appeng.server.ServerHelper.CONTAINER_INTERACTION_KEY;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.ContainerTransfer;
import appeng.api.behaviors.ItemSlotAccess;
import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.stacks.AEKey;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.MEStorage;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.client.texture.CableBusTextures;
import appeng.core.AELog;
import appeng.helpers.Reflected;
import appeng.me.GridAccessException;
import appeng.me.storage.LegacyInventoryStorage;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

public class PartConversionMonitor extends AbstractPartMonitor {

    private static final CableBusTextures FRONT_BRIGHT_ICON = CableBusTextures.PartConversionMonitor_Bright;
    private static final CableBusTextures FRONT_DARK_ICON = CableBusTextures.PartConversionMonitor_Dark;
    private static final CableBusTextures FRONT_DARK_ICON_LOCKED = CableBusTextures.PartConversionMonitor_Dark_Locked;
    private static final CableBusTextures FRONT_COLORED_ICON = CableBusTextures.PartConversionMonitor_Colored;

    @Reflected
    public PartConversionMonitor(final ItemStack is) {
        super(is);
    }

    @Override
    public boolean onPartShiftActivate(final EntityPlayer player, final Vec3 pos) {
        if (player.worldObj.isRemote) {
            return true;
        }

        if (!this.getProxy().isActive()) {
            return false;
        }

        if (!Platform.hasPermissions(this.getLocation(), player)) {
            return false;
        }

        IAEStack<?> displayed = this.getDisplayed();
        if (displayed == null) {
            if (!CONTAINER_INTERACTION_KEY.isKeyDown(player)) {
                this.injectItemToMonitor(player);
            } else {
                this.injectExtraToMonitor(player);
            }
        } else {
            if (displayed instanceof IAEItemStack) {
                this.injectItemToMonitor(player);
            } else if (ContainerItemStrategies
                    .isContainer(player.getCurrentEquippedItem(), displayed.getKey().getType())) {
                        this.injectExtraToMonitor(player);
                    }
        }

        return true;
    }

    @Override
    protected void onLockedInteraction(EntityPlayer player, boolean isShiftDown) {
        IAEStack<?> displayed = getDisplayed();
        if (displayed == null) return;

        if (displayed instanceof IAEItemStack) {
            this.extractItemFromMonitor(player);
        } else {
            this.extractExtraFromMonitor(player);
        }
    }

    protected void injectItemToMonitor(final EntityPlayer player) {
        boolean injectFromInventory = false;

        ItemStack item = player.getCurrentEquippedItem();
        if (item == null && this.getDisplayed() != null) {
            injectFromInventory = true;
            item = ((IAEItemStack) this.getDisplayed()).getItemStack();
        }

        if (item != null) {
            try {
                if (!this.getProxy().isActive()) {
                    return;
                }

                final IEnergySource energy = this.getProxy().getEnergy();
                final IMEMonitor<IAEItemStack> cell = this.getProxy().getStorage().getItemInventory();
                final IAEItemStack input = AEItemStack.create(item);

                if (injectFromInventory) { // From player inventory
                    for (int x = 0; x < player.inventory.getSizeInventory(); x++) {
                        final ItemStack targetStack = player.inventory.getStackInSlot(x);
                        if (input.equals(targetStack)) {
                            final IAEItemStack insertItem = input.copy();
                            insertItem.setStackSize(targetStack.stackSize);
                            final IAEItemStack failedToInsert = Platform
                                    .poweredInsert(energy, cell, insertItem, new PlayerSource(player, this));
                            player.inventory.setInventorySlotContents(
                                    x,
                                    failedToInsert == null ? null : failedToInsert.getItemStack());
                        }
                    }
                } else { // From player hand
                    final IAEItemStack failedToInsert = Platform
                            .poweredInsert(energy, cell, input, new PlayerSource(player, this));
                    player.inventory.setInventorySlotContents(
                            player.inventory.currentItem,
                            failedToInsert == null ? null : failedToInsert.getItemStack());
                }
            } catch (final GridAccessException e) {
                AELog.debug(e);
            }
        }
    }

    protected void extractItemFromMonitor(final EntityPlayer player) {
        if (this.getDisplayed() instanceof IAEItemStack input) {
            try {
                if (!this.getProxy().isActive()) {
                    return;
                }

                final IEnergySource energy = this.getProxy().getEnergy();
                final IMEMonitor<IAEItemStack> cell = this.getProxy().getStorage().getItemInventory();

                final ItemStack is = input.getItemStack();
                input.setStackSize(is.getMaxStackSize());

                final IAEItemStack retrieved = Platform
                        .poweredExtraction(energy, cell, input, new PlayerSource(player, this));
                if (retrieved != null) {
                    ItemStack newItems = retrieved.getItemStack();
                    Platform.addToPlayerInvOrDrop(player, newItems);

                    if (player.openContainer != null) {
                        player.openContainer.detectAndSendChanges();
                    }
                }
            } catch (final GridAccessException e) {
                AELog.debug(e);
            }
        }
    }

    private void injectExtraToMonitor(final EntityPlayer player) {
        final IAEStack<?> displayed = this.getDisplayed();
        final AEKey displayedKey = displayed == null ? null : displayed.getKey();

        try {
            ContainerTransfer.drainInto(
                    ItemSlotAccess.equipped(player),
                    this.createStorage(),
                    this.getProxy().getEnergy(),
                    new PlayerSource(player, this),
                    key -> displayedKey == null || displayedKey.equals(key),
                    1,
                    false,
                    Actionable.MODULATE);
        } catch (final GridAccessException e) {
            AELog.debug(e);
        }
    }

    private void extractExtraFromMonitor(final EntityPlayer player) {
        final IAEStack<?> displayed = this.getDisplayed();
        if (displayed == null) return;

        try {
            ContainerTransfer.fillFrom(
                    ItemSlotAccess.equipped(player),
                    this.createStorage(),
                    this.getProxy().getEnergy(),
                    new PlayerSource(player, this),
                    displayed.getKey(),
                    1,
                    Actionable.MODULATE);
        } catch (final GridAccessException e) {
            AELog.debug(e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private MEStorage createStorage() throws GridAccessException {
        final IStorageGrid storage = this.getProxy().getStorage();
        return new LegacyInventoryStorage(type -> storage.getMEMonitor((IAEStackType) type));
    }

    @Override
    public CableBusTextures getFrontBright() {
        return FRONT_BRIGHT_ICON;
    }

    @Override
    public CableBusTextures getFrontColored() {
        return FRONT_COLORED_ICON;
    }

    @Override
    public CableBusTextures getFrontDark() {
        return this.isLocked() ? FRONT_DARK_ICON_LOCKED : FRONT_DARK_ICON;
    }
}
