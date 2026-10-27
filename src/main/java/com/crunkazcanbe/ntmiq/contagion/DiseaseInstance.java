package com.crunkazcanbe.ntmiq.contagion;

import net.minecraft.nbt.NBTTagCompound;

/** One infection in one body (IQ: handler/contagion/DiseaseInstance, GPL-3). Lives in the bloodstream entry's NBT. */
public class DiseaseInstance {

    public String frameId;
    public long infectedAtTick;
    public float currentSeverity;
    public float resistanceLevel;
    public float mutationAccumulator;
    public long ticksRemaining;
    public String genome;
    public float visibility = 1.0F;
    /** cross-immunity damping: 1 = full force, 0 = your blood already knows this genome */
    public float attenuation = 1.0F;
    public NBTTagCompound frameDef;

    public DiseaseInstance() {}

    public DiseaseInstance(String frameId, long infectedAtTick, long ticksRemaining) {
        this.frameId = frameId;
        this.infectedAtTick = infectedAtTick;
        this.ticksRemaining = ticksRemaining;
    }

    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setString("frame", frameId == null ? "" : frameId);
        nbt.setLong("infTick", infectedAtTick);
        nbt.setFloat("severity", currentSeverity);
        nbt.setFloat("resist", resistanceLevel);
        nbt.setFloat("mutAcc", mutationAccumulator);
        nbt.setLong("ticks", ticksRemaining);
        if (genome != null) nbt.setString("genome", genome);
        nbt.setFloat("vis", visibility);
        nbt.setFloat("att", attenuation);
        if (frameDef != null) nbt.setTag("def", frameDef);
    }

    public static DiseaseInstance readFromNBT(NBTTagCompound nbt) {
        DiseaseInstance i = new DiseaseInstance();
        if (nbt == null) return i;
        if (nbt.hasKey("frame")) i.frameId = nbt.getString("frame");
        i.infectedAtTick = nbt.getLong("infTick");
        i.currentSeverity = nbt.getFloat("severity");
        i.resistanceLevel = nbt.getFloat("resist");
        i.mutationAccumulator = nbt.getFloat("mutAcc");
        i.ticksRemaining = nbt.getLong("ticks");
        if (nbt.hasKey("genome")) i.genome = nbt.getString("genome");
        if (nbt.hasKey("vis")) i.visibility = nbt.getFloat("vis");
        if (nbt.hasKey("att")) i.attenuation = nbt.getFloat("att");
        if (nbt.hasKey("def")) i.frameDef = nbt.getCompoundTag("def");
        return i;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        writeToNBT(nbt);
        return nbt;
    }
}
