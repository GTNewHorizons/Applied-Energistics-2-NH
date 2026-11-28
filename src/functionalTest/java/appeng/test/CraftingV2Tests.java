package appeng.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.DimensionManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import appeng.api.config.CraftingMode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.localization.GuiText;
import appeng.crafting.v2.CraftingJobV2;
import appeng.crafting.v2.CraftingRequest;
import appeng.crafting.v2.CraftingRequest.UsedResolverEntry;
import appeng.crafting.v2.resolvers.CraftableItemResolver.CraftFromPatternTask;
import appeng.test.mockme.MockAESystem;
import appeng.util.item.AEItemStack;
import appeng.util.item.ItemList;
import cpw.mods.fml.common.registry.GameRegistry;
import ggfab.GGItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.common.items.IDMetaTool01;
import gregtech.common.items.MetaGeneratedTool01;

public class CraftingV2Tests {

    static World dummyWorld = null;
    static boolean mixedMetalTestRecipeRegistered = false;
    final int MAX_SIMULATION_STEPS = 10_000;

    final ItemStack bronzePlate, bronzeDoublePlate, bronzeIngot, gtHammer, singleUseGtHammer;
    final ItemStack ironDust, ironIngot, ironPlate, goldDust, goldIngot, goldBlock;
    final ItemStack tinIngot, tinPlate, pigIronIngot, pigIronPlate;

    public CraftingV2Tests() {
        bronzePlate = Materials.Bronze.getPlates(1);
        bronzeDoublePlate = GTOreDictUnificator.get(OrePrefixes.plateDouble, Materials.Bronze, 1);
        bronzeIngot = Materials.Bronze.getIngots(1);
        gtHammer = MetaGeneratedTool01.INSTANCE
                .getToolWithStats(IDMetaTool01.HARDHAMMER.ID, 1, Materials.VanadiumSteel, null, null);
        singleUseGtHammer = GGItemList.SingleUseHardHammer.get(1);
        ironDust = Materials.Iron.getDust(1);
        ironIngot = Materials.Iron.getIngots(1);
        ironPlate = Materials.Iron.getPlates(1);
        goldDust = Materials.Gold.getDust(1);
        goldIngot = Materials.Gold.getIngots(1);
        goldBlock = Materials.Gold.getBlocks(1);
        tinIngot = Materials.Tin.getIngots(1);
        tinPlate = Materials.Tin.getPlates(1);
        pigIronIngot = Materials.PigIron.getIngots(1);
        pigIronPlate = Materials.PigIron.getPlates(1);

        if (!DimensionManager.isDimensionRegistered(256)) {
            DimensionManager.registerProviderType(256, WorldProviderSurface.class, false);
            DimensionManager.registerDimension(256, 256);
        }
        if (dummyWorld == null) {
            dummyWorld = new WorldServer(
                    MinecraftServer.getServer(),
                    new DummySaveHandler(),
                    "DummyTestWorld",
                    256,
                    new WorldSettings(256, GameType.SURVIVAL, false, false, WorldType.DEFAULT),
                    MinecraftServer.getServer().theProfiler) {

                @Override
                public File getChunkSaveLocation() {
                    return new File("dummy-ignoreme");
                }
            };
        }
    }

    private static ItemStack withSize(ItemStack stack, int newSize) {
        stack.stackSize = newSize;
        return stack;
    }

    private void simulateJobAndCheck(CraftingJobV2 job, int maxSteps) {
        for (int step = 0; step < maxSteps && job.simulateFor(0); step++) {}

        assertTrue(job.isDone());
        assertFalse(job.isCancelled());
    }

    private void assertJobPlanEquals(CraftingJobV2 job, IAEItemStack... stacks) {
        assertTrue(job.isDone());
        ItemList plan = new ItemList();
        job.populatePlan(plan);
        for (IAEItemStack stack : stacks) {
            IAEItemStack matching = plan.findPrecise(stack);
            assertNotNull(matching, stack::toString);
            assertEquals(stack.getStackSize(), matching.getStackSize(), () -> "Stack size of " + stack);
            assertEquals(
                    stack.getCountRequestable(),
                    matching.getCountRequestable(),
                    () -> "Requestable count of " + stack);
            matching.setStackSize(0);
            matching.setCountRequestable(0);
        }
        for (IAEItemStack planStack : plan) {
            assertEquals(0, planStack.getStackSize(), () -> "Extra item in the plan: " + planStack);
            assertEquals(0, planStack.getCountRequestable(), () -> "Extra item in the plan: " + planStack);
        }
    }

    private static void collectMatchingRequests(CraftingRequest request, IAEItemStack needle,
            List<CraftingRequest> out) {
        if (request.stack.isSameType(needle)) {
            out.add(request);
        }
        for (UsedResolverEntry resolver : request.usedResolvers) {
            if (resolver.task instanceof CraftFromPatternTask task) {
                for (CraftingRequest childRequest : task.getChildRequests()) {
                    collectMatchingRequests(childRequest, needle, out);
                }
            }
        }
    }

    private static List<CraftingRequest> getMatchingRequests(CraftingJobV2 job, ItemStack stack) {
        final CraftingJobV2 deserialized = CraftingJobV2.deserialize(dummyWorld, job.serialize());
        assertNotNull(deserialized);
        final ArrayList<CraftingRequest> matching = new ArrayList<>();
        collectMatchingRequests(deserialized.originalRequest, AEItemStack.create(stack), matching);
        return matching;
    }

    private void addDummyGappleRecipe(MockAESystem aeSystem) {
        aeSystem.newProcessingPattern().addInput(new ItemStack(Items.gold_ingot, 1))
                .addOutput(new ItemStack(Items.golden_apple, 1)).buildAndAdd();
    }

