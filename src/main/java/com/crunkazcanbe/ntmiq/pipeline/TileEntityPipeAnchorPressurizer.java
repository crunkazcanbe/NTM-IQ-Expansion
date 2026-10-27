package com.crunkazcanbe.ntmiq.pipeline;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import org.jetbrains.annotations.NotNull;

/**
 * IQ's TileEntityPipeAnchorPressurizer: buffers one fluid between the pipe in front and the Industrial Pipe Anchor on top.
 * Only moves fluid while it has compressed air (back side / bottom), 1 mB per tick.
 */
public class TileEntityPipeAnchorPressurizer extends TileEntityLoadedBase implements ITickable, IFluidStandardTransceiverMK2 {

    public final FluidTankNTM[] tanks = new FluidTankNTM[2];
    public int airConsumption = 1;

    public TileEntityPipeAnchorPressurizer() {
        tanks[0] = new FluidTankNTM(Fluids.NONE, 24_000).withOwner(this);
        tanks[1] = new FluidTankNTM(Fluids.AIR, 24_000).withOwner(this);
    }

    ForgeDirection facing() {
        return ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        boolean operating = tanks[0].getFill() > 0 && tanks[1].getFill() > 0;
        if (operating) tanks[1].setFill(Math.max(0, tanks[1].getFill() - airConsumption));

        ForgeDirection dir = facing();
        ForgeDirection back = dir.getOpposite();
        int y = pos.getY();

        for (int[] off : getBlocks()) {
            int bx = pos.getX() + off[0];
            int bz = pos.getZ() + off[1];

            if (tanks[0].getTankType() != Fluids.NONE) {
                this.trySubscribe(tanks[0].getTankType(), world, bx + dir.offsetX, y, bz + dir.offsetZ, dir);
                this.trySubscribe(tanks[0].getTankType(), world, bx, y + 1, bz, ForgeDirection.UP);
                if (operating) {
                    this.tryProvide(tanks[0], world, bx + dir.offsetX, y, bz + dir.offsetZ, dir);
                    this.tryProvide(tanks[0], world, bx, y + 1, bz, ForgeDirection.UP);
                }
            }

            this.trySubscribe(Fluids.AIR, world, bx + back.offsetX, y, bz + back.offsetZ, back);
            this.trySubscribe(Fluids.AIR, world, bx, y - 1, bz, ForgeDirection.DOWN);
        }

        this.networkPackNT(20);
    }

    /** the 2x2 footprint, relative to the core */
    public int[][] getBlocks() {
        switch (this.getBlockMetadata() - BlockDummyable.offset) {
            case 3: return new int[][] {{0, 0}, {-1, 0}, {0, -1}, {-1, -1}};
            case 4: return new int[][] {{0, 0}, {1, 0}, {0, -1}, {1, -1}};
            case 5: return new int[][] {{0, 0}, {-1, 0}, {0, 1}, {-1, 1}};
            default: return new int[][] {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        tanks[0].readFromNBT(nbt, "t0");
        tanks[1].readFromNBT(nbt, "t1");
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        tanks[0].writeToNBT(nbt, "t0");
        tanks[1].writeToNBT(nbt, "t1");
        return super.writeToNBT(nbt);
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        ForgeDirection facing = facing();
        if (dir == ForgeDirection.DOWN || dir == facing.getOpposite()) return type == Fluids.AIR;
        if (dir == facing) return type == tanks[0].getTankType() || tanks[0].getTankType() == Fluids.NONE;
        return false;
    }

    @Override public @NotNull FluidTankNTM[] getReceivingTanks() { return tanks; }
    @Override public @NotNull FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] {tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
}
