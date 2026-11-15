package net.p455w0rd.wirelesscraftingterminal.integration.modules;

import net.p455w0rd.wirelesscraftingterminal.integration.IIntegrationModule;
import net.p455w0rd.wirelesscraftingterminal.integration.IntegrationHelper;

import appeng.helpers.Reflected;

public class RFItem implements IIntegrationModule {

    @Reflected
    public static RFItem instance;

    @Reflected
    public RFItem() {
        IntegrationHelper.testClassExistence(this, cofh.api.energy.IEnergyContainerItem.class);
    }

    @Override
    public void init() {}

    @Override
    public void postInit() {}
}
