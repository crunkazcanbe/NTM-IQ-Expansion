package com.crunkazcanbe.ntmiq.husks;

import java.util.ArrayList;
import java.util.List;

import com.crunkazcanbe.ntmiq.blood.ItemMedicalSyringe;
import com.crunkazcanbe.ntmiq.husks.ItemHumanPart.Stat;
import com.crunkazcanbe.ntmiq.husks.ItemHumanPart.Trait;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * IQ's TileEntityCloner (NTM: IQ Edition, GPL-3; irislgtm / tam69 / IQ team). Slots: 0 medical syringe with a blood
 * sample (owner = genome), 1-5 one of each human part, 6 battery. The tank's fluid becomes the husk's blood (set with a
 * fluid identifier, sneak-click). 100 HE/t for 100 s, then the vat opens and a husk steps out.
 */
public class TileEntityCloner extends TileEntityMachineBase implements ITickable, IEnergyReceiverMK2, IFluidStandardReceiverMK2, IGUIProvider, IControlReceiver {

    public static final int SLOT_SYRINGE = 0, SLOT_PARTS = 1, SLOT_BATTERY = 6, ALL_PARTS = 5, PROCESS_TIME = 100 * 20, SPAWN_DELAY = 25;

    public long power;
    public final long maxPower = 1_000_000;
    public final long consumption = 100;
    public boolean active;
    public int progress;
    public int spawnDelay;
    public float door, prevDoor;
    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this);

    /** client: the sample owner, for the vat preview */
    public String clientOwner = "";

    public TileEntityCloner() {
        super(7, true, true);
    }

    @Override public String getDefaultName() { return "container.cloner"; }

    @Override
    public void update() {
        if (world.isRemote) {
            prevDoor = door;
            door = spawnDelay > 0 ? Math.min(1F, door + 0.05F) : Math.max(0F, door - 0.05F);
            return;
        }

        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, maxPower);

        if (spawnDelay > 0) spawnDelay--;   // only the door animation; the husk is spawned at once (IQ's delayed spawn was lost on unload)

        if (world.getTotalWorldTime() % 20 == 0) for (DirPos p : getConPos()) {
            trySubscribe(world, p);
            if (tank.getTankType() != Fluids.NONE) trySubscribe(tank.getTankType(), world, p);
        }

        if (canProcess()) {
            power -= consumption;
            if (++progress >= PROCESS_TIME) {
                progress = 0;
                cloneBody();
            }
        } else if (sampleUUID() == null) {
            progress = 0;
        }

        networkPackNT(25);
    }

    public boolean canProcess() {
        return active && spawnDelay == 0 && power >= consumption && sampleUUID() != null && tank.getTankType() != Fluids.NONE
                && tank.getFill() >= 1_000 && getDistinctParts().size() == ALL_PARTS;
    }

    /** one stack per part type, in slot order */
    public List<ItemStack> getDistinctParts() {
        List<ItemStack> parts = new ArrayList<>();
        boolean[] found = new boolean[ItemHumanPart.Part.values().length];
        for (int i = SLOT_PARTS; i < SLOT_BATTERY; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.getItem() != IqHusks.humanPart) continue;
            int m = ItemHumanPart.part(s).ordinal();
            if (found[m]) continue;
            found[m] = true;
            parts.add(s);
        }
        return parts;
    }

    String sampleUUID() {
        ItemStack s = inventory.getStackInSlot(SLOT_SYRINGE);
        if (!(s.getItem() instanceof ItemMedicalSyringe) || !s.hasTagCompound()) return null;
        String id = s.getTagCompound().getString(ItemMedicalSyringe.KEY_OWNER_UUID);
        return id.isEmpty() ? null : id;
    }

    private void cloneBody() {
        ItemStack syringe = inventory.getStackInSlot(SLOT_SYRINGE);
        String uuid = sampleUUID();
        if (uuid == null) return;

        int[] totals = new int[Stat.values().length];
        StringBuilder traits = new StringBuilder();
        for (ItemStack part : getDistinctParts()) {
            for (Stat stat : Stat.values()) totals[stat.ordinal()] += ItemHumanPart.getStat(part, stat);
            Trait t = ItemHumanPart.getTrait(part);
            if (t != null) traits.append(traits.length() > 0 ? "," : "").append(t.name());
        }
        for (int i = SLOT_PARTS; i < SLOT_BATTERY; i++) inventory.getStackInSlot(i).shrink(1);   // IQ voids all part slots; we eat one of each

        EntityHusk husk = new EntityHusk(world);
        husk.setupClone(uuid, syringe.getTagCompound().getString(ItemMedicalSyringe.KEY_OWNER_NAME), totals, traits.toString(), tank.getTankType());
        BlockPos out = spawnPos();
        husk.setLocationAndAngles(out.getX() + 0.5, out.getY(), out.getZ() + 0.5, facing().toEnumFacing().getHorizontalAngle(), 0F);
        world.spawnEntity(husk);
        spawnDelay = SPAWN_DELAY;

        syringe.setTagCompound(null);   // sample used up
        inventory.setStackInSlot(SLOT_SYRINGE, syringe);
        tank.setFill(tank.getFill() - 1_000);
        markDirty();
        world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 1F, 0.6F);
    }

    /** first free spot in front of the vat (the door side) */
    private BlockPos spawnPos() {
        EnumFacing f = facing().toEnumFacing();
        BlockPos base = pos;
        for (int i = 1; i <= 3; i++) {
            BlockPos p = base.offset(f, i);
            if (world.getBlockState(p).getBlock() != getBlockType() && world.isAirBlock(p) && world.isAirBlock(p.up())) return p;
        }
        return pos.up(3);   // ponytail: blocked in -> on the roof
    }

    ForgeDirection facing() { return ForgeDirection.getOrientation(getBlockMetadata() - BlockDummyable.offset); }

    /** every horizontal neighbour of the 2x2 bottom layer; parts are found by asking the block who their core is */
    public DirPos[] getConPos() {
        Block b = getBlockType();
        List<DirPos> list = new ArrayList<>();
        if (!(b instanceof BlockDummyable)) return new DirPos[0];
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos p = pos.add(dx, 0, dz);
            if (world.getBlockState(p).getBlock() != b || !pos.equals(((BlockDummyable) b).findCore(world, p))) continue;
            for (ForgeDirection d : new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST }) {
                BlockPos n = p.add(d.offsetX, 0, d.offsetZ);
                if (world.getBlockState(n).getBlock() != b) list.add(new DirPos(n.getX(), n.getY(), n.getZ(), d));
            }
        }
        return list.toArray(new DirPos[0]);
    }

    public void toggle() {
        active = !active;
        markDirty();
    }

    @Override public boolean hasPermission(EntityPlayer player) { return isUseableByPlayer(player); }

    @Override public void receiveControl(NBTTagCompound data) { if (data.getBoolean("toggle")) toggle(); }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(active);
        buf.writeInt(progress);
        buf.writeInt(spawnDelay);
        tank.serialize(buf);
        String owner = "";
        ItemStack s = inventory.getStackInSlot(SLOT_SYRINGE);
        if (sampleUUID() != null) owner = s.getTagCompound().getString(ItemMedicalSyringe.KEY_OWNER_UUID);
        byte[] b = owner.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeShort(b.length);
        buf.writeBytes(b);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        active = buf.readBoolean();
        progress = buf.readInt();
        spawnDelay = buf.readInt();
        tank.deserialize(buf);
        byte[] b = new byte[buf.readShort()];
        buf.readBytes(b);
        clientOwner = new String(b, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        power = nbt.getLong("power");
        active = nbt.getBoolean("active");
        progress = nbt.getInteger("progress");
        tank.readFromNBT(nbt, "tank");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setLong("power", power);
        nbt.setBoolean("active", active);
        nbt.setInteger("progress", progress);
        tank.writeToNBT(nbt, "tank");
        return super.writeToNBT(nbt);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_SYRINGE) return stack.getItem() instanceof ItemMedicalSyringe;
        if (slot == SLOT_BATTERY) return stack.getItem() instanceof IBatteryItem;
        return stack.getItem() == IqHusks.humanPart;
    }

    @Override public boolean canExtractItem(int slot, ItemStack stack, int amount) { return slot != SLOT_BATTERY; }

    @Override public int[] getAccessibleSlotsFromSide(EnumFacing side) { return new int[] { 0, 1, 2, 3, 4, 5 }; }

    @Override public long getPower() { return Math.max(0, Math.min(power, maxPower)); }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] { tank }; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] { tank }; }

    public void setTankType(FluidType type) {
        if (type == tank.getTankType()) return;
        tank.setTankType(type);
        markDirty();
    }

    @Override
    public Container provideContainer(int id, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerCloner(player.inventory, getCheckedInventory());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int id, EntityPlayer player, World world, int x, int y, int z) {
        return new GUICloner(player.inventory, this);
    }

    AxisAlignedBB bb;

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (bb == null) bb = new AxisAlignedBB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 3, pos.getZ() + 2);
        return bb;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() { return 65536.0D; }
}