    @Test
    void noPatternSimulation() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertTrue(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(new ItemStack(Items.stick, 13)));
        assertJobPlanEquals(job, AEItemStack.create(new ItemStack(Items.stick, 13)));
    }

    @Test
    void simplePatternSimulation() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        // Very expensive sticks
        aeSystem.newProcessingPattern().addInput(new ItemStack(Items.diamond, 1))
                .addOutput(new ItemStack(Items.stick, 1)).buildAndAdd();
        // Another pattern that shouldn't match
        addDummyGappleRecipe(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertTrue(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(new ItemStack(Items.stick, 13)));
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 13)));
    }

    @Test
    void noPatternWithItemsSimulation() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.stick, 64));
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 64));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertTrue(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(new ItemStack(Items.stick, 13)));
        assertJobPlanEquals(job, AEItemStack.create(new ItemStack(Items.stick, 13)));
    }

    @Test
    void simplePatternWithItemsSimulation() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 64));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        // Very expensive sticks
        aeSystem.newProcessingPattern().addInput(new ItemStack(Items.diamond, 1))
                .addOutput(new ItemStack(Items.stick, 1)).buildAndAdd();
        // Another pattern that shouldn't match
        addDummyGappleRecipe(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(new ItemStack(Items.stick, 13)));
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 13)));
    }

    private void addPlankPatterns(MockAESystem aeSystem) {
        // Add all types of wood
        for (int meta = 0; meta < 4; meta++) {
            aeSystem.newCraftingPattern().allowBeingASubstitute().addInput(new ItemStack(Blocks.log, 1, meta))
                    .addOutput(new ItemStack(Blocks.planks, 4, meta)).buildAndAdd();
        }
    }

    private void addFuzzyChestPattern(MockAESystem aeSystem) {
        aeSystem.newCraftingPattern().allowUsingSubstitutes()
                // row 1
                .addInput(new ItemStack(Blocks.planks, 1)).addInput(new ItemStack(Blocks.planks, 1))
                .addInput(new ItemStack(Blocks.planks, 1))
                // row 2
                .addInput(new ItemStack(Blocks.planks, 1)).addInput(null).addInput(new ItemStack(Blocks.planks, 1))
                // row 3
                .addInput(new ItemStack(Blocks.planks, 1)).addInput(new ItemStack(Blocks.planks, 1))
                .addInput(new ItemStack(Blocks.planks, 1))
                // end
                .addOutput(new ItemStack(Blocks.chest, 1)).buildAndAdd();
    }

    private void addFuzzyProcessingChestPattern(MockAESystem aeSystem) {
        aeSystem.newProcessingPattern().allowUsingSubstitutes().addInput(new ItemStack(Blocks.planks, 8, 0))
                .addOutput(new ItemStack(Blocks.chest, 1)).buildAndAdd();
    }

    private void addFuzzyBedPattern(MockAESystem aeSystem) {
        aeSystem.newCraftingPattern().allowUsingSubstitutes()
                // row 1
                .addInput(new ItemStack(Blocks.wool, 1, 0)).addInput(new ItemStack(Blocks.wool, 1, 0))
                .addInput(new ItemStack(Blocks.wool, 1, 0))
                // row 2
                .addInput(new ItemStack(Blocks.planks, 1, 0)).addInput(new ItemStack(Blocks.planks, 1, 0))
                .addInput(new ItemStack(Blocks.planks, 1, 0))
                // end
                .addOutput(new ItemStack(Items.bed, 1)).buildAndAdd();
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    void craftChestFromLogs(int woodMetadata) {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Blocks.log, 64, woodMetadata));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addPlankPatterns(aeSystem);
        addFuzzyChestPattern(aeSystem);
        // Another pattern that shouldn't match
        addDummyGappleRecipe(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Blocks.chest, 1));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(new ItemStack(Blocks.chest, 1)));
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Blocks.log, 2, woodMetadata)),
                AEItemStack.create(new ItemStack(Blocks.planks, 0, woodMetadata)).setCountRequestable(8),
                AEItemStack.create(new ItemStack(Blocks.chest, 0)).setCountRequestable(1));
    }

    @Test
    void craftChestFromMixedLogs() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Blocks.log, 1, 0));
        aeSystem.addStoredItem(new ItemStack(Blocks.log, 1, 1));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addPlankPatterns(aeSystem);
        addFuzzyChestPattern(aeSystem);
        // Another pattern that shouldn't match
        addDummyGappleRecipe(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Blocks.chest, 1));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(new ItemStack(Blocks.chest, 1)));
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Blocks.log, 1, 0)),
                AEItemStack.create(new ItemStack(Blocks.log, 1, 1)),
                AEItemStack.create(new ItemStack(Blocks.planks, 0, 0)).setCountRequestable(4),
                AEItemStack.create(new ItemStack(Blocks.planks, 0, 1)).setCountRequestable(4),
                AEItemStack.create(new ItemStack(Blocks.chest, 0)).setCountRequestable(1));
    }

    @Test
    void craftingPatternFuzzyCanUseMixedSubstitutes() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Blocks.wool, 1, 0));
        aeSystem.addStoredItem(new ItemStack(Blocks.wool, 2, 1));
        aeSystem.addStoredItem(new ItemStack(Blocks.planks, 3, 0));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addFuzzyBedPattern(aeSystem);
        // Another pattern that shouldn't match
        addDummyGappleRecipe(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.bed, 1));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(new ItemStack(Items.bed, 1)));
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Blocks.wool, 1, 0)),
                AEItemStack.create(new ItemStack(Blocks.wool, 2, 1)),
                AEItemStack.create(new ItemStack(Blocks.planks, 3, 0)),
                AEItemStack.create(new ItemStack(Items.bed, 0)).setCountRequestable(1));
    }

    @Test
    void processingPatternFuzzySubstitutesCompleteInputGroupsOnly() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Blocks.planks, 12, 0));
        aeSystem.addStoredItem(new ItemStack(Blocks.planks, 20, 1));
        aeSystem.addStoredItem(new ItemStack(Blocks.planks, 8, 2));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addFuzzyProcessingChestPattern(aeSystem);
        // Another pattern that shouldn't match
        addDummyGappleRecipe(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Blocks.chest, 4));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(new ItemStack(Blocks.chest, 4)));
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Blocks.planks, 8, 0)),
                AEItemStack.create(new ItemStack(Blocks.planks, 16, 1)),
                AEItemStack.create(new ItemStack(Blocks.planks, 8, 2)),
                AEItemStack.create(new ItemStack(Blocks.chest, 0)).setCountRequestable(4));
    }

    @Test
    void processingPatternFuzzyDoesNotTreatPartialGroupsAsCraftable() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Blocks.planks, 12, 0));
        aeSystem.addStoredItem(new ItemStack(Blocks.planks, 20, 1));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addFuzzyProcessingChestPattern(aeSystem);
        // Another pattern that shouldn't match
        addDummyGappleRecipe(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Blocks.chest, 4));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertTrue(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(new ItemStack(Blocks.chest, 4)));
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Blocks.planks, 16, 0)),
                AEItemStack.create(new ItemStack(Blocks.planks, 16, 1)),
                AEItemStack.create(new ItemStack(Blocks.chest, 0)).setCountRequestable(4));
    }

    @Test
    void canHandleCyclicalPatterns() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Blocks.log, 4, 0));
        aeSystem.newProcessingPattern().addInput(new ItemStack(Blocks.log, 1))
                .addOutput(new ItemStack(Blocks.planks, 4)).buildAndAdd();
        aeSystem.newProcessingPattern().addInput(new ItemStack(Blocks.planks, 4))
                .addOutput(new ItemStack(Blocks.log, 1)).buildAndAdd();
        for (int plankAmount = 1; plankAmount < 64; plankAmount++) {
            final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Blocks.planks, plankAmount));
            simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
            assertEquals(job.isSimulation(), plankAmount > 16);
        }
    }

    @Test
    void strictNamedItems() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Blocks.log, 4, 0).setStackDisplayName("Named Log"));
        aeSystem.newProcessingPattern().addInput(new ItemStack(Blocks.log, 1))
                .addOutput(new ItemStack(Blocks.planks, 4)).allowBeingASubstitute().buildAndAdd();

        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Blocks.planks, 1));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertEquals(true, job.isSimulation()); // Don't use renamed items
    }

    private void addHammerBronzePlateRecipe(MockAESystem aeSystem) {
        aeSystem.newCraftingPattern().allowBeingASubstitute().allowUsingSubstitutes() //
                .addInput(gtHammer.copy()).addInput(null).addInput(null) //
                .addInput(bronzeIngot.copy()).addInput(null).addInput(null) //
                .addInput(bronzeIngot.copy()).addInput(null).addInput(null) //
                .addOutput(bronzePlate.copy()).buildAndAdd();
    }

    private void addSingleUseHammerPlateRecipe(MockAESystem aeSystem, ItemStack ingot, ItemStack plate) {
        aeSystem.newCraftingPattern().allowBeingASubstitute().allowUsingSubstitutes() //
                .addInput(singleUseGtHammer.copy()).addInput(null).addInput(null) //
                .addInput(ingot.copy()).addInput(null).addInput(null) //
                .addInput(ingot.copy()).addInput(null).addInput(null) //
                .addOutput(plate.copy()).buildAndAdd();
    }

    private void addHammerBronzeDoublePlateRecipe(MockAESystem aeSystem) {
        aeSystem.newCraftingPattern().allowBeingASubstitute().allowUsingSubstitutes() //
                .addInput(bronzePlate.copy()).addInput(null).addInput(null) //
                .addInput(bronzePlate.copy()).addInput(null).addInput(null) //
                .addInput(gtHammer.copy()).addInput(null).addInput(null) //
                .addOutput(bronzeDoublePlate.copy()).buildAndAdd();
    }

    private ItemStack getMixedMetalIngot() {
        return new ItemStack(GameRegistry.findItem("IC2", "itemIngot"), 1, 4);
    }

    private void addMixedMetalRecipe(MockAESystem aeSystem) {
        final ItemStack mixedMetalIngot = getMixedMetalIngot();
        if (!mixedMetalTestRecipeRegistered) {
            mixedMetalTestRecipeRegistered = true;
            GameRegistry.addRecipe(
                    mixedMetalIngot.copy(),
                    "P  ",
                    "B  ",
                    "T  ",
                    'P',
                    pigIronPlate.copy(),
                    'B',
                    bronzePlate.copy(),
                    'T',
                    tinPlate.copy());
        }

        aeSystem.newCraftingPattern().allowUsingSubstitutes() //
                .addInput(pigIronPlate.copy()).addInput(null).addInput(null) //
                .addInput(bronzePlate.copy()).addInput(null).addInput(null) //
                .addInput(tinPlate.copy()).addInput(null).addInput(null) //
                .addOutput(mixedMetalIngot).buildAndAdd();
    }

    @Test
    void canCraftWithGtTool() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(gtHammer.copy());
        aeSystem.addStoredItem(withSize(bronzeIngot.copy(), 2));
        addHammerBronzePlateRecipe(aeSystem);

        final CraftingJobV2 job = aeSystem.makeCraftingJob(bronzePlate);
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertEquals(false, job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(gtHammer.copy()),
                AEItemStack.create(withSize(bronzeIngot.copy(), 2)),
                AEItemStack.create(withSize(bronzePlate.copy(), 0)).setCountRequestable(1));
    }

    @Test
    void canCraft2WithGtTool() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(gtHammer.copy());
        aeSystem.addStoredItem(withSize(bronzeIngot.copy(), 4));
        addHammerBronzePlateRecipe(aeSystem);

        final CraftingJobV2 job = aeSystem.makeCraftingJob(withSize(bronzePlate.copy(), 2));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertEquals(false, job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(gtHammer.copy()),
                AEItemStack.create(withSize(bronzeIngot.copy(), 4)),
                AEItemStack.create(withSize(bronzePlate.copy(), 0)).setCountRequestable(2));
    }

    @Test
    void canReuseGtToolSubstitutingSingleUseHammerBetweenMixedMetalPlateCrafts() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(gtHammer.copy());
        aeSystem.addStoredItem(withSize(bronzeIngot.copy(), 200));
        aeSystem.addStoredItem(withSize(tinIngot.copy(), 200));
        aeSystem.addStoredItem(withSize(pigIronIngot.copy(), 200));
        addSingleUseHammerPlateRecipe(aeSystem, bronzeIngot, bronzePlate);
        addSingleUseHammerPlateRecipe(aeSystem, tinIngot, tinPlate);
        addSingleUseHammerPlateRecipe(aeSystem, pigIronIngot, pigIronPlate);
        addMixedMetalRecipe(aeSystem);

        final CraftingJobV2 job = aeSystem.makeCraftingJob(withSize(getMixedMetalIngot(), 100));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertEquals(false, job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(gtHammer.copy()),
                AEItemStack.create(withSize(bronzeIngot.copy(), 200)),
                AEItemStack.create(withSize(tinIngot.copy(), 200)),
                AEItemStack.create(withSize(pigIronIngot.copy(), 200)),
                AEItemStack.create(withSize(bronzePlate.copy(), 0)).setCountRequestable(100),
                AEItemStack.create(withSize(tinPlate.copy(), 0)).setCountRequestable(100),
                AEItemStack.create(withSize(pigIronPlate.copy(), 0)).setCountRequestable(100),
                AEItemStack.create(withSize(getMixedMetalIngot(), 0)).setCountRequestable(100));
    }

    @Test
    void canCraftDoublePlateWithGtTool() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(gtHammer.copy());
        aeSystem.addStoredItem(withSize(bronzeIngot.copy(), 4));
        addHammerBronzePlateRecipe(aeSystem);
        addHammerBronzeDoublePlateRecipe(aeSystem);

        final CraftingJobV2 job = aeSystem.makeCraftingJob(bronzeDoublePlate);
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertEquals(false, job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(gtHammer.copy()),
                AEItemStack.create(withSize(bronzeIngot.copy(), 4)),
                AEItemStack.create(withSize(bronzePlate.copy(), 0)).setCountRequestable(2),
                AEItemStack.create(withSize(bronzeDoublePlate.copy(), 0)).setCountRequestable(1));
    }

    @Test
    void canCraft2WithGtToolMissing1() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(gtHammer.copy());
        aeSystem.addStoredItem(withSize(bronzeIngot.copy(), 2));
        addHammerBronzePlateRecipe(aeSystem);

        final CraftingJobV2 job = aeSystem.makeCraftingJob(withSize(bronzePlate.copy(), 2));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertEquals(true, job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(gtHammer.copy()),
                AEItemStack.create(withSize(bronzeIngot.copy(), 4)),
                AEItemStack.create(withSize(bronzePlate.copy(), 0)).setCountRequestable(2));
    }

    @Test
    void partialMissingAmount() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(withSize(bronzePlate.copy(), 1));
        aeSystem.newProcessingPattern().addInput(withSize(bronzePlate.copy(), 2)).addOutput(bronzeDoublePlate.copy())
                .buildAndAdd();

        final CraftingJobV2 job = aeSystem.makeCraftingJob(withSize(bronzeDoublePlate.copy(), 1));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertEquals(true, job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(withSize(bronzePlate.copy(), 2)),
                AEItemStack.create(withSize(bronzeDoublePlate.copy(), 0)).setCountRequestable(1));
    }

    @Test
    void differentMissingAmounts() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(withSize(ironIngot, 32));
        aeSystem.addStoredItem(withSize(goldIngot, 64));
        aeSystem.newProcessingPattern().addInput(withSize(ironIngot, 2)).addInput(new ItemStack(Items.gold_ingot, 2))
                .addOutput(new ItemStack(Blocks.gold_block)).buildAndAdd();

        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Blocks.gold_block, 100));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertEquals(true, job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(withSize(goldIngot, 200)),
                AEItemStack.create(withSize(ironIngot, 200)),
                AEItemStack.create(withSize(goldBlock, 0)).setCountRequestable(100));
    }

    @Test
    void complexRecipeChain() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(withSize(ironDust, 2 * 64));
        aeSystem.addStoredItem(withSize(goldDust, 3 * 64));
        aeSystem.newProcessingPattern().addInput(withSize(ironDust, 2)) //
                .addOutput(withSize(ironIngot, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(ironIngot, 1)) //
                .addOutput(withSize(ironPlate, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(ironPlate, 1)) //
                .addInput(withSize(goldDust, 2)) //
                .addOutput(withSize(goldIngot, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(goldIngot, 9)) //
                .addInput(withSize(ironPlate, 1)) //
                .addOutput(withSize(goldBlock, 1)) //
                .buildAndAdd();

        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Blocks.gold_block, 100));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertEquals(true, job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(withSize(ironDust, 2000)),
                AEItemStack.create(withSize(goldDust, 1800)),
                AEItemStack.create(withSize(ironIngot, 0)).setCountRequestable(1000),
                AEItemStack.create(withSize(ironPlate, 0)).setCountRequestable(1000),
                AEItemStack.create(withSize(goldIngot, 0)).setCountRequestable(900),
                AEItemStack.create(withSize(goldBlock, 0)).setCountRequestable(100));
    }

    @Test
    void competingRecipe() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(withSize(ironDust, 4));
        aeSystem.addStoredItem(withSize(goldDust, 1));
        aeSystem.newProcessingPattern().addInput(withSize(ironDust, 2)) //
                .addOutput(withSize(ironIngot, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(ironIngot, 1)) //
                .addOutput(withSize(ironPlate, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(goldDust, 1)) //
                .addOutput(withSize(ironPlate, 1)) //
                .setPriority(1) //
                .buildAndAdd();

        final CraftingJobV2 job = aeSystem.makeCraftingJob(withSize(ironPlate, 3));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(withSize(ironPlate, 3)));
        assertJobPlanEquals(
                job,
                AEItemStack.create(withSize(ironDust, 4)),
                AEItemStack.create(withSize(goldDust, 1)),
                AEItemStack.create(withSize(ironIngot, 0)).setCountRequestable(2),
                AEItemStack.create(withSize(ironPlate, 0)).setCountRequestable(3));

        final CraftingJobV2 jobFailed = aeSystem.makeCraftingJob(withSize(ironPlate, 4));
        simulateJobAndCheck(jobFailed, MAX_SIMULATION_STEPS);
        assertTrue(jobFailed.isSimulation());
        assertJobPlanEquals(
                jobFailed,
                AEItemStack.create(withSize(ironDust, 6)),
                AEItemStack.create(withSize(goldDust, 1)),
                AEItemStack.create(withSize(ironIngot, 0)).setCountRequestable(3),
                AEItemStack.create(withSize(ironPlate, 0)).setCountRequestable(4));
    }

    @Test
    void competingRecipeChain() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(withSize(ironDust, 4));
        aeSystem.addStoredItem(withSize(goldDust, 1));
        aeSystem.newProcessingPattern().addInput(withSize(ironDust, 2)) //
                .addOutput(withSize(ironIngot, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(ironIngot, 1)) //
                .addOutput(withSize(ironPlate, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(goldDust, 1)) //
                .addOutput(withSize(goldIngot, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(goldIngot, 1)) //
                .addOutput(withSize(ironPlate, 1)) //
                .setPriority(1) //
                .buildAndAdd();

        final CraftingJobV2 job = aeSystem.makeCraftingJob(withSize(ironPlate, 3));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(withSize(ironPlate, 3)));
        assertJobPlanEquals(
                job,
                AEItemStack.create(withSize(ironDust, 4)),
                AEItemStack.create(withSize(goldDust, 1)),
                AEItemStack.create(withSize(ironIngot, 0)).setCountRequestable(2),
                AEItemStack.create(withSize(goldIngot, 0)).setCountRequestable(1),
                AEItemStack.create(withSize(ironPlate, 0)).setCountRequestable(3));

        final CraftingJobV2 jobFailed = aeSystem.makeCraftingJob(withSize(ironPlate, 4));
        simulateJobAndCheck(jobFailed, MAX_SIMULATION_STEPS);
        assertTrue(jobFailed.isSimulation());
        assertJobPlanEquals(
                jobFailed,
                AEItemStack.create(withSize(ironDust, 6)),
                AEItemStack.create(withSize(goldDust, 1)),
                AEItemStack.create(withSize(ironIngot, 0)).setCountRequestable(3),
                AEItemStack.create(withSize(goldIngot, 0)).setCountRequestable(1),
                AEItemStack.create(withSize(ironPlate, 0)).setCountRequestable(4));
    }

    @Test
    void competingRecipeChainTreeUsesActualRequestedAmounts() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(withSize(ironDust, 4));
        aeSystem.addStoredItem(withSize(goldDust, 1));
        aeSystem.newProcessingPattern().addInput(withSize(ironDust, 2)) //
                .addOutput(withSize(ironIngot, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(ironIngot, 1)) //
                .addOutput(withSize(ironPlate, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(goldDust, 1)) //
                .addOutput(withSize(goldIngot, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(goldIngot, 1)) //
                .addOutput(withSize(ironPlate, 1)) //
                .setPriority(1) //
                .buildAndAdd();

        final CraftingJobV2 job = aeSystem.makeCraftingJob(withSize(ironPlate, 3));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());

        final List<CraftingRequest> goldIngotRequests = getMatchingRequests(job, withSize(goldIngot.copy(), 1));
        assertEquals(1, goldIngotRequests.size());
        assertEquals(1, goldIngotRequests.get(0).stack.getStackSize());
        assertEquals(0, goldIngotRequests.get(0).remainingToProcess);

        final List<CraftingRequest> goldDustRequests = getMatchingRequests(job, withSize(goldDust.copy(), 1));
        assertEquals(1, goldDustRequests.size());
        assertEquals(1, goldDustRequests.get(0).stack.getStackSize());
        assertEquals(0, goldDustRequests.get(0).remainingToProcess);
    }

    private void addEqualPriorityPlateRecipes(MockAESystem aeSystem) {
        aeSystem.newProcessingPattern().addInput(withSize(ironDust, 2)) //
                .addOutput(withSize(ironIngot, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(ironIngot, 1)) //
                .addOutput(withSize(ironPlate, 1)) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(goldDust, 1)) //
                .addOutput(withSize(ironPlate, 1)) //
                .buildAndAdd();
    }

    @Test
    void competingRecipeSamePriority() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(withSize(ironDust, 4));
        aeSystem.addStoredItem(withSize(goldDust, 1));
        addEqualPriorityPlateRecipes(aeSystem);

        final CraftingJobV2 job = aeSystem.makeCraftingJob(withSize(ironPlate, 3));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertEquals(job.getOutput(), AEItemStack.create(withSize(ironPlate, 3)));
        assertJobPlanEquals(
                job,
                AEItemStack.create(withSize(ironDust, 4)),
                AEItemStack.create(withSize(goldDust, 1)),
                AEItemStack.create(withSize(ironIngot, 0)).setCountRequestable(2),
                AEItemStack.create(withSize(ironPlate, 0)).setCountRequestable(3));

        final CraftingJobV2 jobFailed = aeSystem.makeCraftingJob(withSize(ironPlate, 4));
        simulateJobAndCheck(jobFailed, MAX_SIMULATION_STEPS);
        assertTrue(jobFailed.isSimulation());
        assertJobPlanEquals(
                jobFailed,
                AEItemStack.create(withSize(ironDust, 6)),
                AEItemStack.create(withSize(goldDust, 1)),
                AEItemStack.create(withSize(ironIngot, 0)).setCountRequestable(3),
                AEItemStack.create(withSize(ironPlate, 0)).setCountRequestable(4));
    }

    @Test
    void competingRecipeSamePriorityIronDustOnly() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(withSize(ironDust, 2));
        addEqualPriorityPlateRecipes(aeSystem);

        final CraftingJobV2 job = aeSystem.makeCraftingJob(withSize(ironPlate, 1));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(withSize(ironDust, 2)),
                AEItemStack.create(withSize(ironIngot, 0)).setCountRequestable(1),
                AEItemStack.create(withSize(ironPlate, 0)).setCountRequestable(1));
    }

    @Test
    void competingRecipeSamePriorityGoldDustOnly() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(withSize(goldDust, 1));
        addEqualPriorityPlateRecipes(aeSystem);

        final CraftingJobV2 job = aeSystem.makeCraftingJob(withSize(ironPlate, 1));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(withSize(goldDust, 1)),
                AEItemStack.create(withSize(ironPlate, 0)).setCountRequestable(1));
    }

    @Test
    void competingRecipePatternsListedByPriority() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        addEqualPriorityPlateRecipes(aeSystem);
        aeSystem.newProcessingPattern().addInput(withSize(goldIngot, 1)) //
                .addOutput(withSize(ironPlate, 1)) //
                .setPriority(1) //
                .buildAndAdd();
        aeSystem.newProcessingPattern().addInput(withSize(ironDust, 1)) //
                .addOutput(withSize(ironPlate, 1)) //
                .setPriority(-1) //
                .buildAndAdd();
        aeSystem.cgCache.setMockPatternsFromMethods();

        final List<ICraftingPatternDetails> platePatterns = new ArrayList<>(
                aeSystem.cgCache.getCraftingFor(AEItemStack.create(withSize(ironPlate, 1)), null, 0, dummyWorld));
        assertEquals(4, platePatterns.size());
        assertEquals(1, platePatterns.get(0).getPriority());
        assertEquals(0, platePatterns.get(1).getPriority());
        assertEquals(0, platePatterns.get(2).getPriority());
        assertEquals(-1, platePatterns.get(3).getPriority());
    }

    private void addExpensiveStickPatterns(MockAESystem aeSystem) {
        aeSystem.newProcessingPattern().addInput(new ItemStack(Items.diamond, 1))
                .addOutput(new ItemStack(Items.stick, 1)).buildAndAdd();
        aeSystem.newProcessingPattern().addInput(new ItemStack(Items.gold_ingot, 1))
                .addOutput(new ItemStack(Items.diamond, 1)).buildAndAdd();
    }

    @Test
    void itemsRemovedDuringCalculationAreCrafted() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addExpensiveStickPatterns(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13));
        // The job already took its snapshot of the storage
        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 5));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 8)).setCountRequestable(5),
                AEItemStack.create(new ItemStack(Items.gold_ingot, 5)));
    }

    @Test
    void itemsRemovedAfterCalculationAreReplanned() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 10));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addExpensiveStickPatterns(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 10)).setCountRequestable(3),
                AEItemStack.create(new ItemStack(Items.gold_ingot, 3)));
        assertFalse(job.reconcileWithLiveStorage(), "Unchanged storage should not re-plan");

        // The diamond request was already partially crafted, crafting the rest must still be possible
        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 4));
        assertTrue(job.reconcileWithLiveStorage());
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 6)).setCountRequestable(7),
                AEItemStack.create(new ItemStack(Items.gold_ingot, 7)));
        assertFalse(job.reconcileWithLiveStorage(), "Re-planned job should match the storage");
    }

    @Test
    void itemsRemovedRepeatedlyAreCraftedEachTime() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addExpensiveStickPatterns(aeSystem);
        // Player requests get several attempts, there is no player to send chat messages to
        final CraftingJobV2 job = aeSystem
                .makeCraftingJob(new ItemStack(Items.stick, 13), new PlayerSource(null, null));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());

        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 5));
        assertTrue(job.reconcileWithLiveStorage());
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 8)).setCountRequestable(5),
                AEItemStack.create(new ItemStack(Items.gold_ingot, 5)));

        // The diamond request is now partially crafted, the pattern it used must stay usable for the next shortfall
        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 3));
        assertTrue(job.reconcileWithLiveStorage());
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 5)).setCountRequestable(8),
                AEItemStack.create(new ItemStack(Items.gold_ingot, 8)));
        assertFalse(job.reconcileWithLiveStorage(), "Re-planned job should match the storage");
    }

    @Test
    void replanUsesLiveStorageForNewIngredients() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addExpensiveStickPatterns(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());

        // The gold wasn't part of the plan, but the re-plan needs it and must not use the old snapshot
        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 5));
        aeSystem.removeStoredItem(new ItemStack(Items.gold_ingot, 64));
        assertTrue(job.reconcileWithLiveStorage());
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertTrue(job.isSimulation(), "Gold that is gone must not be planned as extracted");
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 8)).setCountRequestable(5),
                AEItemStack.create(new ItemStack(Items.gold_ingot, 5)));
        assertEquals(
                0,
                job.getStorageAtBeginning().getAvailableItem(AEItemStack.create(new ItemStack(Items.gold_ingot, 1)))
                        .getStackSize());
    }

    @Test
    void replanSeesItemsAddedAfterCalculation() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        addExpensiveStickPatterns(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());

        // Gold inserted after the calculation can be used to craft the missing diamonds
        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 5));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        assertTrue(job.reconcileWithLiveStorage());
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 8)).setCountRequestable(5),
                AEItemStack.create(new ItemStack(Items.gold_ingot, 5)));
    }

    @Test
    void replanKeepsTheRequestedItemOutOfTheItemModel() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.stick, 10));
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addExpensiveStickPatterns(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        // Stored sticks are never extracted to fulfill a stick request
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 13)));

        // More sticks arrive while diamonds go missing. The snapshot has to learn about the sticks, the item model
        // must not, otherwise a re-planned sub-request could extract the item the job is crafting.
        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 5));
        aeSystem.addStoredItem(new ItemStack(Items.stick, 20));
        assertTrue(job.reconcileWithLiveStorage());
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        final IAEItemStack stick = AEItemStack.create(new ItemStack(Items.stick, 1));
        final IAEItemStack modelledSticks = job.getContext().itemModel.findPrecise(stick);
        assertTrue(
                modelledSticks == null || modelledSticks.getStackSize() == 0,
                () -> "The requested item leaked back into the item model: " + modelledSticks);
        assertEquals(30, job.getStorageAtBeginning().getAvailableItem(stick).getStackSize());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 8)).setCountRequestable(5),
                AEItemStack.create(new ItemStack(Items.gold_ingot, 5)));
    }

    @Test
    void fuzzyExtractionReplansAfterPartialRefund() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Blocks.planks, 8, 0));
        aeSystem.addStoredItem(new ItemStack(Blocks.planks, 16, 1));
        addFuzzyProcessingChestPattern(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Blocks.chest, 3));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Blocks.planks, 8, 0)),
                AEItemStack.create(new ItemStack(Blocks.planks, 16, 1)),
                AEItemStack.create(new ItemStack(Blocks.chest, 0)).setCountRequestable(3));

        // A refund by the parent hands back the fuzzy substitute first, the exact match must still be accounted for
        final CraftingRequest planks = job.getContext().getLiveRequests().stream().map(r -> r.request)
                .filter(r -> r.stack.equals(AEItemStack.create(new ItemStack(Blocks.planks, 1, 0)))).findFirst()
                .orElseThrow(AssertionError::new);
        planks.partialRefund(job.getContext(), 8);

        aeSystem.removeStoredItem(new ItemStack(Blocks.planks, 8, 0));
        assertTrue(job.reconcileWithLiveStorage());
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Blocks.planks, 16, 1)),
                AEItemStack.create(new ItemStack(Blocks.chest, 0)).setCountRequestable(3));
    }

    @Test
    void partialRefundSpansAllEntriesOfAFuzzyExtraction() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Blocks.planks, 16, 0));
        aeSystem.addStoredItem(new ItemStack(Blocks.planks, 8, 1));
        addFuzzyProcessingChestPattern(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Blocks.chest, 3));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Blocks.planks, 16, 0)),
                AEItemStack.create(new ItemStack(Blocks.planks, 8, 1)),
                AEItemStack.create(new ItemStack(Blocks.chest, 0)).setCountRequestable(3));

        // The extract task recorded the exact match before the substitute, but hands the substitute back first. A
        // refund larger than the first entry has to be covered by the task's total, not by that entry alone.
        final CraftingRequest planks = job.getContext().getLiveRequests().stream().map(r -> r.request)
                .filter(r -> r.stack.equals(AEItemStack.create(new ItemStack(Blocks.planks, 1, 0)))).findFirst()
                .orElseThrow(AssertionError::new);
        planks.partialRefund(job.getContext(), 17);
        assertEquals(7, planks.stack.getStackSize());
        assertEquals(0, planks.remainingToProcess);
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Blocks.planks, 7, 0)),
                AEItemStack.create(new ItemStack(Blocks.chest, 0)).setCountRequestable(3));
    }

    @Test
    void craftableItemsWithMissingInputsShowTheInputAsMissing() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        // There is a pattern for diamonds, but no gold to craft them with
        addExpensiveStickPatterns(aeSystem);
        final CraftingJobV2 job = aeSystem
                .makeCraftingJob(new ItemStack(Items.stick, 13), new PlayerSource(null, null));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());

        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 5));
        assertTrue(job.reconcileWithLiveStorage());
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertTrue(job.isSimulation());
        // Like a fresh calculation, the diamonds are crafted and the gold they need is what's missing
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 8)).setCountRequestable(5),
                AEItemStack.create(new ItemStack(Items.gold_ingot, 5)));
    }

    @Test
    void plansWithMissingItemsAreNotReplanned() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        aeSystem.newProcessingPattern().addInput(new ItemStack(Items.diamond, 1))
                .addOutput(new ItemStack(Items.stick, 1)).buildAndAdd();
        final CraftingJobV2 job = aeSystem
                .makeCraftingJob(new ItemStack(Items.stick, 20), new PlayerSource(null, null));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertTrue(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(20),
                AEItemStack.create(new ItemStack(Items.diamond, 20)));

        // The plan can't be started anyway, re-planning it would only spend attempts and spam the player
        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 5));
        assertFalse(job.reconcileWithLiveStorage(), "Plans that are already simulations should not be re-planned");
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(20),
                AEItemStack.create(new ItemStack(Items.diamond, 20)));
    }

    @Test
    void ignoreMissingJobsAreNotReplanned() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addExpensiveStickPatterns(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13), CraftingMode.IGNORE_MISSING);
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());

        // The crafting CPU waits for items that are missing in this mode, a re-plan would make the job unsubmittable
        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 5));
        assertFalse(job.reconcileWithLiveStorage(), "Ignore-missing jobs should not be re-planned");
        assertFalse(job.isSimulation());
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 13)));
    }

    @Test
    void plansThatBecomeSimulationsDuringCalculationAreNotReplanned() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        aeSystem.newProcessingPattern().addInput(new ItemStack(Items.diamond, 1))
                .addOutput(new ItemStack(Items.stick, 1)).buildAndAdd();
        final CraftingJobV2 job = aeSystem
                .makeCraftingJob(new ItemStack(Items.stick, 20), new PlayerSource(null, null));
        // The job already took its snapshot of the storage
        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 5));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertTrue(job.isSimulation());
        // Without the re-plan the 13 diamonds of the snapshot stay planned as extracted
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(20),
                AEItemStack.create(new ItemStack(Items.diamond, 20)));
        assertEquals(
                13,
                job.getStorageAtBeginning().getAvailableItem(AEItemStack.create(new ItemStack(Items.diamond, 1)))
                        .getStackSize());
    }

    @Test
    void machineRequestsRetryOnlyOnce() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addExpensiveStickPatterns(aeSystem);
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());

        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 5));
        assertTrue(job.reconcileWithLiveStorage());
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());

        // The only retry is used up, items going missing again must fail the job instead of starting it short
        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 3));
        assertTrue(job.reconcileWithLiveStorage(), "A second shortfall must still be noticed");
        assertTrue(job.isCancelled(), "Machine requests should only be re-planned once");
        assertTrue(job.isSimulation(), "A cancelled job must not be submittable");
        assertEquals(GuiText.CraftingIngredientsKeptChanging.getUnlocalized(), job.getErrorMessage());
    }

    @Test
    void playerRequestsFailAfterTheLastRetry() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        aeSystem.addStoredItem(new ItemStack(Items.gold_ingot, 64));
        addExpensiveStickPatterns(aeSystem);
        final CraftingJobV2 job = aeSystem
                .makeCraftingJob(new ItemStack(Items.stick, 13), new PlayerSource(null, null));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());

        // Players get 5 re-plans
        for (int round = 1; round <= 5; round++) {
            aeSystem.removeStoredItem(new ItemStack(Items.diamond, 1));
            assertTrue(job.reconcileWithLiveStorage(), "Re-plan " + round + " should happen");
            simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
            assertFalse(job.isSimulation(), "Re-plan " + round + " should craft the missing diamond");
        }
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 8)).setCountRequestable(5),
                AEItemStack.create(new ItemStack(Items.gold_ingot, 5)));

        // No re-plans left: the job must fail rather than start with an extraction the network can't satisfy
        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 1));
        assertTrue(job.reconcileWithLiveStorage(), "The shortfall must still be noticed after the last retry");
        assertTrue(job.isCancelled());
        assertTrue(job.isSimulation());
        assertEquals(GuiText.CraftingIngredientsKeptChanging.getUnlocalized(), job.getErrorMessage());
    }

    @Test
    void uncraftableItemsRemovedAfterCalculationAreMissing() {
        MockAESystem aeSystem = new MockAESystem(dummyWorld);
        aeSystem.addStoredItem(new ItemStack(Items.diamond, 13));
        aeSystem.newProcessingPattern().addInput(new ItemStack(Items.diamond, 1))
                .addOutput(new ItemStack(Items.stick, 1)).buildAndAdd();
        final CraftingJobV2 job = aeSystem.makeCraftingJob(new ItemStack(Items.stick, 13));
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertFalse(job.isSimulation());

        aeSystem.removeStoredItem(new ItemStack(Items.diamond, 5));
        assertTrue(job.reconcileWithLiveStorage());
        simulateJobAndCheck(job, MAX_SIMULATION_STEPS);
        assertTrue(job.isSimulation());
        // 8 extracted and 5 simulated, the confirm screen shows the difference to the storage as missing
        assertJobPlanEquals(
                job,
                AEItemStack.create(new ItemStack(Items.stick, 0)).setCountRequestable(13),
                AEItemStack.create(new ItemStack(Items.diamond, 13)));
        assertEquals(
                8,
                job.getStorageAtBeginning().getAvailableItem(AEItemStack.create(new ItemStack(Items.diamond, 1)))
                        .getStackSize());
    }
}
