package com.crunkazcanbe.ntmiq.contagion;

import java.util.Map;

import com.crunkazcanbe.ntmiq.contagion.DiseaseDefinition.PathogenType;
import com.crunkazcanbe.ntmiq.contagion.DiseaseDefinition.TransmissionAxis;
import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * A vial holding ONE read-out of a disease (IQ: handler/contagion/GenomeSample, GPL-3): either a genome segment or one
 * of its frame values. Collect enough of them on a floppy disk and you can rebuild the whole frame, which is what the
 * Dial-A-Dose needs to brew a matching antiserum.
 */
public final class GenomeSample {
    private GenomeSample() {}

    public static final String KEY_SAMPLE = "gsample";
    public static final String KEY_VALUE = "gsvalue";
    public static final String UNKNOWN_SEGMENT = "????????";

    public static final String[] KEYS = {
            "severity", "resistance", "transmission",
            "id", "name", "type",
            "incubation", "duration",
            "baseSeverity", "baseResistance",
            "mutationRate", "mutationInterval", "antigenMutability",
            "transmissionInterval", "aerosol", "touch", "hit", "proximity", "injected",
            "range", "wetBoost", "fomite",
            "uncurable" };

    public static final String[] CORE_KEYS = { "id", "type", "severity", "resistance", "transmission" };

    // IQ's "you guessed" fallbacks for a frame rebuilt from an incomplete disk: a very slow, very dull disease
    public static final int POOR_INCUBATION = 720_000;
    public static final int POOR_INTERVAL = 72_000;

    public static ItemStack make(String key, String value) {
        ItemStack vial = ItemGenomeVial.filled(IqFluids.DNA, ItemGenomeVial.MAX_FLUID);
        NBTTagCompound nbt = vial.getTagCompound();
        nbt.setString(KEY_SAMPLE, key);
        nbt.setString(KEY_VALUE, value);
        return vial;
    }

    public static String getKey(ItemStack vial) {
        return vial.hasTagCompound() && vial.getTagCompound().hasKey(KEY_SAMPLE) ? vial.getTagCompound().getString(KEY_SAMPLE) : null;
    }

    public static String getValue(ItemStack vial) {
        return vial.hasTagCompound() && vial.getTagCompound().hasKey(KEY_VALUE) ? vial.getTagCompound().getString(KEY_VALUE) : null;
    }

    public static String valueOf(String key, String genome, DiseaseDefinition def) {
        if (def == null) return "";
        String g = Genome.ensure(genome, def.id);
        switch (key) {
            case "severity": return g.substring(0, Genome.SEG_SIZE);
            case "resistance": return g.substring(Genome.SEG_SIZE, Genome.SEG_SIZE * 2);
            case "transmission": return g.substring(Genome.SEG_SIZE * 2, Genome.SEG_SIZE * 3);
            case "id": return def.id == null ? "" : def.id;
            case "name": return def.displayName == null ? "" : def.displayName;
            case "type": return def.type == null ? "" : def.type.name();
            case "incubation": return String.valueOf(def.incubationTicks);
            case "duration": return String.valueOf(def.durationTicks);
            case "baseSeverity": return String.valueOf(def.baseSeverity);
            case "baseResistance": return String.valueOf(def.baseResistance);
            case "mutationRate": return String.valueOf(def.baseMutationRate);
            case "mutationInterval": return String.valueOf(def.mutationIntervalTicks);
            case "antigenMutability": return String.valueOf(def.antigenMutability);
            case "transmissionInterval": return String.valueOf(def.transmissionIntervalTicks);
            case "aerosol": return String.valueOf(def.axis(TransmissionAxis.AEROSOL));
            case "touch": return String.valueOf(def.axis(TransmissionAxis.TOUCH));
            case "hit": return String.valueOf(def.axis(TransmissionAxis.HIT));
            case "proximity": return String.valueOf(def.axis(TransmissionAxis.PROXIMITY));
            case "injected": return String.valueOf(def.axis(TransmissionAxis.INJECTED));
            case "range": return String.valueOf(def.range);
            case "wetBoost": return String.valueOf(def.wetBoost);
            case "fomite": return String.valueOf(def.fomite);
            case "uncurable": return String.valueOf(def.uncurable);
            default: return "";
        }
    }

    /** the genome a disk's three segments spell out; missing segments stay "????????" */
    public static String buildGenome(Map<String, String> pieces) {
        return get(pieces, "severity", UNKNOWN_SEGMENT) + get(pieces, "resistance", UNKNOWN_SEGMENT) + get(pieces, "transmission", UNKNOWN_SEGMENT);
    }

    public static boolean isComplete(Map<String, String> pieces) {
        for (String key : CORE_KEYS) {
            String value = pieces == null ? null : pieces.get(key);
            if (value == null || value.isEmpty()) return false;
        }
        return true;
    }

    public static String getMissing(Map<String, String> pieces) {
        StringBuilder sb = new StringBuilder();
        for (String key : CORE_KEYS) {
            String value = pieces == null ? null : pieces.get(key);
            if (value != null && !value.isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(key);
        }
        return sb.toString();
    }

    /** rebuild a frame from the sections on a floppy disk; null until the five core keys are there */
    public static DiseaseDefinition buildDefinition(Map<String, String> pieces) {
        if (!isComplete(pieces)) return null;
        PathogenType type;
        try { type = PathogenType.valueOf(get(pieces, "type", "")); } catch (IllegalArgumentException e) { return null; }

        DiseaseDefinition def = new DiseaseDefinition();
        def.id = get(pieces, "id", "");
        def.displayName = get(pieces, "name", def.id);
        def.type = type;
        def.incubationTicks = intVal(pieces, "incubation", POOR_INCUBATION);
        def.durationTicks = intVal(pieces, "duration", 0);
        def.baseSeverity = floatVal(pieces, "baseSeverity", 0F);
        def.baseResistance = floatVal(pieces, "baseResistance", 0F);
        def.baseMutationRate = floatVal(pieces, "mutationRate", 0F);
        def.mutationIntervalTicks = intVal(pieces, "mutationInterval", POOR_INTERVAL);
        def.antigenMutability = floatVal(pieces, "antigenMutability", 0F);
        def.transmissionIntervalTicks = intVal(pieces, "transmissionInterval", POOR_INTERVAL);
        def.range = floatVal(pieces, "range", 0F);
        def.wetBoost = floatVal(pieces, "wetBoost", 0F);
        def.fomite = boolVal(pieces, "fomite");
        def.uncurable = boolVal(pieces, "uncurable");
        for (TransmissionAxis axis : TransmissionAxis.values())
            def.transmission.put(axis, floatVal(pieces, axis.name().toLowerCase(java.util.Locale.US), 0F));
        return def;
    }

    private static String get(Map<String, String> pieces, String key, String fallback) {
        String value = pieces == null ? null : pieces.get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static int intVal(Map<String, String> pieces, String key, int fallback) {
        try { return Integer.parseInt(get(pieces, key, null)); } catch (Exception e) { return fallback; }
    }

    private static float floatVal(Map<String, String> pieces, String key, float fallback) {
        try { return Float.parseFloat(get(pieces, key, null)); } catch (Exception e) { return fallback; }
    }

    private static boolean boolVal(Map<String, String> pieces, String key) {
        return Boolean.parseBoolean(get(pieces, key, "false"));
    }
}
