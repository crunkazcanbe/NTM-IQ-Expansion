package com.crunkazcanbe.ntmiq.magsep;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.Tags;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Locale;

/**
 * Magnetic disc for the magnetic separator: more parallels and durability per tier, durability in NBT ("dur", one point
 * per second of work). Ported from NTM: IQ Edition (GPL-3; irislgtm, tam69 and the IQ team).
 */
public class ItemMagneticDisc extends Item {

    public enum EnumMagneticDisc {
        IRON(1, 1_000, 0xFFFFFF),
        NEODYMIUM(4, 10_000, 0xE6E6B6),
        VANADIUM(8, 100_000, 0x7F8C6A),
        NICKEL(16, 1_000_000, 0xE8D1C7);

        public final int parallels, durability, color; // color = IQ's material solidColorLight

        EnumMagneticDisc(int parallels, int durability, int color) {
            this.parallels = parallels;
            this.durability = durability;
            this.color = color;
        }
    }

    public ItemMagneticDisc() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setMaxStackSize(1);
        setRegistryName(new ResourceLocation(Tags.MODID, "magnetic_disc"));
        setTranslationKey(Tags.MODID + ".magnetic_disc");
        setCreativeTab(IqContent.TAB);
    }

    public static EnumMagneticDisc getType(ItemStack stack) {
        EnumMagneticDisc[] v = EnumMagneticDisc.values();
        int meta = stack.getMetadata();
        return v[meta < 0 || meta >= v.length ? 0 : meta];
    }

    public static int getDurability(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag == null || !tag.hasKey("dur") ? getType(stack).durability : tag.getInteger("dur");
    }

    public static void setDurability(ItemStack stack, int dur) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("dur", Math.max(0, dur));
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) for (EnumMagneticDisc t : EnumMagneticDisc.values()) items.add(new ItemStack(this, 1, t.ordinal()));
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return super.getTranslationKey() + "." + getType(stack).name().toLowerCase(Locale.US);
    }

    @Override public boolean showDurabilityBar(ItemStack stack) { return getDurability(stack) < getType(stack).durability; }
    @Override public double getDurabilityForDisplay(ItemStack stack) { return 1D - (double) getDurability(stack) / getType(stack).durability; }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        EnumMagneticDisc t = getType(stack);
        list.add(TextFormatting.GREEN + I18n.format("desc.ntmiq.magnetic_disc.parallels", t.parallels));
        list.add(TextFormatting.YELLOW + I18n.format("desc.ntmiq.magnetic_disc.durability", getDurability(stack), t.durability));
    }
}
