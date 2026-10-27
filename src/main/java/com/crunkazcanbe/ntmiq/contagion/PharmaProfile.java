package com.crunkazcanbe.ntmiq.contagion;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * What an antiserum was brewed against (IQ: handler/contagion/PharmaProfile, GPL-3): a disease id and the genome the
 * Dial-A-Dose matched it to. The closer that genome is to the one in the patient, the better the cure works.
 */
public class PharmaProfile {

    public static final String KEY = "pharma";

    public String target;
    public String targetGenome;

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString("target", target == null ? "" : target);
        if (targetGenome != null) nbt.setString("targetGenome", targetGenome);
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return nbt;
    }

    public static PharmaProfile readFromNBT(NBTTagCompound nbt) {
        PharmaProfile p = new PharmaProfile();
        if (nbt == null) return p;
        if (nbt.hasKey("target")) p.target = nbt.getString("target");
        if (nbt.hasKey("targetGenome")) p.targetGenome = nbt.getString("targetGenome");
        return p;
    }

    /** @return null when the stack carries no cure */
    public static PharmaProfile from(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound() || !stack.getTagCompound().hasKey(KEY)) return null;
        return readFromNBT(stack.getTagCompound().getCompoundTag(KEY));
    }
}
