package appeng.container.implementations;

import static appeng.util.Platform.isServer;

import net.minecraft.entity.player.InventoryPlayer;

import org.jspecify.annotations.Nullable;

import appeng.api.stacks.GenericStack;
import appeng.api.storage.KeyConfigInventory;
import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEStack;
import appeng.container.interfaces.IVirtualSlotHolder;
import appeng.parts.automation.PartSharedItemBus;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class ContainerBusIO extends ContainerUpgradeable implements IVirtualSlotHolder {

    public final @Nullable GenericStack[] virtualSlotsClient = new GenericStack[9];
    private final PartSharedItemBus<?> bus;

    public ContainerBusIO(final InventoryPlayer ip, final PartSharedItemBus<?> te) {
        super(ip, te);
        this.bus = te;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        this.updateVirtualSlots(StorageName.CONFIG, this.bus.getConfig(), virtualSlotsClient);
    }

    @Override
    public void receiveSlotStacks(StorageName invName, Int2ObjectMap<IAEStack<?>> slotStacks) {
        final KeyConfigInventory storage = this.bus.getConfig();
        for (var entry : slotStacks.int2ObjectEntrySet()) {
            final IAEStack<?> stack = entry.getValue();
            storage.setKey(entry.getIntKey(), stack == null ? null : stack.getKey());
        }
        if (isServer()) {
            this.updateVirtualSlots(StorageName.CRAFTING_OUTPUT, storage, virtualSlotsClient);
        }
    }
}
