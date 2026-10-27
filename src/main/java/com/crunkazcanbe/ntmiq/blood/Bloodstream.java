package com.crunkazcanbe.ntmiq.blood;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.Constants;

/**
 * Per-entity bloodstream capability (IQ: extprop/HbmBloodstreamProps, GPL-3). Holds the base blood entry, dissolved
 * drug fluids and (F02) pathogen entries; ticks drug decay/effects/overdose symptoms and the symptom potions' bodily
 * effects. Use it through {@link BloodstreamAPI#get}.
 */
public class Bloodstream {

    public static final DamageSource FEVER = new DamageSource("ntmiq.fever").setDamageBypassesArmor();
    public static final DamageSource BLEED = new DamageSource("ntmiq.bleed").setDamageBypassesArmor();
    public static final DamageSource SEPTIC = new DamageSource("ntmiq.septic").setDamageBypassesArmor();
    public static final DamageSource AORTA = new DamageSource("ntmiq.aorta").setDamageBypassesArmor().setDamageIsAbsolute();
    private static final UUID CARDIAC_UUID = UUID.fromString("9f3c2b8e-4a51-4e0f-9c6d-1b7e2a4f0c11");

    private final List<BloodEntry> entries = new ArrayList<>();
    /** free storage for later features (F02 immunity genomes etc.); saved with the bloodstream */
    public NBTTagCompound extra = new NBTTagCompound();
    private FluidType baseBlood = null; // null = HUMAN_BLOOD
    private int coughBoost = 0;

    // ---------------- blood type ----------------
    public FluidType getBloodType() { return baseBlood == null ? BloodFluids.HUMAN_BLOOD : baseBlood; }

    public void setBloodType(FluidType type) {
        if (type == null || type == Fluids.NONE) return;
        BloodEntry base = baseEntry();
        baseBlood = type;
        if (base != null) { base.fluid = type; base.amount = BloodstreamAPI.BASE_BLOOD; }
        else entries.add(new BloodEntry(type, BloodstreamAPI.BASE_BLOOD, null, null));
    }

    private BloodEntry baseEntry() {
        FluidType base = getBloodType();
        for (BloodEntry e : entries) if (!e.isPathogen() && e.fluid == base) return e;
        return null;
    }

    /**
     * IQ refills the base entry to {@link BloodstreamAPI#BASE_BLOOD} on every access, so blood loss never sticks.
     * ponytail: kept as-is (faithful); real blood loss needs a regen rate first.
     */
    private void ensureBaseBlood() {
        BloodEntry base = baseEntry();
        if (base != null) base.amount = BloodstreamAPI.BASE_BLOOD;
        else entries.add(new BloodEntry(getBloodType(), BloodstreamAPI.BASE_BLOOD, null, null));
    }

    // ---------------- contents ----------------
    public List<BloodEntry> getEntries() { ensureBaseBlood(); return entries; }

    public void addDrug(FluidType type, float dose) {
        for (BloodEntry e : entries) if (!e.isPathogen() && e.fluid == type) { e.amount += dose; return; }
        entries.add(new BloodEntry(type, dose, null, new NBTTagCompound()));
    }

    /** F02: add/merge a pathogen entry; {@code data} is only used when the entry is new */
    public BloodEntry addPathogen(String frameId, float dose, NBTTagCompound data) {
        for (BloodEntry e : entries) if (frameId.equals(e.frameId)) { e.amount += dose; return e; }
        BloodEntry e = new BloodEntry(null, dose, frameId, data == null ? new NBTTagCompound() : data);
        entries.add(e);
        return e;
    }

    public boolean hasPathogen(String frameId) {
        for (BloodEntry e : entries) if (frameId.equals(e.frameId)) return true;
        return false;
    }

    public void addBlood(float amount) { BloodEntry b = baseEntry(); if (b != null) b.amount = Math.min(BloodstreamAPI.BASE_BLOOD, b.amount + amount); }
    public void drainBlood(float amount) { BloodEntry b = baseEntry(); if (b != null) b.amount = Math.max(0, b.amount - amount); }
    public float getBloodAmount() { BloodEntry b = baseEntry(); return b == null ? 0 : b.amount; }

    /** base volume + every dissolved fluid (pathogens excluded) */
    public float getTotalFluid() {
        float total = BloodstreamAPI.BASE_BLOOD;
        FluidType base = getBloodType();
        for (BloodEntry e : entries) if (!e.isPathogen() && e.fluid != base && e.amount > 0) total += e.amount;
        return total;
    }

    public int getCoughBoost() { return coughBoost; }

