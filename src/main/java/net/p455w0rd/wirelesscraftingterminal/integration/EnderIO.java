package net.p455w0rd.wirelesscraftingterminal.integration;

import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;

import org.jspecify.annotations.NullMarked;

import appeng.util.Platform;
import crazypants.enderio.enchantment.EnchantmentSoulBound;

/**
 * @author p455w0rd
 *
 */
@NullMarked
public class EnderIO {

    public static boolean isSoulBound(ItemStack stack) {
        if (!Platform.isEIOLoaded) {
            return false;
        }
        Map<Integer, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        for (int enchId : enchants.keySet()) {
            if (enchId >= 0 && enchId < Enchantment.enchantmentsList.length
                    && Enchantment.enchantmentsList[enchId] instanceof EnchantmentSoulBound) {
                return true;
            }
        }
        return false;
    }

}
