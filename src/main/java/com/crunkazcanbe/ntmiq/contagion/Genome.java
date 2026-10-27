package com.crunkazcanbe.ntmiq.contagion;

import java.util.Random;

import com.crunkazcanbe.ntmiq.contagion.DiseaseDefinition.PathogenType;
import com.hbm.handler.radiation.ChunkRadiationManager;
import net.minecraft.entity.EntityLivingBase;

/**
 * 24-character hex genome (IQ: handler/contagion/Genome, GPL-3): 8 for severity, 8 for resistance, 8 for transmission.
 * A disease drifts from its reference genome as it mutates, which makes it nastier and makes an old antiserum fit worse.
 * Everything here but {@link #tickMutation} is pure maths on strings.
 */
public final class Genome {
    private Genome() {}

    public static final int LENGTH = 24;
    public static final String ALPHABET = "0123456789ABCDEF";
    public static final int SEG_SIZE = 8;
    private static final int SEG_SEVERITY = 0, SEG_RESISTANCE = 8, SEG_TRANSMISSION = 16;

    /** every disease id has one fixed reference genome */
    public static String reference(String id) {
        Random rand = new Random(id == null ? 0 : id.hashCode());
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) sb.append(ALPHABET.charAt(rand.nextInt(ALPHABET.length())));
        return sb.toString();
    }

    public static String ensure(String genome, String id) {
        return genome == null || genome.length() != LENGTH ? reference(id) : genome;
    }

    public static String mutate(String genome, int flips, Random rand) {
        char[] chars = genome.toCharArray();
        for (int f = 0; f < flips; f++) chars[rand.nextInt(LENGTH)] = ALPHABET.charAt(rand.nextInt(ALPHABET.length()));
        return new String(chars);
    }

    public static float distance(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return 1.0F;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) if (a.charAt(i) != b.charAt(i)) diff++;
        return diff / (float) a.length();
    }

    public static float similarity(String a, String b) { return 1.0F - distance(a, b); }

    private static float segmentDrift(String genome, String ref, int start) {
        float sum = 0;
        for (int i = start; i < start + SEG_SIZE; i++) sum += Math.abs(value(genome.charAt(i)) - value(ref.charAt(i))) / 15.0F;
        return sum / SEG_SIZE;
    }

    private static int value(char c) { return Math.max(Character.digit(c, 16), 0); }

    /** severity / resistance / visibility of this instance, from how far its genome has drifted */
    public static void derive(DiseaseInstance instance, DiseaseDefinition def) {
        String ref = reference(def.id);
        String genome = ensure(instance.genome, def.id);
        instance.currentSeverity = clamp((def.baseSeverity + segmentDrift(genome, ref, SEG_SEVERITY) * 0.4F) * instance.attenuation);
        instance.resistanceLevel = clamp(def.baseResistance + segmentDrift(genome, ref, SEG_RESISTANCE) * 0.6F);
        instance.visibility = clamp(1.0F - segmentDrift(genome, ref, SEG_TRANSMISSION) * (1.0F + def.antigenMutability * 2.0F));
    }

    private static float clamp(float f) { return Math.max(0.0F, Math.min(1.0F, f)); }

    public static float getTypeMultiplier(PathogenType type) {
        if (type == null) return 1.0F;
        switch (type) {
            case VIRUS: return 3.0F;
            case BACTERIA: return 1.6F;
            case FUNGUS: return 0.6F;
            case PARASITE: return 1.0F;
            case PRION: return 0.0F;   // prions do not mutate
            default: return 2.0F;
        }
    }

    /** radiation speeds mutation up, as in IQ */
    public static float getRadBoost(EntityLivingBase entity) {
        try {
            double rads = ChunkRadiationManager.proxy.getRadiation(entity.world, entity.getPosition());
            return 1.0F + (float) Math.min(rads / 50.0D, 2.0D);
        } catch (Exception e) {
            return 1.0F;
        }
    }

    public static void tickMutation(DiseaseInstance instance, DiseaseDefinition def, EntityLivingBase entity) {
        if (entity == null || entity.world.isRemote || instance.ticksRemaining <= 0) return;
        int interval = Math.max(1, def.mutationIntervalTicks);
        if (entity.world.getTotalWorldTime() % interval != 0) return;

        instance.mutationAccumulator += def.baseMutationRate * getTypeMultiplier(def.type) * getRadBoost(entity);
        while (instance.mutationAccumulator >= 1.0F) {
            instance.mutationAccumulator -= 1.0F;
            instance.genome = mutate(ensure(instance.genome, def.id), 1, entity.world.rand);
            derive(instance, def);
        }
    }
}
