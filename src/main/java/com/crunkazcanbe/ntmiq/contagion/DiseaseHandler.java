package com.crunkazcanbe.ntmiq.contagion;

import java.util.ArrayList;
import java.util.List;

import com.crunkazcanbe.ntmiq.blood.BloodEntry;
import com.crunkazcanbe.ntmiq.blood.Bloodstream;
import com.crunkazcanbe.ntmiq.blood.BloodstreamAPI;
import com.crunkazcanbe.ntmiq.contagion.DiseaseDefinition.TransmissionAxis;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

/**
 * The contagion tick (IQ: handler/contagion/DiseaseHandler + the pathogen half of HbmBloodstreamProps, GPL-3;
 * irislgtm, tam69 and the IQ team). Every living entity carries F01's bloodstream, so players and mobs run the same
 * code here: a pathogen is a {@link BloodEntry} with a frame id whose {@code data} holds its {@link DiseaseInstance}.
 *
 * <p>Immunity is a list of cured genomes kept in the bloodstream's {@code extra} tag: an exact match makes you immune,
 * a near match only weakens the disease ({@link #attenuationFor}).
 */
public final class DiseaseHandler {
    private DiseaseHandler() {}

    public static final String KEY_IMMUNITY = "ntmiqImmunity";
    /** below this genome similarity an old infection gives you nothing */
    private static final float CROSS_IMMUNITY_SIM = 0.6F;
    private static final double CAP_RANGE = 32D;

    // ---------------- per-entity tick (registered as a BloodstreamAPI.Ticker) ----------------

    public static void tick(EntityLivingBase host, Bloodstream blood) {
        if (!ContagionConfig.enabled || host.world.isRemote) return;

        List<String> fomiteFrames = new ArrayList<>();
        boolean any = false;

        for (BloodEntry entry : new ArrayList<>(blood.getEntries())) {
            if (!entry.isPathogen() || entry.amount <= 0) continue;
            any = true;
            if (entry.data == null) entry.data = new NBTTagCompound();

            DiseaseInstance instance = DiseaseInstance.readFromNBT(entry.data);
            DiseaseDefinition def = DiseaseRegistry.resolve(entry.frameId, instance.frameDef);
            if (def == null) continue;

            if (instance.frameId == null || instance.frameId.isEmpty()) {
                instance.frameId = entry.frameId;
                instance.infectedAtTick = host.world.getTotalWorldTime();
                instance.ticksRemaining = def.durationTicks;
                instance.frameDef = def.toNBT();
            }

            if (!tickInstance(host, blood, def, instance)) {           // ran its course -> you are immune to this genome
                addImmunity(blood, Genome.ensure(instance.genome, def.id));
                entry.amount = 0;                                       // F01's tick drops spent pathogen entries
                continue;
            }

            instance.writeToNBT(entry.data);

            if (def.fomite && ContagionConfig.fomites && ContagionConfig.naturalSpread) fomiteFrames.add(def.id);
            if (ContagionConfig.naturalSpread) spread(host, blood, def, instance);
        }

        if (any) ItemContagion.tick(host, fomiteFrames);
    }

    /** @return false when the infection is over */
    public static boolean tickInstance(EntityLivingBase entity, Bloodstream blood, DiseaseDefinition def, DiseaseInstance instance) {
        PathogenCondition condition = PathogenCondition.get(def.type);
        if (condition == null) return false;

        if (instance.genome == null) {
            instance.genome = def.getReferenceGenome();
            Genome.derive(instance, def);
        }

        if (entity.world.getTotalWorldTime() - instance.infectedAtTick >= def.incubationTicks) {
            condition.tickSymptoms(blood, entity, def, instance);
            instance.currentSeverity = Math.min(1.0F,
                    instance.currentSeverity + condition.getSeverityRamp(def) / 100000F * instance.attenuation);
        }
        Genome.tickMutation(instance, def, entity);

        if (!def.uncurable) {
            if (instance.ticksRemaining > 0) instance.ticksRemaining--;
            if (instance.ticksRemaining <= 0) return false;
        }
        return true;
    }

    // ---------------- spreading ----------------

