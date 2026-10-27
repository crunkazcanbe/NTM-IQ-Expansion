package com.crunkazcanbe.ntmiq.contagion;

import java.util.EnumMap;
import java.util.Map;

import com.crunkazcanbe.ntmiq.blood.Bloodstream;
import com.crunkazcanbe.ntmiq.blood.IqPotions;
import com.crunkazcanbe.ntmiq.contagion.DiseaseDefinition.PathogenType;
import com.crunkazcanbe.ntmiq.contagion.DiseaseDefinition.TransmissionAxis;
import com.hbm.lib.ModDamageSource;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;

/**
 * What each kind of pathogen does to a body (IQ: handler/contagion/condition/*, GPL-3). The symptom potions themselves
 * come from F01 ({@link IqPotions}); this only decides which ones a disease hands out, how fast it gets worse and how
 * well it travels. Subclasses are nested so the whole behaviour table reads in one file.
 */
public abstract class PathogenCondition {

    private static final Map<PathogenType, PathogenCondition> REGISTRY = new EnumMap<>(PathogenType.class);

    static {
        for (PathogenCondition c : new PathogenCondition[] { new Virus(), new Bacteria(), new Fungus(), new Parasite(), new Prion() })
            REGISTRY.put(c.getType(), c);
    }

    public static PathogenCondition get(PathogenType type) { return type == null ? null : REGISTRY.get(type); }

    public abstract PathogenType getType();

    public void tickSymptoms(Bloodstream blood, EntityLivingBase entity, DiseaseDefinition def, DiseaseInstance instance) {}

    public float getSpreadModifier(TransmissionAxis axis, DiseaseInstance instance) { return 1.0F; }

    public boolean isContagiousDuringIncubation() { return false; }

    public float getSeverityRamp(DiseaseDefinition def) { return 1.0F; }

    private static PotionEffect fx(net.minecraft.potion.Potion potion, int seconds, int amplifier) {
        return new PotionEffect(potion, seconds, Math.max(0, amplifier));
    }

    // ---------------- the five kinds ----------------

    public static class Virus extends PathogenCondition {
        @Override public PathogenType getType() { return PathogenType.VIRUS; }
        @Override public boolean isContagiousDuringIncubation() { return true; }
        @Override public float getSeverityRamp(DiseaseDefinition def) { return 1.5F; }

        @Override
        public void tickSymptoms(Bloodstream blood, EntityLivingBase e, DiseaseDefinition def, DiseaseInstance instance) {
            long tick = e.world.getTotalWorldTime();
            e.addPotionEffect(fx(IqPotions.symptomCough, 40, 0));
            if (tick % 80 < 40) e.addPotionEffect(fx(MobEffects.NAUSEA, 40, 0));
            if (blood != null && tick % 60 == 0) blood.drainBlood(0.5F + instance.currentSeverity);
        }

        @Override
        public float getSpreadModifier(TransmissionAxis axis, DiseaseInstance instance) {
            return axis == TransmissionAxis.AEROSOL ? 1.5F : 1.0F;
        }
    }

    public static class Bacteria extends PathogenCondition {
        @Override public PathogenType getType() { return PathogenType.BACTERIA; }

        @Override
        public void tickSymptoms(Bloodstream blood, EntityLivingBase e, DiseaseDefinition def, DiseaseInstance instance) {
            float sev = instance.currentSeverity;
            e.addPotionEffect(fx(IqPotions.symptomFever, 40, (int) (sev * 2)));
            if (sev > 0.5F) e.addPotionEffect(fx(IqPotions.symptomSeptic, 40, (int) ((sev - 0.5F) * 2)));
            if (e.getRNG().nextInt(Math.max(10, 60 - (int) (sev * 40))) == 0) e.attackEntityFrom(DamageSource.GENERIC, 0.5F + sev);
        }

        @Override
        public float getSpreadModifier(TransmissionAxis axis, DiseaseInstance instance) {
            if (axis == TransmissionAxis.TOUCH) return 1.5F;
            if (axis == TransmissionAxis.AEROSOL) return 0.5F;
            return 1.0F;
        }
    }

    public static class Fungus extends PathogenCondition {
        @Override public PathogenType getType() { return PathogenType.FUNGUS; }
        @Override public float getSeverityRamp(DiseaseDefinition def) { return 0.4F; }

        @Override
        public void tickSymptoms(Bloodstream blood, EntityLivingBase e, DiseaseDefinition def, DiseaseInstance instance) {
            e.addPotionEffect(fx(IqPotions.symptomSneeze, 40, 0));
            e.addPotionEffect(fx(IqPotions.symptomCough, 40, 0));
            if (e.getRNG().nextInt(40) == 0) e.addPotionEffect(fx(MobEffects.HUNGER, 200, 0));
        }
    }

    public static class Parasite extends PathogenCondition {
        @Override public PathogenType getType() { return PathogenType.PARASITE; }
        @Override public boolean isContagiousDuringIncubation() { return true; }
        @Override public float getSeverityRamp(DiseaseDefinition def) { return 2.0F; }

        @Override
        public void tickSymptoms(Bloodstream blood, EntityLivingBase e, DiseaseDefinition def, DiseaseInstance instance) {
            float sev = instance.currentSeverity;
            e.addPotionEffect(fx(MobEffects.HUNGER, 40, (int) (sev * 2)));
            e.addPotionEffect(fx(IqPotions.symptomFever, 40, (int) (sev * 2)));
            if (sev > 0.4F) e.addPotionEffect(fx(IqPotions.symptomHemorrhage, 40, (int) ((sev - 0.4F) * 2)));
            if (sev > 0.7F) e.addPotionEffect(fx(IqPotions.symptomSeptic, 40, (int) ((sev - 0.7F) * 2)));
            if (e.getRNG().nextInt(Math.max(8, 50 - (int) (sev * 40))) == 0) e.attackEntityFrom(DamageSource.GENERIC, sev * 2);
        }
    }

    public static class Prion extends PathogenCondition {
        @Override public PathogenType getType() { return PathogenType.PRION; }

        /** prions ramp over the whole disease instead of at a fixed rate */
        @Override
        public float getSeverityRamp(DiseaseDefinition def) {
            return (1.0F - def.baseSeverity) / Math.max(1, def.durationTicks - def.incubationTicks) * 100000F;
        }

        @Override
        public void tickSymptoms(Bloodstream blood, EntityLivingBase e, DiseaseDefinition def, DiseaseInstance instance) {
            long infectedFor = e.world.getTotalWorldTime() - instance.infectedAtTick;
            float progress = Math.min(1.0F, infectedFor / (float) Math.max(1, def.durationTicks));
            e.addPotionEffect(fx(IqPotions.symptomSchizophrenia, 40, (int) (instance.currentSeverity * 2)));
            e.addPotionEffect(fx(IqPotions.symptomParalysis, 40, (int) (instance.currentSeverity * 1.5F)));
            if (e.getRNG().nextInt(Math.max(20, (int) (80 - progress * 60))) == 0)
                e.attackEntityFrom(ModDamageSource.digamma, 1.0F + instance.currentSeverity * 2.0F);
        }
    }
}
