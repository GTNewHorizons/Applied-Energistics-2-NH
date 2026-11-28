package appeng.api.stacks;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.Nullable;

import io.netty.buffer.ByteBuf;

final class ItemKeyType extends AEKeyType {

    ItemKeyType() {
        super("item", AEItemKey.class);
    }

    @Override
    public int getAmountPerOperation() {
        return 1;
    }

    @Override
    public int getAmountPerByte() {
        return 8;
    }

    @Override
    public int getAmountPerUnit() {
        return 1;
    }

    @Nullable
    @Override
    public String getUnitSymbol() {
        return null;
    }

    @Override
    public boolean supportsFuzzyRangeSearch() {
        return true;
    }

    @Nullable
    @Override
    public AEKey readFromNBT(NBTTagCompound input) {
        return AEItemKey.fromNBT(input);
    }

    @Nullable
    @Override
    public AEKey readFromPacket(ByteBuf input) throws IOException {
        return AEItemKey.fromPacket(input);
    }

}
