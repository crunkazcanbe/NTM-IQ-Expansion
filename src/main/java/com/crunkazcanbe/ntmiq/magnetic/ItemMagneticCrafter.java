package com.crunkazcanbe.ntmiq.magnetic;

import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** IQ's ItemMagneticCrafter: chestplate mod / handheld crafting grid that pulls ingredients from magnetic crates (JEI "+" button). */
public class ItemMagneticCrafter extends ItemArmorMod {

    public ItemMagneticCrafter() {
        super(ArmorModHandler.plate_only, false, true, false, false, "magnetic_crafter");
        ModItems.ALL_ITEMS.remove(this);   // CE base self-adds; registered by IqMagnetic
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote) player.openGui(Tags.MODID, IqMagnetic.GUI_CRAFTER, world, 0, 0, 0);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(TextFormatting.YELLOW + I18nUtil.resolveKey("item.magnetic_crafter.desc"));
        list.add(TextFormatting.YELLOW + I18nUtil.resolveKey("item.magnetic_crafter.use"));
        list.add("");
        super.addInformation(stack, world, list, flag);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addDesc(List<String> list, ItemStack stack, ItemStack armor) {
        list.add(TextFormatting.YELLOW + "  " + stack.getDisplayName() + " (" + I18nUtil.resolveKey("item.magnetic_crafter.shortDesc") + ")");
    }

    /** held, or installed as an armor mod */
    public static boolean has(EntityPlayer player, Class<?> type) {
        if (type.isInstance(player.getHeldItemMainhand().getItem())) return true;
        for (ItemStack armor : player.inventory.armorInventory) {
            if (armor.isEmpty() || !ArmorModHandler.hasMods(armor)) continue;
            for (ItemStack mod : ArmorModHandler.pryMods(armor)) if (mod != null && type.isInstance(mod.getItem())) return true;
        }
        return false;
    }
}
