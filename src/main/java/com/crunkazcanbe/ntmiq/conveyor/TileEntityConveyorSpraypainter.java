package com.crunkazcanbe.ntmiq.conveyor;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityLoadedBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** IQ's TileEntityConveyorSpraypainter: 100 HE + 5 mB ink per item on the belt above it. */
public class TileEntityConveyorSpraypainter extends TileEntityLoadedBase implements ITickable, IEnergyReceiverMK2, IFluidStandardTransceiverMK2 {

    public static final long MAX_POWER = 50_000;
    public long power;
    public int usage = 100;
    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this);

    @Override
    public void update() {
        if (world.isRemote) return;
        for (DirPos p : getConPos()) {
            this.trySubscribe(world, p.getPos().getX(), p.getPos().getY(), p.getPos().getZ(), p.getDir());
            if (tank.getTankType() != Fluids.NONE) {
                this.trySubscribe(tank.getTankType(), world, p.getPos().getX(), p.getPos().getY(), p.getPos().getZ(), p.getDir());
                this.tryProvide(tank, world, p.getPos().getX(), p.getPos().getY(), p.getPos().getZ(), p.getDir());
            }
        }
        if (power >= usage) process();
        networkPackNT(50);
    }

    protected DirPos[] getConPos() {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new DirPos[] { new DirPos(x + 1, y, z, Library.POS_X), new DirPos(x - 1, y, z, Library.NEG_X),
                new DirPos(x, y, z + 1, Library.POS_Z), new DirPos(x, y, z - 1, Library.NEG_Z) };
    }

    public boolean process() {
        boolean did = false;
        for (EntityMovingItem item : world.getEntitiesWithinAABB(EntityMovingItem.class,
                new AxisAlignedBB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1.5, pos.getZ() + 1))) {
            ItemStack stack = item.getItemStack();
            if (stack.isEmpty()) continue;
            int count = stack.getCount();
            int needed = SpraypainterRecipes.AMOUNT * count;
            if (tank.getFill() < needed) continue;
            ItemStack output = SpraypainterRecipes.getOutput(stack, tank.getTankType());
            if (output.isEmpty()) continue;
            output.setCount(count);

            item.setDead();
            EntityMovingItem out = new EntityMovingItem(world);
            out.setPosition(item.posX, item.posY, item.posZ);
            out.setItemStack(output);
            world.spawnEntity(out);
            tank.setFill(tank.getFill() - needed);
            did = true;
        }
        if (did) {
            power -= usage;
            world.playSound(null, pos, IqConveyor.spraypaint, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        return did;
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        tank.deserialize(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        power = nbt.getLong("power");
        tank.readFromNBT(nbt, "tank");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setLong("power", power);
        tank.writeToNBT(nbt, "tank");
        return nbt;
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] { tank }; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] { tank }; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] { tank }; }

    @Override public boolean canConnect(ForgeDirection dir) { return dir != ForgeDirection.UP && dir != ForgeDirection.UNKNOWN; }
    @Override public boolean canConnect(FluidType type, ForgeDirection dir) { return dir != ForgeDirection.UP && dir != ForgeDirection.UNKNOWN; }

    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public void setPower(long power) { this.power = power; }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 2, 2));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }
}
