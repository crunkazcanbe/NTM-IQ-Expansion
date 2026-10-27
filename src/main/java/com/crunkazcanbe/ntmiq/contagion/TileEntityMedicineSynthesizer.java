package com.crunkazcanbe.ntmiq.contagion;

import com.crunkazcanbe.ntmiq.blood.IqBlood;
import com.crunkazcanbe.ntmiq.blood.ItemFluidSyringe;
import com.crunkazcanbe.ntmiq.blood.ItemMedicalSyringe;
import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ITickable;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Dial-A-Dose medicine synthesizer (IQ: tileentity/machine/TileEntityMedicineSynthesizer, GPL-3; irislgtm, tam69 and
 * the IQ team). Feed it a floppy disk describing a disease, a pharmaceutical computing unit and an empty medical
 * syringe:
 * <ul>
 * <li>tank holding ANTISERUM (or anything pharma): brews a cure matched to the genome on the disk, eating the blood
 *     vial of that disease in the process.</li>
 * <li>tank holding HUMAN_BLOOD (anything contagious): works the other way and grows the disease itself into the
 *     syringe. The Dial-A-Disease, as IQ puts it.</li>
 * </ul>
 * One batch takes the disease's own incubation time, so a fast bug is quick to fight and a slow one is a project.
 */
