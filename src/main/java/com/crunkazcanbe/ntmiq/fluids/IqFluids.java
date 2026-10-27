package com.crunkazcanbe.ntmiq.fluids;

import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.Fluids.CD_Canister;
import com.hbm.inventory.fluid.Fluids.CD_Gastank;
import com.hbm.inventory.fluid.trait.*;
import com.hbm.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_Toxin.ToxinDirectDamage;
import com.hbm.inventory.fluid.trait.FT_Toxin.ToxinEffects;
import com.hbm.lib.ModDamageSource;
import com.hbm.render.misc.EnumSymbol;
import com.hbm.util.ArmorRegistry.HazardClass;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.*;

import static com.hbm.inventory.fluid.Fluids.*;

/**
 * F24 from NTM: IQ Edition (irislgtm / tam69 / the IQ team, GPL-3): the 94 fluids IQ adds over NTM Space, registered the
 * CE-Space way as CE "foreign" fluids with fixed ids — but through CE's {@link Fluids#additionalListeners} instead of a mixin.
 *
 * <p>For other add-on code: reference the fields (or {@code Fluids.fromName("PISS")}) from your preInit on. The objects are
 * created once and only re-registered on CE's postInit {@code reloadFluids()}, so traits you add with {@code addTraits}
 * stay. A NEW trait class must be put in {@link #registerTrait} first, or CE's trait-config writer crashes on it.
 * IQ-only traits not ported here (owned by other slices): FT_Drug / FT_Pharma / FT_Pathogen / FT_Consumable (F01),
 * FT_ConstructionFoam (F19), FT_Heavenly / FT_Hellish (F05).
 *
 * <p>IDs: {@link #BASE_ID} + position in {@link #load()}. APPEND ONLY — ids are saved in worlds and item metadata.
 */
public final class IqFluids {
    private IqFluids() {}

    /** CE-Space starts at 4000; we stay well clear */
    public static final int BASE_ID = 6000;

    /** every IQ fluid, in id order */
    public static final List<FluidType> ALL = new ArrayList<>();
    private static final Map<String, FluidType> BY_NAME = new HashMap<>();
    /** IQ ships no tank_*.png for these -> keep CE's tinted default tank texture */
    private static final Set<String> NO_TANK_TEX = new HashSet<>(Arrays.asList("ANTISERUM", "CAUSTIC_SODA", "COMPOST", "DROSSAIR",
            "FERTILIZER", "HUMAN_BLOOD", "PISS", "PISS_DEPLETED", "PISS_HOT", "SALT_WATER", "SLAKED_LIME", "STALEAIR"));

    private static int nextId;
    private static boolean fresh;

