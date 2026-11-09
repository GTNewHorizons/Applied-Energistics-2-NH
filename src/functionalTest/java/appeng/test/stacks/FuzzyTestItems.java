package appeng.test.stacks;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Items for fuzzy search tests. Unlike vanilla items, other mods register no ore dictionary entries for them.
 */
public final class FuzzyTestItems {

    static final String WEAR = "Wear";
    static final String MAX_WEAR = "MaxWear";

    static final String WILDCARD_ORE = "aeFuzzyTestWildcard";
    static final String SHARED_ORE = "aeFuzzyTestShared";
    static final String TOOL_ORE = "aeFuzzyTestTool";

    /** Damageable with a max damage of 100. */
    static final Item TOOL = new WearTool();
    static final Item VARIANTS = new Item().setHasSubtypes(true);
    /** Every damage is in {@link #WILDCARD_ORE}. */
    static final Item WILDCARD_ORE_ITEM = new Item().setHasSubtypes(true);
    /** Damage 0 is in both {@link #WILDCARD_ORE} and {@link #SHARED_ORE}. */
    static final Item SHARED_ORE_ITEM = new Item().setHasSubtypes(true);
    /** Damageable with a max damage of 100, and every damage is in {@link #TOOL_ORE}. */
    static final Item ORE_TOOL = new Item().setMaxDamage(100).setMaxStackSize(1);

    private FuzzyTestItems() {}

    public static void register() {
        GameRegistry.registerItem(TOOL, "fuzzyTool");
        GameRegistry.registerItem(VARIANTS, "fuzzyVariants");
        GameRegistry.registerItem(WILDCARD_ORE_ITEM, "fuzzyWildcardOre");
        GameRegistry.registerItem(SHARED_ORE_ITEM, "fuzzySharedOre");
        GameRegistry.registerItem(ORE_TOOL, "fuzzyOreTool");

        OreDictionary.registerOre(WILDCARD_ORE, new ItemStack(WILDCARD_ORE_ITEM, 1, OreDictionary.WILDCARD_VALUE));
        OreDictionary.registerOre(WILDCARD_ORE, new ItemStack(SHARED_ORE_ITEM, 1, 0));
        OreDictionary.registerOre(SHARED_ORE, new ItemStack(SHARED_ORE_ITEM, 1, 0));
        OreDictionary.registerOre(TOOL_ORE, new ItemStack(ORE_TOOL, 1, OreDictionary.WILDCARD_VALUE));
    }

    /**
     * Like tools that keep their durability in NBT: {@link #WEAR} and {@link #MAX_WEAR} override the display damage and
     * the max damage.
     */
    private static final class WearTool extends Item {

        private WearTool() {
            this.setMaxDamage(100);
            this.setMaxStackSize(1);
        }

        @Override
        public int getDisplayDamage(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            return tag != null && tag.hasKey(WEAR) ? tag.getInteger(WEAR) : super.getDisplayDamage(stack);
        }

        @Override
        public int getMaxDamage(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            return tag != null && tag.hasKey(MAX_WEAR) ? tag.getInteger(MAX_WEAR) : super.getMaxDamage(stack);
        }
    }
}
