package com.crunkazcanbe.ntmiq.husks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * IQ's ItemHumanPart (NTM: IQ Edition, GPL-3; irislgtm / tam69 / IQ team): five body parts (metadata), each with five
 * 0-100 % stats and a 20 % chance of one trait. The Cloner sums the stats of one of each part into a husk body.
 */
public class ItemHumanPart extends Item {

    private static final Random RAND = new Random();
    private static final float TRAIT_CHANCE = 0.2F;

    public enum Part { HEAD, TORSO, ARMS, LEGS, HEART }

    public enum Stat { VITALITY, STRENGTH, AGILITY, TOUGHNESS, RECOVERY }

    public enum Trait {
        TWITCH(Part.HEAD), NUMB(Part.HEAD),
        HARDY(Part.TORSO), GRISTLE(Part.TORSO),
        BLOODTHIRST(Part.ARMS), WHITE_KNUCKLES(Part.ARMS),
        SURE_FOOTED(Part.LEGS), SPRING_HEEL(Part.LEGS),
        SECOND_WIND(Part.HEART), CLOTTING(Part.HEART);

        public final Part part;

        Trait(Part part) { this.part = part; }

        static List<Trait> of(Part part) {
            List<Trait> list = new ArrayList<>();
            for (Trait t : values()) if (t.part == part) list.add(t);
            return list;
        }
    }

    public ItemHumanPart() {
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    public static Part part(ItemStack stack) {
        int m = stack.getMetadata();
        return m >= 0 && m < Part.values().length ? Part.values()[m] : Part.HEAD;
    }

    /** a random part with stats in [min, max] */
    public static ItemStack make(Part part, int min, int max) {
        ItemStack stack = new ItemStack(IqHusks.humanPart, 1, part.ordinal());
        NBTTagCompound nbt = new NBTTagCompound();
        for (Stat stat : Stat.values()) nbt.setInteger(stat.name(), Math.min(100, max <= min ? min : min + RAND.nextInt(max - min + 1)));
        if (RAND.nextFloat() < TRAIT_CHANCE) {
            List<Trait> traits = Trait.of(part);
            nbt.setString("trait", traits.get(RAND.nextInt(traits.size())).name());
        }
        stack.setTagCompound(nbt);
        return stack;
    }

    public static int getStat(ItemStack stack, Stat stat) {
        return stack.hasTagCompound() ? stack.getTagCompound().getInteger(stat.name()) : 0;
    }

    public static Trait getTrait(ItemStack stack) {
        if (!stack.hasTagCompound()) return null;
        try {
            String name = stack.getTagCompound().getString("trait");
            return name.isEmpty() ? null : Trait.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return "item.ntmiq.human_part_" + part(stack).name().toLowerCase(Locale.US);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) for (Part p : Part.values()) items.add(make(p, 0, 100));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        Trait trait = getTrait(stack);
        if (trait != null) list.add(TextFormatting.RED + I18nUtil.resolveKey("item.human_part.trait." + trait.name().toLowerCase(Locale.US)));
        for (Stat stat : Stat.values())
            list.add(TextFormatting.YELLOW + I18nUtil.resolveKey("item.human_part.stat." + stat.name().toLowerCase(Locale.US)) + ": " + getStat(stack, stat) + "%");
    }
}
