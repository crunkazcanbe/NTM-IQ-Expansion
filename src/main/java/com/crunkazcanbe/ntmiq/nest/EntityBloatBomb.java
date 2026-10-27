package com.crunkazcanbe.ntmiq.nest;

import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.projectile.EntityAcidBomb;
import com.hbm.lib.ModDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * IQ's EntityBloatBomb (NTM: IQ Edition, GPL-3): the nether blaster's acid bomb that bursts into a musty bloatmusk
 * cloud. Extends CE's acid bomb, so CE's acid bomb renderer draws it (1.12 looks renderers up by superclass).
 */
public class EntityBloatBomb extends EntityAcidBomb {

    public EntityBloatBomb(World world) { super(world); }

    public EntityBloatBomb(World world, double x, double y, double z) { super(world, x, y, z); }

    @Override
    protected void onImpact(RayTraceResult r) {
        if (world.isRemote) return;
        double cx, cy, cz;
        if (r.typeOfHit == RayTraceResult.Type.ENTITY) {
            if (r.entityHit instanceof EntityGlyphid) return;   // flies through its own kind, like CE's acid bomb
            r.entityHit.attackEntityFrom(new EntityDamageSourceIndirect(ModDamageSource.s_acid, this, thrower), damage);
            cx = r.entityHit.posX; cy = r.entityHit.posY; cz = r.entityHit.posZ;
        } else {
            cx = r.getBlockPos().getX() + 0.5; cy = r.getBlockPos().getY() + 0.5; cz = r.getBlockPos().getZ() + 0.5;
        }
        EntityMist mist = new EntityMist(world);
        mist.setType(IqFluids.MUSTY_BLOATMUSK);
        mist.setPosition(cx, cy, cz);
        mist.setArea(4, 2);
        mist.setDuration(60);
        world.spawnEntity(mist);
        setDead();
    }
}
