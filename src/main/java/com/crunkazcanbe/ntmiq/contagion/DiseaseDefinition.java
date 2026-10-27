package com.crunkazcanbe.ntmiq.contagion;

import java.util.EnumMap;

import com.hbm.util.ArmorRegistry.HazardClass;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

/**
 * One disease "frame" (IQ: handler/contagion/DiseaseDefinition, GPL-3; irislgtm, tam69 and the IQ team): what a
 * pathogen is and how it behaves. Frames are shared by every instance of that disease and travel in NBT (syringes,
 * floppy disks, infected entities) so a wild disease survives a world reload even though nothing registered it.
 */
public class DiseaseDefinition {

    public enum PathogenType { VIRUS, BACTERIA, FUNGUS, PARASITE, PRION }
    public enum TransmissionAxis { AEROSOL, TOUCH, HIT, PROXIMITY, INJECTED }

    public String id = "";
    public String displayName = "";
    public PathogenType type;
    public int incubationTicks;
    public int durationTicks;
    public float baseSeverity = 0.5F;
    public EnumMap<TransmissionAxis, Float> transmission = new EnumMap<>(TransmissionAxis.class);
    public int transmissionIntervalTicks = 20;
    public float range = 2.0F;
    public float wetBoost = 8.0F;
    public boolean fomite;
    public float baseMutationRate = 0.05F;
    public int mutationIntervalTicks = 1200;
    public float antigenMutability;
    public float baseResistance;
    /** prions: antiserum never clears it (IQ keeps this as drama, the cure path is the same) */
    public boolean uncurable;

    public DiseaseDefinition() {}

    public DiseaseDefinition(String id, String displayName, PathogenType type, int incubationTicks, int durationTicks,
            float baseSeverity, EnumMap<TransmissionAxis, Float> transmission, int transmissionIntervalTicks, float range,
            float wetBoost, boolean fomite, float baseMutationRate, int mutationIntervalTicks, float antigenMutability,
            float baseResistance, boolean uncurable) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.incubationTicks = incubationTicks;
        this.durationTicks = durationTicks;
        this.baseSeverity = baseSeverity;
        this.transmission = transmission;
        this.transmissionIntervalTicks = transmissionIntervalTicks;
        this.range = range;
        this.wetBoost = wetBoost;
        this.fomite = fomite;
        this.baseMutationRate = baseMutationRate;
        this.mutationIntervalTicks = mutationIntervalTicks;
        this.antigenMutability = antigenMutability;
        this.baseResistance = baseResistance;
        this.uncurable = uncurable;
    }

    public String getReferenceGenome() { return Genome.reference(id); }

    public float axis(TransmissionAxis axis) {
        Float value = transmission == null ? null : transmission.get(axis);
        return value == null ? 0F : value;
    }

    public HazardClass getHazardClass() { return getHazardClass(type); }

    /**
     * CE only has a BACTERIA hazard class, so virus/bacteria/prion ride on it and the bigger fungal spores / parasite
     * eggs use the particle classes — a gas mask or hazmat with the matching filter still blocks the disease.
     */
    public static HazardClass getHazardClass(PathogenType type) {
        if (type == null) return null;
        switch (type) {
            case FUNGUS: return HazardClass.PARTICLE_FINE;
            case PARASITE: return HazardClass.PARTICLE_COARSE;
            default: return HazardClass.BACTERIA;
        }
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("id", id);
        nbt.setString("disp", displayName == null ? "" : displayName);
        if (type != null) nbt.setString("type", type.name());
        nbt.setInteger("incub", incubationTicks);
        nbt.setInteger("dur", durationTicks);
        nbt.setFloat("sev", baseSeverity);
        nbt.setInteger("transInterval", transmissionIntervalTicks);
        nbt.setFloat("range", range);
        nbt.setFloat("wet", wetBoost);
        nbt.setBoolean("fomite", fomite);
        nbt.setFloat("mutRate", baseMutationRate);
        nbt.setInteger("mutInterval", mutationIntervalTicks);
        nbt.setFloat("ag", antigenMutability);
        nbt.setFloat("res", baseResistance);
        nbt.setBoolean("uncurable", uncurable);

        NBTTagList list = new NBTTagList();
        if (transmission != null) for (TransmissionAxis axis : transmission.keySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("axis", axis.name());
            tag.setFloat("mult", transmission.get(axis));
            list.appendTag(tag);
        }
        nbt.setTag("trans", list);
        return nbt;
    }

    public static DiseaseDefinition readFromNBT(NBTTagCompound nbt) {
        if (nbt == null) return null;
        DiseaseDefinition def = new DiseaseDefinition();
        def.id = nbt.getString("id");
        def.displayName = nbt.getString("disp");
        String typeName = nbt.getString("type");
        if (!typeName.isEmpty()) try { def.type = PathogenType.valueOf(typeName); } catch (IllegalArgumentException e) { /* dropped type */ }
        def.incubationTicks = nbt.getInteger("incub");
        def.durationTicks = nbt.getInteger("dur");
        def.baseSeverity = nbt.getFloat("sev");
        def.transmissionIntervalTicks = nbt.getInteger("transInterval");
        def.range = nbt.getFloat("range");
        def.wetBoost = nbt.getFloat("wet");
        def.fomite = nbt.getBoolean("fomite");
        def.baseMutationRate = nbt.getFloat("mutRate");
        def.mutationIntervalTicks = nbt.getInteger("mutInterval");
        def.antigenMutability = nbt.getFloat("ag");
        def.baseResistance = nbt.getFloat("res");
        def.uncurable = nbt.getBoolean("uncurable");

        NBTTagList list = nbt.getTagList("trans", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            try { def.transmission.put(TransmissionAxis.valueOf(tag.getString("axis")), tag.getFloat("mult")); }
            catch (IllegalArgumentException e) { /* dropped axis */ }
        }
        return def;
    }
}
