package com.crunkazcanbe.ntmiq.blood;

import java.util.ArrayList;
import java.util.List;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.trait.FluidTrait;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;

/**
 * IQ's FT_Drug: a fluid that dissolves into the bloodstream. Effects scale with concentration; past {@link #threshold}
 * (fraction of total blood volume) the side-effect symptoms kick in. Decay is a concentration-dependent half-life.
 * Ported from NTM: IQ Edition (GPL-3, irislgtm / tam69 and the IQ team).
 */
@SuppressWarnings("deprecation")
public class FT_Drug extends FluidTrait {

    public int consumption = 1;
    public float threshold;
    public float dissipationRate = 3.4F;
    /** the fluid this trait sits on — set by {@link BloodFluids#attach} (CE traits have no back-reference) */
    public FluidType type;
    private final List<FT_Consumable.ConsumableEffect> effects = new ArrayList<>();
    private final List<String> specialEffects = new ArrayList<>();
    private final List<String> sideEffects = new ArrayList<>();

    public FT_Drug setConsumption(int rate) { this.consumption = rate; return this; }
    public FT_Drug setThreshold(float threshold) { this.threshold = threshold; return this; }
    public FT_Drug setDissipationRate(float rate) { this.dissipationRate = rate; return this; }
    public FT_Drug addEffect(Potion potion, int amplifier) { effects.add(new FT_Consumable.ConsumableEffect(potion, amplifier)); return this; }
    public FT_Drug addSpecialEffect(String langKey) { specialEffects.add(langKey); return this; }
    /** @param symptomKey a key from {@link BloodstreamAPI#getSymptom} */
    public FT_Drug addSideEffect(String symptomKey) { sideEffects.add(symptomKey); return this; }

    public boolean hasSpecialEffect(String key) { return specialEffects.contains(key); }
    public List<FT_Consumable.ConsumableEffect> getEffects() { return effects; }
    public List<String> getSideEffects() { return sideEffects; }

    /** per-tick multiplier at a given concentration (in units of the threshold) */
    public float getDecayFactor(float concentration) {
        float hl = this.dissipationRate / (1F + concentration * 2F);
        return (float) Math.pow(0.5, 1.0 / (hl * 20));
    }

    /** rough seconds one dose lasts, for the tooltip */
    public int getEffectDuration() {
        float dose = Math.max(10, consumption * 10F);
        double halfLives = Math.log(0.5 / dose) / Math.log(getDecayFactor(0F));
        return (int) (halfLives / 20F);
    }

    @Override
    public void addInfoHidden(List<String> info) {
        info.add(TextFormatting.AQUA + "[" + I18n.translateToLocal("hbmfluid.trait.drug") + "]");
        if (effects.isEmpty() && specialEffects.isEmpty()) info.add(TextFormatting.YELLOW + "   - " + I18n.translateToLocal("hbmfluid.trait.noEffects"));
        for (FT_Consumable.ConsumableEffect e : effects) info.add(TextFormatting.YELLOW + "   - " + FT_Consumable.effectName(e));
        for (String e : specialEffects) info.add(TextFormatting.YELLOW + "   - " + I18n.translateToLocal(e));
        info.add(TextFormatting.YELLOW + "   " + I18n.translateToLocalFormatted("hbmfluid.trait.consumption", consumption));
        if (threshold > 0) {
            info.add(TextFormatting.RED + "   " + I18n.translateToLocal("hbmfluid.trait.whenInjected"));
            info.add(TextFormatting.RED + "   " + I18n.translateToLocalFormatted("hbmfluid.trait.threshold", (int) (threshold * 100) + "%"));
        }
        if (!sideEffects.isEmpty()) {
            info.add(TextFormatting.RED + "   " + I18n.translateToLocal("hbmfluid.trait.sideEffects"));
            for (String key : sideEffects) {
                List<FT_Consumable.ConsumableEffect> s = BloodstreamAPI.getSymptom(key);
                info.add(TextFormatting.RED + "   - " + (s == null || s.isEmpty() ? key : I18n.translateToLocal(s.get(0).potion.getName())));
            }
        }
        if (dissipationRate > 0) info.add(TextFormatting.GRAY + "   " + I18n.translateToLocalFormatted("hbmfluid.trait.dissipation", getEffectDuration()));
    }

    /** dissolve one dose into the target's blood */
    public void apply(EntityLivingBase entity, double intensity) {
        if (entity == null || !entity.isEntityAlive() || type == null) return;
        BloodstreamAPI.get(entity).addDrug(type, (int) Math.max(1, intensity * this.consumption));
        if (specialEffects.contains("clear_effects")) entity.clearActivePotions();
        if (specialEffects.contains("clear_bad_effects"))
            for (net.minecraft.potion.PotionEffect pe : new ArrayList<>(entity.getActivePotionEffects()))
                if (isBad(pe.getPotion())) entity.removePotionEffect(pe.getPotion());
    }

    /** Potion.isBadEffect() is client-only in 1.12; read the field */
    private static boolean isBad(Potion p) {
        Boolean bad = net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(Potion.class, p, "field_76418_K");
        return bad != null && bad;
    }
}
