package appeng.test.behaviors;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import org.jspecify.annotations.Nullable;

import appeng.api.behaviors.ItemSlotAccess;
import appeng.api.stacks.AEKey;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

/**
 * Records what is given to the player instead of giving it.
 */
final class TestSlot extends ItemSlotAccess {

    private @Nullable ItemStack stack;
    private final List<ItemStack> given = new ArrayList<>();
    private final Object2LongOpenHashMap<AEKey> leftovers = new Object2LongOpenHashMap<>();

    TestSlot(@Nullable ItemStack stack) {
        super(fakePlayer());
        this.stack = stack;
    }

    static EntityPlayer fakePlayer() {
        return FakePlayerFactory.getMinecraft(MinecraftServer.getServer().worldServerForDimension(0));
    }

    @Override
    public @Nullable ItemStack get() {
        return this.stack;
    }

    @Override
    public void set(@Nullable ItemStack stack) {
        this.stack = stack == null || stack.stackSize <= 0 ? null : stack;
    }

    @Override
    public void giveOrDrop(ItemStack stack) {
        this.given.add(stack.copy());
    }

    @Override
    public void giveLeftover(AEKey what, long amount) {
        if (amount > 0) {
            this.leftovers.addTo(what, amount);
        }
    }

    List<ItemStack> getGiven() {
        return this.given;
    }

    long getLeftover(AEKey what) {
        return this.leftovers.getLong(what);
    }
}
