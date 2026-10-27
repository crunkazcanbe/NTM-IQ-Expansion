package com.crunkazcanbe.ntmiq.blood;

import java.util.List;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * IQ's fluid syringe (items/tool/ItemFluidSyringe, GPL-3): fill it from any HBM fluid slot, right-click to inject
 * yourself (10 mB), left-click a mob to inject it. The combat syringe (100 mB) can sneak-inject 100 mB at double intensity.
 * Also holds the fluid-in-NBT helpers every F01 item uses (saved by fluid NAME).
 */
@SuppressWarnings("deprecation")
public class ItemFluidSyringe extends Item implements IFillableItem {

    private final int maxDose;

    public ItemFluidSyringe(String name, int maxDose) {
        this.maxDose = maxDose;
        setMaxStackSize(1);
        setRegistryName("ntmiq", name);
        setTranslationKey("ntmiq." + name);
        addPropertyOverride(new ResourceLocation("ntmiq", "filled"), (s, w, e) -> fill(s) > 0 ? 1F : 0F);
    }

    // ---- shared fluid NBT helpers ----
    public static FluidType type(ItemStack s) {
        return s.hasTagCompound() && s.getTagCompound().hasKey("fluid") ? Fluids.fromName(s.getTagCompound().getString("fluid")) : Fluids.NONE;
    }

    public static int fill(ItemStack s) { return s.hasTagCompound() ? s.getTagCompound().getInteger("fill") : 0; }

    public static void set(ItemStack s, FluidType t, int fill) {
        if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
        if (fill <= 0 || t == null || t == Fluids.NONE) {
            s.getTagCompound().removeTag("fluid");
            s.getTagCompound().removeTag("fill");
            return;
        }
        s.getTagCompound().setString("fluid", t.getName());
        s.getTagCompound().setInteger("fill", fill);
    }

    /** a syringe of this kind pre-filled (loot, creative) */
    public ItemStack filled(FluidType t) {
        ItemStack s = new ItemStack(this);
        set(s, t, maxDose);
        return s;
    }

    static SoundEvent syringeSound() { return SoundEvent.REGISTRY.getObject(new ResourceLocation("hbm", "item.syringe")); }

    static void playSyringe(Entity at) {
        SoundEvent snd = syringeSound();
        if (snd != null) at.world.playSound(null, at.posX, at.posY, at.posZ, snd, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    // ---- use ----
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        int fill = fill(stack);
        if (fill <= 0) return new ActionResult<>(EnumActionResult.PASS, stack);
        if (!world.isRemote) {
            boolean overdose = maxDose > 10 && player.isSneaking();
            int dose = Math.min(overdose ? 100 : 10, fill);
            inject(stack, player, dose, overdose ? 2.0F : 1.0F);
            playSyringe(player);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /** IQ: left-clicking a mob injects 10 mB into it instead of hitting it */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity target) {
        if (!(target instanceof EntityLivingBase) || fill(stack) <= 0) return false;
        if (!player.world.isRemote) {
            inject(stack, (EntityLivingBase) target, Math.min(10, fill(stack)), 1.0F);
            playSyringe(player);
        }
        return true;
    }

    private static void inject(ItemStack stack, EntityLivingBase target, int dose, float intensity) {
        FluidType type = type(stack);
        BloodstreamAPI.inject(target, type, dose, intensity, true);
        BloodstreamAPI.applyStackPayload(stack, target, intensity);
        set(stack, type, fill(stack) - dose);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(I18n.translateToLocal(maxDose > 10 ? "desc.item.syringe.combat" : "desc.item.syringe.single") + " " + type(stack).getLocalizedName());
        list.add(I18n.translateToLocal("desc.item.syringe.amount") + " " + fill(stack) + "/" + maxDose + "mB");
        if (maxDose > 10) {
            list.add(I18n.translateToLocal("desc.item.syringe.useSelfSneak"));
            list.add(I18n.translateToLocal("desc.item.syringe.sneakOverdose"));
        } else {
            list.add(I18n.translateToLocal("desc.item.syringe.useSelf"));
        }
        list.add(I18n.translateToLocal("desc.item.syringe.useEntity"));
    }

    // ---- IFillableItem ----
    @Override public boolean acceptsFluid(FluidType t, ItemStack s) { return fill(s) == 0 || type(s) == t; }

    @Override
    public int tryFill(FluidType t, int amount, ItemStack s) {
        if (!acceptsFluid(t, s)) return amount;
        int add = Math.min(maxDose - fill(s), amount);
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
}
