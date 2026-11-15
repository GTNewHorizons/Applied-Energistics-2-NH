package thaumicenergistics.common.storage;

import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import io.netty.buffer.ByteBuf;

@NullMarked
public final class EssentiaKeyType extends AEKeyType {

    public static final EssentiaKeyType INSTANCE = new EssentiaKeyType();

    public static final String ID = "essentia";

    private EssentiaKeyType() {
        super(ID, AEEssentiaKey.class);
    }

    @Override
    public int getAmountPerOperation() {
        return 1;
    }

    @Override
    public int getAmountPerByte() {
        return 2;
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
    public boolean hasVariants() {
        return false;
    }

    @Nullable
    @Override
    public AEKey readFromNBT(NBTTagCompound input) {
        return AEEssentiaKey.fromNBT(input);
    }

    @Nullable
    @Override
    public AEKey readFromPacket(ByteBuf input) throws IOException {
        return AEEssentiaKey.fromPacket(input);
    }

}
