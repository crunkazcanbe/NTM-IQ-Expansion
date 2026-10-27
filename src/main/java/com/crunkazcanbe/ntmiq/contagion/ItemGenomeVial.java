package com.crunkazcanbe.ntmiq.contagion;

import java.util.List;

import com.crunkazcanbe.ntmiq.blood.ItemFluidSyringe;
import com.crunkazcanbe.ntmiq.blood.ItemMedicalSyringe;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * Glass vial (IQ: items/ItemVial, GPL-3): 100 mB of anything. Filled with blood it is a sample of whatever was in that
 * bloodstream (frame + genome in NBT); filled with DNA by the Magnetic Separator it is one genome sample
 * ({@link GenomeSample}) to be written onto a floppy disk.
 */
@SuppressWarnings("deprecation")
public class ItemGenomeVial extends Item implements IFillableItem {

    public static final int MAX_FLUID = 100;

    public ItemGenomeVial(String name) {
        setRegistryName("ntmiq", name);
        setTranslationKey("ntmiq." + name);
        setMaxStackSize(64);
        addPropertyOverride(new ResourceLocation("ntmiq", "filled"), (s, w, e) -> ItemFluidSyringe.fill(s) > 0 ? 1F : 0F);
    }

    public static ItemStack filled(FluidType type, int amount) {
        ItemStack stack = new ItemStack(com.crunkazcanbe.ntmiq.contagion.IqContagion.vial);
        ItemFluidSyringe.set(stack, type, amount);
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack;
    }

    /** the disease this vial holds a sample of, if any */
    public static String readFrame(ItemStack vial) {
        if (vial.isEmpty() || !vial.hasTagCompound()) return null;
        NBTTagCompound nbt = vial.getTagCompound();
        if (nbt.hasKey("frame")) return nbt.getString("frame");
        if (nbt.hasKey("mut")) return nbt.getCompoundTag("mut").getString("frame");
        return null;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        FluidType type = ItemFluidSyringe.type(stack);
        if (ItemFluidSyringe.fill(stack) > 0) list.add(type.getLocalizedName() + ": " + ItemFluidSyringe.fill(stack) + "mB");

        String sample = GenomeSample.getKey(stack);
        if (sample != null) list.add(TextFormatting.AQUA + I18n.translateToLocalFormatted("item.ntmiq.vial.sample", sample, GenomeSample.getValue(stack)));

        String frame = readFrame(stack);
        if (frame != null) {
            DiseaseDefinition def = DiseaseRegistry.get(frame);
            list.add(TextFormatting.RED + I18n.translateToLocalFormatted("item.ntmiq.vial.frame", def == null ? frame : def.displayName));
        }
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(ItemMedicalSyringe.KEY_OWNER_NAME))
            list.add(I18n.translateToLocalFormatted("desc.item.medicalSyringe.owner", stack.getTagCompound().getString(ItemMedicalSyringe.KEY_OWNER_NAME)));
    }

    // ---- IFillableItem: a vial takes fluid from any HBM slot but never gives it back ----
    @Override public boolean acceptsFluid(FluidType t, ItemStack s) { return ItemFluidSyringe.fill(s) == 0 || ItemFluidSyringe.type(s) == t; }

    @Override
    public int tryFill(FluidType t, int amount, ItemStack s) {
        if (!acceptsFluid(t, s)) return amount;
        int add = Math.min(amount, MAX_FLUID - ItemFluidSyringe.fill(s));
        ItemFluidSyringe.set(s, t, ItemFluidSyringe.fill(s) + add);
        return amount - add;
    }

    @Override public boolean providesFluid(FluidType t, ItemStack s) { return false; }
    @Override public int tryEmpty(FluidType t, int amount, ItemStack s) { return amount; }

    @Override public FluidType getFirstFluidType(ItemStack s) { FluidType t = ItemFluidSyringe.type(s); return t == Fluids.NONE ? null : t; }
    @Override public int getFill(ItemStack s) { return ItemFluidSyringe.fill(s); }
}
