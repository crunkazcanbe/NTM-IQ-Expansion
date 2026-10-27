package com.crunkazcanbe.ntmiq.contagion;

import java.util.List;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemArmorMod;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * Contagion gloves (IQ: items/armor/ItemModGloves, GPL-3): a chestplate mod that takes the hit when you handle a
 * contaminated item, wearing down one point each time. Rubber gloves are cheap and short-lived, the desh-lined pair
 * lasts far longer.
 */
@SuppressWarnings("deprecation")
public class ItemContagionGloves extends ItemArmorMod {

    public ItemContagionGloves(String name, int durability) {
        super(ArmorModHandler.extra, false, true, false, false, name);
        ModItems.ALL_ITEMS.remove(this);    // CE's base self-registers; the add-on registers it itself
        setMaxDamage(durability);
        setMaxStackSize(1);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(TextFormatting.YELLOW + I18n.translateToLocal("item.gloves.desc"));
        list.add((stack.getMaxDamage() - stack.getItemDamage()) + "/" + stack.getMaxDamage());
        list.add("");
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public void addDesc(List<String> list, ItemStack stack, ItemStack armor) {
        list.add(TextFormatting.DARK_PURPLE + "  " + stack.getDisplayName()
                + " (" + (stack.getMaxDamage() - stack.getItemDamage()) + "/" + stack.getMaxDamage() + ")");
    }

    /** @return true if gloves took this contact; they wear out and fall off at 0 */
    public static boolean wear(EntityPlayer player) {
        ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (chest.isEmpty()) return false;

        ItemStack gloves = ArmorModHandler.pryMod(chest, ArmorModHandler.extra);
        if (gloves.isEmpty() || !(gloves.getItem() instanceof ItemContagionGloves)) return false;

        gloves.setItemDamage(gloves.getItemDamage() + 1);
        if (gloves.getItemDamage() >= gloves.getMaxDamage()) {
            ArmorModHandler.removeMod(chest, ArmorModHandler.extra);
            return false;
        }
        ArmorModHandler.applyMod(chest, gloves);
        return true;
    }
}
