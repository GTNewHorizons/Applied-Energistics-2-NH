package thaumicenergistics.common.inventory;

import appeng.api.storage.KeyConfigInventory;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.common.storage.AEEssentiaKey;

public class CreativeEssentiaCellConfig extends KeyConfigInventory {

    public CreativeEssentiaCellConfig() {
        super(null, Aspect.aspects.size());

        int i = 0;
        for (Aspect aspect : Aspect.aspects.values()) {
            this.setKey(i, AEEssentiaKey.of(aspect));
            i++;
        }
    }
}