    public static FluidType STIMPAK, SUPER_STIMPAK, MEDX, PSYCHO, AWESOME, ANTIDOTE, RADAWAY, RADX, CAY, POISON, GANJA;
    public static FluidType NUTRIENT_SLOP, TAMSLOP, TICL4, HEAVY_SAND_SLOP, MONAZITE_SLOP, NETHERAIR;
    public static FluidType BURNING_BLOATMUSK, MUSTY_BLOATMUSK, BLOATMUSK, MUSKY_PHEROMONE, CURDLING_BLOOD_HOT, CURDLING_BLOOD;
    public static FluidType SATANS_BLOOD, WORMWOOD, BRIMSTONE, VERDIGRIS, GALL, BRIMTUMEN, BLOOD_OF_THE_MARTYR, HAEMOGLOBIN, AMBROSIA;
    public static FluidType MORNINGSTARS_FIRE, HEPARIN, DNA, CHEVREUL_SALT, DIALYSATE, DEW_OF_THE_GARDEN, ARGENT_BLOAT_PUS, BLOAT_PUS, BLOATSPIRIT;
    public static FluidType PISS, PISS_HOT, PISS_DEPLETED;
    public static FluidType NATCRETE, BUURCRETE, SCRAPCRETE, BUURMIC_ACID, DILUTED_BUURMIC_ACID, LIGHT_BUURMIUM_SOLUTION;
    public static FluidType SULFUR_DIOXIDE, SODIUM_METAVANADATE, SOLDERING_ALLOY;
    public static FluidType INK_WHITE, INK_ORANGE, INK_MAGENTA, INK_LIGHT_BLUE, INK_YELLOW, INK_LIME, INK_PINK, INK_GRAY, INK_LIGHT_GRAY,
            INK_CYAN, INK_PURPLE, INK_BLUE, INK_BROWN, INK_GREEN, INK_RED, INK_BLACK;
    public static FluidType CONSTRUCTION_FOAM, CONSTRUCTION_FOAM_CONCRETE, CONSTRUCTION_FOAM_REINFORCED_CONCRETE, CONSTRUCTION_FOAM_DUCRETE,
            CONSTRUCTION_FOAM_REINFORCED_DUCRETE, CONSTRUCTION_FOAM_LIMESTONE, CONSTRUCTION_FOAM_NETHERRACK, CONSTRUCTION_FOAM_DEPTHROCK,
            CONSTRUCTION_FOAM_NETHER_DEPTHROCK;
    public static FluidType STALEAIR, DROSSAIR, COLOSTAMINE, MALAISE, PUTRYIN, GRAY_GOO, DORMANT_GRAY_GOO, BLOOD_VAPORS, LUNG_FUEL;
    public static FluidType CAUSTIC_SODA, SLAKED_LIME, SALT_WATER, FERTILIZER, COMPOST, HUMAN_BLOOD, ANTISERUM;

    /** the 16 inks in dye-metadata order (0 = white .. 15 = black) */
    public static FluidType[] inks() {
        return new FluidType[] { INK_WHITE, INK_ORANGE, INK_MAGENTA, INK_LIGHT_BLUE, INK_YELLOW, INK_LIME, INK_PINK, INK_GRAY,
                INK_LIGHT_GRAY, INK_CYAN, INK_PURPLE, INK_BLUE, INK_BROWN, INK_GREEN, INK_RED, INK_BLACK };
    }

    /** call from the @Mod constructor: CE runs Fluids.init() in its own preInit, before ours */
    public static void register() {
        registerTrait("ink", FT_Ink.class);
        Fluids.additionalListeners.add(IqFluids::load);
    }

    public static void registerTrait(String name, Class<? extends FluidTrait> clazz) {
        if (FluidTrait.traitNameMap.containsKey(name)) return;
        FluidTrait.traitNameMap.put(name, clazz);
        FluidTrait.traitList.add(clazz);
    }

