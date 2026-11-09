package appeng.api.features.transfer;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import appeng.api.networking.IGridHost;

/**
 * Lets mods add their own components to the Transfer Terminal.
 * <p>
 * Components that extend an AE2 bus, plane or interface are covered by the inspector of their base class, register only
 * if the component stores additional filters.
 */
public interface ITransferInspectorRegistry {

    /**
     * @param machineClass component class or interface, a registration for a more specific class wins
     * @param inspector    reads components of that class
     */
    <T extends IGridHost> void register(Class<T> machineClass, ITransferInspector<? super T> inspector);

    /**
     * @return the inspector of the closest registered superclass, then of any implemented interface, or null if the
     *         class is not a transfer component
     */
    @Nullable
    ITransferInspector<?> getInspector(Class<?> machineClass);

    Set<Class<?>> getRegisteredClasses();
}
