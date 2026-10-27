package com.crunkazcanbe.ntmiq.husks;

import java.util.UUID;

import com.crunkazcanbe.ntmiq.husks.ItemHumanPart.Stat;
import com.crunkazcanbe.ntmiq.husks.ItemHumanPart.Trait;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.FoodStats;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The body a player currently wears (IQ: HbmPlayerProps.bodyStats/bodyTraits/huskName/huskUUID + the ModEventHandler
 * trait hooks). Kept in the player's entity NBT under "ntmiqBody": it is saved with the player and NOT copied to the
 * respawned player, so dying in a husk body puts you back in your own (empty-stat) body, like IQ.
 * Stats become attribute modifiers on the same formulas IQ uses for husk base values.
 */
public final class HuskBody {
    private HuskBody() {}

    static final String KEY = "ntmiqBody";
    private static final UUID HEALTH = UUID.fromString("3f9e0c62-6d2e-4a8e-9b51-1a2b3c4d0f01"), DAMAGE = UUID.fromString("3f9e0c62-6d2e-4a8e-9b51-1a2b3c4d0f02"),
            SPEED = UUID.fromString("3f9e0c62-6d2e-4a8e-9b51-1a2b3c4d0f03"), KNOCKBACK = UUID.fromString("3f9e0c62-6d2e-4a8e-9b51-1a2b3c4d0f04");

    static NBTTagCompound tag(EntityPlayer p) { return p.getEntityData().getCompoundTag(KEY); }

    static int[] stats(EntityPlayer p) {
        int[] s = tag(p).getIntArray("stats");
        return s.length == Stat.values().length ? s : new int[Stat.values().length];
    }

    static String traits(EntityPlayer p) { return tag(p).getString("traits"); }

    /** whose body the player is in (skin owner); the player himself unless he swapped */
    static String ownerName(EntityPlayer p) { String s = tag(p).getString("owner"); return s.isEmpty() ? p.getName() : s; }

    static String ownerUuid(EntityPlayer p) { String s = tag(p).getString("ownerUUID"); return s.isEmpty() ? p.getUniqueID().toString() : s; }

    static void set(EntityPlayer p, int[] stats, String traits, String owner, String ownerUuid) {
        NBTTagCompound t = new NBTTagCompound();
        t.setIntArray("stats", stats == null ? new int[Stat.values().length] : stats);
        t.setString("traits", traits == null ? "" : traits);
        t.setString("owner", owner == null ? "" : owner);
        t.setString("ownerUUID", ownerUuid == null ? "" : ownerUuid);
        p.getEntityData().setTag(KEY, t);
        applyModifiers(p);
    }

    static double stat(int[] s, Stat stat) { return s.length > stat.ordinal() ? s[stat.ordinal()] : 0; }

    /** IQ husk formulas minus the vanilla base: health +vit/20, damage +str/100, speed +agi/20000, knockback res +tough/1000 */
    static void applyModifiers(EntityPlayer p) {
        int[] s = stats(p);
        modifier(p, SharedMonsterAttributes.MAX_HEALTH, HEALTH, stat(s, Stat.VITALITY) / 20D);
        modifier(p, SharedMonsterAttributes.ATTACK_DAMAGE, DAMAGE, stat(s, Stat.STRENGTH) / 100D);
        modifier(p, SharedMonsterAttributes.MOVEMENT_SPEED, SPEED, stat(s, Stat.AGILITY) / 20000D);
        modifier(p, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, KNOCKBACK, stat(s, Stat.TOUGHNESS) / 1000D);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void modifier(EntityLivingBase e, IAttribute attr, UUID id, double amount) {
        IAttributeInstance inst = e.getEntityAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0) inst.applyModifier(new AttributeModifier(id, "ntmiq husk body", amount, 0));
    }

    static boolean has(EntityPlayer p, Trait t) {
        return !p.world.isRemote && ("," + traits(p) + ",").contains("," + t.name() + ",");
    }

    // ---------------------------------------------------------------- IQ ModEventHandler body hooks

    public static class Events {
        @SubscribeEvent
        public void tick(TickEvent.PlayerTickEvent e) {
            EntityPlayer p = e.player;
            if (p.world.isRemote || e.phase != TickEvent.Phase.END || !p.getEntityData().hasKey(KEY)) return;

            NBTTagCompound data = tag(p);
            float last = data.hasKey("lastHealth") ? data.getFloat("lastHealth") : p.getHealth();
            float healed = p.getHealth() - last;
            double recovery = stat(stats(p), Stat.RECOVERY);
            if (healed > 0F && recovery > 0) p.heal((float) (healed * recovery / 100D));
            data.setFloat("lastHealth", p.getHealth());

            if (p.getHealth() < p.getMaxHealth() && has(p, Trait.CLOTTING) && p.ticksExisted % 40 == 0
                    && p.motionX * p.motionX + p.motionZ * p.motionZ < 0.001D) p.heal(1F);
            if (has(p, Trait.HARDY) && p.ticksExisted % 100 == 0) {
                FoodStats food = p.getFoodStats();
                if (food.getSaturationLevel() < food.getFoodLevel()) food.setFoodSaturationLevel(Math.min(food.getSaturationLevel() + 0.5F, food.getFoodLevel()));
            }
            if (has(p, Trait.SECOND_WIND) && p.getHealth() < 4F && p.ticksExisted % 200 == 0) p.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 100, 0));
            // IQ adds +0.2 jump motion server side, which a client-moved player never sees; jump boost syncs on its own
            if (has(p, Trait.SPRING_HEEL) && p.ticksExisted % 100 == 0) p.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 140, 1, true, false));
        }

        @SubscribeEvent
        public void hurt(LivingHurtEvent e) {
            if (e.getEntity().world.isRemote) return;
            if (e.getEntityLiving() instanceof EntityPlayer) {
                EntityPlayer p = (EntityPlayer) e.getEntityLiving();
                if (has(p, Trait.NUMB) && p.getRNG().nextFloat() < 0.25F) { e.setCanceled(true); return; }
                if (has(p, Trait.TWITCH)) p.addPotionEffect(new PotionEffect(MobEffects.SPEED, 60, 0));
                if (has(p, Trait.GRISTLE)) e.setAmount(e.getAmount() * 0.75F);
            }
            Entity src = e.getSource().getTrueSource();
            if (src instanceof EntityPlayer) {
                EntityPlayer hitter = (EntityPlayer) src;
                if (has(hitter, Trait.BLOODTHIRST)) hitter.heal(1F);
                if (has(hitter, Trait.WHITE_KNUCKLES)) {
                    double dx = e.getEntityLiving().posX - hitter.posX, dz = e.getEntityLiving().posZ - hitter.posZ, d = Math.sqrt(dx * dx + dz * dz);
                    if (d > 0.01D) e.getEntityLiving().addVelocity(dx / d * 0.4D, 0.1D, dz / d * 0.4D);
                }
            }
        }

        @SubscribeEvent
        public void fall(LivingFallEvent e) {
            if (!e.getEntity().world.isRemote && e.getEntityLiving() instanceof EntityPlayer && has((EntityPlayer) e.getEntityLiving(), Trait.SURE_FOOTED)) e.setCanceled(true);
        }
    }
}
