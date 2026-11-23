package appeng.container.implementations;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEItemStack;
import appeng.container.stackedit.ContainerStackEdit;
import appeng.container.sync.ActionHandler;
import appeng.container.sync.StreamCodecs;
import appeng.util.item.AEItemStack;

public class ContainerStackRename extends ContainerStackEdit {

    private final ActionHandler<String> nameAction;

    public ContainerStackRename(final InventoryPlayer ip, final Object te) {
        super(ip, te);
        this.nameAction = this.syncRegistrar().actionC2S("name", StreamCodecs.string()).onServerAction(this::setName);
    }

    public void submitName(final String name) {
        this.nameAction.send(name);
    }

    private void setName(final String name) {
        this.submit(stack -> {
            if (!(stack instanceof IAEItemStack itemStack)) {
                return null;
            }

            final ItemStack renamed = itemStack.getItemStack().setStackDisplayName(name);
            return AEItemStack.create(renamed).setStackSize(itemStack.getStackSize());
        });
    }
}
