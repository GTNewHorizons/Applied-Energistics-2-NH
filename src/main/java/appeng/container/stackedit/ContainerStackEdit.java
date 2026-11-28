package appeng.container.stackedit;

import java.util.function.Function;

import net.minecraft.entity.player.InventoryPlayer;

import org.jspecify.annotations.Nullable;

import appeng.api.storage.data.IAEStack;
import appeng.container.ContainerSubGui;
import appeng.container.PrimaryGui;
import appeng.container.sync.SyncCodecs;
import appeng.container.sync.handlers.ObjectSyncHandler;

public abstract class ContainerStackEdit extends ContainerSubGui {

    private final ObjectSyncHandler<@Nullable IAEStack<?>> stack;
    private @Nullable IStackEditTarget target;
    private @Nullable Runnable stackListener;

    protected ContainerStackEdit(final InventoryPlayer ip, final Object anchor) {
        super(ip, anchor);
        this.stack = this.syncRegistrar().objectS2C("stack", SyncCodecs.aeStack(), null)
                .onClientChange((oldValue, newValue) -> {
                    if (this.stackListener != null) {
                        this.stackListener.run();
                    }
                });
    }

    void bind(final IStackEditTarget target) {
        this.target = target;
        final IAEStack<?> current = target.getStack();
        this.stack.set(current == null ? null : current.copy());
        // Opening the GUI already sent the initial sync without the stack, so send it now instead of next tick.
        this.getSyncManager().flushSync();
    }

    public @Nullable IAEStack<?> getStack() {
        return this.stack.get();
    }

    public void setStackListener(final @Nullable Runnable listener) {
        this.stackListener = listener;
    }

    /**
     * Applies {@code edit} to a copy of the target's current stack and returns to the primary GUI.
     *
     * @param edit returns the stack to store, or {@code null} to leave the target unchanged
     */
    protected final void submit(final Function<IAEStack<?>, @Nullable IAEStack<?>> edit) {
        final IStackEditTarget target = this.target;
        final IAEStack<?> shown = this.stack.get();
        final IAEStack<?> current = target == null ? null : target.getStack();
        if (target != null && shown != null && current != null && current.isSameType(shown)) {
            final IAEStack<?> edited = edit.apply(current.copy());
            if (edited != null) {
                target.setStack(edited);
            }
        }

        final PrimaryGui primaryGui = this.getPrimaryGui();
        if (primaryGui != null) {
            primaryGui.open(this.getInventoryPlayer().player);
        }
    }
}
