package appeng.test.behaviors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import appeng.api.behaviors.ContainerItemResult;

public class ContainerItemResultFunctionalTest {

    @Test
    void movingNothingIsNone() {
        final ItemStack container = new ItemStack(Items.bucket);

        assertSame(ContainerItemResult.none(), ContainerItemResult.of(null, 1000));
        assertSame(ContainerItemResult.none(), ContainerItemResult.of(container, 0));
        assertSame(ContainerItemResult.none(), ContainerItemResult.of(container, -1));

        assertTrue(ContainerItemResult.none().isEmpty());
        assertNull(ContainerItemResult.none().getContainer());
        assertEquals(0, ContainerItemResult.none().getAmount());
    }

    @Test
    void keepsContainerAndAmount() {
        final ItemStack container = new ItemStack(Items.water_bucket);
        final ContainerItemResult result = ContainerItemResult.of(container, 1000);

        assertFalse(result.isEmpty());
        assertSame(container, result.getContainer());
        assertEquals(1000, result.getAmount());
    }
}
