package appeng.api.features.transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.util.Platform;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

/**
 * What an {@link ITransferInspector} reports about a component. Everything except the matcher and the live handlers is
 * sent to the client.
 */
public final class TransferDescription {

    public final List<SettingEntry> settings;
    public final List<ItemStack> upgrades;
    public final List<ConfigEntry> config;
    public final boolean hasCraftingCard;
    public final boolean canImport;
    public final boolean canExport;
    public final boolean sticky;
    public final boolean hasPriority;
    public final int priority;
    @Nullable
    public final String oreFilter;

    /** True if AE routes stacks into this component by priority, false if it works on its own schedule. */
    public final boolean storageHandler;

    /** Mode used by fuzzy config entries, null if none are fuzzy. Server only. */
    @Nullable
    public final FuzzyMode fuzzyMode;

    /** Server only, {@link TransferMatcher#NONE} on the client. */
    public final TransferMatcher matcher;

    /**
     * The component's storage handlers for a stack type, the same instances AE routes stacks into. Used to tell which
     * row a simulated insert reached. Server only, null for components that are not storage.
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    public final Function<IAEStackType<?>, List<IMEInventoryHandler>> liveHandlers;

    /** Slot that holds one of {@link #liveHandlers}, or -1. Server only, null for components without slots. */
    @Nullable
    public final ToIntFunction<IMEInventoryHandler<?>> slotOf;

