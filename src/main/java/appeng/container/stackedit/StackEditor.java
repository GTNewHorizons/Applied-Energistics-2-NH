package appeng.container.stackedit;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;

import appeng.container.AEBaseContainer;
import appeng.container.sync.ActionHandler;
import appeng.container.sync.StreamCodecs;
import appeng.container.sync.SyncRegistrar;

public final class StackEditor {

    private final AEBaseContainer owner;
    private final IntFunction<@Nullable IStackEditTarget> targets;
    private final Map<StackEditMode, ActionHandler<Integer>> requests = new EnumMap<>(StackEditMode.class);

    /**
     * @param targets resolves a slot index to its target on the server, or {@code null} if the slot cannot be edited
     */
    public StackEditor(final AEBaseContainer owner, final SyncRegistrar sync, final String key,
            final IntFunction<@Nullable IStackEditTarget> targets, final StackEditMode mode,
            final StackEditMode... modes) {
        this.owner = owner;
        this.targets = targets;

        final SyncRegistrar child = sync.child(key);
        for (final StackEditMode m : EnumSet.of(mode, modes)) {
            this.requests.put(
                    m,
                    child.actionC2S(m.name(), StreamCodecs.intValue()).onServerAction(slot -> this.open(m, slot)));
        }
    }

    public void request(final StackEditMode mode, final int slot) {
        final ActionHandler<Integer> request = this.requests.get(mode);
        if (request != null) {
            request.send(slot);
        }
    }

    private void open(final StackEditMode mode, final int slot) {
        if (this.owner.getInventoryPlayer().getItemStack() != null) {
            return;
        }

        final IStackEditTarget target = this.targets.apply(slot);
        if (target == null || target.getStack() == null) {
            return;
        }

        if (this.owner.openSubGui(mode.getGui()) instanceof ContainerStackEdit editor) {
            editor.bind(target);
        }
    }
}
