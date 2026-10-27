package com.crunkazcanbe.ntmiq.contagion;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.util.I18nUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;

/**
 * "Pharma" fluid trait (IQ: inventory/fluid/trait/FT_Pharma, GPL-3): antiserum cures the disease its profile names.
 * Too strong a dose leaves you dizzy and hungry. Registered through IqFluids.registerTrait as "ntmiq_pharma".
 */
public class FT_Pharma extends FluidTrait {

    @Override
    public void addInfoHidden(List<String> info) {
        info.add(TextFormatting.GREEN + "[" + I18nUtil.resolveKey("hbmfluid.trait.pharma") + "]");
    }

    public static void apply(PharmaProfile profile, EntityLivingBase target, float intensity) {
        if (profile == null || target == null) return;
        DiseaseHandler.cure(target, profile, intensity);

        if (intensity > 1.5F) {
            int tier = Math.min(3, (int) ((intensity - 1.5F) / 0.5F));
            target.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, tier));
            target.addPotionEffect(new PotionEffect(MobEffects.HUNGER, 400, tier));
        }
    }

    @Override public void serializeJSON(JsonWriter writer) throws IOException {}
    @Override public void deserializeJSON(JsonObject obj) {}
}
