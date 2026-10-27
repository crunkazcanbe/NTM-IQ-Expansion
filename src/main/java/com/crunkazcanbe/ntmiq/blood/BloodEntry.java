package com.crunkazcanbe.ntmiq.blood;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/**
 * One thing dissolved in a bloodstream (IQ: handler/contagion/BloodEntry). Either a fluid ({@link #fluid} set,
 * {@link #frameId} null) or a pathogen ({@link #frameId} set, {@link #fluid} null) — pathogens belong to F02
 * contagion, which keeps whatever it needs in {@link #data}. Fluids are saved by NAME so add-on fluid IDs may shift.
 */
public class BloodEntry {
    public FluidType fluid;
    public float amount;
    public String frameId;
    public NBTTagCompound data;

    public BloodEntry(FluidType fluid, float amount, String frameId, NBTTagCompound data) {
        this.fluid = fluid;
        this.amount = amount;
        this.frameId = frameId;
        this.data = data;
    }

    public boolean isPathogen() { return frameId != null; }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        if (fluid != null) nbt.setString("fluid", fluid.getName());
        nbt.setFloat("amount", amount);
        if (frameId != null) nbt.setString("frame", frameId);
        if (data != null) nbt.setTag("mut", data);
        return nbt;
    }

    /** @return null for a fluid that no longer exists */
    public static BloodEntry readFromNBT(NBTTagCompound nbt) {
        String frame = nbt.hasKey("frame") ? nbt.getString("frame") : null;
        FluidType fluid = null;
        if (frame == null) {
            fluid = Fluids.fromName(nbt.getString("fluid"));
            if (fluid == Fluids.NONE) return null;
        }
        NBTTagCompound data = nbt.hasKey("mut", Constants.NBT.TAG_COMPOUND) ? nbt.getCompoundTag("mut") : null;
        return new BloodEntry(fluid, nbt.getFloat("amount"), frame, data);
    }
}
