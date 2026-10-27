package com.crunkazcanbe.ntmiq.blood;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hbm.config.VersatileConfig;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

/**
 * Public API of the F01 bloodstream (NTM: IQ Edition's HbmBloodstreamProps + Injectables + SymptomPool, GPL-3).
 * Every {@link EntityLivingBase} carries a {@link Bloodstream} capability: its base blood (10 000 mB of HUMAN_BLOOD
 * by default) plus dissolved drug fluids and, from F02 on, pathogen entries. Server-side only — the client never needs it.
 *
 * <p>Plug points for later features:
 * <ul>
 * <li>{@link #registerTicker} — runs every server tick per living entity after drugs decay (F02 ticks its pathogen
 *     entries here; entries with a frameId and amount ≤ 0 are removed afterwards).</li>
 * <li>{@link #registerPayload} — stack-NBT payloads a syringe carries into the target (F02: "pathogen", "pharma").</li>
 * <li>{@link #registerSampler} — adds extra pathogen tags when a medical syringe draws a sample.</li>
 * <li>{@link #registerBehavior} — what a base blood type does (F03 husks with odd blood).</li>
 * <li>{@link #registerSymptom} / {@link #getSymptom} — named symptom bundles used as drug side effects and by diseases.</li>
 * </ul>
 */
public final class BloodstreamAPI {
    private BloodstreamAPI() {}

    public static final float BASE_BLOOD = 10_000F;

    @CapabilityInject(Bloodstream.class)
    public static Capability<Bloodstream> CAP = null;

    /** runs per living entity per server tick */
    public interface Ticker { void tick(EntityLivingBase entity, Bloodstream blood); }

    /** applies stack NBT under its key to an injected target */
    public interface StackPayload { void apply(ItemStack stack, EntityLivingBase target, float intensity); }

    /** appends pathogen tags ({frame, amount, mut}) to a medical-syringe sample */
    public interface SampleSource { void addSample(EntityLivingBase target, NBTTagList pathogens); }

    static final List<Ticker> TICKERS = new ArrayList<>();
    static final Map<String, StackPayload> PAYLOADS = new LinkedHashMap<>();
    static final List<SampleSource> SAMPLERS = new ArrayList<>();
    private static final Map<String, BloodBehavior> BEHAVIORS = new HashMap<>();
    private static final Map<String, List<FT_Consumable.ConsumableEffect>> SYMPTOMS = new HashMap<>();

    public static void registerTicker(Ticker t) { TICKERS.add(t); }
    public static void registerPayload(String nbtKey, StackPayload p) { PAYLOADS.put(nbtKey, p); }
    public static void registerSampler(SampleSource s) { SAMPLERS.add(s); }

    public static void registerBehavior(FluidType type, BloodBehavior b) {
        if (type != null && type != Fluids.NONE) BEHAVIORS.put(type.getName(), b);
    }

    public static BloodBehavior getBehavior(FluidType type) { return type == null ? null : BEHAVIORS.get(type.getName()); }

    public static void registerSymptom(String key, FT_Consumable.ConsumableEffect... effects) { SYMPTOMS.put(key, Arrays.asList(effects)); }

    /** @return null if unknown */
    public static List<FT_Consumable.ConsumableEffect> getSymptom(String key) { return SYMPTOMS.get(key); }

    /** the entity's bloodstream; never null for a living entity once the capability is attached */
    public static Bloodstream get(EntityLivingBase entity) {
        Bloodstream b = entity.getCapability(CAP, null);
        return b != null ? b : new Bloodstream(); // ponytail: throwaway for entities that somehow lack it
    }

