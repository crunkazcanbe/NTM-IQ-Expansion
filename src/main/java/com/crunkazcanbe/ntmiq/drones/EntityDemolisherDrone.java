package com.crunkazcanbe.ntmiq.drones;

import com.hbm.entity.item.EntityDeliveryDrone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Demolisher Drone from NTM: IQ Edition (1.7.10, GPL-3; irislgtm, tam69 and the IQ team). IQ made it a flag on the
 * delivery drone; here it is a subclass of CE's EntityDeliveryDrone, so CE's waypoints and crates drive it as usual.
 * It does not climb over walls: it chews a 3x3 tunnel through anything on its route (drops are voided), at a rate of
 * speed / (hardness * 15) per tick. Express and chunkloading variants as for transport drones.
 */
public class EntityDemolisherDrone extends EntityDeliveryDrone {

    protected final HashMap<BlockPos, Double> mineProgressMap = new HashMap<>();

    public EntityDemolisherDrone(World world) {
        super(world);
    }

    public void setExpress() {
        dataManager.set(IS_EXPRESS, true);
    }

    @Override
    public void onUpdate() {
        if (!world.isRemote) collidedHorizontally = false; // IQ: canClimbWalls() == false for demolishers
        super.onUpdate();
        if (!world.isRemote) tryMine();
    }

    protected void tryMine() {
        if (targetY == -1) return;
        Vec3d dir = new Vec3d(targetX - posX, targetY - posY, targetZ - posZ);
        if (dir.length() < 0.01) return;
        dir = dir.normalize();

        boolean travelX = Math.abs(dir.x) > Math.abs(dir.z);
        int cy = MathHelper.floor(posY + 0.375);
        int hx = travelX ? MathHelper.floor(posX + Math.signum(dir.x)) : MathHelper.floor(posX);
        int hz = travelX ? MathHelper.floor(posZ) : MathHelper.floor(posZ + Math.signum(dir.z));

        for (int dy = -1; dy <= 1; dy++)
            for (int dw = -1; dw <= 1; dw++)
                mineBlock(travelX ? new BlockPos(hx, cy + dy, hz + dw) : new BlockPos(hx + dw, cy + dy, hz));

        // forget cracks that are no longer in front of the drone
        Iterator<BlockPos> it = mineProgressMap.keySet().iterator();
        while (it.hasNext()) {
            BlockPos p = it.next();
            boolean inHead = travelX
                    ? p.getX() == hx && Math.abs(p.getY() - cy) <= 1 && Math.abs(p.getZ() - hz) <= 1
                    : p.getZ() == hz && Math.abs(p.getY() - cy) <= 1 && Math.abs(p.getX() - hx) <= 1;
            if (!inHead) {
                clearCrack(p);
                it.remove();
            }
        }
    }

    protected void mineBlock(BlockPos pos) {
        if (!world.isBlockLoaded(pos)) return;
        IBlockState st = world.getBlockState(pos);
        float hardness = st.getBlockHardness(world, pos);
        if (st.getBlock().isAir(st, world, pos) || hardness < 0 || DroneMining.isProtected(st)) {
            clearCrack(pos);
            mineProgressMap.remove(pos);
            return;
        }
        double progress = hardness == 0 ? 1 : mineProgressMap.getOrDefault(pos, 0D) + getSpeed() / (hardness * 15D);
        if (progress >= 1) {
            DroneMining.destroy(world, pos, st);
            clearCrack(pos);
            mineProgressMap.remove(pos);
        } else {
            mineProgressMap.put(pos, progress);
            world.sendBlockBreakProgress(DroneMining.breakerId(this, pos), pos, (int) Math.floor(progress * 10));
        }
    }

    protected void clearCrack(BlockPos pos) {
        world.sendBlockBreakProgress(DroneMining.breakerId(this, pos), pos, -1);
    }

    @Override
    public boolean hitByEntity(Entity attacker) {
        if (isDead) return false;
        if (attacker instanceof EntityPlayer && !world.isRemote) {
            setDead();
            for (int i = 0; i < inventory.getSlots(); i++)
                if (!inventory.getStackInSlot(i).isEmpty()) entityDropItem(inventory.getStackInSlot(i), 1F);
            int meta = ItemMiningDrone.DEMOLISHER;
            if (dataManager.get(IS_EXPRESS)) meta += 2;
            if (chunkLoading) meta += 1;
            entityDropItem(new ItemStack(IqDrones.drone, 1, meta), 1F);
        }
        return false;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        NBTTagList grid = new NBTTagList();
        for (Map.Entry<BlockPos, Double> e : mineProgressMap.entrySet()) {
            NBTTagCompound t = new NBTTagCompound();
            t.setLong("pos", e.getKey().toLong());
            t.setDouble("progress", e.getValue());
            grid.appendTag(t);
        }
        nbt.setTag("mineGrid", grid);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        mineProgressMap.clear();
        NBTTagList grid = nbt.getTagList("mineGrid", 10);
        for (int i = 0; i < grid.tagCount(); i++) {
            NBTTagCompound t = grid.getCompoundTagAt(i);
            mineProgressMap.put(BlockPos.fromLong(t.getLong("pos")), t.getDouble("progress"));
        }
    }
}
