package net.p455w0rd.wirelesscraftingterminal.items;

import java.util.List;

import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.input.Keyboard;

import com.google.common.base.Splitter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.p455w0rd.wirelesscraftingterminal.handlers.LocaleHandler;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

public class ItemInfinityBooster extends Item {

	public boolean showEffect = false;

	public ItemInfinityBooster() {
		super();
		this.setTextureName(Reference.MODID + ":infinityBoosterCard");
		this.setMaxStackSize(1);
	}

	public Item hasEffect(boolean hasEffect) {
		this.showEffect = hasEffect;
		return this;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean hasEffect(ItemStack is, int pass) {
		return this.showEffect;
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return 1;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack is, EntityPlayer player, List list, boolean par4) {
		String shift = LocaleHandler.PressShift.getLocal().replace("Shift", EnumChatFormatting.YELLOW + "" + EnumChatFormatting.BOLD + "" + EnumChatFormatting.ITALIC + "Shift" + EnumChatFormatting.GRAY);
		list.add(EnumChatFormatting.AQUA + "==============================");
		if (isShiftKeyDown()) {
			String info = LocaleHandler.InfinityBoosterDesc.getLocal();
			for (String line : Splitter.on("\n").split(WordUtils.wrap(info, 37, "\n", false))) {
				list.add(line.trim());
			}
			list.add("");
			String onlyWorks = LocaleHandler.OnlyWorks.getLocal();
			for (String line : Splitter.on("\n").split(WordUtils.wrap(onlyWorks, 27, "\n", false))) {
				list.add(EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "" + EnumChatFormatting.ITALIC + line.trim());
			}
		}
		else {
			list.add(shift);
		}
		super.addInformation(is, player, list, par4);
	}
	
	@SideOnly(Side.CLIENT)
	public static boolean isShiftKeyDown() {
		return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

	}

}
