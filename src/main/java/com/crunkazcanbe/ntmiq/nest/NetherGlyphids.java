package com.crunkazcanbe.ntmiq.nest;

import java.util.function.Function;

import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.mob.glyphid.*;
import com.hbm.entity.projectile.EntityAcidBomb;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * IQ's nine nether glyphids as plain subclasses of CE's glyphids (NTM: IQ Edition, GPL-3; irislgtm / tam69 / IQ team).
 * All: immune to fire, IQ nether skins, drop netherglyphid meat. Scout withers on hit, brawler is explosion/projectile
 * proof and blasts back when hit. IQ fluid attacks (F03 agent): behemoth spits burning bloatmusk and dies into a musty
 * bloatmusk cloud, blaster lobs bloat bombs, brenda dies into a musky pheromone mist that hatches nether grunts.
 * Not ported: the nether Big Man Johnson's balefire blast (kept CE's nuke).
 */
@Mod.EventBusSubscriber(modid = "ntmiq")
public final class NetherGlyphids {
    private NetherGlyphids() {}

    static ResourceLocation skin(String name) { return new ResourceLocation("ntmiq", "textures/entity/" + name + ".png"); }

    static final ResourceLocation GRUNT = skin("glyphid_nether"), SCOUT = skin("glyphid_nether_scout"),
            BOMBARDIER = skin("glyphid_nether_bombardier"), BRAWLER = skin("glyphid_nether_brawler"), DIGGER = skin("glyphid_nether_digger"),
            BLASTER = skin("glyphid_nether_blaster"), BEHEMOTH = skin("glyphid_nether_behemoth"), BRENDA = skin("glyphid_nether_brenda"),
            NUCLEAR = skin("glyphid_nether_nuclear");

    /** fire damage does nothing; null = let CE decide */
    static float[] fireproof(DamageSource d) { return d.isFireDamage() ? new float[] { 0F, 1F } : null; }

    /**
     * CE's onDeath spawns its own mist (sulfuric acid / pheromone) with no hook to change it; IQ's nether twins use IQ
     * fluids instead, so retype the cloud super.onDeath() spawned this very tick.
     */
    static void retypeMist(EntityLivingBase e, FluidType from, FluidType to) {
        for (EntityMist m : e.world.getEntitiesWithinAABB(EntityMist.class, e.getEntityBoundingBox().grow(2)))
            if (m.ticksExisted == 0 && m.getType() == from) m.setType(to);
    }

