package com.crunkazcanbe.ntmiq.drones;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Quarry Drone from NTM: IQ Edition (1.7.10, GPL-3; irislgtm, tam69 and the IQ team), ported to CE 1.12.2.
 * Finds the nearest linked quarry-marker group (within 64 blocks of its crate), then digs the area inside the markers
 * layer by layer in 3x3 patches. Without a second marker height it goes 32 layers deep. Returns to the crate when full.
 * IQ balance kept: 3x mining rate, slow flight (0.25), and ore blocks are broken without drops (that is the
 * Spelunking Drone's job).
 */
public class EntityQuarryDrone extends EntityCrateDrone {

    public static final double mineRate = 3.0D;
    public static final int quarryDepth = 32;

    protected int minX, maxX, minZ, maxZ;
    protected int topY, bottomY;
    protected boolean boundsFound = false;
    protected int curX, curZ, curY;
    protected int dirX = 1;
    protected final HashMap<BlockPos, Double> mineProgress = new HashMap<>();
    protected boolean quarryComplete = false;
    protected DroneState state = DroneState.WORKING;

    public enum DroneState { WORKING, RETURNING, HOMED }

    public EntityQuarryDrone(World world) {
        super(world);
        setSize(1.5F, 1.5F);
    }

    @Override
    protected int itemMeta() {
        return ItemMiningDrone.QUARRY;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) return;
        switch (state) {
            case WORKING -> doWork();
            case RETURNING -> doReturn();
            case HOMED -> { }
        }
    }

    protected void doWork() {
        if (!boundsFound && !findMarkers()) {
            state = DroneState.HOMED;
            return;
        }
        if (isFull()) {
            state = DroneState.RETURNING;
            return;
        }

        double dx = curX + 0.5 - posX, dy = curY + 1 - posY, dz = curZ + 0.5 - posZ;
        if (MathHelper.sqrt(dx * dx + dy * dy + dz * dz) > 1) {
            setTarget(curX + 0.5, curY + 1, curZ + 0.5);
            if (posY > topY + 2 || collidedHorizontally || collidedVertically) mineBlockAhead();
            return;
        }

        minePatch();
        if (isPatchClear() && !jumpToWork()) advance();
    }

    protected boolean jumpToWork() {
        int bx = curX, bz = curZ, best = Integer.MAX_VALUE;
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                if (isUnmineable(new BlockPos(x, curY, z))) continue;
                int d = (x - curX) * (x - curX) + (z - curZ) * (z - curZ);
                if (d < best) {
                    best = d;
                    bx = x;
                    bz = z;
                }
            }
        }
        if (best == Integer.MAX_VALUE) return false;
        curX = bx;
        curZ = bz;
        return true;
    }

    protected void minePatch() {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                int x = curX + dx, z = curZ + dz;
                if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) mineBlock(new BlockPos(x, curY, z));
            }
    }

    protected boolean isPatchClear() {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                int x = curX + dx, z = curZ + dz;
                if (x < minX || x > maxX || z < minZ || z > maxZ) continue;
                if (!isUnmineable(new BlockPos(x, curY, z))) return false;
            }
        return true;
    }

    /** snake across the layer, then step down */
    protected void advance() {
        int nextX = curX + dirX;
        if (nextX >= minX && nextX <= maxX) {
            curX = nextX;
            return;
        }
        if (curZ >= maxZ) {
            if (curY > bottomY) {
                curY--;
                curX = minX;
                curZ = minZ;
                dirX = 1;
            } else {
                quarryComplete = true;
                state = DroneState.RETURNING;
            }
            return;
        }
        curZ++;
        dirX = -dirX;
        curX = dirX == 1 ? minX : maxX;
    }

    protected boolean findMarkers() {
        int radius = 64;
        BlockPos nearest = null;
        int best = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -4; dy <= 4; dy++)
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++) {
                    m.setPos(baseX + dx, baseY + dy, baseZ + dz);
                    if (!world.isBlockLoaded(m) || world.getBlockState(m).getBlock() != IqDrones.quarryMarker) continue;
                    int d = dx * dx + dz * dz + dy * dy * 4;
                    if (d < best) {
                        best = d;
                        nearest = m.toImmutable();
                    }
                }
        if (nearest == null) return false;

        // BFS over the marker links (IQ)
        HashSet<BlockPos> markers = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        markers.add(nearest);
        queue.add(nearest);
        while (!queue.isEmpty()) {
            TileEntity te = world.getTileEntity(queue.poll());
            if (!(te instanceof TileEntityQuarryMarker)) continue;
            for (int[] l : ((TileEntityQuarryMarker) te).links) {
                if (l == null) continue;
                BlockPos n = new BlockPos(l[0], l[1], l[2]);
                if (markers.add(n)) queue.add(n);
            }
        }
        if (markers.size() < 2) return false;

        int loX = Integer.MAX_VALUE, loY = Integer.MAX_VALUE, loZ = Integer.MAX_VALUE;
        int hiX = Integer.MIN_VALUE, hiY = Integer.MIN_VALUE, hiZ = Integer.MIN_VALUE;
        for (BlockPos p : markers) {
            loX = Math.min(loX, p.getX()); hiX = Math.max(hiX, p.getX());
            loY = Math.min(loY, p.getY()); hiY = Math.max(hiY, p.getY());
            loZ = Math.min(loZ, p.getZ()); hiZ = Math.max(hiZ, p.getZ());
        }
        minX = loX + 1;
        maxX = hiX - 1;
        minZ = loZ + 1;
        maxZ = hiZ - 1;
        if (minX > maxX || minZ > maxZ) return false;

        topY = hiY - 1;
        bottomY = hiY > loY ? loY + 1 : Math.max(1, topY - quarryDepth);
        if (topY < bottomY) return false;

        int workY = -1;
        outer:
        for (int y = topY; y >= bottomY; y--)
            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++)
                    if (!isUnmineable(new BlockPos(x, y, z))) {
                        workY = y;
                        break outer;
                    }
        if (workY == -1) return false;

        int[][] corners = { { minX, minZ }, { maxX, minZ }, { minX, maxZ }, { maxX, maxZ } };
        int corner = Math.abs(getEntityId()) % 4;
        curX = corners[corner][0];
        curZ = corners[corner][1];
        curY = workY;
        dirX = 1;
        boundsFound = true;
        return true;
    }

    protected void doReturn() {
        Vec3d dist = new Vec3d(posX - (baseX + 0.5), posY - (baseY + 1), posZ - (baseZ + 0.5));
        if (dist.length() < 1.5) {
            if (unloadAtBase()) state = quarryComplete ? DroneState.HOMED : DroneState.WORKING;
        } else {
            setTarget(baseX + 0.5, baseY + 1, baseZ + 0.5);
        }
    }

    protected void mineBlockAhead() {
        if (targetY == -1) return;
        Vec3d dir = new Vec3d(targetX - posX, targetY - posY, targetZ - posZ);
        if (dir.length() < 0.01) return;
        dir = dir.normalize();
        int cx = MathHelper.floor(posX + dir.x);
        int cy = MathHelper.floor(posY + 0.375 + dir.y);
        int cz = MathHelper.floor(posZ + dir.z);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                int x = cx + dx, z = cz + dz;
                if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) mineBlock(new BlockPos(x, cy, z));
            }
    }

    protected void mineBlock(BlockPos pos) {
        if (isUnmineable(pos)) return;
        IBlockState st = world.getBlockState(pos);
        float hardness = st.getBlockHardness(world, pos);
        if (hardness < 0) return;

        double progress = hardness == 0 ? 1 : mineProgress.getOrDefault(pos, 0D) + mineRate / (hardness * 15D);
        if (progress >= 1) {
            breakBlock(pos, st);
            mineProgress.remove(pos);
        } else {
            mineProgress.put(pos, progress);
            world.sendBlockBreakProgress(DroneMining.breakerId(this, pos), pos, (int) Math.floor(progress * 10));
        }
    }

    protected void breakBlock(BlockPos pos, IBlockState st) {
        boolean ore = DroneMining.isOre(world, pos, st);
        java.util.List<net.minecraft.item.ItemStack> drops = ore ? java.util.Collections.emptyList() : DroneMining.drops(world, pos, st);
        DroneMining.destroy(world, pos, st);
        for (net.minecraft.item.ItemStack d : drops) store(d);
    }

    protected boolean isUnmineable(BlockPos pos) {
        IBlockState st = world.getBlockState(pos);
        if (st.getBlock().isAir(st, world, pos) || DroneMining.isProtected(st)) return true;
        return pos.getX() == baseX && pos.getZ() == baseZ;
    }

    @Override
    protected boolean canClimbWalls() {
        return false;
    }

    @Override
    public double getSpeed() {
        return 0.25D; // slower than a regular transport drone
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setByte("state", (byte) state.ordinal());
        nbt.setBoolean("boundsFound", boundsFound);
        nbt.setBoolean("quarryComplete", quarryComplete);
        nbt.setIntArray("bounds", new int[] { minX, maxX, topY, bottomY, minZ, maxZ, curX, curY, curZ, dirX });
        NBTTagCompound mine = new NBTTagCompound();
        int i = 0;
        for (Map.Entry<BlockPos, Double> e : mineProgress.entrySet()) {
            NBTTagCompound t = new NBTTagCompound();
            t.setLong("pos", e.getKey().toLong());
            t.setDouble("progress", e.getValue());
            mine.setTag("entry" + i++, t);
        }
        mine.setInteger("size", i);
        nbt.setTag("mine", mine);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        state = DroneState.values()[Math.min(nbt.getByte("state"), DroneState.values().length - 1)];
        boundsFound = nbt.getBoolean("boundsFound");
        quarryComplete = nbt.getBoolean("quarryComplete");
        int[] b = nbt.getIntArray("bounds");
        if (b.length == 10) {
            minX = b[0]; maxX = b[1]; topY = b[2]; bottomY = b[3]; minZ = b[4];
            maxZ = b[5]; curX = b[6]; curY = b[7]; curZ = b[8]; dirX = b[9];
        } else {
            boundsFound = false;
        }
        mineProgress.clear();
        NBTTagCompound mine = nbt.getCompoundTag("mine");
        for (int i = 0; i < mine.getInteger("size"); i++) {
            NBTTagCompound t = mine.getCompoundTag("entry" + i);
            mineProgress.put(BlockPos.fromLong(t.getLong("pos")), t.getDouble("progress"));
        }
    }
}
