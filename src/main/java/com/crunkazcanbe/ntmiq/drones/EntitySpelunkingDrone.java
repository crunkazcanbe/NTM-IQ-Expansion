package com.crunkazcanbe.ntmiq.drones;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashMap;

/**
 * Spelunking Drone from NTM: IQ Edition (1.7.10, GPL-3; irislgtm, tam69 and the IQ team), ported to CE 1.12.2.
 * Wanders, flood-fills the air around it (1500 nodes) for an exposed ore block, flies there through open air (BFS
 * pathing, 2000 nodes), mines only ores into its 27-slot hold and flies back to its drone crate when full.
 */
public class EntitySpelunkingDrone extends EntityCrateDrone {

    private static final int[][] DIRS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };

    protected int oreX, oreY, oreZ;
    protected int mineX, mineY = -1, mineZ;
    protected double mineProgress;
    protected int scanTimer = 0;
    protected int homeWait = 0;
    protected int wanderCooldown = 0;
    protected int wanderX, wanderZ;
    protected BlockPos pathStep;
    protected int pathRecalc = 0;
    protected DroneState state = DroneState.SEEKING;

    public enum DroneState { SEEKING, MINING, RETURNING }

    public EntitySpelunkingDrone(World world) {
        super(world);
    }

    @Override
    protected int itemMeta() {
        return ItemMiningDrone.SPELUNKER;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) return;
        switch (state) {
            case SEEKING -> doSeeking();
            case MINING -> doMining();
            case RETURNING -> doReturning();
        }
    }

    protected void doSeeking() {
        if (isFull()) {
            state = DroneState.RETURNING;
            return;
        }
        if (homeWait > 0) {
            homeWait--;
            setTarget(baseX + 0.5, baseY + 1, baseZ + 0.5);
            return;
        }
        if (scanTimer-- <= 0) {
            scanTimer = 30;
            if (findReachableOre()) {
                resetMine();
                pathRecalc = 0;
                state = DroneState.MINING;
                return;
            }
        }
        if (--wanderCooldown <= 0) {
            wanderCooldown = 60;
            wanderX = MathHelper.floor(posX) + rand.nextInt(49) - 24;
            wanderZ = MathHelper.floor(posZ) + rand.nextInt(49) - 24;
        }
        setTarget(wanderX + 0.5, posY, wanderZ + 0.5);
    }

    protected void doMining() {
        if (isFull()) {
            state = DroneState.RETURNING;
            return;
        }
        BlockPos ore = new BlockPos(oreX, oreY, oreZ);
        if (world.isAirBlock(ore)) {
            resetMine();
            state = DroneState.SEEKING;
            return;
        }
        if (new Vec3d(oreX + 0.5 - posX, oreY + 0.5 - posY, oreZ + 0.5 - posZ).length() < 2) {
            mineBlock(ore);
            return;
        }
        if (pathRecalc-- <= 0) {
            pathRecalc = 20;
            pathStep = pathfind(oreX, oreY, oreZ);
        }
        if (pathStep == null) {
            state = DroneState.SEEKING;
            return;
        }
        setTarget(pathStep.getX() + 0.5, pathStep.getY() + 0.5, pathStep.getZ() + 0.5);
    }

    protected void doReturning() {
        Vec3d dist = new Vec3d(posX - (baseX + 0.5), posY - (baseY + 1), posZ - (baseZ + 0.5));
        if (dist.length() < 1.5) {
            if (unloadAtBase()) {
                homeWait = 100;
                state = DroneState.SEEKING;
            }
            return;
        }
        if (pathRecalc-- <= 0) {
            pathRecalc = 20;
            pathStep = pathfind(baseX, baseY + 1, baseZ);
        }
        if (pathStep != null) setTarget(pathStep.getX() + 0.5, pathStep.getY() + 0.5, pathStep.getZ() + 0.5);
        else setTarget(posX, posY + 16, posZ); // no air route: go up and try again
    }

    /** IQ: another drone within ~2 blocks is already on that ore */
    protected boolean hasDroneNearby(BlockPos p) {
        for (Entity e : world.getEntitiesWithinAABB(com.hbm.entity.item.EntityDroneBase.class,
                new AxisAlignedBB(p.getX() - 1.5, p.getY() - 1.5, p.getZ() - 1.5, p.getX() + 2.5, p.getY() + 2.5, p.getZ() + 2.5)))
            if (e != this) return true;
        return false;
    }

    protected boolean findReachableOre() {
        BlockPos start = new BlockPos(this);
        HashMap<BlockPos, BlockPos> seen = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.put(start, null);
        int limit = 1500;
        while (!queue.isEmpty() && limit-- > 0) {
            BlockPos cur = queue.poll();
            for (int[] d : DIRS) {
                BlockPos a = cur.add(d[0], d[1], d[2]);
                if (!world.isBlockLoaded(a)) continue;
                IBlockState st = world.getBlockState(a);
                if (st.getBlock().isAir(st, world, a) || st.getMaterial().isLiquid()) continue;
                if (hasDroneNearby(a)) continue;
                if (DroneMining.isOre(world, a, st)) {
                    oreX = a.getX();
                    oreY = a.getY();
                    oreZ = a.getZ();
                    return true;
                }
            }
            for (int[] d : DIRS) {
                BlockPos n = cur.add(d[0], d[1], d[2]);
                if (seen.containsKey(n) || !world.isBlockLoaded(n) || !world.isAirBlock(n)) continue;
                seen.put(n, cur);
                queue.add(n);
            }
        }
        return false;
    }

    /** BFS through air/liquid; returns the first step from here toward a cell next to the target */
    protected BlockPos pathfind(int tx, int ty, int tz) {
        BlockPos start = new BlockPos(this);
        HashMap<BlockPos, BlockPos> cameFrom = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        cameFrom.put(start, null);
        BlockPos goal = null;
        int limit = 2000;
        while (!queue.isEmpty() && limit-- > 0) {
            BlockPos cur = queue.poll();
            if (Math.abs(cur.getX() - tx) + Math.abs(cur.getY() - ty) + Math.abs(cur.getZ() - tz) == 1) {
                goal = cur;
                break;
            }
            for (int[] d : DIRS) {
                BlockPos n = cur.add(d[0], d[1], d[2]);
                if (cameFrom.containsKey(n) || !world.isBlockLoaded(n)) continue;
                IBlockState st = world.getBlockState(n);
                if (!st.getBlock().isAir(st, world, n) && !st.getMaterial().isLiquid()) continue;
                cameFrom.put(n, cur);
                queue.add(n);
            }
        }
        if (goal == null) return null;
        BlockPos step = goal;
        while (true) {
            BlockPos prev = cameFrom.get(step);
            if (prev == null) return null; // IQ: already standing next to the target
            if (prev.equals(start)) return step;
            step = prev;
        }
    }

    protected void mineBlock(BlockPos pos) {
        IBlockState st = world.getBlockState(pos);
        if (st.getBlock().isAir(st, world, pos) || st.getMaterial().isLiquid() || st.getBlock() == net.minecraft.init.Blocks.BEDROCK) return;
        float hardness = st.getBlockHardness(world, pos);
        if (hardness < 0) return;

        if (pos.getX() != mineX || pos.getY() != mineY || pos.getZ() != mineZ) {
            mineX = pos.getX();
            mineY = pos.getY();
            mineZ = pos.getZ();
            mineProgress = 0;
        }
        mineProgress = hardness == 0 ? 1 : mineProgress + getSpeed() / (hardness * 15D);
        if (mineProgress >= 1) {
            breakAndCollect(pos, st);
            resetMine();
        } else {
            world.sendBlockBreakProgress(getEntityId(), pos, (int) Math.floor(mineProgress * 10));
        }
    }

    protected void breakAndCollect(BlockPos pos, IBlockState st) {
        boolean ore = DroneMining.isOre(world, pos, st);
        java.util.List<ItemStack> drops = ore ? DroneMining.drops(world, pos, st) : java.util.Collections.emptyList();
        DroneMining.destroy(world, pos, st);
        for (ItemStack d : drops) store(d);
    }

    protected void resetMine() {
        mineX = 0;
        mineY = -1;
        mineZ = 0;
        mineProgress = 0;
    }

    @Override
    public double getSpeed() {
        return 0.375D;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setByte("state", (byte) state.ordinal());
        nbt.setInteger("oreX", oreX);
        nbt.setInteger("oreY", oreY);
        nbt.setInteger("oreZ", oreZ);
        nbt.setDouble("mineProgress", mineProgress);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        state = DroneState.values()[Math.min(nbt.getByte("state"), DroneState.values().length - 1)];
        oreX = nbt.getInteger("oreX");
        oreY = nbt.getInteger("oreY");
        oreZ = nbt.getInteger("oreZ");
        mineProgress = nbt.getDouble("mineProgress");
    }
}