    /** CE's bombardier loop spawns acid bombs inline; a nether blaster's are swapped for IQ's bloat bombs as they spawn */
    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent e) {
        if (e.getWorld().isRemote || e.getEntity().getClass() != EntityAcidBomb.class) return;
        EntityAcidBomb acid = (EntityAcidBomb) e.getEntity();
        if (!(acid.getThrower() instanceof Blaster)) return;
        EntityBloatBomb bomb = new EntityBloatBomb(e.getWorld(), acid.posX, acid.posY, acid.posZ);
        bomb.setThrower(acid.getThrower());
        bomb.motionX = acid.motionX;
        bomb.motionY = acid.motionY;
        bomb.motionZ = acid.motionZ;
        bomb.rotationYaw = acid.rotationYaw;
        bomb.rotationPitch = acid.rotationPitch;
        bomb.damage = acid.damage;
        e.setCanceled(true);
        e.getWorld().spawnEntity(bomb);
    }

    static ItemStack meat(ItemStack s) {
        if (s.getItem() == ModItems.glyphid_meat) return new ItemStack(IqNest.meatNether, s.getCount());
        if (s.getItem() == ModItems.glyphid_meat_grilled) return new ItemStack(IqNest.meatNetherGrilled, s.getCount());
        return s;
    }

    /** CE glyphid (from a hive spawner) -> its nether twin; most specific CE class first */
    static EntityGlyphid toNether(EntityGlyphid g) {
        Function<World, EntityGlyphid> f =
                g instanceof EntityGlyphidScout ? Scout::new :
                g instanceof EntityGlyphidNuclear ? Nuclear::new :
                g instanceof EntityGlyphidBehemoth ? Behemoth::new :
                g instanceof EntityGlyphidBrenda ? Brenda::new :
                g instanceof EntityGlyphidBlaster ? Blaster::new :
                g instanceof EntityGlyphidBombardier ? Bombardier::new :
                g instanceof EntityGlyphidDigger ? Digger::new :
                g instanceof EntityGlyphidBrawler ? Brawler::new : Grunt::new;
        return f.apply(g.world);
    }

    public static class Grunt extends EntityGlyphid {
        public Grunt(World w) { super(w); isImmuneToFire = true; }
        @Override public ResourceLocation getSkin() { return GRUNT; }
        @Override public float[] getCurrentDTDR(DamageSource d, float a, float pdt, float p) { float[] f = fireproof(d); return f != null ? f : super.getCurrentDTDR(d, a, pdt, p); }
        @Override public EntityItem entityDropItem(ItemStack s, float y) { return super.entityDropItem(meat(s), y); }
    }

    public static class Scout extends EntityGlyphidScout {
        public Scout(World w) { super(w); isImmuneToFire = true; }
        @Override public ResourceLocation getSkin() { return SCOUT; }
        @Override public float[] getCurrentDTDR(DamageSource d, float a, float pdt, float p) { float[] f = fireproof(d); return f != null ? f : super.getCurrentDTDR(d, a, pdt, p); }
        @Override public EntityItem entityDropItem(ItemStack s, float y) { return super.entityDropItem(meat(s), y); }

        @Override
        public boolean attackEntityAsMob(Entity target) {
            boolean hit = super.attackEntityAsMob(target);
            if (hit && target instanceof EntityLivingBase) ((EntityLivingBase) target).addPotionEffect(new PotionEffect(MobEffects.WITHER, 10 * 20, 3));
            return hit;
        }
    }

    public static class Bombardier extends EntityGlyphidBombardier {
        public Bombardier(World w) { super(w); isImmuneToFire = true; }
        @Override public ResourceLocation getSkin() { return BOMBARDIER; }
        @Override public float[] getCurrentDTDR(DamageSource d, float a, float pdt, float p) { float[] f = fireproof(d); return f != null ? f : super.getCurrentDTDR(d, a, pdt, p); }
        @Override public EntityItem entityDropItem(ItemStack s, float y) { return super.entityDropItem(meat(s), y); }
        @Override public int getBombCount() { return 3; }
    }

    public static class Brawler extends EntityGlyphidBrawler {
        public Brawler(World w) { super(w); isImmuneToFire = true; }
        @Override public ResourceLocation getSkin() { return BRAWLER; }
        @Override public EntityItem entityDropItem(ItemStack s, float y) { return super.entityDropItem(meat(s), y); }

        @Override
        public float[] getCurrentDTDR(DamageSource d, float a, float pdt, float p) {
            if (d.isFireDamage() || d.isExplosion() || d.isProjectile()) return new float[] { 0F, 1F };
            return super.getCurrentDTDR(d, a, pdt, p);
        }

        /** IQ: shrugs off explosions and projectiles, and blows up (2.5) whenever something that isn't a glyphid hits it */
        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (source.isExplosion() || source.isProjectile()) return false;
            if (!world.isRemote && !(source.getTrueSource() instanceof EntityGlyphid))
                world.createExplosion(this, posX, posY, posZ, 2.5F, ForgeEventFactory.getMobGriefingEvent(world, this));
            return super.attackEntityFrom(source, amount);
        }
    }

    /** IQ's digger also lobs shrapnel every 5 s; CE has no shrapnel entity API to reuse, so it digs like CE's */
    public static class Digger extends EntityGlyphidDigger {
        public Digger(World w) { super(w); isImmuneToFire = true; }
        @Override public ResourceLocation getSkin() { return DIGGER; }
        @Override public float[] getCurrentDTDR(DamageSource d, float a, float pdt, float p) { float[] f = fireproof(d); return f != null ? f : super.getCurrentDTDR(d, a, pdt, p); }
        @Override public EntityItem entityDropItem(ItemStack s, float y) { return super.entityDropItem(meat(s), y); }
    }

    public static class Blaster extends EntityGlyphidBlaster {
        public Blaster(World w) { super(w); isImmuneToFire = true; }
        @Override public ResourceLocation getSkin() { return BLASTER; }
        @Override public float[] getCurrentDTDR(DamageSource d, float a, float pdt, float p) { float[] f = fireproof(d); return f != null ? f : super.getCurrentDTDR(d, a, pdt, p); }
        @Override public EntityItem entityDropItem(ItemStack s, float y) { return super.entityDropItem(meat(s), y); }
    }

    public static class Behemoth extends EntityGlyphidBehemoth {
        public Behemoth(World w) { super(w); isImmuneToFire = true; }
        @Override public ResourceLocation getSkin() { return BEHEMOTH; }
        @Override public float[] getCurrentDTDR(DamageSource d, float a, float pdt, float p) { float[] f = fireproof(d); return f != null ? f : super.getCurrentDTDR(d, a, pdt, p); }
        @Override public EntityItem entityDropItem(ItemStack s, float y) { return super.entityDropItem(meat(s), y); }

        /** IQ: spits burning bloatmusk instead of sulfuric acid */
        @Override
        public void acidAttack() {
            if (world.isRemote || !(getAttackTarget() instanceof EntityLivingBase) || getDistance(getAttackTarget()) >= 20) return;
            addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 2 * 20, 6));
            world.spawnEntity(new EntityChemical(world, this, 0, 0, 0).setFluid(IqFluids.BURNING_BLOATMUSK));
        }

        @Override
        public void onDeath(DamageSource source) {
            super.onDeath(source);
            if (!world.isRemote) retypeMist(this, Fluids.SULFURIC_ACID, IqFluids.MUSTY_BLOATMUSK);
        }
    }

    /** IQ's nether brenda is NOT fire immune, but shrugs off fire / explosions / projectiles through DT/DR */
    public static class Brenda extends EntityGlyphidBrenda {
        public Brenda(World w) { super(w); }
        @Override public ResourceLocation getSkin() { return BRENDA; }
        @Override public EntityItem entityDropItem(ItemStack s, float y) { return super.entityDropItem(meat(s), y); }

        /** IQ: musky pheromone death mist, and the 12 hatchlings are nether grunts */
        @Override
        public void onDeath(DamageSource source) {
            super.onDeath(source);
            if (world.isRemote || getHealth() > 0F) return;
            retypeMist(this, Fluids.PHEROMONE, IqFluids.MUSKY_PHEROMONE);
            for (EntityGlyphid g : world.getEntitiesWithinAABB(EntityGlyphid.class, getEntityBoundingBox().grow(4))) {
                if (g.getClass() != EntityGlyphid.class || g.ticksExisted != 0) continue;
                Grunt grunt = new Grunt(world);
                grunt.setLocationAndAngles(g.posX, g.posY, g.posZ, g.rotationYaw, 0F);
                g.setDead();
                world.spawnEntity(grunt);
            }
        }

        @Override
        public float[] getCurrentDTDR(DamageSource d, float a, float pdt, float p) {
            if (d.isFireDamage() || d.isExplosion() || d.isProjectile()) return new float[] { 0F, 1F };
            return super.getCurrentDTDR(d, a, pdt, p);
        }
    }

    /** "Nether Big Man Johnson" — IQ makes it a balefire blast; kept CE's nuclear death here */
    public static class Nuclear extends EntityGlyphidNuclear {
        public Nuclear(World w) { super(w); }
        @Override public ResourceLocation getSkin() { return NUCLEAR; }
        @Override public float[] getCurrentDTDR(DamageSource d, float a, float pdt, float p) { float[] f = fireproof(d); return f != null ? f : super.getCurrentDTDR(d, a, pdt, p); }
        @Override public EntityItem entityDropItem(ItemStack s, float y) { return super.entityDropItem(meat(s), y); }
    }
}
