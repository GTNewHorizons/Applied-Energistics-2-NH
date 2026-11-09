package appeng.test.api.features.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;

import appeng.api.config.AccessRestriction;
import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.features.transfer.TransferComponentInfo;
import appeng.api.features.transfer.TransferDescription;
import appeng.api.features.transfer.TransferMatcher;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TransferComponentInfoFunctionalTest {

    private static TransferComponentInfo roundTrip(final TransferComponentInfo info) {
        final ByteBuf buffer = Unpooled.buffer();
        info.write(buffer);
        final TransferComponentInfo read = TransferComponentInfo.read(buffer);
        assertEquals(0, buffer.readableBytes(), "The whole row should be consumed");
        return read;
    }

    @Test
    public void fullRowSurvivesTheNetwork() {
        final AEItemStack iron = AEItemStack.create(new ItemStack(Items.iron_ingot, 1));
        final AEFluidStack water = AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 1));
        final TransferDescription description = TransferDescription.builder()
                .setting(Settings.ACCESS, AccessRestriction.READ)
                .setting(Settings.REDSTONE_CONTROLLED, RedstoneMode.LOW_SIGNAL).upgrade(new ItemStack(Items.diamond, 2))
                .config(iron, true).config(water, false).hasCraftingCard(true).canImport(true).canExport(false)
                .sticky(true).storageHandler(true).priority(-7).oreFilter("ingot*").fuzzyMode(FuzzyMode.PERCENT_50)
                .build();

        final TransferComponentInfo read = roundTrip(
                new TransferComponentInfo(
                        new ItemStack(Items.apple, 1),
                        "My Bus",
                        "{\"text\":\"x\"}",
                        1,
                        -2,
                        3,
                        -4,
                        (byte) 5,
                        6,
                        description));

        assertEquals(Items.apple, read.selfRep.getItem());
        assertEquals("My Bus", read.customName);
        assertEquals("{\"text\":\"x\"}", read.nameSuffixJson);
        assertEquals(1, read.x);
        assertEquals(-2, read.y);
        assertEquals(3, read.z);
        assertEquals(-4, read.dim);
        assertEquals(5, read.side);
        assertEquals(6, read.networkTier);

        final TransferDescription result = read.description;
        assertEquals(2, result.settings.size());
        assertSame(Settings.ACCESS, result.settings.get(0).setting);
        assertSame(AccessRestriction.READ, result.settings.get(0).value);
        assertSame(RedstoneMode.LOW_SIGNAL, result.settings.get(1).value);
        assertEquals(2, result.upgrades.get(0).stackSize);
        assertEquals(iron, result.config.get(0).stack);
        assertTrue(result.config.get(0).fuzzy);
        assertEquals(water, result.config.get(1).stack);
        assertFalse(result.config.get(1).fuzzy);
        assertTrue(result.hasCraftingCard);
        assertTrue(result.canImport);
        assertFalse(result.canExport);
        assertTrue(result.sticky);
        assertTrue(result.storageHandler);
        assertTrue(result.hasPriority);
        assertEquals(-7, result.priority);
        assertEquals("ingot*", result.oreFilter);
    }

    @Test
    public void emptyRowSurvivesTheNetwork() {
        final TransferComponentInfo read = roundTrip(
                new TransferComponentInfo(
                        null,
                        "",
                        null,
                        0,
                        0,
                        0,
                        0,
                        (byte) 0,
                        0,
                        TransferDescription.builder().build()));

        assertNull(read.selfRep);
        assertNull(read.nameSuffixJson);
        assertFalse(read.description.hasPriority);
        assertNull(read.description.oreFilter);
        assertTrue(read.description.config.isEmpty());
    }

    @Test
    public void matcherStaysOnTheServer() {
        final TransferDescription description = TransferDescription.builder().matcher((stack, direction) -> true)
                .build();

        final TransferComponentInfo read = roundTrip(
                new TransferComponentInfo(null, "", null, 0, 0, 0, 0, (byte) 0, 0, description));

        assertSame(TransferMatcher.NONE, read.description.matcher);
    }

    @Test
    public void queryMarksMatchingConfigEntriesAndSurvivesTheNetwork() {
        final AEItemStack iron = AEItemStack.create(new ItemStack(Items.iron_ingot, 1));
        final AEItemStack gold = AEItemStack.create(new ItemStack(Items.gold_ingot, 1));
        final TransferMatcher listed = new TransferMatcher() {

            @Override
            public boolean canTransfer(final appeng.api.storage.data.IAEStack<?> stack,
                    final appeng.api.config.TransferDirection direction) {
                return true;
            }

            @Override
            public boolean isExplicitlyListed(final appeng.api.storage.data.IAEStack<?> stack) {
                return true;
            }
        };
        final TransferDescription description = TransferDescription.builder().config(iron, false).config(gold, false)
                .matcher(listed).build();

        final TransferComponentInfo queried = new TransferComponentInfo(
                null,
                "",
                null,
                0,
                0,
                0,
                0,
                (byte) 0,
                0,
                description).withQuery(gold);

        assertTrue(queried.explicitlyListed);
        final TransferComponentInfo read = roundTrip(queried);
        assertFalse(read.description.config.get(0).matchesQuery);
        assertTrue(read.description.config.get(1).matchesQuery);
        assertFalse(queried.withQuery(null).description.config.get(1).matchesQuery);
    }
}
