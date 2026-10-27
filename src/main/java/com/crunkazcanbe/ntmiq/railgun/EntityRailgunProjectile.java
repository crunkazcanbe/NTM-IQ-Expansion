package com.crunkazcanbe.ntmiq.railgun;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.entity.projectile.EntityThrowableNT;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.particle.helper.ExplosionCreator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

/**
 * Louis' sabot. Ported from NTM: IQ Edition (1.7.10, GPL-3) — irislgtm, Alexmaster75 and the IQ team.
 * Chunk-loads itself while flying (it covers ~50 blocks a tick) and shows up on NTM radars as a shell.
 */
public class EntityRailgunProjectile extends EntityThrowableNT implements IChunkLoader, IRadarDetectableNT {

    private static final DataParameter<Integer> TYPE = EntityDataManager.createKey(EntityRailgunProjectile.class, DataSerializers.VARINT);

    public boolean ricocheted = false;
    public int fluidId = 0;
    public int fluidFill = 0;
    private ForgeChunkManager.Ticket loaderTicket;
    private ChunkPos loaded;

    public EntityRailgunProjectile(World world) {
        super(world);
        setSize(0.5F, 0.5F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(TYPE, 0);
    }

    /** the jacket falls off after 3 ticks (IQ's "sabot separation") */
    public boolean sabotSeparated() {
        return ticksExisted > 3;
    }

    @Override
    public void init(ForgeChunkManager.Ticket ticket) {
        if (!world.isRemote && ticket != null && loaderTicket == null) {
            loaderTicket = ticket;
            loaderTicket.bindEntity(this);
        }
    }

    @Override
    public void onUpdate() {
        if (!world.isRemote) {
            if (loaderTicket == null) init(ForgeChunkManager.requestTicket(MainRegistry.instance, world, ForgeChunkManager.Type.ENTITY));
            if (ticksExisted == 4) {
                ExplosionCreator.composeEffect(world, posX, posY, posZ, 3, 0.5F, 0.4F, 5F, 3, 4, 15, 1F, 1F, -2F, 100);
                world.playSound(null, posX, posY, posZ, HBMSoundHandler.fireFatman, SoundCategory.HOSTILE, 75F, 1F);
            }
        }
        super.onUpdate();
        if (!world.isRemote && !isDead) loadNeighboringChunks((int) Math.floor(posX / 16D), (int) Math.floor(posZ / 16D));
    }

    @Override
    public void loadNeighboringChunks(int cx, int cz) {
        if (loaderTicket == null) return;
        ChunkPos now = new ChunkPos(cx, cz);
        if (now.equals(loaded)) return;
        if (loaded != null) ForgeChunkManager.unforceChunk(loaderTicket, loaded);
        ForgeChunkManager.forceChunk(loaderTicket, now);
        loaded = now;
    }

    @Override
    protected void onImpact(RayTraceResult mop) {
        if (world.isRemote || isDead) return;
        if (mop.typeOfHit == RayTraceResult.Type.ENTITY && mop.entityHit instanceof EntityRailgunProjectile) return;
        getType().onImpact(this, mop);
    }

    @Override
    public float getGravityVelocity() {
        return 0.05F;
    }

    @Override
    protected float getAirDrag() {
        return (float) (1D - getType().bc / 20D);
    }

    public void killAndClear() {
        setDead();
    }

    @Override
    public void setDead() {
        super.setDead();
        if (!world.isRemote && loaderTicket != null) {
            ForgeChunkManager.releaseTicket(loaderTicket);
            loaderTicket = null;
        }
    }

    public EntityRailgunProjectile setType(int type) {
        dataManager.set(TYPE, type);
        return this;
    }

    public int getTypeId() {
        return dataManager.get(TYPE);
    }

    public ItemAmmoRailgun.RailgunSabot getType() {
        return ItemAmmoRailgun.type(getTypeId());
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("type", getTypeId());
        nbt.setBoolean("ricocheted", ricocheted);
        nbt.setInteger("fluidId", fluidId);
        nbt.setInteger("fluidFill", fluidFill);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        setType(nbt.getInteger("type"));
        ricocheted = nbt.getBoolean("ricocheted");
        fluidId = nbt.getInteger("fluidId");
        fluidFill = nbt.getInteger("fluidFill");
    }

    @Override
    public String getTranslationKey() {
        return "radar.target.railgun_" + getType().name;
    }

    @Override
    public int getBlipLevel() {
        return IRadarDetectableNT.SPECIAL;
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        return params.scanShells;
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return !(params.smartMode && motionY >= 0);
    }
}
