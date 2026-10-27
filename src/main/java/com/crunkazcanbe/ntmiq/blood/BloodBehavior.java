package com.crunkazcanbe.ntmiq.blood;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * What a creature's BASE blood fluid does to it (IQ: handler/blood/BloodBehavior). Normal blood (HUMAN_BLOOD) has no
 * behaviour; husks (F03) swap their blood type and get these. Register more with {@link BloodstreamAPI#registerBehavior}.
 * IQ's StatBonus (husk body stats) is left to F03 via {@link #applyStats}.
 */
public abstract class BloodBehavior {

    /** F03 husk stat totals, indexed by its body-stat enum ordinal */
    public void applyStats(int[] totals) { }

    public void onHurt(EntityLivingBase entity, LivingHurtEvent event) { }

    /** called on BOTH sides every tick */
    public void onTick(EntityLivingBase entity) { }

    public static class Steam extends BloodBehavior {
        private final int rate;

        public Steam(int rate) { this.rate = rate; }

        @Override
        public void onTick(EntityLivingBase e) {
            if (!e.world.isRemote || e.world.rand.nextInt(rate) != 0) return;
            double angle = Math.toRadians(e.rotationYaw);
            double side = e.world.rand.nextBoolean() ? 0.22D : -0.22D;
            e.world.spawnParticle(EnumParticleTypes.CLOUD, e.posX + Math.cos(angle) * side, e.posY + e.getEyeHeight(), e.posZ + Math.sin(angle) * side, 0D, 0.015D, 0D);
        }
    }

    public static class Nitroglycerin extends BloodBehavior {
        @Override
        public void onHurt(EntityLivingBase e, LivingHurtEvent event) {
            if (event.getSource().isExplosion()) return;
            e.extinguish();
            e.world.createExplosion(e, e.posX, e.posY + 0.5D, e.posZ, 4F, true);
        }
    }

    public static class Hellish extends BloodBehavior {
        @Override
        public void onHurt(EntityLivingBase e, LivingHurtEvent event) {
            if (event.getSource().isFireDamage()) event.setCanceled(true);
        }

        @Override
        public void onTick(EntityLivingBase e) {
            if (!e.world.isRemote && e.isBurning()) e.extinguish();
        }
    }

    public static class Concrete extends BloodBehavior {
        @Override
        public void onHurt(EntityLivingBase e, LivingHurtEvent event) {
            event.setAmount(event.getAmount() * (1F - 0.6F));
        }

        @Override
        public void onTick(EntityLivingBase e) {
            if (!e.world.isRemote && e.ticksExisted % 40 == 0) e.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 1));
        }
    }

    public static class Cay extends BloodBehavior {
        @Override
        public void onTick(EntityLivingBase e) {
            if (!e.world.isRemote && e.ticksExisted % 20 == 0) e.addPotionEffect(new PotionEffect(IqPotions.turkishRage, 100, 0));
        }
    }

    public static class Helium extends BloodBehavior {
        @Override
        public void onTick(EntityLivingBase e) {
            e.fallDistance = 0F;
            if (!e.isSneaking()) e.motionY = Math.max(e.motionY, 0.1D);
        }
    }
}
