package appeng.container.implementations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.crafting.PatternMatch;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.DimensionalCoord;
import appeng.api.util.IInterfaceViewable;
import appeng.crafting.target.PatternTargetList;
import appeng.crafting.target.PatternTargetQuery;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

/**
 * Builds the Pattern Terminal's target list and runs the panel's clicks; {@link ContainerPatternTerm} owns one.
 */
final class PatternTargetController {

    private final ContainerPatternTerm container;

    private ItemStack lastPattern;
    private boolean refreshRequested;
    private int revision;
    private List<PatternTargetQuery.Entry> lastTargets = Collections.emptyList();
    private List<PatternTargetQuery.Conflict> lastConflicts = Collections.emptyList();
    private List<PatternTargetList.CardSource> lastTargetCards = Collections.emptyList();
    private PatternTargetList clientTargets = PatternTargetList.EMPTY;

    PatternTargetController(ContainerPatternTerm container) {
        this.container = container;
    }

    /**
     * Returns the providers the server last sent; client side only.
     */
    PatternTargetList getClientTargets() {
        return this.clientTargets;
    }

    /**
     * Stores the list the server sent; client side only.
     */
    void setClientTargets(PatternTargetList list) {
        this.clientTargets = list;
    }

    /**
     * Rebuilds and sends the list when the encoded pattern changed; call it from detectAndSendChanges.
     */
    void update() {
        final ItemStack pattern = this.container.getEncodedPattern();
        // The query walks every provider of the network, so it runs only when the encoded pattern changes.
        if (!this.refreshRequested && ItemStack.areItemStacksEqual(pattern, this.lastPattern)) {
            return;
        }
        this.refreshRequested = false;
        this.revision++;
        this.lastPattern = pattern == null ? null : pattern.copy();
        this.lastTargets = Collections.emptyList();
        this.lastTargetCards = Collections.emptyList();
        this.lastConflicts = Collections.emptyList();

        final EntityPlayer player = this.container.getPlayerInv().player;
        final IGridNode node = this.container.getNetworkNode();
        final ICraftingPatternDetails details = readPattern(pattern, player);
        if (details == null || node == null || node.getGrid() == null) {
            this.container.patternTargetsAction.send(
                    new PatternTargetList(
                            this.revision,
                            pattern != null,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            null));
            return;
        }

        final ItemStack card = capacityCard();
        final Map<IGrid, Boolean> canBuild = new HashMap<>();
        final List<PatternTargetQuery.Entry> shown = new ArrayList<>();
        final List<PatternTargetList.CardSource> cardSources = new ArrayList<>();
        PatternTargetList.CardSource availableCard = null;
        for (PatternTargetQuery.Entry entry : PatternTargetQuery.run(node.getGrid(), details).entries) {
            if (entry.match == PatternMatch.NO_MATCH) {
                continue;
            }
            PatternTargetList.CardSource cardSource = PatternTargetList.CardSource.NONE;
            if (entry.freeSlots <= 0) {
                if (card == null || PatternTargetQuery.findCapacityCardSlot(entry.provider, card) < 0) {
                    continue;
                }
                // Looked up once, and only when a full interface could take a card.
                if (availableCard == null) {
                    availableCard = this.findCapacityCardSource(card);
                }
                if (availableCard == PatternTargetList.CardSource.NONE) {
                    continue;
                }
                cardSource = availableCard;
            }
            // A grid behind a Pattern Repeater can deny BUILD; a row there would only fail on click.
            if (canBuildOn(entry.provider, player, canBuild)) {
                shown.add(entry);
                cardSources.add(cardSource);
            }
        }
        this.lastTargets = shown;
        this.lastTargetCards = cardSources;

        final List<PatternTargetQuery.Conflict> conflictsWithBuild = new ArrayList<>();
        for (PatternTargetQuery.Conflict conflict : PatternTargetQuery.findConflicts(node.getGrid(), details)) {
            if (canBuildOn(conflict.provider, player, canBuild)) {
                conflictsWithBuild.add(conflict);
            }
        }
        this.lastConflicts = conflictsWithBuild;
        final Set<IInterfaceViewable> withSamePattern = Collections.newSetFromMap(new IdentityHashMap<>());
        for (PatternTargetQuery.Conflict conflict : this.lastConflicts) {
            if (conflict.sameRecipe) {
                withSamePattern.add(conflict.provider);
            }
        }

        final List<PatternTargetList.Entry> entries = new ArrayList<>(shown.size());
        for (int i = 0; i < shown.size(); i++) {
            final PatternTargetQuery.Entry entry = shown.get(i);
            final IInterfaceViewable provider = entry.provider;
            final ItemStack displayRep = provider.getDisplayRep();
            final ItemStack icon = displayRep != null ? displayRep : provider.getSelfRep();
            final DimensionalCoord location = provider.getLocation();
            entries.add(
                    new PatternTargetList.Entry(
                            i,
                            icon,
                            provider.getRawName(),
                            ContainerInterfaceTerminal.serializeSuffix(provider.getPatternTargetSuffix()),
                            location.x,
                            location.y,
                            location.z,
                            location.getDimension(),
                            entry.match,
                            entry.freeSlots,
                            entry.totalSlots,
                            cardSources.get(i),
                            withSamePattern.contains(provider)));
        }

        final List<PatternTargetList.Conflict> conflicts = new ArrayList<>(this.lastConflicts.size());
        for (int i = 0; i < this.lastConflicts.size(); i++) {
            final PatternTargetQuery.Conflict conflict = this.lastConflicts.get(i);
            final IInterfaceViewable provider = conflict.provider;
            final ItemStack displayRep = provider.getDisplayRep();
            final DimensionalCoord location = provider.getLocation();
            conflicts.add(
                    new PatternTargetList.Conflict(
                            i,
                            displayRep != null ? displayRep : provider.getSelfRep(),
                            provider.getRawName(),
                            ContainerInterfaceTerminal.serializeSuffix(provider.getPatternTargetSuffix()),
                            location.x,
                            location.y,
                            location.z,
                            location.getDimension(),
                            conflict.sameRecipe));
        }
        this.container.patternTargetsAction.send(
                new PatternTargetList(
                        this.revision,
                        true,
                        entries,
                        conflicts,
                        conflicts.isEmpty() ? null : PatternTargetQuery.primaryOutput(details)));
    }

