package appeng.test.behaviors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.jspecify.annotations.Nullable;

import appeng.api.behaviors.ContainerItemResult;
import appeng.api.behaviors.ContainerTransferResult;

final class BehaviorAssertions {

    private BehaviorAssertions() {}

    static void assertStack(@Nullable ItemStack expected, @Nullable ItemStack actual) {
        assertTrue(
                ItemStack.areItemStacksEqual(expected, actual),
                () -> "expected " + describe(expected) + " but was " + describe(actual));
    }

    static void assertStacks(List<ItemStack> expected, List<ItemStack> actual) {
        assertEquals(expected.size(), actual.size(), () -> "unexpected stacks " + describe(actual));
        for (int i = 0; i < expected.size(); i++) {
            assertStack(expected.get(i), actual.get(i));
        }
    }

    static void assertResult(ItemStack expectedContainer, long expectedAmount, ContainerItemResult actual) {
        assertEquals(expectedAmount, actual.getAmount());
        assertStack(expectedContainer, actual.getContainer());
    }

    static void assertNone(ContainerItemResult actual) {
        assertTrue(actual.isEmpty(), () -> "expected no result but moved " + actual.getAmount());
        assertNull(actual.getContainer());
    }

    static void assertResult(long expectedAmount, int expectedContainers, ContainerTransferResult actual) {
        assertEquals(expectedAmount, actual.getAmount());
        assertEquals(expectedContainers, actual.getContainers());
    }

    static void assertNone(ContainerTransferResult actual) {
        assertResult(0, 0, actual);
    }

    private static String describe(@Nullable ItemStack stack) {
        return stack == null ? "null" : stack + " " + stack.getTagCompound();
    }

    private static String describe(List<ItemStack> stacks) {
        final StringBuilder builder = new StringBuilder("[");
        for (ItemStack stack : stacks) {
            if (builder.length() > 1) {
                builder.append(", ");
            }
            builder.append(describe(stack));
        }
        return builder.append(']').toString();
    }
}
