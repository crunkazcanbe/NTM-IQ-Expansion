package com.crunkazcanbe.ntmiq.contagion;

import java.util.EnumMap;
import java.util.Random;

import com.crunkazcanbe.ntmiq.blood.ItemFluidSyringe;
import com.crunkazcanbe.ntmiq.blood.ItemMedicalSyringe;
import com.crunkazcanbe.ntmiq.contagion.DiseaseDefinition.PathogenType;
import com.crunkazcanbe.ntmiq.contagion.DiseaseDefinition.TransmissionAxis;
import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * Rolls a brand-new disease (IQ: handler/contagion/WildDisease, GPL-3) — never a prion, those are MKU's business. Wild
 * frames are not registered on sight: they ride along in the syringe's NBT and only enter the registry when something
 * reads them, so every world grows its own set.
 */
public final class WildDisease {
    private WildDisease() {}

    private static final PathogenType[] TYPES = { PathogenType.VIRUS, PathogenType.BACTERIA, PathogenType.FUNGUS, PathogenType.PARASITE };
    private static final String NAME_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static DiseaseDefinition generate(Random rand) {
        String id = "wild_" + Integer.toHexString(rand.nextInt(0xFFFFFF)) + "_" + Integer.toHexString(rand.nextInt(0xFFFF));
        PathogenType type = TYPES[rand.nextInt(TYPES.length)];

        EnumMap<TransmissionAxis, Float> transmission = new EnumMap<>(TransmissionAxis.class);
        transmission.put(TransmissionAxis.AEROSOL, 0.02F + rand.nextFloat() * 0.08F);
        if (rand.nextBoolean()) transmission.put(TransmissionAxis.TOUCH, 0.05F + rand.nextFloat() * 0.2F);
        if (rand.nextBoolean()) transmission.put(TransmissionAxis.PROXIMITY, 0.01F + rand.nextFloat() * 0.05F);
        if (type == PathogenType.PARASITE) transmission.put(TransmissionAxis.HIT, 0.05F + rand.nextFloat() * 0.15F);

        boolean fomite = type == PathogenType.FUNGUS || type == PathogenType.PARASITE || rand.nextBoolean();

        return new DiseaseDefinition(id, generateName(rand), type,
                (60 + rand.nextInt(1140)) * 20,              // incubation: 1-20 minutes
                (1800 + rand.nextInt(12600)) * 20,           // duration: 30 minutes - 4 hours
                0.15F + rand.nextFloat() * 0.35F,
                transmission, 40 + rand.nextInt(120), 1.5F + rand.nextFloat() * 2.0F, 4.0F, fomite,
                0.02F + rand.nextFloat() * 0.08F, 600 + rand.nextInt(1200), rand.nextFloat() * 0.4F,
                rand.nextFloat() * 0.2F, false);
    }

    private static String generateName(Random rand) {
        StringBuilder sb = new StringBuilder();
        int letters = 3 + rand.nextInt(2);
        for (int i = 0; i < letters; i++) sb.append(NAME_CHARS.charAt(rand.nextInt(NAME_CHARS.length())));
        return sb.append(String.format("%02d", rand.nextInt(100))).toString();
    }

    /** a filled medical syringe carrying one wild disease (chest loot, rare zombie drop, Vector Lab Kit) */
    public static ItemStack makeSyringe(Random rand) {
        DiseaseDefinition def = generate(rand);

        ItemStack syringe = new ItemStack(com.crunkazcanbe.ntmiq.blood.IqBlood.medicalSyringe);
        ItemFluidSyringe.set(syringe, IqFluids.HUMAN_BLOOD, ItemMedicalSyringe.MAX_DOSE);

        NBTTagCompound mut = new NBTTagCompound();
        mut.setString("genome", def.getReferenceGenome());
        mut.setTag("def", def.toNBT());

        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("frame", def.id);
        entry.setFloat("amount", 100F);
        entry.setTag("mut", mut);

        NBTTagList list = new NBTTagList();
        list.appendTag(entry);
        syringe.getTagCompound().setTag(ItemMedicalSyringe.KEY_PATHOGENS, list);
        return syringe;
    }
}