    private static ICraftingPatternDetails readPattern(ItemStack pattern, EntityPlayer player) {
        return pattern != null && pattern.getItem() instanceof ICraftingPatternItem item
                ? item.getPatternForItem(pattern, player.worldObj)
                : null;
    }

    private boolean isCurrent(PatternTargetList.Click click, ItemStack pattern) {
        return !this.refreshRequested && click.revision() == this.revision
                && ItemStack.areItemStacksEqual(pattern, this.lastPattern);
    }

    /**
     * Returns whether the player has BUILD on the provider's grid, caching the answer per grid.
     */
    private static boolean canBuildOn(IInterfaceViewable provider, EntityPlayer player, Map<IGrid, Boolean> cache) {
        final IGridNode providerNode = provider.getGridNode(ForgeDirection.UNKNOWN);
        final IGrid providerGrid = providerNode == null ? null : providerNode.getGrid();
        return providerGrid != null && cache.computeIfAbsent(
                providerGrid,
                g -> g.<ISecurityGrid>getCache(ISecurityGrid.class).hasPermission(player, SecurityPermissions.BUILD));
    }

    /**
     * Moves the conflicting pattern at this index of the last list into the player's inventory.
     */
    void extractConflict(PatternTargetList.Click click) {
        final int index = click.index();
        final IGridNode node = this.container.getNetworkNode();
        if (!this.isCurrent(click, this.container.getEncodedPattern()) || node == null
                || node.getGrid() == null
                || index < 0
                || index >= this.lastConflicts.size()
                || !this.container.hasPermission(SecurityPermissions.BUILD)) {
            return;
        }

        final EntityPlayer player = this.container.getPlayerInv().player;
        final ItemStack extracted = PatternTargetQuery
                .extractPattern(this.lastConflicts.get(index), node.getGrid(), player);
        if (extracted == null) {
            return;
        }
        if (!player.inventory.addItemStackToInventory(extracted)) {
            player.entityDropItem(extracted, 0);
        }
        // The encoded pattern did not change, so the lists must be rebuilt by hand to drop the extracted one.
        this.refreshRequested = true;
    }

