package com.glodblock.github.inventory;

import appeng.api.storage.data.IAEItemStack;

public interface IPatternConsumer {

    void acceptPattern(IAEItemStack[] inputs, IAEItemStack[] outputs);
}