    // ---------------- ticking (server) ----------------
    void tick(EntityLivingBase entity) {
        ensureBaseBlood();
        FluidType base = getBloodType();
        entries.removeIf(e -> !e.isPathogen() && e.fluid != base && e.amount < 0.5F);

        float total = getTotalFluid();
        for (BloodEntry e : entries) {
            if (e.isPathogen() || e.fluid == base) continue;
            FT_Drug drug = e.fluid.getTrait(FT_Drug.class);
            if (drug == null) continue;
            float cap = drug.threshold > 0 ? drug.threshold : 0.01F;
            e.amount = Math.max(0, e.amount * drug.getDecayFactor(e.amount / total / cap));
            if (e.amount <= 0) continue;
            float pct = e.amount / total;
            int level = (int) (pct / (cap * 0.25F));
            for (FT_Consumable.ConsumableEffect fx : drug.getEffects()) {
                int ticks = fx.potion.isInstant() ? 1 : 20;
                entity.addPotionEffect(new PotionEffect(fx.potion, ticks, Math.max(fx.amplifier + level, 0)));
            }
            if (pct > cap) {
                int tier = (int) ((pct - cap) / (cap * 0.25F));
                for (String key : drug.getSideEffects()) {
                    List<FT_Consumable.ConsumableEffect> symptom = BloodstreamAPI.getSymptom(key);
                    if (symptom != null) for (FT_Consumable.ConsumableEffect fx : symptom)
                        entity.addPotionEffect(new PotionEffect(fx.potion, 20, fx.amplifier + tier));
                }
            }
        }

        for (BloodstreamAPI.Ticker t : BloodstreamAPI.TICKERS) t.tick(entity, this);
        entries.removeIf(e -> e.isPathogen() && e.amount <= 0);

        tickSymptoms(entity);
        ensureBaseBlood();
        syncCardiac(entity);
    }

    /** the bodily side of IQ's symptom potions */
    private void tickSymptoms(EntityLivingBase e) {
        long tick = e.world.getTotalWorldTime();
        int id = e.getEntityId();
        java.util.Random rand = e.world.rand;

        boolean sneeze = e.isPotionActive(IqPotions.symptomSneeze), cough = e.isPotionActive(IqPotions.symptomCough);
        if (sneeze) {
            coughBoost = Math.max(coughBoost, 180);
            if (tick % 120 == id % 120) burst(e, IqBlood.SND_SNEEZE, 1.2F, EnumParticleTypes.SPIT, 15);
            if (rand.nextInt(80) == 0) e.attackEntityFrom(DamageSource.GENERIC, 0.5F);
        }
        if (cough) {
            coughBoost = Math.max(coughBoost, 120);
            if (tick % 120 == id % 120) burst(e, SoundEvent.REGISTRY.getObject(new ResourceLocation("hbm", "player.cough")), 1.0F, EnumParticleTypes.SMOKE_NORMAL, 10);
            if (rand.nextInt(80) == 0) e.attackEntityFrom(DamageSource.GENERIC, 0.5F);
        }
        if (!sneeze && !cough && coughBoost > 0) coughBoost--;

        if (e.isPotionActive(IqPotions.symptomFever)) {
            if (rand.nextInt(40) == 0) e.attackEntityFrom(FEVER, 1F);
            if (e.isInWater() && tick % 20 == 0) e.heal(1F);
        }
        if (e.isPotionActive(MobEffects.NAUSEA) && e instanceof EntityPlayer && rand.nextInt(20) == 0)
            ((EntityPlayer) e).getFoodStats().addStats(-1, 0);
        if (e.isPotionActive(IqPotions.symptomVomit)) {
            if (e instanceof EntityPlayer && rand.nextInt(10) == 0) ((EntityPlayer) e).getFoodStats().addStats(-2, 0);
            coughBoost = Math.max(coughBoost, 100);
            if (tick % 60 == id % 60) burst(e, IqBlood.SND_VOMIT, 1.0F, EnumParticleTypes.SLIME, 15);
        }
        if (e.isPotionActive(IqPotions.symptomHemorrhage)) {
            if (tick % 80 == id % 80) burst(e, IqBlood.SND_VOMIT, 0.8F, EnumParticleTypes.REDSTONE, 20);
            if (rand.nextInt(30) == 0) e.attackEntityFrom(BLEED, 6F);
        }
        if (e.isPotionActive(IqPotions.symptomSchizophrenia) && tick % 40 == 0)
            e.getEntityData().setInteger("schizoShader", rand.nextInt(4)); // IQ's client shader reads this; no shader ported yet
        if (e.isPotionActive(IqPotions.symptomParalysis) || e.isPotionActive(IqPotions.symptomComa) || e.isPotionActive(IqPotions.symptomCardiac)) {
            e.motionX = 0;
            e.motionZ = 0;
        }
        if (e.isPotionActive(IqPotions.symptomSeizure)) {
            e.motionX += rand.nextGaussian() * 0.15;
            e.motionZ += rand.nextGaussian() * 0.15;
            if (e instanceof EntityPlayer && rand.nextInt(100) == 0) {
                List<ItemStack> inv = ((EntityPlayer) e).inventory.mainInventory;
                int a = rand.nextInt(36), b = rand.nextInt(36);
                ItemStack tmp = inv.get(a);
                inv.set(a, inv.get(b));
                inv.set(b, tmp);
            }
        }
        if (e.isPotionActive(IqPotions.symptomSeptic) && rand.nextInt(20) == 0) e.attackEntityFrom(SEPTIC, 2F);
        if (e.isPotionActive(IqPotions.symptomNecrosis) && rand.nextInt(20) == 0) e.attackEntityFrom(DamageSource.GENERIC, 3F);
        if (e.isPotionActive(IqPotions.symptomAortic)) e.attackEntityFrom(AORTA, 1000F);
    }

