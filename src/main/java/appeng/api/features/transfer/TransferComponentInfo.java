package appeng.api.features.transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import appeng.api.storage.data.IAEStack;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

/**
 * One row of the Transfer Terminal: a component's description plus where it is.
 */
public final class TransferComponentInfo {

    /** {@link #simulatedAmount} of a row that no insert was simulated for. */
    public static final long NOT_SIMULATED = -1;

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

    /**
     * Whether sticky storage on this component claims the current query, see {@link TransferMatcher#capturesAsSticky}.
     */
    public final boolean stickyCapture;

    /** How much of the query a simulated insert put here, or {@link #NOT_SIMULATED}. */
    public final long simulatedAmount;

    /** Whether sticky storage here claimed the simulated insert, which keeps it out of every non-sticky storage. */
    public final boolean stickyClaimed;

    /** When the simulated insert first reached this row, lower is earlier. Server only, used for sorting. */
    public final int simulationOrder;

    /** Where in the component the simulated insert went, empty for components without slots. */
    public final List<SlotAmount> simulatedSlots;

    public TransferComponentInfo(@Nullable final ItemStack selfRep, final String customName,
            @Nullable final String nameSuffixJson, final int x, final int y, final int z, final int dim,
            final byte side, final int networkTier, final TransferDescription description) {
        this(
                selfRep,
                customName,
                nameSuffixJson,
                x,
                y,
                z,
                dim,
                side,
                networkTier,
                description,
                false,
                false,
                NOT_SIMULATED,
                false,
                Integer.MAX_VALUE,
                Collections.emptyList());
    }

    private TransferComponentInfo(@Nullable final ItemStack selfRep, final String customName,
            @Nullable final String nameSuffixJson, final int x, final int y, final int z, final int dim,
            final byte side, final int networkTier, final TransferDescription description,
            final boolean explicitlyListed, final boolean stickyCapture, final long simulatedAmount,
            final boolean stickyClaimed, final int simulationOrder, final List<SlotAmount> simulatedSlots) {
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
        this.stickyCapture = stickyCapture;
        this.simulatedAmount = simulatedAmount;
        this.stickyClaimed = stickyClaimed;
        this.simulationOrder = simulationOrder;
        this.simulatedSlots = simulatedSlots;
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
                query != null && this.description.matcher.isExplicitlyListed(query),
                query != null && this.description.matcher.capturesAsSticky(query),
                this.simulatedAmount,
                this.stickyClaimed,
                this.simulationOrder,
                this.simulatedSlots);
    }

    /**
     * @param amount        how much of the query a simulated insert put here
     * @param order         when the insert first reached this row, lower is earlier
     * @param stickyClaimed whether sticky storage here claimed the insert
     * @param slots         where in the component the insert went
     * @return a copy carrying the result of a simulated insert
     */
    public TransferComponentInfo withSimulation(final long amount, final int order, final boolean stickyClaimed,
            final List<SlotAmount> slots) {
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
                this.description,
                this.explicitlyListed,
                this.stickyCapture,
                amount,
                stickyClaimed,
                order,
                Collections.unmodifiableList(new ArrayList<>(slots)));
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
        out.writeBoolean(this.stickyCapture);
        out.writeLong(this.simulatedAmount);
        out.writeBoolean(this.stickyClaimed);
        out.writeShort(this.simulatedSlots.size());
        for (final SlotAmount slot : this.simulatedSlots) {
            out.writeShort(slot.slot);
            out.writeLong(slot.amount);
        }
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
        final boolean stickyCapture = in.readBoolean();
        final long simulatedAmount = in.readLong();
        final boolean stickyClaimed = in.readBoolean();
        final int slotCount = in.readUnsignedShort();
        final List<SlotAmount> slots = slotCount == 0 ? Collections.emptyList() : new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            final int slot = in.readUnsignedShort();
            slots.add(new SlotAmount(slot, in.readLong()));
        }
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
                TransferDescription.read(in),
                false,
                stickyCapture,
                simulatedAmount,
                stickyClaimed,
                Integer.MAX_VALUE,
                Collections.unmodifiableList(slots));
    }

    /** Part of a simulated insert that went into one slot of a component. */
    public static final class SlotAmount {

        public final int slot;
        public final long amount;

        public SlotAmount(final int slot, final long amount) {
            this.slot = slot;
            this.amount = amount;
        }
    }
}