    /** create (first call) or re-register (CE's reloadFluids) every fluid; values from IQ's Fluids.java */
    private static void load() {
        nextId = BASE_ID;
        ToxinDirectDamage acidLung = new ToxinDirectDamage(ModDamageSource.acid, 1F, 20, HazardClass.GAS_LUNG, false);

        // pharma (F01 adds the FT_Drug effects)
        STIMPAK = f("STIMPAK", 0xFF4444, 0, 0, 0, EnumSymbol.NONE); t(STIMPAK, LIQUID);
        SUPER_STIMPAK = f("SUPER_STIMPAK", 0xFF6666, 0, 0, 0, EnumSymbol.NONE); t(SUPER_STIMPAK, LIQUID);
        MEDX = f("MEDX", 0xAAAAFF, 0, 0, 0, EnumSymbol.NONE); t(MEDX, LIQUID);
        PSYCHO = f("PSYCHO", 0xFFAA00, 0, 0, 0, EnumSymbol.NONE); t(PSYCHO, LIQUID);
        AWESOME = f("AWESOME", 0xFF7FED, 0, 0, 0, EnumSymbol.NONE); t(AWESOME, LIQUID);
        ANTIDOTE = f("ANTIDOTE", 0x00FF7F, 0, 0, 0, EnumSymbol.NONE); t(ANTIDOTE, LIQUID);
        RADAWAY = f("RADAWAY", 0xD8A838, 0, 0, 0, EnumSymbol.NONE); t(RADAWAY, LIQUID);
        RADX = f("RADX", 0xBB4B00, 0, 0, 0, EnumSymbol.NONE); t(RADX, LIQUID);
        CAY = f("CAY", 0x8A3324, 0, 0, 0, EnumSymbol.NONE); t(CAY, LIQUID);
        POISON = f("POISON", 0x9B2F2F, 1, 0, 0, EnumSymbol.NONE);
        t(POISON, LIQUID, new FT_Toxin().addEntry(new ToxinDirectDamage(ModDamageSource.cloud, 3F, 20, HazardClass.PARTICLE_FINE, false)));
        GANJA = f("GANJA", 0x4E7A27, 0, 0, 0, EnumSymbol.NONE); t(GANJA, LIQUID);

        // slops, ore chemistry
        NUTRIENT_SLOP = f("NUTRIENT_SLOP", 0x7CC35E, 0, 0, 0, EnumSymbol.NONE); t(NUTRIENT_SLOP, LIQUID, VISCOUS);
        TAMSLOP = f("TAMSLOP", 0x5B9E3F, 0, 0, 0, EnumSymbol.NONE); t(TAMSLOP, LIQUID, VISCOUS);
        TICL4 = f("TICL4", 0xF2F2A4, 3, 0, 2, EnumSymbol.ACID); t(TICL4, LIQUID, new FT_Corrosive(30));
        HEAVY_SAND_SLOP = f("HEAVY_SAND_SLOP", 0x8B7D6B, 0, 0, 0, EnumSymbol.NONE); t(HEAVY_SAND_SLOP, LIQUID, VISCOUS);
        MONAZITE_SLOP = f("MONAZITE_SLOP", 0xC4A84B, 0, 0, 0, EnumSymbol.NONE); t(MONAZITE_SLOP, LIQUID, VISCOUS);
        NETHERAIR = f("NETHERAIR", 0xB34B4B, 0, 0, 0, EnumSymbol.NONE);
        t(NETHERAIR, GASEOUS, new FT_Toxin().addEntry(new ToxinDirectDamage(ModDamageSource.cloud, 1.5F, 20, HazardClass.GAS_MONOXIDE, false))
                .addEntry(new ToxinDirectDamage(ModDamageSource.cloud, 1.5F, 20, HazardClass.GAS_LUNG, false)));
        c(NETHERAIR, new CD_Gastank(0x8C21FF, 0xFFFFFF));

        // glyphid / nether fluids (F05 adds FT_Drug + FT_Hellish / FT_Heavenly)
        BURNING_BLOATMUSK = f("BURNING_BLOATMUSK", 0xFFAA00, 2, 3, 0, EnumSymbol.NONE);
        t(BURNING_BLOATMUSK, LIQUID, new FT_Combustible(FuelGrade.LOW, 250_000)); c(BURNING_BLOATMUSK, new CD_Canister(0xFFAA00));
        MUSTY_BLOATMUSK = f("MUSTY_BLOATMUSK", 0x9C8A1F, 3, 0, 0, EnumSymbol.NONE);
        t(MUSTY_BLOATMUSK, GASEOUS, new FT_Toxin().addEntry(acidLung)); c(MUSTY_BLOATMUSK, new CD_Gastank(0x9C8A1F, 0xFFFFFF));
        BLOATMUSK = f("BLOATMUSK", 0xFFD800, 1, 2, 0, EnumSymbol.NONE);
        t(BLOATMUSK, GASEOUS, new FT_Heatable().setEff(HeatingType.BOILER, 1.0D).addStep(120, 1, BURNING_BLOATMUSK, 1));
        c(BLOATMUSK, new CD_Gastank(0xFFD800, 0xFFFFFF));
        MUSKY_PHEROMONE = f("MUSKY_PHEROMONE", 0x7B5EA7, 1, 0, 0, EnumSymbol.NONE);
        t(MUSKY_PHEROMONE, LIQUID, new FT_Pheromone(3)); c(MUSKY_PHEROMONE, new CD_Canister(0x7B5EA7));
        CURDLING_BLOOD_HOT = f("CURDLING_BLOOD_HOT", 0x1A0000, 1, 0, 0, EnumSymbol.NONE);
        t(CURDLING_BLOOD_HOT, GASEOUS); c(CURDLING_BLOOD_HOT, new CD_Gastank(0x1A0000, 0xFFFFFF));
        CURDLING_BLOOD = f("CURDLING_BLOOD", 0x3A0000, 1, 0, 0, EnumSymbol.NONE);
        t(CURDLING_BLOOD, LIQUID, new FT_Heatable().setEff(HeatingType.PWR, 1.0D).addStep(400, 1, CURDLING_BLOOD_HOT, 1), new FT_PWRModerator(3.0D));
        c(CURDLING_BLOOD, new CD_Canister(0x3A0000));
        SATANS_BLOOD = f("SATANS_BLOOD", 0x6B0000, 2, 0, 0, EnumSymbol.NONE); t(SATANS_BLOOD, LIQUID); c(SATANS_BLOOD, new CD_Canister(0x6B0000));
        WORMWOOD = f("WORMWOOD", 0x223322, 3, 0, 0, EnumSymbol.NONE);
        t(WORMWOOD, LIQUID, new FT_Toxin().addEntry(acidLung)); c(WORMWOOD, new CD_Canister(0x223322));
        BRIMSTONE = f("BRIMSTONE", 0xD4B000, 2, 3, 1, EnumSymbol.NONE);
        t(BRIMSTONE, LIQUID, new FT_Combustible(FuelGrade.HIGH, 600_000)); c(BRIMSTONE, new CD_Canister(0xD4B000));
        VERDIGRIS = f("VERDIGRIS", 0x3A9E6E, 2, 0, 0, EnumSymbol.NONE); t(VERDIGRIS, LIQUID); c(VERDIGRIS, new CD_Canister(0x3A9E6E));
        GALL = f("GALL", 0x7A6B00, 3, 0, 0, EnumSymbol.NONE); t(GALL, LIQUID, new FT_Toxin().addEntry(acidLung)); c(GALL, new CD_Canister(0x7A6B00));
        BRIMTUMEN = f("BRIMTUMEN", 0x2E2E2E, 1, 2, 0, EnumSymbol.NONE); t(BRIMTUMEN, LIQUID); c(BRIMTUMEN, new CD_Canister(0x2E2E2E));
        BLOOD_OF_THE_MARTYR = f("BLOOD_OF_THE_MARTYR", 0xC00000, 2, 0, 0, EnumSymbol.NONE);
        t(BLOOD_OF_THE_MARTYR, LIQUID); c(BLOOD_OF_THE_MARTYR, new CD_Canister(0xC00000));
        HAEMOGLOBIN = f("HAEMOGLOBIN", 0x8A2B2B, 1, 0, 0, EnumSymbol.NONE); t(HAEMOGLOBIN, LIQUID); c(HAEMOGLOBIN, new CD_Canister(0x8A2B2B));
        AMBROSIA = f("AMBROSIA", 0xFFE8A0, 0, 0, 0, EnumSymbol.NONE); t(AMBROSIA, LIQUID); c(AMBROSIA, new CD_Canister(0xFFE8A0));
        MORNINGSTARS_FIRE = f("MORNINGSTARS_FIRE", 0xFFFFFF, 2, 4, 2, EnumSymbol.NONE);
        t(MORNINGSTARS_FIRE, GASEOUS, new FT_Flammable(1_000_000), new FT_Combustible(FuelGrade.AERO, 1_500_000), new FT_Rocket(321, 1_564_000));
        c(MORNINGSTARS_FIRE, new CD_Gastank(0xFFFFFF, 0xE0E0E0));
        HEPARIN = f("HEPARIN", 0xE09090, 0, 0, 0, EnumSymbol.NONE); t(HEPARIN, LIQUID); c(HEPARIN, new CD_Canister(0xE09090));
        DNA = f("DNA", 0x6A5ACD, 0, 0, 0, EnumSymbol.NONE); t(DNA, LIQUID);
        CHEVREUL_SALT = f("CHEVREUL_SALT", 0x40A8A8, 0, 0, 0, EnumSymbol.NONE); t(CHEVREUL_SALT, LIQUID); c(CHEVREUL_SALT, new CD_Canister(0x40A8A8));
        DIALYSATE = f("DIALYSATE", 0x90C8E8, 0, 0, 0, EnumSymbol.NONE); t(DIALYSATE, LIQUID); c(DIALYSATE, new CD_Canister(0x90C8E8));
        DEW_OF_THE_GARDEN = f("DEW_OF_THE_GARDEN", 0xF7F0C0, 4, 0, 4, EnumSymbol.NONE);
        t(DEW_OF_THE_GARDEN, LIQUID, new FT_Corrosive(30)); c(DEW_OF_THE_GARDEN, new CD_Canister(0xF7F0C0));
        ARGENT_BLOAT_PUS = f("ARGENT_BLOAT_PUS", 0xD8D8D8, 2, 0, 0, EnumSymbol.NONE);
        t(ARGENT_BLOAT_PUS, LIQUID, new FT_Toxin().addEntry(acidLung)); c(ARGENT_BLOAT_PUS, new CD_Canister(0xD8D8D8));
        BLOAT_PUS = f("BLOAT_PUS", 0x8FA63F, 3, 0, 0, EnumSymbol.NONE);
        t(BLOAT_PUS, LIQUID, new FT_Toxin().addEntry(acidLung)); c(BLOAT_PUS, new CD_Canister(0x8FA63F));
        BLOATSPIRIT = f("BLOATSPIRIT", 0xE8E8FF, 1, 3, 1, EnumSymbol.NONE);
        t(BLOATSPIRIT, GASEOUS, new FT_Combustible(FuelGrade.AERO, 1_375_000)); c(BLOATSPIRIT, new CD_Gastank(0xE8E8FF, 0xFFFFFF));

        // Piss: reactor coolant that decomposes (PWR / heat exchanger: PISS -> PISS_HOT -> PISS_DEPLETED, reprocessed in the chem plant)
        PISS = f("PISS", 0xCAC07D, 0, 0, 0, EnumSymbol.NONE);
        PISS_HOT = f("PISS_HOT", 0x7A3506, 0, 1, 0, EnumSymbol.NONE);
        PISS_DEPLETED = f("PISS_DEPLETED", 0x962608, 0, 0, 0, EnumSymbol.NONE);
        t(PISS, new FT_Corrosive(5), LIQUID,
                new FT_Toxin().addEntry(new ToxinEffects(HazardClass.BACTERIA, false).add(new PotionEffect(MobEffects.POISON, 300, 1), new PotionEffect(MobEffects.BLINDNESS, 100, 0))),
                new FT_Heatable().setEff(HeatingType.PWR, 2.5D).setEff(HeatingType.HEATEXCHANGER, 1.0D).setEff(HeatingType.BOILER, 0.2D).addStep(50, 1, PISS_HOT, 1));
        t(PISS_HOT, new FT_Corrosive(45), LIQUID, VISCOUS, new FT_Coolable(PISS_DEPLETED, 1, 1, 69).setEff(CoolingType.HEATEXCHANGER, 4.2D));
        if (fresh) PISS_HOT.setTemp(135);
        t(PISS_DEPLETED, new FT_Corrosive(30), LIQUID, VISCOUS);

        // concretes, buurmium (F07 machines use these)
        NATCRETE = f("NATCRETE", 0xA09696, 0, 1, 0, EnumSymbol.NONE); t(NATCRETE, LIQUID); if (fresh) NATCRETE.setTemp(1500);
        BUURCRETE = f("BUURCRETE", 0xA4A08D, 0, 0, 0, EnumSymbol.RADIATION); t(BUURCRETE, LIQUID);
        SCRAPCRETE = f("SCRAPCRETE", 0xACACAC, 0, 0, 0, EnumSymbol.NONE); t(SCRAPCRETE, LIQUID);
        BUURMIC_ACID = f("BUURMIC_ACID", 0xD0D411, 0, 0, 2, EnumSymbol.ACID); t(BUURMIC_ACID, LIQUID, new FT_Corrosive(2137));
        DILUTED_BUURMIC_ACID = f("DILUTED_BUURMIC_ACID", 0xE0E448, 0, 0, 1, EnumSymbol.ACID); t(DILUTED_BUURMIC_ACID, LIQUID, new FT_Corrosive(10));
        LIGHT_BUURMIUM_SOLUTION = f("LIGHT_BUURMIUM_SOLUTION", 0xFFFF00, 0, 0, 0, EnumSymbol.RADIATION); t(LIGHT_BUURMIUM_SOLUTION, LIQUID, VISCOUS);

        SULFUR_DIOXIDE = f("SULFUR_DIOXIDE", 0xC9C069, 2, 0, 0, EnumSymbol.ASPHYXIANT); t(SULFUR_DIOXIDE, GASEOUS, new FT_Corrosive(10));
        SODIUM_METAVANADATE = f("SODIUM_METAVANADATE", 0xD9D05A, 3, 0, 1, EnumSymbol.ACID); t(SODIUM_METAVANADATE, LIQUID, VISCOUS, new FT_Corrosive(20));
        SOLDERING_ALLOY = f("SOLDERING_ALLOY", 0x999999, 2, 0, 0, EnumSymbol.NONE); t(SOLDERING_ALLOY, LIQUID);

        // F16 spraypainter inks
        INK_WHITE = ink("INK_WHITE", 0xF0F0F0, 0);
        INK_ORANGE = ink("INK_ORANGE", 0xF2761E, 1);
        INK_MAGENTA = ink("INK_MAGENTA", 0xAF46C5, 2);
        INK_LIGHT_BLUE = ink("INK_LIGHT_BLUE", 0x5B9BD5, 3);
        INK_YELLOW = ink("INK_YELLOW", 0xFED43E, 4);
        INK_LIME = ink("INK_LIME", 0x7FC638, 5);
        INK_PINK = ink("INK_PINK", 0xE887A4, 6);
        INK_GRAY = ink("INK_GRAY", 0x474F52, 7);
        INK_LIGHT_GRAY = ink("INK_LIGHT_GRAY", 0x9D9D97, 8);
        INK_CYAN = ink("INK_CYAN", 0x158991, 9);
        INK_PURPLE = ink("INK_PURPLE", 0x8932B8, 10);
        INK_BLUE = ink("INK_BLUE", 0x364CB5, 11);
        INK_BROWN = ink("INK_BROWN", 0x835432, 12);
        INK_GREEN = ink("INK_GREEN", 0x4E7A27, 13);
        INK_RED = ink("INK_RED", 0xB02E2D, 14);
        INK_BLACK = ink("INK_BLACK", 0x1D1D21, 15);

        // F19 construction foam (F19 adds FT_ConstructionFoam)
        CONSTRUCTION_FOAM = foam("CONSTRUCTION_FOAM", 0xE6E2DA);
        CONSTRUCTION_FOAM_CONCRETE = foam("CONSTRUCTION_FOAM_CONCRETE", 0xC6C2BA);
        CONSTRUCTION_FOAM_REINFORCED_CONCRETE = foam("CONSTRUCTION_FOAM_REINFORCED_CONCRETE", 0xB0ACA4);
        CONSTRUCTION_FOAM_DUCRETE = foam("CONSTRUCTION_FOAM_DUCRETE", 0xBFB9A3);
        CONSTRUCTION_FOAM_REINFORCED_DUCRETE = foam("CONSTRUCTION_FOAM_REINFORCED_DUCRETE", 0xA8A28C);
        CONSTRUCTION_FOAM_LIMESTONE = foam("CONSTRUCTION_FOAM_LIMESTONE", 0xD6CCAD);
        CONSTRUCTION_FOAM_NETHERRACK = foam("CONSTRUCTION_FOAM_NETHERRACK", 0x9E4E48);
        CONSTRUCTION_FOAM_DEPTHROCK = foam("CONSTRUCTION_FOAM_DEPTHROCK", 0x8A8A8A);
        CONSTRUCTION_FOAM_NETHER_DEPTHROCK = foam("CONSTRUCTION_FOAM_NETHER_DEPTHROCK", 0x9A6A66);

        // atmospheres, contagion (F02/F06), haemodialysis
        STALEAIR = f("STALEAIR", 0xCCCCCC, 0, 0, 0, EnumSymbol.NONE); t(STALEAIR, GASEOUS); c(STALEAIR, new CD_Gastank(0xCCCCCC, 0xFFFF00));
        DROSSAIR = f("DROSSAIR", 0x1E5B2E, 4, 0, 0, EnumSymbol.NONE); t(DROSSAIR, GASEOUS, new FT_Poison(true, 1)); c(DROSSAIR, new CD_Gastank(0x1E5B2E, 0x0A2E15));
        COLOSTAMINE = f("COLOSTAMINE", 0xE6D9A8, 0, 0, 0, EnumSymbol.NONE); t(COLOSTAMINE, LIQUID, VISCOUS);
        MALAISE = f("MALAISE", 0x8E5FB0, 0, 0, 0, EnumSymbol.NONE); t(MALAISE, LIQUID, VISCOUS);
        PUTRYIN = f("PUTRYIN", 0x7A8C3A, 0, 0, 0, EnumSymbol.NONE); t(PUTRYIN, LIQUID, VISCOUS);
        GRAY_GOO = f("GRAY_GOO", 0x8C8F94, 0, 0, 0, EnumSymbol.NONE); t(GRAY_GOO, LIQUID, VISCOUS);
        DORMANT_GRAY_GOO = f("DORMANT_GRAY_GOO", 0x4E5054, 0, 0, 0, EnumSymbol.NONE); t(DORMANT_GRAY_GOO, LIQUID, VISCOUS);
        BLOOD_VAPORS = f("BLOOD_VAPORS", 0x8A0A0A, 1, 1, 0, EnumSymbol.NONE); t(BLOOD_VAPORS, GASEOUS, new FT_Flammable(25_000)); c(BLOOD_VAPORS, new CD_Gastank(0x8A0A0A, 0xE0C0C0));
        LUNG_FUEL = f("LUNG_FUEL", 0xB01B1B, 1, 2, 0, EnumSymbol.NONE);
        if (fresh) {   // IQ: registerCalculatedFuel(LUNG_FUEL, KEROSENE_REFORM heat energy, 1.5, AERO) — CE's helper is private, same maths here
            // CE attaches its fluid traits AFTER this runs (they come from its trait config), so a CE fluid's
            // FT_Flammable can still be null here; that null crashed the whole pack on 10-16. Fall back to
            // CE's default reformed-kerosene heat energy.
            FT_Flammable kf = KEROSENE_REFORM.getTrait(FT_Flammable.class);
            long base = kf != null ? kf.getHeatEnergy() : 1_250_000L;
            LUNG_FUEL.addTraits(LIQUID, P_FUEL, new FT_Rocket(321, 1_564_000), new FT_Flammable(round(base)), new FT_Combustible(FuelGrade.AERO, round((long) (base * 1.5D))));
            LUNG_FUEL.addContainers(new CD_Canister(0xFF2C2C));
        }

        // misc chemistry / farming (F13 growth chamber uses salt water, fertilizer, compost)
        CAUSTIC_SODA = f("CAUSTIC_SODA", 0xE4ECEE, 3, 0, 1, EnumSymbol.ACID); t(CAUSTIC_SODA, LIQUID, VISCOUS, new FT_Corrosive(45));
        SLAKED_LIME = f("SLAKED_LIME", 0xE0DDD0, 1, 0, 0, EnumSymbol.NONE); t(SLAKED_LIME, LIQUID, VISCOUS);
        SALT_WATER = f("SALT_WATER", 0x3333FF, 0, 0, 0, EnumSymbol.NONE); t(SALT_WATER, LIQUID, UNSIPHONABLE);
        FERTILIZER = f("FERTILIZER", 0x808853, 2, 0, 3, EnumSymbol.OXIDIZER); t(FERTILIZER, LIQUID);
        COMPOST = f("COMPOST", 0x4F3F17, 0, 0, 0, EnumSymbol.NONE); t(COMPOST, LIQUID, VISCOUS, UNSIPHONABLE);
        HUMAN_BLOOD = f("HUMAN_BLOOD", 0x8A0A0A, 0, 0, 0, EnumSymbol.NONE);   // F02 adds FT_Pathogen
        ANTISERUM = f("ANTISERUM", 0x88FF88, 0, 0, 0, EnumSymbol.NONE);       // F01 adds FT_Pharma
        // append new fluids HERE only (ids!)
    }

