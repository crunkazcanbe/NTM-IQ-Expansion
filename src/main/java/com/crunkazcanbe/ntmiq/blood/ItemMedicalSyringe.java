package com.crunkazcanbe.ntmiq.blood;

import java.util.List;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import static com.crunkazcanbe.ntmiq.blood.ItemFluidSyringe.fill;
import static com.crunkazcanbe.ntmiq.blood.ItemFluidSyringe.set;
import static com.crunkazcanbe.ntmiq.blood.ItemFluidSyringe.type;

/**
 * IQ's medical syringe (GPL-3): an EMPTY one draws a 1000 mB blood sample (right-click = yourself, left-click = a mob),
 * tagged with its owner and any pathogens in the bloodstream; sneak-use injects the whole content. F02/F03 read the
 * sample tags ({@link #KEY_OWNER_UUID}, {@link #KEY_OWNER_NAME}, {@link #KEY_PATHOGENS}).
 */
@SuppressWarnings("deprecation")
public class ItemMedicalSyringe extends Item implements IFillableItem {

    public static final int MAX_DOSE = 1000;
    public static final String KEY_OWNER_UUID = "ownerUUID";
    public static final String KEY_OWNER_NAME = "ownerName";
    public static final String KEY_PATHOGENS = "pathogen";

    public ItemMedicalSyringe(String name) {
        setMaxStackSize(1);
        setRegistryName("ntmiq", name);
        setTranslationKey("ntmiq." + name);
        addPropertyOverride(new ResourceLocation("ntmiq", "filled"), (s, w, e) -> fill(s) > 0 ? 1F : 0F);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) use(stack, player, player.isSneaking());
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if (!(entity instanceof EntityLivingBase)) return false;
        if (!entity.world.isRemote) use(stack, (EntityLivingBase) entity, player.isSneaking());
        return true;
    }

    private static void use(ItemStack stack, EntityLivingBase target, boolean sneaking) {
        if (sneaking && fill(stack) > 0) inject(stack, target);
        else if (!sneaking && fill(stack) == 0) extractSample(stack, target);
    }

    private static void inject(ItemStack stack, EntityLivingBase target) {
        BloodstreamAPI.inject(target, type(stack), fill(stack), 1.0F, true);
        BloodstreamAPI.applyStackPayload(stack, target, 1.0F);
        stack.setTagCompound(null);
        ItemFluidSyringe.playSyringe(target);
    }

    private static void extractSample(ItemStack stack, EntityLivingBase target) {
        NBTTagList pathogens = new NBTTagList();
        for (BloodEntry e : BloodstreamAPI.get(target).getEntries()) {
            if (!e.isPathogen()) continue;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("frame", e.frameId);
            tag.setFloat("amount", e.amount);
            if (e.data != null) tag.setTag("mut", e.data.copy());
            pathogens.appendTag(tag);
        }
        for (BloodstreamAPI.SampleSource s : BloodstreamAPI.SAMPLERS) s.addSample(target, pathogens);

        set(stack, dominantFluid(target), MAX_DOSE);
        NBTTagCompound nbt = stack.getTagCompound();
        nbt.setString(KEY_OWNER_UUID, target.getUniqueID().toString());
        nbt.setString(KEY_OWNER_NAME, target.getName());
        nbt.setTag(KEY_PATHOGENS, pathogens);
        target.attackEntityFrom(DamageSource.GENERIC, 1.0F);
        ItemFluidSyringe.playSyringe(target);
    }

    /** the most plentiful fluid in the blood (normally the base blood) */
    private static FluidType dominantFluid(EntityLivingBase target) {
        float best = 0F;
        FluidType type = BloodFluids.HUMAN_BLOOD;
        for (BloodEntry e : BloodstreamAPI.get(target).getEntries())
            if (!e.isPathogen() && e.amount > best) { best = e.amount; type = e.fluid; }
        return type;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(I18n.translateToLocal("desc.item.medicalSyringe.useSelf"));
        list.add(I18n.translateToLocal("desc.item.medicalSyringe.useEntity"));
        list.add(I18n.translateToLocal("desc.item.medicalSyringe.sneakSelf"));
        list.add(I18n.translateToLocal("desc.item.medicalSyringe.sneakEntity"));
        if (fill(stack) > 0) {
            list.add(type(stack).getLocalizedName() + ": " + fill(stack) + "mB");
            if (stack.getTagCompound().hasKey(KEY_OWNER_NAME))
                list.add(I18n.translateToLocalFormatted("desc.item.medicalSyringe.owner", stack.getTagCompound().getString(KEY_OWNER_NAME)));
        }
    }

    // ---- IFillableItem ----
    @Override public boolean acceptsFluid(FluidType t, ItemStack s) { return fill(s) == 0 || type(s) == t; }

    @Override
    public int tryFill(FluidType t, int amount, ItemStack s) {
        if (!acceptsFluid(t, s)) return amount;
        int add = Math.min(amount, MAX_DOSE - fill(s));
        set(s, t, fill(s) + add);
        return amount - add;
    }

    @Override public boolean providesFluid(FluidType t, ItemStack s) { return fill(s) > 0 && type(s) == t; }

    @Override
    public int tryEmpty(FluidType t, int amount, ItemStack s) {
        if (!providesFluid(t, s)) return 0;
        int take = Math.min(amount, fill(s));
        set(s, t, fill(s) - take);
        return take;
    }

    @Override public FluidType getFirstFluidType(ItemStack s) { FluidType t = type(s); return t == Fluids.NONE ? null : t; }

    @Override public int getFill(ItemStack s) { return fill(s); }

    /** IQ's "Notch" syringe: creative-only, pre-filled with Notch's blood (a clone sample for F03) */
    public static class Notch extends ItemMedicalSyringe {
        public Notch(String name) { super(name); }

        @Override
        public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
            if (!isInCreativeTab(tab)) return;
            ItemStack s = new ItemStack(this);
            set(s, BloodFluids.HUMAN_BLOOD, MAX_DOSE);
            s.getTagCompound().setString(KEY_OWNER_UUID, "069a79f4-44e9-4726-a5be-fca90e38aaf5");
            s.getTagCompound().setString(KEY_OWNER_NAME, "Notch");
            items.add(s);
        }
    }
}
