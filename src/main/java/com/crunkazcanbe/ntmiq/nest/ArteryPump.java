package com.crunkazcanbe.ntmiq.nest;

import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * Artery Pump: taps a Satan's Blood bedrock artery ({@link NestBlocks.Artery}) anywhere straight below it. IQ lets its
 * derricks read the artery as a fluid reserve; CE's oil drills hard-code CE's oil blocks with no API, so this is our own
 * simple pump. 1 000 HE per 100 mB every second, infinite like bedrock oil, pushes into pipes on all sides. Right-click shows the tank.
 */
public final class ArteryPump {
    private ArteryPump() {}

    public static class PumpBlock extends BlockContainer {
        PumpBlock() { super(Material.IRON); setSoundType(SoundType.METAL); }

        @Override public TileEntity createNewTileEntity(World world, int meta) { return new PumpTile(); }
        @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hx, float hy, float hz) {
            TileEntity te = world.getTileEntity(pos);
            if (world.isRemote || !(te instanceof PumpTile)) return true;
            PumpTile t = (PumpTile) te;
            player.sendStatusMessage(new TextComponentString(t.tank.getTankType().getLocalizedName() + ": " + t.tank.getFill() + " / " + t.tank.getMaxFill()
                    + " mB, " + t.power + " HE" + (t.arteryY < 0 ? " - no artery below" : "")), true);
            return true;
        }
    }

    public static class PumpTile extends TileEntityLoadedBase implements ITickable, IEnergyReceiverMK2, IFluidStandardSenderMK2 {
        public static final long MAX_POWER = 50_000, POWER_PER_OP = 1_000;
        public static final int PER_OP = 100;

        public final FluidTankNTM tank = new FluidTankNTM(IqFluids.SATANS_BLOOD, 16_000).withOwner(this);
        public long power;
        int arteryY = -1;

        @Override
        public void update() {
            if (world.isRemote) return;
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                int x = pos.getX() + d.offsetX, y = pos.getY() + d.offsetY, z = pos.getZ() + d.offsetZ;
                trySubscribe(world, x, y, z, d);
                if (tank.getFill() > 0) tryProvide(tank, world, x, y, z, d);
            }
            if (world.getTotalWorldTime() % 20 != 0) return;
            if (arteryY < 0 || world.getBlockState(new BlockPos(pos.getX(), arteryY, pos.getZ())).getBlock() != IqNest.artery) arteryY = findArtery();
            if (arteryY >= 0 && power >= POWER_PER_OP && tank.getFill() + PER_OP <= tank.getMaxFill()) {
                power -= POWER_PER_OP;
                tank.setFill(tank.getFill() + PER_OP);
                markDirty();
            }
        }

        private int findArtery() {
            for (int y = pos.getY() - 1; y >= 0; y--) if (world.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())).getBlock() == IqNest.artery) return y;
            return -1;
        }

        @Override public long getPower() { return power; }
        @Override public void setPower(long power) { this.power = power; }
        @Override public long getMaxPower() { return MAX_POWER; }

        @Override public @NotNull FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[] { tank }; }
        @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] { tank }; }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            super.readFromNBT(nbt);
            tank.readFromNBT(nbt, "tank");
            power = nbt.getLong("power");
        }

        @Override
        public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            tank.writeToNBT(nbt, "tank");
            nbt.setLong("power", power);
            return super.writeToNBT(nbt);
        }

        @Override public void serialize(ByteBuf buf) { super.serialize(buf); tank.serialize(buf); }
        @Override public void deserialize(ByteBuf buf) { super.deserialize(buf); tank.deserialize(buf); }
    }
}
