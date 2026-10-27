package com.crunkazcanbe.ntmiq.contagion;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * Pharmaceutical computing unit (IQ: items/machine/ItemPharmaUnit, GPL-3): the Dial-A-Dose burns one charge of it per
 * batch. Sixteen doses and it is scrap.
 */
@SuppressWarnings("deprecation")
public class ItemPharmaUnit extends Item {

    public ItemPharmaUnit(String name) {
        setRegistryName("ntmiq", name);
        setTranslationKey("ntmiq." + name);
        setMaxStackSize(1);
        setMaxDamage(16);
        setNoRepair();
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(TextFormatting.YELLOW + I18n.translateToLocal("item.ntmiq.pharma_computing_unit.desc"));
        list.add((stack.getMaxDamage() - stack.getItemDamage()) + "/" + stack.getMaxDamage());
    }
}