    /**
     * IQ's Injectables.process: put {@code amount} mB of {@code type} into the entity. Injections respect CE potion
     * sickness and give a little of it; corrosive fluids hurt when injected.
     */
    public static void inject(EntityLivingBase entity, FluidType type, float amount, float intensity, boolean isInjection) {
        if (entity == null || type == null || type == Fluids.NONE) return;
        if (isInjection && VersatileConfig.hasPotionSickness(entity)) return;

        if (isInjection && type.hasTrait(FT_Corrosive.class)) entity.attackEntityFrom(DamageSource.GENERIC, amount * intensity * 0.5F);
        FT_Consumable cons = type.getTrait(FT_Consumable.class);
        if (cons != null) cons.apply(entity, intensity);
        FT_Drug drug = type.getTrait(FT_Drug.class);
        if (drug != null) drug.apply(entity, intensity);

        if (type == Fluids.ENDERJUICE) {
            double x = entity.posX + entity.getRNG().nextGaussian() * 16, z = entity.posZ + entity.getRNG().nextGaussian() * 16;
            BlockPos top = entity.world.getTopSolidOrLiquidBlock(new BlockPos(x, entity.posY, z));
            entity.setPositionAndUpdate(x, top.getY(), z);
        }
        if (type == Fluids.XPJUICE && entity instanceof EntityPlayer) ((EntityPlayer) entity).addExperience((int) (10 * intensity));

        if (isInjection) VersatileConfig.applyPotionSickness(entity, type == BloodFluids.SUPER_STIMPAK ? 15 : 5);
    }

    /** run every registered payload whose key is in the stack's NBT */
    public static void applyStackPayload(ItemStack stack, EntityLivingBase target, float intensity) {
        if (stack.isEmpty() || !stack.hasTagCompound() || target == null) return;
        for (Map.Entry<String, StackPayload> e : PAYLOADS.entrySet())
            if (stack.getTagCompound().hasKey(e.getKey())) e.getValue().apply(stack, target, intensity);
    }

    /** IQ's SymptomPool defaults (needs {@link IqPotions} created) */
    static void defaultSymptoms() {
        registerSymptom("rash", fx(IqPotions.symptomRash, 0));
        registerSymptom("sneeze", fx(IqPotions.symptomSneeze, 0));
        registerSymptom("cough", fx(IqPotions.symptomCough, 0), fx(MobEffects.MINING_FATIGUE, 1));
        registerSymptom("nausea", fx(MobEffects.NAUSEA, 0), fx(MobEffects.HUNGER, 1));
        registerSymptom("fever", fx(IqPotions.symptomFever, 0), fx(MobEffects.MINING_FATIGUE, 2));
        registerSymptom("vomit", fx(IqPotions.symptomVomit, 0), fx(MobEffects.HUNGER, 2));
        registerSymptom("schizophrenia", fx(IqPotions.symptomSchizophrenia, 0), fx(MobEffects.BLINDNESS, 0));
        registerSymptom("hemorrhage", fx(IqPotions.symptomHemorrhage, 0), fx(MobEffects.WEAKNESS, 2));
        registerSymptom("paralysis", fx(IqPotions.symptomParalysis, 0), fx(MobEffects.SLOWNESS, 4), fx(MobEffects.MINING_FATIGUE, 4));
        registerSymptom("seizure", fx(IqPotions.symptomSeizure, 0), fx(MobEffects.NAUSEA, 2));
        registerSymptom("septic", fx(IqPotions.symptomSeptic, 0), fx(MobEffects.SLOWNESS, 2));
        registerSymptom("coma", fx(IqPotions.symptomComa, 0), fx(MobEffects.BLINDNESS, 0));
        registerSymptom("aortic", fx(IqPotions.symptomAortic, 0));
        registerSymptom("necrosis", fx(IqPotions.symptomNecrosis, 0), fx(MobEffects.MINING_FATIGUE, 3));
        registerSymptom("cardiac", fx(IqPotions.symptomCardiac, 0), fx(MobEffects.BLINDNESS, 0));
        registerSymptom("harm", fx(MobEffects.INSTANT_DAMAGE, 0));
        registerSymptom("confusion", fx(MobEffects.NAUSEA, 0));
        registerSymptom("wither", fx(MobEffects.WITHER, 0));
        registerSymptom("hunger", fx(MobEffects.HUNGER, 1));
        registerSymptom("death", fx(com.hbm.potion.HbmPotion.death, 0));
    }

    private static FT_Consumable.ConsumableEffect fx(Potion p, int amp) { return new FT_Consumable.ConsumableEffect(p, amp); }
}
