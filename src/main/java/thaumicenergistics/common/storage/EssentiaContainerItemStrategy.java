package thaumicenergistics.common.storage;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import appeng.api.behaviors.ContainerItemResult;
import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.stacks.GenericStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.blocks.BlockJarItem;
import thaumcraft.common.blocks.ItemJarFilled;
import thaumcraft.common.items.ItemEssence;
import thaumicenergistics.api.IThEEssentiaContainerPermission;
import thaumicenergistics.api.items.IRestrictedEssentiaContainerItem;
import thaumicenergistics.common.integration.tc.EssentiaItemContainerHelper;

/**
 * Handles phials, jars and other {@link IEssentiaContainerItem}s whitelisted in {@link EssentiaItemContainerHelper}.
 * Containers that cannot hold partial amounts (phials) are only filled or emptied as a whole.
 */
@NullMarked
public final class EssentiaContainerItemStrategy implements ContainerItemStrategy<AEEssentiaKey> {

    @Override
    public boolean isContainer(@Nullable ItemStack stack) {
        return stack != null
                && (stack.getItem() instanceof IEssentiaContainerItem || stack.getItem() instanceof BlockJarItem);
    }

    @Override
    public @Nullable GenericStack getContainedStack(ItemStack container) {
        if (!(container.getItem() instanceof IEssentiaContainerItem essentiaContainer)) {
            return null;
        }

        final AspectList aspects = essentiaContainer.getAspects(container);
        final Aspect aspect = getStoredAspect(aspects);
        if (aspect == null) {
            return null;
        }
        final int amount = aspects.getAmount(aspect);
        return amount <= 0 ? null : new GenericStack(AEEssentiaKey.of(aspect), amount);
    }

    @Override
    public ContainerItemResult fill(ItemStack container, AEEssentiaKey key, long amount) {
        // Not kept in a static field, as strategies are registered in preInit, before the helper can be created.
        final EssentiaItemContainerHelper helper = EssentiaItemContainerHelper.INSTANCE;
        if (amount <= 0
                || helper.getItemType(container) != EssentiaItemContainerHelper.AspectItemType.EssentiaContainer) {
            return ContainerItemResult.none();
        }

        final Item item = container.getItem();
        final Aspect aspect = key.getAspect();
        if (item instanceof ItemJarFilled && helper.doesJarHaveLabel(container)
                && aspect != helper.getJarLabelAspect(container)) {
            return ContainerItemResult.none();
        }
        if (item instanceof IRestrictedEssentiaContainerItem restricted && !restricted.acceptsAspect(aspect)) {
            return ContainerItemResult.none();
        }

        final IThEEssentiaContainerPermission info = helper.getContainerInfo(item, container.getItemDamage());
        if (info == null) {
            return ContainerItemResult.none();
        }

        final int capacity = info.maximumCapacity();
        final int space = capacity - helper.getContainerStoredAmount(container);
        final int toFill = limit(amount, space, capacity, info.canHoldPartialAmount());
        if (toFill <= 0) {
            return ContainerItemResult.none();
        }

        if (item instanceof ItemEssence) {
            return ContainerItemResult.of(helper.createFilledPhial(aspect), toFill);
        }
        if (item instanceof BlockJarItem) {
            return ContainerItemResult
                    .of(helper.createFilledJar(aspect, toFill, container.getItemDamage(), false), toFill);
        }

        final IEssentiaContainerItem essentiaContainer = (IEssentiaContainerItem) item;
        AspectList aspects = essentiaContainer.getAspects(container);
        if (aspects == null) {
            aspects = new AspectList();
        } else if (aspects.size() > 0 && aspect != getStoredAspect(aspects)) {
            // Don't mix aspects.
            return ContainerItemResult.none();
        }
        aspects.add(aspect, toFill);

        final ItemStack result = container.copy();
        essentiaContainer.setAspects(result, aspects);
        return ContainerItemResult.of(result, toFill);
    }

    @Override
    public ContainerItemResult drain(ItemStack container, AEEssentiaKey key, long amount) {
        if (amount <= 0 || EssentiaItemContainerHelper.INSTANCE.getItemType(container)
                != EssentiaItemContainerHelper.AspectItemType.EssentiaContainer) {
            return ContainerItemResult.none();
        }

        // Empty jars are containers too, but hold nothing to drain.
        if (!(container.getItem() instanceof IEssentiaContainerItem essentiaContainer)) {
            return ContainerItemResult.none();
        }

        final IThEEssentiaContainerPermission info = EssentiaItemContainerHelper.INSTANCE
                .getContainerInfo(container.getItem(), container.getItemDamage());
        if (info == null) {
            return ContainerItemResult.none();
        }

        final Aspect aspect = key.getAspect();
        final AspectList aspects = essentiaContainer.getAspects(container);
        if (aspects == null || aspect != getStoredAspect(aspects)) {
            return ContainerItemResult.none();
        }

        final int stored = aspects.getAmount(aspect);
        final int toDrain = limit(amount, stored, info.maximumCapacity(), info.canHoldPartialAmount());
        if (toDrain <= 0) {
            return ContainerItemResult.none();
        }

        if (toDrain == stored || toDrain == info.maximumCapacity()) {
            final ItemStack emptied = this.createEmptied(container, aspect);
            if (emptied != null) {
                return ContainerItemResult.of(emptied, toDrain);
            }
        }

        final AspectList remaining = aspects.copy();
        remaining.remove(aspect, toDrain);
        final ItemStack result = container.copy();
        essentiaContainer.setAspects(result, remaining);
        return ContainerItemResult.of(result, toDrain);
    }

    /**
     * @return the empty form of a phial or jar, keeping the label of labeled jars, or null for other containers.
     */
    private @Nullable ItemStack createEmptied(ItemStack container, Aspect aspect) {
        final EssentiaItemContainerHelper helper = EssentiaItemContainerHelper.INSTANCE;
        final Item item = container.getItem();
        if (item instanceof ItemEssence) {
            return helper.createEmptyPhial();
        }
        if (item instanceof ItemJarFilled) {
            return helper.doesJarHaveLabel(container)
                    ? helper.createFilledJar(aspect, 0, container.getItemDamage(), true)
                    : helper.createEmptyJar(container.getItemDamage());
        }
        return null;
    }

    /**
     * Limits a requested amount by what is possible. Containers without partial amounts move exactly their capacity.
     */
    private static int limit(long requested, int possible, int capacity, boolean partial) {
        if (partial) {
            return (int) Math.min(requested, possible);
        }
        return requested >= capacity && possible >= capacity ? capacity : 0;
    }

    private static @Nullable Aspect getStoredAspect(@Nullable AspectList aspects) {
        if (aspects == null) {
            return null;
        }
        final Aspect[] stored = aspects.getAspects();
        return stored.length == 0 ? null : stored[0];
    }
}
