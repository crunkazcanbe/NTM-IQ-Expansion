package com.crunkazcanbe.ntmiq.blood;

import java.util.List;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.IDynamicModels;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.PlayerInformPacketLegacy;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * IQ's Blood Monitor (items/armor/ItemModBloodMonitor, GPL-3): leggings mod that lists your bloodstream on the HUD —
 * base blood as a whole percentage, every dissolved fluid to two decimals. Uses CE's HUD info lines (ids 979+).
 */
@SuppressWarnings("deprecation")
public class ItemBloodMonitor extends ItemArmorMod {

    public ItemBloodMonitor(String name) {
        super(ArmorModHandler.legs_only, false, false, true, false, name);
        ModItems.ALL_ITEMS.remove(this);
        IDynamicModels.INSTANCES.remove(this);
    }

    @Override @SideOnly(Side.CLIENT) public boolean ownsModelLocation(ModelResourceLocation loc) { return false; }

    @Override
    public void modUpdate(EntityLivingBase entity, ItemStack armor) {
        if (entity.world.isRemote || entity.world.getTotalWorldTime() % 5 != 0 || !(entity instanceof EntityPlayerMP)) return;
        Bloodstream blood = BloodstreamAPI.get(entity);
        float total = blood.getTotalFluid();
        int i = 0;
        for (BloodEntry e : blood.getEntries()) {
            if (e.amount <= 0 || e.isPathogen()) continue;
            String value;
            TextFormatting color;
            if (e.fluid == blood.getBloodType()) {
                value = (int) (e.amount * 100F / total) + "%";
                color = TextFormatting.AQUA;
            } else {
                int h = (int) (e.amount * 10000F / total);
                value = h / 100 + "." + (h % 100) / 10 + (h % 10) + "%";
                color = TextFormatting.GREEN;
            }
            ITextComponent line = new TextComponentTranslation(e.fluid.getTranslationKey());
            line.getStyle().setColor(color);
            line.appendSibling(new TextComponentString(TextFormatting.RESET + ": " + value));
            PacketDispatcher.wrapper.sendTo(new PlayerInformPacketLegacy(line, 979 + i++, 4000), (EntityPlayerMP) entity);
        }
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(TextFormatting.YELLOW + I18n.translateToLocal("item.blood_monitor.desc"));
        list.add("");
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public void addDesc(List<String> list, ItemStack stack, ItemStack armor) {
        list.add(TextFormatting.YELLOW + "  " + stack.getDisplayName() + " (" + I18n.translateToLocal("item.blood_monitor.short") + ")");
    }
}
