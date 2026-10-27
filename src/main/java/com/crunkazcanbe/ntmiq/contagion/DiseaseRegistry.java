package com.crunkazcanbe.ntmiq.contagion;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.crunkazcanbe.ntmiq.contagion.DiseaseDefinition.PathogenType;
import com.crunkazcanbe.ntmiq.contagion.DiseaseDefinition.TransmissionAxis;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Known disease frames (IQ: handler/contagion/DiseaseRegistry, GPL-3). Wild diseases are not registered up front — they
 * arrive inside item/entity NBT and are restored into the registry on first sight, so a save keeps its own outbreak.
 */
public final class DiseaseRegistry {
    private DiseaseRegistry() {}

    private static final Map<String, DiseaseDefinition> REGISTRY = new HashMap<>();

    public static void register(DiseaseDefinition def) { if (def != null && def.id != null) REGISTRY.put(def.id, def); }

    public static DiseaseDefinition get(String id) { return id == null ? null : REGISTRY.get(id); }

    /** registered frame, else the one carried in {@code frameDef} */
    public static DiseaseDefinition resolve(String frameId, NBTTagCompound frameDef) {
        DiseaseDefinition def = get(frameId);
        return def != null ? def : restoreDef(frameId, frameDef);
    }

    /** @param instanceNBT a serialized {@link DiseaseInstance} whose "def" tag holds the frame */
    public static DiseaseDefinition restore(String frameId, NBTTagCompound instanceNBT) {
        if (instanceNBT == null || !instanceNBT.hasKey("def")) return get(frameId);
        return restoreDef(frameId, instanceNBT.getCompoundTag("def"));
    }

    public static DiseaseDefinition restoreDef(String frameId, NBTTagCompound frameDef) {
        if (frameId == null || frameDef == null) return null;
        DiseaseDefinition existing = REGISTRY.get(frameId);
        if (existing != null) return existing;
        DiseaseDefinition def = DiseaseDefinition.readFromNBT(frameDef);
        if (def != null && frameId.equals(def.id) && def.type != null) {
            register(def);
            return def;
        }
        return null;
    }

    /** IQ's only hand-written disease: MKU, the slow incurable prion (ServerConfig-independent, it only spreads if asked to) */
    public static void seedDefaults() {
        if (REGISTRY.containsKey("mku")) return;
        EnumMap<TransmissionAxis, Float> trans = new EnumMap<>(TransmissionAxis.class);
        trans.put(TransmissionAxis.AEROSOL, 0.05F);
        trans.put(TransmissionAxis.TOUCH, 0.0F);
        trans.put(TransmissionAxis.HIT, 0.25F);
        trans.put(TransmissionAxis.PROXIMITY, 0.02F);
        trans.put(TransmissionAxis.INJECTED, 1.0F);
        register(new DiseaseDefinition("mku", "MKU", PathogenType.PRION,
                144000, 864000, 0.7F, trans, 60, 3.0F, 4.0F, false, 0.02F, 1800, 0.3F, 0.2F, true));
    }
}
