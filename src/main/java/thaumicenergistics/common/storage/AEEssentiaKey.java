package thaumicenergistics.common.storage;

import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.util.Platform;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumicenergistics.common.integration.ModsList;

@NullMarked
public final class AEEssentiaKey extends AEKey {

    static final String NBT_ASPECT_TAG = "AspectTag";

    private final Aspect aspect;

    private AEEssentiaKey(Aspect aspect) {
        this.aspect = aspect;
    }

    public static AEEssentiaKey of(Aspect aspect) {
        return new AEEssentiaKey(aspect);
    }

    @Nullable
    public static AEEssentiaKey of(@Nullable String aspectTag) {
        if (aspectTag == null || aspectTag.isEmpty()) {
            return null;
        }
        Aspect aspect = Aspect.getAspect(aspectTag);
        return aspect == null ? null : of(aspect);
    }

    @Nullable
    public static AEEssentiaKey fromNBT(@Nullable NBTTagCompound input) {
        return input == null ? null : of(input.getString(NBT_ASPECT_TAG));
    }

    @Nullable
    public static AEEssentiaKey fromPacket(ByteBuf input) throws IOException {
        return of(ByteBufUtils.readUTF8String(input));
    }

    @Override
    public AEKeyType getType() {
        return EssentiaKeyType.INSTANCE;
    }

    @Override
    public AEEssentiaKey dropSecondary() {
        return this;
    }

    @Override
    public Aspect getPrimaryKey() {
        return this.aspect;
    }

    @Override
    protected void writeIdentityToNBT(NBTTagCompound output) {
        output.setString(NBT_ASPECT_TAG, this.aspect.getTag());
    }

    @Override
    protected void writeIdentityToPacket(ByteBuf output) throws IOException {
        ByteBufUtils.writeUTF8String(output, this.aspect.getTag());
    }

    public Aspect getAspect() {
        return this.aspect;
    }

    @Override
    public AEEssentiaStack toAEStack(long amount) {
        return new AEEssentiaStack(this.aspect, amount);
    }

    @Override
    public String getId() {
        return this.aspect.getTag();
    }

    @Override
    public String getModId() {
        return "thaumcraft";
    }

    @Override
    public String getDisplayName() {
        return this.aspect.getName();
    }

    @Override
    public IChatComponent getDisplayComponent() {
        return new ChatComponentText(this.getDisplayName());
    }

    @Nullable
    @Override
    public ItemStack wrapForDisplayOrFilter() {
        return this.wrapForDisplayOrFilter(1L);
    }

    @Nullable
    @Override
    public ItemStack wrapForDisplayOrFilter(long amount) {
        final int size = Platform.longToInt(Math.max(amount, 1L));
        if (ModsList.ASPECT_RECIPE_INDEX.isLoaded()) {
            final ItemStack stack = new ItemStack(com.gtnewhorizons.aspectrecipeindex.ModItems.itemAspect, size);
            com.gtnewhorizons.aspectrecipeindex.common.items.ItemAspect.setAspect(stack, this.aspect);
            return stack;
        }
        if (ModsList.THAUMCRAFT_NEI_PLUGIN.isLoaded()) {
            final ItemStack stack = new ItemStack(com.djgiannuzz.thaumcraftneiplugin.ModItems.itemAspect);
            com.djgiannuzz.thaumcraftneiplugin.items.ItemAspect
                    .setAspects(stack, new AspectList().add(this.aspect, size));
            return stack;
        }
        return null;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || obj instanceof AEEssentiaKey other && this.aspect == other.aspect;
    }

    @Override
    public int hashCode() {
        return this.aspect.getTag().hashCode();
    }

    @Override
    public String toString() {
        return this.aspect.getTag();
    }
}