    /**
     * Puts the encoded pattern into the provider at this index of the last list, installing a card first if needed.
     */
    void sendToTarget(PatternTargetList.Click click) {
        final int index = click.index();
        final ItemStack pattern = this.container.getEncodedPattern();
        final IGridNode node = this.container.getNetworkNode();
        if (!this.isCurrent(click, pattern) || pattern == null
                || node == null
                || node.getGrid() == null
                || index < 0
                || index >= this.lastTargets.size()
                || !this.container.hasPermission(SecurityPermissions.BUILD)) {
            return;
        }

        final PatternTargetQuery.Entry selected = this.lastTargets.get(index);
        final IInterfaceViewable provider = selected.provider;
        final EntityPlayer player = this.container.getPlayerInv().player;
        final IGrid grid = node.getGrid();
        if (!PatternTargetQuery.canModify(provider, grid, player)) {
            this.refreshRequested = true;
            return;
        }
        final ICraftingPatternDetails details = readPattern(pattern, player);
        final PatternMatch match = details == null ? PatternMatch.NO_MATCH
                : PatternTargetQuery.matchProvider(provider, details);
        if (match == PatternMatch.NO_MATCH || match == PatternMatch.NOT_READY
                || match == PatternMatch.UNKNOWN && selected.match == PatternMatch.MATCH) {
            this.refreshRequested = true;
            return;
        }
        if (PatternTargetQuery.insertPattern(provider, grid, player, pattern)) {
            this.refreshRequested = true;
            this.container.consumeEncodedPattern();
            return;
        }
        if (this.lastTargetCards.get(index) == PatternTargetList.CardSource.NONE
                || !this.installCapacityCard(provider, grid, player)) {
            return;
        }
        this.refreshRequested = true;
        if (PatternTargetQuery.insertPattern(provider, grid, player, pattern)) {
            this.container.consumeEncodedPattern();
        }
    }

    private static ItemStack capacityCard() {
        return AEApi.instance().definitions().materials().cardPatternCapacity().maybeStack(1).orNull();
    }

    private static boolean isCapacityCard(ItemStack stack, ItemStack card) {
        return stack != null && stack.getItem() == card.getItem() && stack.getItemDamage() == card.getItemDamage();
    }

    private PatternTargetList.CardSource findCapacityCardSource(ItemStack card) {
        final InventoryPlayer inventory = this.container.getPlayerInv();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            if (isCapacityCard(inventory.getStackInSlot(i), card)) {
                return PatternTargetList.CardSource.INVENTORY;
            }
        }
        final IMEMonitor<IAEItemStack> monitor = this.container.getItemMonitor();
        if (monitor != null && this.container.hasPermission(SecurityPermissions.EXTRACT)) {
            final IAEItemStack simulated = monitor
                    .extractItems(AEItemStack.create(card), Actionable.SIMULATE, this.container.getActionSource());
            if (simulated != null && simulated.getStackSize() > 0) {
                return PatternTargetList.CardSource.NETWORK;
            }
        }
        return PatternTargetList.CardSource.NONE;
    }

    private boolean installCapacityCard(IInterfaceViewable provider, IGrid grid, EntityPlayer player) {
        final ItemStack card = capacityCard();
        // Checked before the card leaves the player or the network, so a refused click takes nothing.
        if (card == null || !PatternTargetQuery.canModify(provider, grid, player)) {
            return false;
        }
        final int slot = PatternTargetQuery.findCapacityCardSlot(provider, card);
        if (slot < 0) {
            return false;
        }

        final InventoryPlayer inventory = this.container.getPlayerInv();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            if (isCapacityCard(inventory.getStackInSlot(i), card)) {
                inventory.decrStackSize(i, 1);
                PatternTargetQuery.installCapacityCard(provider, slot, card);
                return true;
            }
        }

        final IMEMonitor<IAEItemStack> monitor = this.container.getItemMonitor();
        if (monitor == null || !this.container.hasPermission(SecurityPermissions.EXTRACT)) {
            return false;
        }
        final IAEItemStack extracted = Platform.poweredExtraction(
                this.container.getPowerSource(),
                monitor,
                AEItemStack.create(card),
                this.container.getActionSource());
        if (extracted == null || extracted.getStackSize() <= 0) {
            return false;
        }
        PatternTargetQuery.installCapacityCard(provider, slot, card);
        return true;
    }
}
