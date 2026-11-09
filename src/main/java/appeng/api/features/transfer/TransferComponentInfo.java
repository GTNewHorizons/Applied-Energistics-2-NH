package appeng.api.features.transfer;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

/**
 * One row of the Transfer Terminal: a component's description plus where it is.
 */
public final class TransferComponentInfo {

    @Nullable
    public final ItemStack selfRep;
    public final String customName;
    @Nullable
    public final String nameSuffixJson;
    public final int x;
    public final int y;
    public final int z;
    public final int dim;

    /** Only used to tell two parts on one block apart, it is not shown to the player. */
    public final byte side;

    /** 0 for the terminal's own network, otherwise the position of the component's network in the search order. */
    public final int networkTier;
    public final TransferDescription description;

    /** Whether the component's whitelist names the current query. Server only, used for sorting. */
    public final boolean explicitlyListed;

    public TransferComponentInfo(@Nullable final ItemStack selfRep, final String customName,
            @Nullable final String nameSuffixJson, final int x, final int y, final int z, final int dim,
            final byte side, final int networkTier, final TransferDescription description) {
        this(selfRep, customName, nameSuffixJson, x, y, z, dim, side, networkTier, description, false);
    }

    private TransferComponentInfo(@Nullable final ItemStack selfRep, final String customName,
            @Nullable final String nameSuffixJson, final int x, final int y, final int z, final int dim,
            final byte side, final int networkTier, final TransferDescription description,
            final boolean explicitlyListed) {
        this.selfRep = selfRep;
        this.customName = customName;
        this.nameSuffixJson = nameSuffixJson;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
        this.side = side;
        this.networkTier = networkTier;
        this.description = description;
        this.explicitlyListed = explicitlyListed;
    }

    /**
     * @return a copy with the per-query state updated for the given stack, null clears it
     */
    public TransferComponentInfo withQuery(@Nullable final IAEStack<?> query) {
        return new TransferComponentInfo(
                this.selfRep,
                this.customName,
                this.nameSuffixJson,
                this.x,
                this.y,
                this.z,
                this.dim,
                this.side,
                this.networkTier,
                this.description.withQuery(query),
                query != null && this.description.matcher.isExplicitlyListed(query));
    }

    /**
     * @return a stable name for ordering rows, it does not depend on the server's language
     */
    public String getSortName() {
        if (!this.customName.isEmpty()) {
            return this.customName;
        }
        return this.selfRep == null ? "" : this.selfRep.getUnlocalizedName();
    }

    public void write(final ByteBuf out) {
        out.writeBoolean(this.selfRep != null);
        if (this.selfRep != null) {
            ByteBufUtils.writeItemStack(out, this.selfRep);
        }
        ByteBufUtils.writeUTF8String(out, this.customName);
        out.writeBoolean(this.nameSuffixJson != null);
        if (this.nameSuffixJson != null) {
            ByteBufUtils.writeUTF8String(out, this.nameSuffixJson);
        }
        out.writeInt(this.x);
        out.writeInt(this.y);
        out.writeInt(this.z);
        out.writeInt(this.dim);
        out.writeByte(this.side);
        out.writeInt(this.networkTier);
        this.description.write(out);
    }

    public static TransferComponentInfo read(final ByteBuf in) {
        final ItemStack selfRep = in.readBoolean() ? ByteBufUtils.readItemStack(in) : null;
        final String customName = ByteBufUtils.readUTF8String(in);
        final String suffix = in.readBoolean() ? ByteBufUtils.readUTF8String(in) : null;
        final int x = in.readInt();
        final int y = in.readInt();
        final int z = in.readInt();
        final int dim = in.readInt();
        final byte side = in.readByte();
        final int tier = in.readInt();
        return new TransferComponentInfo(
                selfRep,
                customName,
                suffix,
                x,
                y,
                z,
                dim,
                side,
                tier,
                TransferDescription.read(in));
    }
}