public class TileEntityMedicineSynthesizer extends TileEntityMachineBase implements ITickable, IEnergyReceiverMK2,
        IFluidStandardReceiverMK2, IGUIProvider {

    public static final int FLUID_PER_DOSE = 100;
    public static final int POWER_PER_TICK = 200;

    public final FluidTankNTM tank;
    public long power;
    public long maxPower = 1_000_000;
    public int progress;
    public int maxProgress = 100;
    public boolean active;
    public String displayGenome = "";

    public TileEntityMedicineSynthesizer() {
        super(7);
        tank = new FluidTankNTM(IqFluids.ANTISERUM, 16_000).withOwner(this);
    }

    @Override
    public String getDefaultName() {
        return "container.ntmiq.medicine_synthesizer";
    }

    /** the tank decides what this machine is for */
    public boolean isDiseaseMode() {
        return tank.getTankType().hasTrait(FT_Pathogen.class);
    }

    /** the disease the inserted floppy disk describes, either stored whole or rebuilt from its sections */
    public DiseaseDefinition getDefinition() {
        ItemStack floppy = inventory.getStackInSlot(2);
        if (floppy.isEmpty() || !(floppy.getItem() instanceof ItemGenomeDisk)) return null;
        DiseaseDefinition def = ItemGenomeDisk.getStoredDef(floppy);
        if (def == null) def = GenomeSample.buildDefinition(ItemGenomeDisk.getSections(floppy));
        return def;
    }

    private String readGenome() {
        ItemStack floppy = inventory.getStackInSlot(2);
        if (floppy.isEmpty() || !(floppy.getItem() instanceof ItemGenomeDisk)) return "";
        String genome = ItemGenomeDisk.getGenome(floppy);
        return genome == null ? "" : genome;
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        power = Library.chargeTEFromItems(inventory, 4, power, maxPower);
        tank.setType(6, inventory);   // fluid identifier in slot 6 picks antiserum (cure) or blood (disease)

        for (DirPos pos : getConPos()) {
            trySubscribe(world, pos);
            if (tank.getTankType() != Fluids.NONE) trySubscribe(tank.getTankType(), world, pos);
        }

        DiseaseDefinition def = getDefinition();
        maxProgress = def == null ? 100 : Math.max(1, def.incubationTicks);
        displayGenome = readGenome();

        boolean can = canProcess(def);
        active = can || progress > 0;

        if (can) {
            power -= POWER_PER_TICK;
            progress++;
            if (progress >= maxProgress) {
                progress = 0;
                process(def);
                markDirty();
            }
        } else {
            progress = 0;
        }

        networkPackNT(15);
    }

    private boolean canProcess(DiseaseDefinition def) {
        if (def == null || power < POWER_PER_TICK || tank.getFill() < FLUID_PER_DOSE) return false;

        ItemStack syringe = inventory.getStackInSlot(0);
        if (syringe.isEmpty() || !(syringe.getItem() instanceof ItemMedicalSyringe) || ItemFluidSyringe.fill(syringe) > 0) return false;
        if (!(inventory.getStackInSlot(3).getItem() instanceof ItemPharmaUnit)) return false;
        if (!inventory.getStackInSlot(5).isEmpty()) return false;

        if (!isDiseaseMode()) {       // a cure needs a blood sample of the disease it is meant for
            ItemStack vial = inventory.getStackInSlot(1);
            if (vial.isEmpty() || !(vial.getItem() instanceof ItemGenomeVial)) return false;
            String frame = ItemGenomeVial.readFrame(vial);
            if (frame == null || !frame.equals(def.id)) return false;
        }
        return true;
    }

    private void process(DiseaseDefinition def) {
        ItemStack syringe = inventory.getStackInSlot(0).copy();
        String genome = readGenome();

        if (isDiseaseMode()) {
            ItemFluidSyringe.set(syringe, tank.getTankType(), ItemMedicalSyringe.MAX_DOSE);

            NBTTagCompound mut = new NBTTagCompound();
            if (!genome.isEmpty()) mut.setString("genome", genome);
            mut.setTag("def", def.toNBT());

            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("frame", def.id == null ? "" : def.id);
            entry.setFloat("amount", 100F);
            entry.setTag("mut", mut);

            NBTTagList list = new NBTTagList();
            list.appendTag(entry);
            syringe.getTagCompound().setTag(ItemMedicalSyringe.KEY_PATHOGENS, list);
        } else {
            ItemFluidSyringe.set(syringe, tank.getTankType(), ItemMedicalSyringe.MAX_DOSE);
            PharmaProfile profile = new PharmaProfile();
            profile.target = def.id;
            profile.targetGenome = genome.isEmpty() ? null : genome;
            syringe.getTagCompound().setTag(PharmaProfile.KEY, profile.toNBT());
            inventory.setStackInSlot(1, ItemStack.EMPTY);
        }

        ItemStack pharma = inventory.getStackInSlot(3);
        pharma.setItemDamage(pharma.getItemDamage() + 1);
        if (pharma.getItemDamage() >= pharma.getMaxDamage()) inventory.setStackInSlot(3, ItemStack.EMPTY);

        tank.setFill(tank.getFill() - FLUID_PER_DOSE);
        inventory.setStackInSlot(5, syringe);
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    }

    public long getPowerScaled(long i) { return power * i / Math.max(1, maxPower); }
    public int getProgressScaled(int i) { return progress * i / Math.max(1, maxProgress); }

    public DirPos[] getConPos() {
        ForgeDirection dir = ForgeDirection.getOrientation(getBlockMetadata() - BlockDummyable.offset);
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        // ports on the four sides of the core column, at the machine's own level and the one above
        return new DirPos[] {
                new DirPos(x + 2, y, z, Library.POS_X), new DirPos(x - 2, y, z, Library.NEG_X),
                new DirPos(x, y, z + 2, Library.POS_Z), new DirPos(x, y, z - 2, Library.NEG_Z),
                new DirPos(x + dir.offsetX, y + 1, z + dir.offsetZ, dir) };
    }

    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) {
        switch (slot) {
            case 0: return stack.getItem() instanceof ItemMedicalSyringe && ItemFluidSyringe.fill(stack) == 0;
            case 1: return !isDiseaseMode() && stack.getItem() instanceof ItemGenomeVial;
            case 2: return stack.getItem() instanceof ItemGenomeDisk;
            case 3: return stack.getItem() instanceof ItemPharmaUnit;
            case 4: case 6: return true;
            default: return false;
        }
    }

    @Override public boolean canExtractItem(int slot, ItemStack stack, int amount) { return slot == 5; }
    @Override public int[] getAccessibleSlotsFromSide(net.minecraft.util.EnumFacing side) { return new int[] { 0, 1, 2, 3, 5 }; }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeInt(maxProgress);
        buf.writeBoolean(active);
        tank.serialize(buf);
        BufferUtil.writeString(buf, displayGenome);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        maxProgress = buf.readInt();
        active = buf.readBoolean();
        tank.deserialize(buf);
        displayGenome = BufferUtil.readString(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        power = nbt.getLong("power");
        progress = nbt.getInteger("progress");
        maxProgress = nbt.hasKey("maxProgress") ? nbt.getInteger("maxProgress") : 100;
        displayGenome = nbt.getString("displayGenome");
        tank.readFromNBT(nbt, "tank");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setLong("power", power);
        nbt.setInteger("progress", progress);
        nbt.setInteger("maxProgress", maxProgress);
        nbt.setString("displayGenome", displayGenome);
        tank.writeToNBT(nbt, "tank");
        return super.writeToNBT(nbt);
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] { tank }; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] { tank }; }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerMedicineSynthesizer(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIMedicineSynthesizer(player.inventory, this);
    }

    private net.minecraft.util.math.AxisAlignedBB bb;

    @Override
    public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
        if (bb == null) bb = new net.minecraft.util.math.AxisAlignedBB(pos.getX() - 2, pos.getY(), pos.getZ() - 2,
                pos.getX() + 3, pos.getY() + 3, pos.getZ() + 3);
        return bb;
    }

    @Override @SideOnly(Side.CLIENT) public double getMaxRenderDistanceSquared() { return 65536D; }
}
