package com.crunkazcanbe.ntmiq.blood;

import java.util.ArrayList;
import java.util.List;

import com.hbm.inventory.fluid.trait.FluidTrait;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;

/** IQ's FT_Consumable: a fluid you can drink / drip — food, saturation and potion effects. Ported from NTM: IQ Edition (GPL-3). */
@SuppressWarnings("deprecation")
public class FT_Consumable extends FluidTrait {

    /** a potion + amplifier pair (IQ: FT_Consumable.ConsumableEffect, potion IDs → Potion objects) */
    public static class ConsumableEffect {
        public final Potion potion;
        public final int amplifier;

        public ConsumableEffect(Potion potion, int amplifier) {
            this.potion = potion;
            this.amplifier = amplifier;
        }
    }

    public float foodLevel;
    public float saturation;
    public int consumption = 1;
    public boolean clearsEffects = false;
    private final List<ConsumableEffect> effects = new ArrayList<>();
    private final List<String> specialEffects = new ArrayList<>();

    public FT_Consumable setFood(float foodLevel, float saturation) { this.foodLevel = foodLevel; this.saturation = saturation; return this; }
    public FT_Consumable addEffect(Potion potion, int amplifier) { effects.add(new ConsumableEffect(potion, amplifier)); return this; }
    public FT_Consumable addSpecialEffect(String langKey) { specialEffects.add(langKey); return this; }
    public FT_Consumable setConsumption(int rate) { this.consumption = rate; return this; }
    public FT_Consumable setClearsEffects(boolean clears) { this.clearsEffects = clears; return this; }

    @Override
    public void addInfoHidden(List<String> info) {
        info.add(TextFormatting.DARK_GREEN + "[" + I18n.translateToLocal("hbmfluid.trait.consumable") + "]");
        info.add(TextFormatting.YELLOW + "- " + I18n.translateToLocal("hbmfluid.trait.food") + ": " + foodLevel + " shk/mB");
        info.add(TextFormatting.YELLOW + "- " + I18n.translateToLocal("hbmfluid.trait.saturation") + ": " + saturation + " shk/mB");
        info.add(TextFormatting.YELLOW + "- " + I18n.translateToLocalFormatted("hbmfluid.trait.consumption", consumption));
        if (clearsEffects) info.add(TextFormatting.YELLOW + "   - " + I18n.translateToLocal("hbmfluid.trait.clearsEffects"));
        for (ConsumableEffect e : effects) info.add(TextFormatting.YELLOW + "   - " + effectName(e));
        for (String e : specialEffects) info.add(TextFormatting.YELLOW + "   - " + I18n.translateToLocal(e));
    }

    static String effectName(ConsumableEffect e) {
        String amp = e.amplifier > 0 ? " " + I18n.translateToLocal("potion.potency." + e.amplifier).trim() : "";
        return I18n.translateToLocal(e.potion.getName()) + amp;
    }

    public void apply(EntityLivingBase entity, double intensity) {
        if (entity == null || !entity.isEntityAlive()) return;
        if (foodLevel > 0 && entity instanceof EntityPlayer) {
            int food = (int) (foodLevel * intensity / 20.0);
            if (food > 0) ((EntityPlayer) entity).getFoodStats().addStats(food, saturation * (float) intensity / 20.0F);
        }
        applyEffects(entity);
    }

    public void applyEffects(EntityLivingBase entity) {
        if (clearsEffects) {
            entity.clearActivePotions();
            return;
        }
        if (foodLevel >= 1.0F) entity.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 40, 2));
        else if (foodLevel >= 0.2F) entity.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 40, 1));
        else if (foodLevel >= 0.1F) entity.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 40, 0));
        for (ConsumableEffect e : effects) entity.addPotionEffect(new PotionEffect(e.potion, 40, e.amplifier));
    }
}
