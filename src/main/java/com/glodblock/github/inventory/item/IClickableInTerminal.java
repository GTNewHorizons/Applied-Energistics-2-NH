package com.glodblock.github.inventory.item;

import com.glodblock.github.util.Util;

public interface IClickableInTerminal {

    void setClickedInterface(Util.DimensionalCoordSide tile);

    Util.DimensionalCoordSide getClickedInterface();
}
