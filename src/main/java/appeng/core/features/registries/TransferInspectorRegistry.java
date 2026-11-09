package appeng.core.features.registries;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import appeng.api.features.transfer.ITransferInspector;
import appeng.api.features.transfer.ITransferInspectorRegistry;
import appeng.api.networking.IGridHost;

/**
 * Maps component classes to the inspector that reads them.
 */
public class TransferInspectorRegistry implements ITransferInspectorRegistry {

    private final Map<Class<?>, ITransferInspector<?>> inspectors = new LinkedHashMap<>();
    private final Map<Class<?>, Optional<ITransferInspector<?>>> resolved = new ConcurrentHashMap<>();

    @Override
    public <T extends IGridHost> void register(final Class<T> machineClass,
            final ITransferInspector<? super T> inspector) {
        this.inspectors.put(machineClass, inspector);
        this.resolved.clear();
    }

    @Override
    public ITransferInspector<?> getInspector(final Class<?> machineClass) {
        return this.resolved.computeIfAbsent(machineClass, this::resolve).orElse(null);
    }

    @Override
    public Set<Class<?>> getRegisteredClasses() {
        return Collections.unmodifiableSet(this.inspectors.keySet());
    }

    private Optional<ITransferInspector<?>> resolve(final Class<?> machineClass) {
        for (Class<?> current = machineClass; current != null; current = current.getSuperclass()) {
            final ITransferInspector<?> inspector = this.inspectors.get(current);
            if (inspector != null) {
                return Optional.of(inspector);
            }
        }

        final Deque<Class<?>> pending = new ArrayDeque<>();
        final Set<Class<?>> seen = new HashSet<>();
        for (Class<?> current = machineClass; current != null; current = current.getSuperclass()) {
            Collections.addAll(pending, current.getInterfaces());
        }

        while (!pending.isEmpty()) {
            final Class<?> candidate = pending.poll();
            if (!seen.add(candidate)) {
                continue;
            }

            final ITransferInspector<?> inspector = this.inspectors.get(candidate);
            if (inspector != null) {
                return Optional.of(inspector);
            }
            Collections.addAll(pending, candidate.getInterfaces());
        }

        return Optional.empty();
    }
}