    /** create or re-register one fluid; sets {@link #fresh} so traits/containers are only added once */
    private static FluidType f(String name, int color, int p, int fl, int r, EnumSymbol symbol) {
        int id = nextId++;
        ResourceLocation tex = name.equals("STALEAIR")   // IQ ships no icon for it
                ? new ResourceLocation("hbm", "textures/gui/fluids/air.png")
                : new ResourceLocation(Tags.MODID, "textures/gui/fluids/" + name.toLowerCase(Locale.US) + ".png");
        FluidType type = BY_NAME.get(name);
        fresh = type == null;
        if (fresh) {
            type = new FluidType(name, id, color, p, fl, r, symbol, tex);
            BY_NAME.put(name, type);
            ALL.add(type);
        } else {
            type.setupForeign(name, id, color, p, fl, r, symbol, tex);
        }
        type.renderWithTint = NO_TANK_TEX.contains(name);   // otherwise CE draws hbm:textures/models/tank/tank_<NAME>.png
        return type;
    }

    private static void t(FluidType type, FluidTrait... traits) {
        if (fresh) type.addTraits(traits);
    }

    private static void c(FluidType type, Object... containers) {
        if (fresh) type.addContainers(containers);
    }

    private static FluidType ink(String name, int color, int dye) {
        FluidType type = f(name, color, 0, 0, 0, EnumSymbol.NONE);
        t(type, LIQUID, new FT_Ink(dye));
        return type;
    }

    private static FluidType foam(String name, int color) {
        FluidType type = f(name, color, 0, 0, 0, EnumSymbol.NONE);
        t(type, LIQUID, VISCOUS);
        return type;
    }

    /** CE Fluids.round (private) */
    private static long round(long l) {
        if (l > 10_000_000L) return l - l % 100_000L;
        if (l > 1_000_000L) return l - l % 10_000L;
        if (l > 100_000L) return l - l % 1_000L;
        if (l > 10_000L) return l - l % 100L;
        return l > 1_000L ? l - l % 10L : l;
    }
}
