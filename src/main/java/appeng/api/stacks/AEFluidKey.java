package appeng.api.stacks;

import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.jspecify.annotations.Nullable;

import com.glodblock.github.common.item.ItemFluidPacket;

import appeng.api.storage.data.IAEFluidStack;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import codechicken.nei.item.ItemFluidDisplay;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

/**
 * Identifies a fluid by its {@link Fluid}. Fluid NBT is not part of the identity.
 */
public final class AEFluidKey extends AEKey {

    private static final String NBT_FLUID_NAME = "FluidName";

    private final Fluid fluid;

    private AEFluidKey(Fluid fluid) {
        this.fluid = fluid;
    }

    public static @Nullable AEFluidKey of(@Nullable FluidStack stack) {
        if (stack == null || stack.getFluid() == null) {
            return null;
        }
        return of(stack.getFluid());
    }

    public static AEFluidKey of(Fluid fluid) {
        return new AEFluidKey(fluid);
    }

    public static @Nullable AEFluidKey fromNBT(@Nullable NBTTagCompound input) {
        if (input == null || input.hasNoTags()) {
            return null;
        }

        final Fluid fluid = FluidRegistry.getFluid(input.getString(NBT_FLUID_NAME));
        return fluid == null ? null : of(fluid);
    }

    public static @Nullable AEFluidKey fromPacket(ByteBuf input) throws IOException {
        Fluid fluid = FluidRegistry.getFluid(ByteBufUtils.readUTF8String(input));
        return fluid == null ? null : of(fluid);
    }

    @Override
    public AEKeyType getType() {
        return AEKeyType.fluids();
    }

    @Override
    public AEFluidKey dropSecondary() {
        return this;
    }

    @Override
    public Fluid getPrimaryKey() {
        return this.fluid;
    }

    @Override
    protected void writeIdentityToNBT(NBTTagCompound output) {
        output.setString(NBT_FLUID_NAME, this.fluid.getName());
    }

    @Override
    protected void writeIdentityToPacket(ByteBuf output) throws IOException {
        ByteBufUtils.writeUTF8String(output, this.fluid.getName());
    }

    public Fluid getFluid() {
        return this.fluid;
    }

    @Override
    public String getId() {
        String name = FluidRegistry.getDefaultFluidName(this.fluid);
        return name == null ? this.fluid.getName() : name;
    }

    @Override
    public String getModId() {
        String name = FluidRegistry.getDefaultFluidName(this.fluid);
        if (name == null) {
            return "";
        }
        int separator = name.indexOf(':');
        return separator < 0 ? name : name.substring(0, separator);
    }

    @Override
    public String getDisplayName() {
        return this.fluid.getLocalizedName(this.toStack());
    }

    @Override
    public IChatComponent getDisplayComponent() {
        String unlocalizedName = this.fluid.getUnlocalizedName();
        if (StatCollector.canTranslate(unlocalizedName)) {
            return new ChatComponentTranslation(unlocalizedName);
        }

        return new ChatComponentText(this.getDisplayName());
    }

    public FluidStack toStack() {
        return this.toStack(1000);
    }

    public FluidStack toStack(long amount) {
        return new FluidStack(this.fluid, Platform.longToInt(amount));
    }

    @Override
    public IAEFluidStack toAEStack(long amount) {
        return AEFluidStack.create(this.toStack(1)).setStackSize(amount);
    }

    @Override
    public ItemStack wrapForDisplayOrFilter() {
        return this.wrapForDisplayOrFilter(1000L);
    }

    @Override
    public ItemStack wrapForDisplayOrFilter(long amount) {
        if (Platform.isNEILoaded) {
            return ItemFluidDisplay.createStack(this.fluid, amount);
        }
        ItemStack stack = ItemFluidPacket.newDisplayStack(this.toStack());
        ItemFluidPacket.setFluidAmount(stack, amount);
        return stack;
    }

    /**
     * @return whether {@code other} holds this fluid. NBT of {@code other} is ignored.
     */
    public boolean matches(@Nullable FluidStack other) {
        return other != null && other.getFluid() == this.fluid;
    }

    public boolean matches(@Nullable Fluid other) {
        return other == this.fluid;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || obj instanceof AEFluidKey other && this.fluid == other.fluid;
    }

    @Override
    public int hashCode() {
        return this.fluid.getName().hashCode();
    }

    @Override
    public String toString() {
        return this.fluid.getName();
    }
}
