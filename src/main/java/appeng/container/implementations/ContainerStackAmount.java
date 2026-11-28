package appeng.container.implementations;

import net.minecraft.entity.player.InventoryPlayer;

import appeng.container.stackedit.ContainerStackEdit;
import appeng.container.sync.ActionHandler;
import appeng.container.sync.StreamCodecs;

public class ContainerStackAmount extends ContainerStackEdit {

    private final ActionHandler<Long> amountAction;

    public ContainerStackAmount(final InventoryPlayer ip, final Object te) {
        super(ip, te);
        this.amountAction = this.syncRegistrar().actionC2S("amount", StreamCodecs.longValue())
                .onServerAction(this::setAmount);
    }

    public void submitAmount(final long amount) {
        this.amountAction.send(amount);
    }

    private void setAmount(final long amount) {
        this.submit(stack -> amount > 0 ? stack.setStackSize(amount) : null);
    }
}