    private TransferDescription(final Builder builder) {
        this.settings = Collections.unmodifiableList(builder.settings);
        this.upgrades = Collections.unmodifiableList(builder.upgrades);
        this.config = Collections.unmodifiableList(builder.config);
        this.hasCraftingCard = builder.hasCraftingCard;
        this.canImport = builder.canImport;
        this.canExport = builder.canExport;
        this.sticky = builder.sticky;
        this.hasPriority = builder.hasPriority;
        this.priority = builder.priority;
        this.oreFilter = builder.oreFilter;
        this.storageHandler = builder.storageHandler;
        this.fuzzyMode = builder.fuzzyMode;
        this.matcher = builder.matcher;
        this.liveHandlers = builder.liveHandlers;
        this.slotOf = builder.slotOf;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a builder starting from this description, for wrapping another inspector's result
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    public void write(final ByteBuf out) {
        out.writeShort(this.settings.size());
        for (final SettingEntry entry : this.settings) {
            out.writeByte(entry.setting.ordinal());
            out.writeByte(entry.value.ordinal());
        }

        out.writeShort(this.upgrades.size());
        for (final ItemStack upgrade : this.upgrades) {
            ByteBufUtils.writeItemStack(out, upgrade);
        }

        out.writeShort(this.config.size());
        for (final ConfigEntry entry : this.config) {
            Platform.writeStackByte(entry.stack, out);
            out.writeBoolean(entry.fuzzy);
            out.writeBoolean(entry.matchesQuery);
        }

        out.writeBoolean(this.hasCraftingCard);
        out.writeBoolean(this.canImport);
        out.writeBoolean(this.canExport);
        out.writeBoolean(this.sticky);
        out.writeBoolean(this.storageHandler);
        out.writeBoolean(this.hasPriority);
        out.writeInt(this.priority);
        out.writeBoolean(this.oreFilter != null);
        if (this.oreFilter != null) {
            ByteBufUtils.writeUTF8String(out, this.oreFilter);
        }
    }

    public static TransferDescription read(final ByteBuf in) {
        final Builder builder = builder();

        final int settingCount = in.readUnsignedShort();
        for (int i = 0; i < settingCount; i++) {
            final Settings setting = Settings.values()[in.readUnsignedByte()];
            final int ordinal = in.readUnsignedByte();
            for (final Enum<?> value : setting.getPossibleValues()) {
                if (value.ordinal() == ordinal) {
                    builder.setting(setting, value);
                    break;
                }
            }
        }

        final int upgradeCount = in.readUnsignedShort();
        for (int i = 0; i < upgradeCount; i++) {
            builder.upgrade(ByteBufUtils.readItemStack(in));
        }

        final int configCount = in.readUnsignedShort();
        for (int i = 0; i < configCount; i++) {
            final IAEStack<?> stack = Platform.readStackByte(in);
            final boolean fuzzy = in.readBoolean();
            builder.config.add(new ConfigEntry(stack, fuzzy, in.readBoolean()));
        }

        builder.hasCraftingCard(in.readBoolean());
        builder.canImport(in.readBoolean());
        builder.canExport(in.readBoolean());
        builder.sticky(in.readBoolean());
        builder.storageHandler(in.readBoolean());
        final boolean hasPriority = in.readBoolean();
        final int priority = in.readInt();
        if (hasPriority) {
            builder.priority(priority);
        }
        if (in.readBoolean()) {
            builder.oreFilter(ByteBufUtils.readUTF8String(in));
        }
        return builder.build();
    }

    /**
     * @return a copy with {@link ConfigEntry#matchesQuery} updated for the given stack
     */
    public TransferDescription withQuery(@Nullable final IAEStack<?> query) {
        final Builder builder = new Builder(this);
        builder.config.clear();
        for (final ConfigEntry entry : this.config) {
            builder.config.add(entry.withMatch(query != null && entry.matches(query, this.fuzzyMode)));
        }
        return builder.build();
    }

    public static final class SettingEntry {

        public final Settings setting;
        public final Enum<?> value;

        public SettingEntry(final Settings setting, final Enum<?> value) {
            this.setting = setting;
            this.value = value;
        }
    }

    public static final class ConfigEntry {

        public final IAEStack<?> stack;
        public final boolean fuzzy;
        public final boolean matchesQuery;

        public ConfigEntry(final IAEStack<?> stack, final boolean fuzzy, final boolean matchesQuery) {
            this.stack = stack;
            this.fuzzy = fuzzy;
            this.matchesQuery = matchesQuery;
        }

        private ConfigEntry withMatch(final boolean match) {
            return new ConfigEntry(this.stack, this.fuzzy, match);
        }

        private boolean matches(final IAEStack<?> query, @Nullable final FuzzyMode fuzzyMode) {
            if (this.stack.getStackType() != query.getStackType()) {
                return false;
            }
            return this.fuzzy && fuzzyMode != null ? this.stack.fuzzyComparison(query, fuzzyMode)
                    : this.stack.isSameType((Object) query);
        }
    }

    public static final class Builder {

        private final List<SettingEntry> settings = new ArrayList<>();
        private final List<ItemStack> upgrades = new ArrayList<>();
        private final List<ConfigEntry> config = new ArrayList<>();
        private boolean hasCraftingCard;
        private boolean canImport;
        private boolean canExport;
        private boolean sticky;
        private boolean storageHandler;
        private boolean hasPriority;
        private int priority;
        private String oreFilter;
        private FuzzyMode fuzzyMode;
        private TransferMatcher matcher = TransferMatcher.NONE;
        @SuppressWarnings("rawtypes")
        private Function<IAEStackType<?>, List<IMEInventoryHandler>> liveHandlers;
        private ToIntFunction<IMEInventoryHandler<?>> slotOf;

        private Builder() {}

        private Builder(final TransferDescription from) {
            this.settings.addAll(from.settings);
            this.upgrades.addAll(from.upgrades);
            this.config.addAll(from.config);
            this.hasCraftingCard = from.hasCraftingCard;
            this.canImport = from.canImport;
            this.canExport = from.canExport;
            this.sticky = from.sticky;
            this.storageHandler = from.storageHandler;
            this.hasPriority = from.hasPriority;
            this.priority = from.priority;
            this.oreFilter = from.oreFilter;
            this.fuzzyMode = from.fuzzyMode;
            this.matcher = from.matcher;
            this.liveHandlers = from.liveHandlers;
            this.slotOf = from.slotOf;
        }

        public Builder setting(final Settings setting, final Enum<?> value) {
            this.settings.add(new SettingEntry(setting, value));
            return this;
        }

        public Builder upgrade(final ItemStack upgrade) {
            this.upgrades.add(upgrade);
            return this;
        }

        public Builder upgrades(final List<ItemStack> upgrades) {
            this.upgrades.addAll(upgrades);
            return this;
        }

        public Builder config(final IAEStack<?> stack, final boolean fuzzy) {
            this.config.add(new ConfigEntry(stack, fuzzy, false));
            return this;
        }

        public Builder hasCraftingCard(final boolean hasCraftingCard) {
            this.hasCraftingCard = hasCraftingCard;
            return this;
        }

        public Builder canImport(final boolean canImport) {
            this.canImport = canImport;
            return this;
        }

        public Builder canExport(final boolean canExport) {
            this.canExport = canExport;
            return this;
        }

        public Builder sticky(final boolean sticky) {
            this.sticky = sticky;
            return this;
        }

        public Builder storageHandler(final boolean storageHandler) {
            this.storageHandler = storageHandler;
            return this;
        }

        public Builder priority(final int priority) {
            this.hasPriority = true;
            this.priority = priority;
            return this;
        }

        public Builder oreFilter(@Nullable final String oreFilter) {
            this.oreFilter = oreFilter;
            return this;
        }

        public Builder fuzzyMode(@Nullable final FuzzyMode fuzzyMode) {
            this.fuzzyMode = fuzzyMode;
            return this;
        }

        public Builder matcher(final TransferMatcher matcher) {
            this.matcher = matcher;
            return this;
        }

        @SuppressWarnings("rawtypes")
        public Builder liveHandlers(@Nullable final Function<IAEStackType<?>, List<IMEInventoryHandler>> liveHandlers) {
            this.liveHandlers = liveHandlers;
            return this;
        }

        public Builder slotOf(@Nullable final ToIntFunction<IMEInventoryHandler<?>> slotOf) {
            this.slotOf = slotOf;
            return this;
        }

        public TransferDescription build() {
            return new TransferDescription(this);
        }
    }
}
