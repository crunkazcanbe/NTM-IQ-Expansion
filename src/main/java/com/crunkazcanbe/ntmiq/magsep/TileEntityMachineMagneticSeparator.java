package com.crunkazcanbe.ntmiq.magsep;

import com.crunkazcanbe.ntmiq.contagion.DiseaseDefinition;
import com.crunkazcanbe.ntmiq.contagion.DiseaseInstance;
import com.crunkazcanbe.ntmiq.contagion.DiseaseRegistry;
import com.crunkazcanbe.ntmiq.contagion.GenomeSample;
import com.crunkazcanbe.ntmiq.contagion.ItemGenomeVial;
import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.BobMathUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Magnetic Separator: needs a magnetic disc (its tier sets the parallels and wears down while working); the drum closes
 * and spins up while working and opens again after 2 s idle. Slots: 0 battery, 1 input, 2 disc, 3-8 outputs.
 * Ported from NTM: IQ Edition (GPL-3; irislgtm, tam69 and the IQ team). IQ's genome-sample path (blood vial + heparin)
 * is not ported until the contagion system exists; the generic item recipe path is complete.
 */
public class TileEntityMachineMagneticSeparator extends TileEntityMachineBase implements ITickable, IEnergyReceiverMK2,
        IFluidStandardTransceiverMK2, IControlReceiver, IGUIProvider {

    public static final int[] OPENING_ANIMATION_TICKS = { 144, 43, 93 };
    public static final int[] CLOSING_ANIMATION_TICKS = { 65, 64, 231 };
    public static final float ROTATION_MULTIPLIER = 8.0F;

    public final FluidTankNTM inputTank, outputTank;
    public final ModuleMachineMagneticSeparator module;
    public long power;
    public long maxPower = 1_000_000;
    public boolean didProcess = false;
    public boolean open = true;
    public int idleTicks = 1000;

    // client animation state
    public int animationTicks, animAcceleration, animRotation, animPause, rotation;
    private AudioWrapper audio;

    public TileEntityMachineMagneticSeparator() {
        super(9);
        inputTank = new FluidTankNTM(Fluids.NONE, 24_000).withOwner(this);
        outputTank = new FluidTankNTM(Fluids.NONE, 24_000).withOwner(this);
        module = new ModuleMachineMagneticSeparator(0, this, inventory, 1, 3, inputTank, outputTank);
    }

    @Override
    public String getDefaultName() {
        return "container.ntmiq.magnetic_separator";
    }

    public int getParallels() {
        ItemStack disc = inventory.getStackInSlot(2);
        return disc.getItem() instanceof ItemMagneticDisc ? ItemMagneticDisc.getType(disc).parallels : 0;
    }

    @Override
    public void update() {
        if (maxPower <= 0) maxPower = 1_000_000;

        if (!world.isRemote) {
            GenericRecipe recipe = module.getRecipe();
            if (recipe != null) maxPower = recipe.power * 100;
            maxPower = BobMathUtil.max(power, maxPower, 1_000_000);
            power = Library.chargeTEFromItems(inventory, 0, power, maxPower);

            for (DirPos p : getConPos()) {
                trySubscribe(world, p);
                if (inputTank.getTankType() != Fluids.NONE) trySubscribe(inputTank.getTankType(), world, p);
                if (outputTank.getFill() > 0) tryProvide(outputTank, world, p);
            }

            int parallels = getParallels();
            boolean hasDisc = parallels > 0;
            if (hasDisc && processGenomeSamples()) {     // F02: a blood vial spun with heparin breaks into genome samples
                didProcess = true;
            } else {
                genomeProgress = 0;
                module.parallels = hasDisc ? parallels : 1;
                module.update(1D, module.parallels / 2D, hasDisc, ItemStack.EMPTY);
                didProcess = module.didProcess;
            }

            if (didProcess) {
                damageDisc();
                idleTicks = 0;
            } else if (idleTicks < 1000) {
                idleTicks++;
            }

            boolean shouldOpen = idleTicks > 40;
            if (open != shouldOpen) setState(shouldOpen);

            if (module.markDirty) markDirty();
            networkPackNT(50);
        } else {
            tickAnimation();
        }
    }

    private void tickAnimation() {
        if (animationTicks > 0) {
            animationTicks--;
            if (open) {
                stopAudio();
                if (animAcceleration < OPENING_ANIMATION_TICKS[0]) animAcceleration++;
                else if (animPause < OPENING_ANIMATION_TICKS[1]) animPause++;
                else if (animRotation < OPENING_ANIMATION_TICKS[2]) animRotation++;
            } else {
                if (animPause < CLOSING_ANIMATION_TICKS[0]) animPause++;
                else if (animRotation < CLOSING_ANIMATION_TICKS[1]) animRotation++;
                else if (animAcceleration < CLOSING_ANIMATION_TICKS[2]) animAcceleration++;
                else rotation++;
            }
        } else if (!open) {
            rotation++;
            if (audio == null) {
                audio = createAudioLoop();
                audio.startSound();
            } else if (!audio.isPlaying()) {
                audio = rebootAudio(audio);
            }
        }
    }

    private void setState(boolean state) {
        open = state;
        resetAnimation();
        world.playSound(null, pos, state ? IqMagSep.magOpen : IqMagSep.magClose, SoundCategory.BLOCKS, 1F, 1F);
    }

    private void resetAnimation() {
        stopAudio();
        animAcceleration = 0;
        animPause = 0;
        animRotation = 0;
        animationTicks = open ? 20 * 20 : 21 * 20;
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return MainRegistry.proxy.getLoopedSound(IqMagSep.magLoop, SoundCategory.BLOCKS, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, 1F, 16F, 1F);
    }

    @Override public void onChunkUnload() { super.onChunkUnload(); stopAudio(); }
    @Override public void invalidate() { super.invalidate(); stopAudio(); }

    private void stopAudio() {
        if (audio != null) { audio.stopSound(); audio = null; }
    }

    public static final int HEPARIN_PER_SAMPLE = 100;
    public static final int GENOME_DURATION = 100;
    public int genomeProgress = 0;

    /**
     * IQ's genome-sample path (F02): a blood vial carrying a disease, spun with heparin, splits into 1-6 random genome
     * samples of that disease — the read-outs you write onto a floppy disk for the Dial-A-Dose.
     */
    private boolean processGenomeSamples() {
        ItemStack vial = inventory.getStackInSlot(1);
        if (!(vial.getItem() instanceof ItemGenomeVial)) return false;
        String frame = ItemGenomeVial.readFrame(vial);
        if (frame == null) return false;
        if (inputTank.getTankType() != IqFluids.HEPARIN || inputTank.getFill() < HEPARIN_PER_SAMPLE) return false;

        boolean space = false;
        for (int i = 3; i <= 8; i++) if (inventory.getStackInSlot(i).isEmpty()) { space = true; break; }
        if (!space) return false;

        genomeProgress++;
        if (genomeProgress < GENOME_DURATION) return true;
        genomeProgress = 0;

        String genome = null;
        DiseaseDefinition def = null;
        if (vial.hasTagCompound() && vial.getTagCompound().hasKey("mut")) {
            DiseaseInstance inst = DiseaseInstance.readFromNBT(vial.getTagCompound().getCompoundTag("mut"));
            genome = inst.genome;
            def = DiseaseRegistry.resolve(frame, inst.frameDef);
        }
        if (def == null) def = DiseaseRegistry.get(frame);
        if (def == null) return false;
        if (genome == null) genome = def.getReferenceGenome();

        vial.shrink(1);
        if (vial.isEmpty()) inventory.setStackInSlot(1, ItemStack.EMPTY);
        inputTank.setFill(inputTank.getFill() - HEPARIN_PER_SAMPLE);

        List<String> keys = new ArrayList<>(Arrays.asList(GenomeSample.KEYS));
        Collections.shuffle(keys, world.rand);
        int count = 1 + world.rand.nextInt(Math.min(6, keys.size()));
        for (int i = 0; i < count; i++) {
            ItemStack sample = GenomeSample.make(keys.get(i), GenomeSample.valueOf(keys.get(i), genome, def));
            for (int s = 3; s <= 8; s++) if (inventory.getStackInSlot(s).isEmpty()) { inventory.setStackInSlot(s, sample); break; }
        }

        markDirty();
        return true;
    }

    /** one durability point per second of work; the disc breaks at 0 */
    private void damageDisc() {
        ItemStack disc = inventory.getStackInSlot(2);
        if (!(disc.getItem() instanceof ItemMagneticDisc) || world.getTotalWorldTime() % 20 != 0) return;
        int dur = ItemMagneticDisc.getDurability(disc) - 1;
        if (dur <= 0) {
            inventory.setStackInSlot(2, ItemStack.EMPTY);
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 1F, 1F);
        } else {
            ItemMagneticDisc.setDurability(disc, dur);
        }
    }

    public DirPos[] getConPos() {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new DirPos[] {
                new DirPos(x + 3, y, z, Library.POS_X), new DirPos(x - 3, y, z, Library.NEG_X),
                new DirPos(x, y, z + 3, Library.POS_Z), new DirPos(x, y, z - 3, Library.NEG_Z) };
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        inputTank.serialize(buf);
        outputTank.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(didProcess);
        buf.writeBoolean(open);
        module.serialize(buf);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        inputTank.deserialize(buf);
        outputTank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        didProcess = buf.readBoolean();
        boolean wasOpen = open;
        open = buf.readBoolean();
        if (open != wasOpen) resetAnimation();
        module.deserialize(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        inputTank.readFromNBT(nbt, "i0");
        outputTank.readFromNBT(nbt, "o0");
        power = nbt.getLong("power");
        maxPower = nbt.getLong("maxPower");
        open = nbt.getBoolean("open");
        module.readFromNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        inputTank.writeToNBT(nbt, "i0");
        outputTank.writeToNBT(nbt, "o0");
        nbt.setLong("power", power);
        nbt.setLong("maxPower", maxPower);
        nbt.setBoolean("open", open);
        module.writeToNBT(nbt);
        return super.writeToNBT(nbt);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true;
        if (slot == 2) return stack.getItem() instanceof ItemMagneticDisc;
        if (slot >= 3) return false;
        return module.isItemValid(slot, stack);
    }

    @Override public boolean canExtractItem(int slot, ItemStack stack, int amount) { return slot >= 3 && slot <= 8; }
    @Override public int[] getAccessibleSlotsFromSide(EnumFacing side) { return new int[] { 1, 3, 4, 5, 6, 7, 8 }; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] { inputTank }; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] { outputTank }; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] { inputTank, outputTank }; }

    @Override public boolean hasPermission(EntityPlayer player) { return isUseableByPlayer(player); }

    @Override
    public void receiveControl(NBTTagCompound data) {
        if (data.hasKey("index") && data.hasKey("selection") && data.getInteger("index") == 0) {
            module.recipe = data.getString("selection");
            markChanged();
        }
    }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerMachineMagneticSeparator(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIMachineMagneticSeparator(player.inventory, this);
    }

    private AxisAlignedBB bb;

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (bb == null) bb = new AxisAlignedBB(pos.getX() - 2, pos.getY(), pos.getZ() - 2, pos.getX() + 3, pos.getY() + 3, pos.getZ() + 3);
        return bb;
    }

    @Override @SideOnly(Side.CLIENT) public double getMaxRenderDistanceSquared() { return 65536D; }
}
