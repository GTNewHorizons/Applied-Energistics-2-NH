package appeng.test.behaviors;

import static appeng.test.behaviors.BehaviorAssertions.assertStack;
import static appeng.test.behaviors.TestContainers.WATER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import appeng.api.behaviors.ItemSlotAccess;
import appeng.api.stacks.GenericStack;
import appeng.items.misc.ItemMEStackPacket;

public class ItemSlotAccessFunctionalTest {

    private final EntityPlayer player = TestSlot.fakePlayer();

    @BeforeEach
    @AfterEach
    void clearPlayer() {
        // The fake player is shared by all tests.
        this.player.inventory.clearInventory(null, -1);
        this.player.inventory.setItemStack(null);
        this.player.inventory.currentItem = 0;
    }

    @Test
    void equippedKeepsTheHotbarSlotItWasCreatedFor() {
        this.player.inventory.currentItem = 3;
        final ItemSlotAccess access = ItemSlotAccess.equipped(this.player);
        this.player.inventory.currentItem = 4;

        access.set(new ItemStack(Items.stick));
        assertStack(new ItemStack(Items.stick), this.player.inventory.getStackInSlot(3));
        assertNull(this.player.inventory.getStackInSlot(4));
        assertStack(new ItemStack(Items.stick), access.get());

        access.set(null);
        assertNull(this.player.inventory.getStackInSlot(3));
    }

    @Test
    void carriedNotifiesChanges() {
        final AtomicInteger changes = new AtomicInteger();
        final ItemSlotAccess access = ItemSlotAccess.carried(this.player, changes::incrementAndGet);

        access.set(new ItemStack(Items.stick));
        assertStack(new ItemStack(Items.stick), this.player.inventory.getItemStack());
        assertStack(new ItemStack(Items.stick), access.get());
        assertEquals(1, changes.get());
    }

    @Test
    void slotAccessesTheContainerSlot() {
        final Slot slot = new Slot(new InventoryBasic("test", false, 1), 0, 0, 0);
        final ItemSlotAccess access = ItemSlotAccess.slot(this.player, slot);

        access.set(new ItemStack(Items.stick));
        assertStack(new ItemStack(Items.stick), slot.getStack());
        assertStack(new ItemStack(Items.stick), access.get());
    }

    @Test
    void givesLeftoversAsStackPackets() {
        ItemSlotAccess.equipped(this.player).giveLeftover(WATER, 500);

        final ItemStack packet = this.findInInventory();
        assertNotNull(packet);
        assertTrue(packet.getItem() instanceof ItemMEStackPacket);
        assertEquals(new GenericStack(WATER, 500), GenericStack.fromAEStack(ItemMEStackPacket.toAEStack(packet)));
    }

    @Test
    void givesNothingForEmptyLeftovers() {
        ItemSlotAccess.equipped(this.player).giveLeftover(WATER, 0);

        assertNull(this.findInInventory());
    }

    private @Nullable ItemStack findInInventory() {
        for (ItemStack stack : this.player.inventory.mainInventory) {
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }
}