    private static void spread(EntityLivingBase host, Bloodstream blood, DiseaseDefinition def, DiseaseInstance instance) {
        PathogenCondition condition = PathogenCondition.get(def.type);
        if (isIncubating(host, def, instance, condition)) return;
        if (ContagionConfig.aerosol) spreadAxis(host, blood, def, instance, condition, TransmissionAxis.AEROSOL, def.transmissionIntervalTicks, 1.0D);
        if (ContagionConfig.touch) spreadAxis(host, blood, def, instance, condition, TransmissionAxis.TOUCH, 10, 0.0D);
        if (ContagionConfig.proximity) spreadAxis(host, blood, def, instance, condition, TransmissionAxis.PROXIMITY, 40, 2.0D);
    }

    private static void spreadAxis(EntityLivingBase host, Bloodstream blood, DiseaseDefinition def, DiseaseInstance instance,
            PathogenCondition condition, TransmissionAxis axis, int interval, double rangeMul) {
        float base = def.axis(axis);
        if (base <= 0 || host.world.getTotalWorldTime() % Math.max(1, interval) != 0) return;

        double range = axis == TransmissionAxis.AEROSOL && host.isWet() ? def.range * def.wetBoost : def.range * rangeMul;
        List<EntityLivingBase> nearby = host.world.getEntitiesWithinAABB(EntityLivingBase.class, host.getEntityBoundingBox().grow(range),
                e -> e != host);
        if (overCap(host, nearby, instance.frameId)) return;

        for (EntityLivingBase target : nearby) transmit(host, blood, target, def, instance, condition, axis, base, true);
    }

    /**
     * Server rule: an outbreak stays local. Once this many infected bodies stand around the carrier, it stops
     * finding new hosts (the ones already sick still run their course).
     */
    private static boolean overCap(EntityLivingBase host, List<EntityLivingBase> nearby, String frameId) {
        if (ContagionConfig.maxInfectedNearby <= 0) return false;
        int infected = 0;
        for (EntityLivingBase e : nearby) {
            if (host.getDistanceSq(e) > CAP_RANGE * CAP_RANGE) continue;
            if (BloodstreamAPI.get(e).hasPathogen(frameId) && ++infected >= ContagionConfig.maxInfectedNearby) return true;
        }
        return false;
    }

    private static boolean isIncubating(EntityLivingBase host, DiseaseDefinition def, DiseaseInstance instance, PathogenCondition condition) {
        if (condition != null && condition.isContagiousDuringIncubation()) return false;
        return host.world.getTotalWorldTime() - instance.infectedAtTick < def.incubationTicks;
    }

    private static void transmit(EntityLivingBase host, Bloodstream hostBlood, EntityLivingBase target, DiseaseDefinition def,
            DiseaseInstance instance, PathogenCondition condition, TransmissionAxis axis, float base, boolean armorBlocks) {
        if (!ContagionConfig.mobToPlayer && target instanceof EntityPlayer && !(host instanceof EntityPlayer)) return;
        if (isImmuneOrInfected(target, instance.frameId, instance.genome)) return;

        float spread = condition == null ? 1.0F : condition.getSpreadModifier(axis, instance);
        if (axis == TransmissionAxis.AEROSOL) spread *= 1.0F + Math.min(180, hostBlood.getCoughBoost()) / 150.0F;

        if (host.world.rand.nextFloat() >= base * spread) return;
        if (armorBlocks && isArmorProtected(target, def)) return;
        infect(target, def, 10F + def.baseSeverity * 10F, Genome.mutate(Genome.ensure(instance.genome, def.id), 1, host.world.rand));
    }

    /** IQ's melee axis; called from the add-on's LivingHurtEvent */
    public static void handleHit(EntityLivingBase attacker, EntityLivingBase target) {
        if (!ContagionConfig.enabled || !ContagionConfig.naturalSpread || !ContagionConfig.hit) return;
        if (attacker == null || target == null || attacker.world.isRemote) return;

        Bloodstream blood = BloodstreamAPI.get(attacker);
        for (BloodEntry entry : new ArrayList<>(blood.getEntries())) {
            if (!entry.isPathogen() || entry.amount <= 0) continue;
            DiseaseInstance instance = DiseaseInstance.readFromNBT(entry.data);
            instance.frameId = entry.frameId;
            DiseaseDefinition def = DiseaseRegistry.resolve(entry.frameId, instance.frameDef);
            if (def == null || def.axis(TransmissionAxis.HIT) <= 0) continue;
            PathogenCondition condition = PathogenCondition.get(def.type);
            if (isIncubating(attacker, def, instance, condition)) continue;
            transmit(attacker, blood, target, def, instance, condition, TransmissionAxis.HIT, def.axis(TransmissionAxis.HIT), true);
        }
    }

