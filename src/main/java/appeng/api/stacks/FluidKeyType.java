package appeng.api.stacks;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.Nullable;

import io.netty.buffer.ByteBuf;

final class FluidKeyType extends AEKeyType {

    FluidKeyType() {
        super("fluid", AEFluidKey.class);
    }

    @Override
    public int getAmountPerOperation() {
        return 1000;
    }

    @Override
    public int getAmountPerByte() {
        return 8 * 256;
    }

    @Override
    public int getAmountPerUnit() {
        return 1000;
    }

    @Override
    public String getUnitSymbol() {
        return "mB";
    }

    @Override
    public boolean hasVariants() {
        return false;
    }

    @Nullable
    @Override
    public AEKey readFromNBT(NBTTagCompound input) {
        return AEFluidKey.fromNBT(input);
    }

    @Nullable
    @Override
    public AEKey readFromPacket(ByteBuf input) throws IOException {
        return AEFluidKey.fromPacket(input);
    }

}
