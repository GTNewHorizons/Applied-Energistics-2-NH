package appeng.core.features.registries;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Test;

import appeng.api.features.transfer.ITransferInspector;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;

public final class TransferInspectorRegistryTest {

    private final TransferInspectorRegistry registry = new TransferInspectorRegistry();

    private static <T extends IGridHost> ITransferInspector<T> inspector() {
        return host -> null;
    }

    @Test
    public void unknownClassHasNoInspector() {
        assertNull(this.registry.getInspector(Base.class));
    }

    @Test
    public void exactClassWinsOverSuperclass() {
        final ITransferInspector<Base> base = inspector();
        final ITransferInspector<Sub> sub = inspector();
        this.registry.register(Base.class, base);
        this.registry.register(Sub.class, sub);

        assertSame(sub, this.registry.getInspector(Sub.class));
        assertSame(base, this.registry.getInspector(Base.class));
    }

    @Test
    public void subclassInheritsNearestSuperclass() {
        final ITransferInspector<Base> base = inspector();
        this.registry.register(Base.class, base);

        assertSame(base, this.registry.getInspector(SubSub.class));
    }

    @Test
    public void superclassWinsOverInterface() {
        final ITransferInspector<Marker> marker = inspector();
        final ITransferInspector<Base> base = inspector();
        this.registry.register(Marker.class, marker);
        this.registry.register(Base.class, base);

        assertSame(base, this.registry.getInspector(SubSub.class));
    }

    @Test
    public void interfaceIsUsedWhenNoSuperclassMatches() {
        final ITransferInspector<Marker> marker = inspector();
        this.registry.register(Marker.class, marker);

        assertSame(marker, this.registry.getInspector(SubSub.class));
        assertSame(marker, this.registry.getInspector(Sub.class));
    }

    @Test
    public void laterRegistrationReplacesCachedResult() {
        final ITransferInspector<Base> base = inspector();
        final ITransferInspector<Sub> sub = inspector();
        this.registry.register(Base.class, base);
        assertSame(base, this.registry.getInspector(Sub.class));

        this.registry.register(Sub.class, sub);

        assertSame(sub, this.registry.getInspector(Sub.class));
    }

    @Test
    public void registeredClassesAreListed() {
        this.registry.register(Base.class, inspector());

        assertTrue(this.registry.getRegisteredClasses().contains(Base.class));
    }

    private interface Marker extends IGridHost {
    }

    private static class Base implements IGridHost {

        @Override
        public IGridNode getGridNode(final ForgeDirection dir) {
            return null;
        }

        @Override
        public AECableType getCableConnectionType(final ForgeDirection dir) {
            return AECableType.GLASS;
        }

        @Override
        public void securityBreak() {}
    }

    private static class Sub extends Base implements Marker {
    }

    private static final class SubSub extends Sub {
    }
}