    /** sound + a spray of particles out of the mouth (stands in for IQ's custom vomit particle) */
    private static void burst(EntityLivingBase e, SoundEvent sound, float pitch, EnumParticleTypes particle, int count) {
        if (sound != null) e.world.playSound(null, e.posX, e.posY, e.posZ, sound, SoundCategory.PLAYERS, 1.0F, pitch);
        if (!(e.world instanceof WorldServer)) return;
        Vec3d look = e.getLookVec();
        ((WorldServer) e.world).spawnParticle(particle, e.posX + look.x * 0.4, e.posY + e.getEyeHeight() - 0.15, e.posZ + look.z * 0.4,
                count, look.x * 0.3, 0.1, look.z * 0.3, 0.1);
    }

    /**
     * IQ sets max health to blood/500 (= 20 with full blood, which IQ always has) and to 1 during cardiac arrest.
     * Done as an attribute modifier on players so other mods' max-health changes survive.
     */
    private static void syncCardiac(EntityLivingBase e) {
        if (!(e instanceof EntityPlayer)) return;
        IAttributeInstance attr = e.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        AttributeModifier old = attr.getModifier(CARDIAC_UUID);
        if (!e.isPotionActive(IqPotions.symptomCardiac)) {
            if (old != null) attr.removeModifier(old);
            return;
        }
        if (old != null) return;
        attr.applyModifier(new AttributeModifier(CARDIAC_UUID, "ntmiq cardiac arrest", 1.0D - attr.getAttributeValue(), 0));
        if (e.getHealth() > 1.0F) e.setHealth(1.0F);
    }

    // ---------------- persistence ----------------
    public NBTTagCompound serialize() {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (BloodEntry e : entries) list.appendTag(e.writeToNBT(new NBTTagCompound()));
        nbt.setTag("entries", list);
        if (baseBlood != null) nbt.setString("baseBlood", baseBlood.getName());
        nbt.setTag("extra", extra);
        return nbt;
    }

    public void deserialize(NBTTagCompound nbt) {
        entries.clear();
        NBTTagList list = nbt.getTagList("entries", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            BloodEntry e = BloodEntry.readFromNBT(list.getCompoundTagAt(i));
            if (e != null) entries.add(e);
        }
        FluidType t = nbt.hasKey("baseBlood") ? Fluids.fromName(nbt.getString("baseBlood")) : Fluids.NONE;
        baseBlood = t == Fluids.NONE ? null : t;
        extra = nbt.getCompoundTag("extra");
    }

    // ---------------- capability plumbing ----------------
    public static class Storage implements Capability.IStorage<Bloodstream> {
        @Override public NBTBase writeNBT(Capability<Bloodstream> cap, Bloodstream inst, EnumFacing side) { return inst.serialize(); }
        @Override public void readNBT(Capability<Bloodstream> cap, Bloodstream inst, EnumFacing side, NBTBase nbt) {
            if (nbt instanceof NBTTagCompound) inst.deserialize((NBTTagCompound) nbt);
        }
    }

    public static class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final Bloodstream inst = new Bloodstream();

        @Override public boolean hasCapability(Capability<?> cap, EnumFacing side) { return cap == BloodstreamAPI.CAP; }
        @Override public <T> T getCapability(Capability<T> cap, EnumFacing side) { return cap == BloodstreamAPI.CAP ? BloodstreamAPI.CAP.cast(inst) : null; }
        @Override public NBTTagCompound serializeNBT() { return inst.serialize(); }
        @Override public void deserializeNBT(NBTTagCompound nbt) { inst.deserialize(nbt); }
    }
}
