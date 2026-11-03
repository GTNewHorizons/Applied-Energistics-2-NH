package com.glodblock.github.crossmod.opencomputers;

import li.cil.oc.api.Driver;

public class OCDriverInit {

    public static void run() {
        Driver.add(new DriverOCPatternEditor());
        Driver.add(new DriverOCPatternEditor.Provider());
        Driver.add(new DriverLevelMaintainer());
        Driver.add(new DriverLevelMaintainer.Provider());
        Driver.add(new DriverDualFluidInterface());
        Driver.add(new DriverDualFluidInterface.Provider());
    }
}