    public static boolean isArmorProtected(EntityLivingBase target, DiseaseDefinition def) {
        HazardClass haz = def == null ? null : def.getHazardClass();
        return haz != null && ArmorRegistry.hasProtection(target, EntityEquipmentSlot.HEAD, haz);
    }

    // ---------------- infecting and curing ----------------

    /** put a disease into a body, honouring cross-immunity. Shared by spread, syringes and pathogen fluids. */
    public static void infect(EntityLivingBase target, DiseaseDefinition def, float dose, String genome) {
        if (!ContagionConfig.enabled || target == null || def == null || def.id == null) return;
        Bloodstream blood = BloodstreamAPI.get(target);
        if (blood.hasPathogen(def.id)) {
            blood.addPathogen(def.id, dose, null);
            return;
        }
        String g = Genome.ensure(genome, def.id);
        if (hasImmunity(blood, g)) return;

        DiseaseInstance instance = new DiseaseInstance(def.id, target.world.getTotalWorldTime(), def.durationTicks);
        instance.genome = g;
        instance.attenuation = attenuationFor(blood, g);
        instance.frameDef = def.toNBT();
        Genome.derive(instance, def);
        blood.addPathogen(def.id, dose, instance.toNBT());
    }

    public static boolean isImmuneOrInfected(EntityLivingBase target, String frameId, String genome) {
        if (target == null || frameId == null) return false;
        Bloodstream blood = BloodstreamAPI.get(target);
        return blood.hasPathogen(frameId) || hasImmunity(blood, Genome.ensure(genome, frameId));
    }

    /** antiserum: eat away at a matching infection, and remember the genome once it is gone */
    public static void cure(EntityLivingBase target, PharmaProfile profile, float intensity) {
        if (target == null || profile == null || profile.target == null) return;
        Bloodstream blood = BloodstreamAPI.get(target);

        for (BloodEntry entry : new ArrayList<>(blood.getEntries())) {
            if (!entry.isPathogen() || !profile.target.equals(entry.frameId)) continue;
            DiseaseInstance instance = DiseaseInstance.readFromNBT(entry.data);
            instance.frameId = entry.frameId;
            DiseaseDefinition def = DiseaseRegistry.resolve(entry.frameId, instance.frameDef);
            if (def == null) { entry.amount = 0; return; }
            if (def.uncurable) return;

            instance.genome = Genome.ensure(instance.genome, def.id);
            float match = profile.targetGenome == null ? 1.0F : Genome.similarity(profile.targetGenome, instance.genome);
            float resistanceFactor = 1.0F / (1.0F + instance.resistanceLevel);
            entry.amount -= entry.amount * intensity * match * resistanceFactor;

            if (entry.amount <= 0) {
                addImmunity(blood, instance.genome);
            } else {
                instance.resistanceLevel = Math.min(1.0F, instance.resistanceLevel + (1.0F - match) * 0.3F);  // a bad match breeds resistance
                if (entry.data == null) entry.data = new NBTTagCompound();
                instance.writeToNBT(entry.data);
            }
            return;
        }
    }

    // ---------------- immunity, kept in the bloodstream's extra tag ----------------

    public static void addImmunity(Bloodstream blood, String genome) {
        if (genome == null) return;
        NBTTagList list = blood.extra.getTagList(KEY_IMMUNITY, Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) if (genome.equals(list.getStringTagAt(i))) return;
        list.appendTag(new NBTTagString(genome));
        blood.extra.setTag(KEY_IMMUNITY, list);
    }

    public static boolean hasImmunity(Bloodstream blood, String genome) {
        if (genome == null) return false;
        NBTTagList list = blood.extra.getTagList(KEY_IMMUNITY, Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) if (genome.equals(list.getStringTagAt(i))) return true;
        return false;
    }

    /** 1 = nothing in your blood recognises this genome, 0 = you have had it already */
    public static float attenuationFor(Bloodstream blood, String genome) {
        NBTTagList list = blood.extra.getTagList(KEY_IMMUNITY, Constants.NBT.TAG_STRING);
        float best = 0;
        for (int i = 0; i < list.tagCount(); i++) best = Math.max(best, Genome.similarity(genome, list.getStringTagAt(i)));
        if (best <= CROSS_IMMUNITY_SIM) return 1.0F;
        return Math.max(0F, Math.min(1F, (1.0F - best) / (1.0F - CROSS_IMMUNITY_SIM)));
    }
}
