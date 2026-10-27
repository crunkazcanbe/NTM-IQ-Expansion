package com.crunkazcanbe.ntmiq.blood;

import java.util.List;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.items.IDynamicModels;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.lib.ModDamageSource;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.crunkazcanbe.ntmiq.blood.ItemFluidSyringe.fill;
import static com.crunkazcanbe.ntmiq.blood.ItemFluidSyringe.set;
import static com.crunkazcanbe.ntmiq.blood.ItemFluidSyringe.type;

/**
 * IQ's Auto Drip (items/armor/ItemModAutoFeed, GPL-3): a helmet mod holding 4000 mB of a drink or drug fluid that it
 * drips into you every second. Hot fluids scald, corrosive ones burn. Fill it in any HBM fluid slot before applying.
 */
@SuppressWarnings("deprecation")
public class ItemAutoDrip extends ItemArmorMod implements IFillableItem {

    public static final int MAX_FLUID = 4000;
    public static final DamageSource BOILED = new DamageSource("ntmiq.boiled");

    public ItemAutoDrip(String name) {
        super(ArmorModHandler.helmet_only, true, false, false, false, name);
        ModItems.ALL_ITEMS.remove(this);    // CE's base self-registers; we register it ourselves
        IDynamicModels.INSTANCES.remove(this); // and draw it with our own layered JSON model (name = ntmiq:<name>, set by ItemBase)
        addPropertyOverride(new ResourceLocation("ntmiq", "fill"), (s, w, e) -> fill(s) >= MAX_FLUID ? 1F : fill(s) > 0 ? 0.5F : 0F);
    }

    @Override @SideOnly(Side.CLIENT) public boolean ownsModelLocation(ModelResourceLocation loc) { return false; }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(TextFormatting.LIGHT_PURPLE + I18n.translateToLocalFormatted("item.auto_drip.fill", type(stack).getLocalizedName(), fill(stack), MAX_FLUID));
        list.add(TextFormatting.GREEN + I18n.translateToLocal("item.auto_drip.desc"));
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public void addDesc(List<String> list, ItemStack stack, ItemStack armor) {
        list.add(TextFormatting.RED + "  " + stack.getDisplayName() + " (" + type(stack).getLocalizedName() + ": " + fill(stack) + " mB)");
    }

    @Override
    public void modUpdate(EntityLivingBase entity, ItemStack armor) {
        if (entity.world.isRemote || entity.ticksExisted % 20 != 0) return;
        ItemStack mod = ArmorModHandler.pryMods(armor)[ArmorModHandler.helmet_only];
        if (mod == null || mod.isEmpty() || mod.getItem() != this) return;
        FluidType type = type(mod);
        int fill = fill(mod);
        if (fill <= 0 || type == Fluids.NONE) return;
        FT_Consumable cons = type.getTrait(FT_Consumable.class);
        FT_Drug drug = type.getTrait(FT_Drug.class);
        if (cons == null && drug == null) return;

        if (type.temperature >= 100) entity.attackEntityFrom(BOILED, 2.0F);
        FT_Corrosive corrosive = type.getTrait(FT_Corrosive.class);
        if (corrosive != null) entity.attackEntityFrom(ModDamageSource.acid, corrosive.getRating() / 10F);

        set(mod, type, Math.max(0, fill - (cons != null ? cons.consumption : drug.consumption)));
        if (cons != null) {
            float accum = mod.getTagCompound().getFloat("hungerAccum") + cons.foodLevel;
            int whole = (int) accum;
            if (whole > 0 && entity instanceof EntityPlayer) {
                ((EntityPlayer) entity).getFoodStats().addStats(whole, cons.saturation * whole);
                accum -= whole;
            }
            mod.getTagCompound().setFloat("hungerAccum", accum);
            cons.applyEffects(entity);
        }
        if (drug != null) BloodstreamAPI.inject(entity, type, 0, 1.0F, false);
        ArmorModHandler.applyMod(armor, mod); // write the drained mod back into the helmet
    }

    // ---- IFillableItem: fill only, never drain ----
    @Override
    public boolean acceptsFluid(FluidType t, ItemStack s) {
        if (t == BloodFluids.HUMAN_BLOOD) return false;
        if (fill(s) > 0 && type(s) != t) return false;
        return t.hasTrait(FT_Consumable.class) || t.hasTrait(FT_Drug.class);
    }

    @Override
    public int tryFill(FluidType t, int amount, ItemStack s) {
        if (!acceptsFluid(t, s)) return amount;
        int add = Math.min(amount, MAX_FLUID - fill(s));
        set(s, t, fill(s) + add);
        return amount - add;
    }

    @Override public boolean providesFluid(FluidType t, ItemStack s) { return false; }
    @Override public int tryEmpty(FluidType t, int amount, ItemStack s) { return 0; }
    @Override public FluidType getFirstFluidType(ItemStack s) { FluidType t = type(s); return t == Fluids.NONE ? null : t; }
    @Override public int getFill(ItemStack s) { return fill(s); }
}
