package appeng.test.util.partitionlist;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Predicate;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import appeng.api.stacks.AEItemKey;
import appeng.util.partitionlist.OreFilterPartitionList;

public class OreFilterPartitionListFunctionalTest {

    private static AEItemKey ironIngot;
    private static AEItemKey goldIngot;

    @BeforeAll
    public static void registerTestOres() {
        OreDictionary.registerOre("ingotIron", new ItemStack(Items.iron_ingot));
        OreDictionary.registerOre("ingotGold", new ItemStack(Items.gold_ingot));

        ironIngot = AEItemKey.of(new ItemStack(Items.iron_ingot));
        goldIngot = AEItemKey.of(new ItemStack(Items.gold_ingot));
    }

    @Test
    public void repeatedConjunctiveOperandRejectsAllItems() {
        Predicate<AEItemKey> filter = OreFilterPartitionList.makeFilter("!ingotIron&ingotIron");

        assertNotNull(filter);
        assertFalse(filter.test(ironIngot));
        assertFalse(filter.test(goldIngot));
    }
}
