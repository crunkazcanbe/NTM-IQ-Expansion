package com.crunkazcanbe.ntmiq.contagion;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.util.I18nUtil;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;

/**
 * "Contagious" fluid trait (IQ: inventory/fluid/trait/FT_Pathogen, GPL-3): blood can carry diseases, so a syringe or
 * tank of it carries whatever its NBT says. Registered through IqFluids.registerTrait as "ntmiq_pathogen".
 */
public class FT_Pathogen extends FluidTrait {

    @Override
    public void addInfoHidden(List<String> info) {
        info.add(TextFormatting.RED + "[" + I18nUtil.resolveKey("hbmfluid.trait.contagious") + "]");
    }

    public static void infect(EntityLivingBase target, String frameId, float dose, float intensity, String genome) {
        if (!ContagionConfig.enabled || !ContagionConfig.injected) return;
        DiseaseDefinition frame = DiseaseRegistry.get(frameId);
        if (frame == null) return;
        DiseaseHandler.infect(target, frame, dose * Math.max(0.1F, intensity), genome);
    }

    /** the "pathogen" payload a medical syringe carries: a list of {frame, amount, mut{genome, def}} */
    public static void applyPayload(NBTTagList list, EntityLivingBase target, float intensity) {
        if (target == null || list == null) return;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            String frameId = entry.getString("frame");
            if (frameId.isEmpty()) continue;
            String genome = null;
            if (entry.hasKey("mut", Constants.NBT.TAG_COMPOUND)) {
                NBTTagCompound mut = entry.getCompoundTag("mut");
                if (mut.hasKey("genome")) genome = mut.getString("genome");
                DiseaseRegistry.restore(frameId, mut);   // a wild disease brings its own frame along
            }
            infect(target, frameId, entry.hasKey("amount") ? entry.getFloat("amount") : 10F, intensity, genome);
        }
    }

    @Override public void serializeJSON(JsonWriter writer) throws IOException {}
    @Override public void deserializeJSON(JsonObject obj) {}
}
