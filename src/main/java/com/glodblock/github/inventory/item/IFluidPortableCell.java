package com.glodblock.github.inventory.item;

import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.storage.ITerminalHost;
import appeng.container.interfaces.IInventorySlotAware;

public interface IFluidPortableCell extends ITerminalHost, IInventorySlotAware, IEnergySource, IGuiItemObject {
}
