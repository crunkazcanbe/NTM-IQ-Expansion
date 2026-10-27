package com.crunkazcanbe.ntmiq.blood;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.MixerRecipes.MixerRecipe;
import com.hbm.items.ModItems;
import com.hbm.potion.HbmPotion;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;

import static com.hbm.inventory.OreDictManager.QUARTZ;

/**
 * F01's side of the fluids: IQ's FT_Drug / FT_Consumable traits on the drug fluids (created by F24's {@link IqFluids})
 * and on CE fluids that already exist, plus IQ's blood-type behaviours and drug mixer recipes.
 */
public final class BloodFluids {
    private BloodFluids() {}

    public static FluidType HUMAN_BLOOD, STIMPAK, SUPER_STIMPAK, MEDX, PSYCHO, RADAWAY, RADX, CAY, GANJA;

    /** our preInit: IqFluids has already created its fluids inside CE's Fluids.init() */
    static void create() {
        IqFluids.registerTrait("ntmiq_drug", FT_Drug.class);          // CE writes trait JSON by these names
        IqFluids.registerTrait("ntmiq_consumable", FT_Consumable.class);
        HUMAN_BLOOD = IqFluids.HUMAN_BLOOD;
        STIMPAK = IqFluids.STIMPAK;
        SUPER_STIMPAK = IqFluids.SUPER_STIMPAK;
        MEDX = IqFluids.MEDX;
        PSYCHO = IqFluids.PSYCHO;
        RADAWAY = IqFluids.RADAWAY;
        RADX = IqFluids.RADX;
        CAY = IqFluids.CAY;
        GANJA = IqFluids.GANJA;
        traits();
        Fluids.additionalListeners.add(BloodFluids::traits); // after CE's reloadFluids re-reads trait JSON, put ours back
    }

    /** IQ balance (Fluids.java), traits re-applied idempotently (CE keys traits by class) */
    static void traits() {
        drug(STIMPAK, new FT_Drug().setThreshold(0.01F).setDissipationRate(10F).addEffect(IqPotions.stimulated, 0).addSideEffect("cough"));
        drug(SUPER_STIMPAK, new FT_Drug().setThreshold(0.01F).setDissipationRate(15F).addEffect(IqPotions.stimulated, 1).addSideEffect("hemorrhage"));
        drug(MEDX, new FT_Drug().setThreshold(0.01F).setDissipationRate(30F).addEffect(IqPotions.medx, 0).addSideEffect("nausea"));
        drug(PSYCHO, new FT_Drug().setThreshold(0.01F).setDissipationRate(15F).addEffect(IqPotions.roidRage, 0).addSideEffect("schizophrenia"));
        drug(RADAWAY, new FT_Drug().setThreshold(0.01F).setDissipationRate(20F).addEffect(HbmPotion.radaway, 0));
        drug(RADX, new FT_Drug().setThreshold(0.01F).setDissipationRate(20F).addEffect(HbmPotion.radx, 0));
        drug(CAY, new FT_Drug().setConsumption(50).setThreshold(0.02F).setDissipationRate(30F).addEffect(IqPotions.turkishRage, 0));
        drug(GANJA, new FT_Drug().setConsumption(50).setThreshold(0.02F).setDissipationRate(30F).addEffect(IqPotions.ganja, 0));
        drug(IqFluids.AWESOME, new FT_Drug().setThreshold(0.01F).setDissipationRate(15F).addEffect(MobEffects.SPEED, 3).addEffect(MobEffects.HASTE, 3)
                .addEffect(MobEffects.STRENGTH, 3).addEffect(MobEffects.JUMP_BOOST, 3).addEffect(MobEffects.RESISTANCE, 3).addEffect(MobEffects.FIRE_RESISTANCE, 3)
                .addEffect(MobEffects.WATER_BREATHING, 3).addEffect(MobEffects.REGENERATION, 3).addEffect(MobEffects.INVISIBILITY, 3).addSideEffect("aortic"));
        drug(IqFluids.ANTIDOTE, new FT_Drug().addSpecialEffect("clear_bad_effects"));
        drug(IqFluids.POISON, new FT_Drug().setThreshold(0.005F).setDissipationRate(5F).addSideEffect("seizure"));
        for (FluidType piss : new FluidType[] { IqFluids.PISS, IqFluids.PISS_HOT, IqFluids.PISS_DEPLETED })
            drug(piss, new FT_Drug().setThreshold(0.005F).setDissipationRate(10F).addSideEffect("wither").addSideEffect("confusion").addSideEffect("hunger"));
        drug(Fluids.ESTRADIOL, new FT_Drug().setThreshold(0.005F).setDissipationRate(60F).addEffect(HbmPotion.death, 0));
        drug(Fluids.XPJUICE, new FT_Drug().setConsumption(10).addSpecialEffect("hbmfluid.trait.drip_xp"));
        drug(Fluids.ENDERJUICE, new FT_Drug().setConsumption(10).addSpecialEffect("hbmfluid.trait.teleporting"));

        food("SALIENT", new FT_Consumable().setFood(0.15F, 0.1F));
        food("MUG", new FT_Consumable().setFood(0.2F, 0.1F));
        food("MUG_HOT", new FT_Consumable().setFood(0.2F, 0.1F));
        food("BLOOD", new FT_Consumable().setFood(0.3F, 0.1F).addEffect(MobEffects.STRENGTH, 0));
        food("BLOOD_HOT", new FT_Consumable().setFood(0.3F, 0.1F).addEffect(MobEffects.STRENGTH, 0));
        food("NUTRIENT_SLOP", new FT_Consumable().setFood(0.1F, 0.02F));
        food("TAMSLOP", new FT_Consumable().setFood(1.0F, 0.1F).addEffect(MobEffects.REGENERATION, 2));
        // IQ drinks neither CE nor F24 has (yet): picked up by name whenever something registers them
        food("SCUTTERBLOOD", new FT_Consumable().setFood(0.3F, 0.1F).addEffect(MobEffects.STRENGTH, 0));
        food("COFFEE", new FT_Consumable().addEffect(MobEffects.SPEED, 1));
        food("TEA", new FT_Consumable());
        food("HONEY", new FT_Consumable());
        food("MILK", new FT_Consumable().setClearsEffects(true));
        food("SMILK", new FT_Consumable().setClearsEffects(true).setFood(0.1F, 0.02F));
        food("EMILK", new FT_Consumable().setClearsEffects(true));
        food("CMILK", new FT_Consumable().setClearsEffects(true));
        food("CREAM", new FT_Consumable());

        // IQ's BloodBehavior table, minus the husk stat bonuses (F03)
        BloodstreamAPI.registerBehavior(Fluids.fromName("BLOOD_HOT"), new BloodBehavior.Steam(6));
        BloodstreamAPI.registerBehavior(Fluids.fromName("CURDLING_BLOOD"), new BloodBehavior.Steam(2));
        BloodstreamAPI.registerBehavior(Fluids.fromName("SATANS_BLOOD"), new BloodBehavior.Hellish());
        BloodstreamAPI.registerBehavior(Fluids.CONCRETE, new BloodBehavior.Concrete());
        BloodstreamAPI.registerBehavior(Fluids.HELIUM3, new BloodBehavior.Helium());
        BloodstreamAPI.registerBehavior(Fluids.HELIUM4, new BloodBehavior.Helium());
        BloodstreamAPI.registerBehavior(CAY, new BloodBehavior.Cay());
        BloodstreamAPI.registerBehavior(Fluids.NITROGLYCERIN, new BloodBehavior.Nitroglycerin());
    }

    private static void drug(FluidType t, FT_Drug d) {
        if (t == null || t == Fluids.NONE) return;
        d.type = t;
        t.addTraits(d);
    }

    private static void food(String name, FT_Consumable c) {
        FluidType t = Fluids.fromName(name);
        if (t != Fluids.NONE) t.addTraits(c);
    }

    /** IQ's MixerRecipes for the drugs; the AMBROSIA variants only once something registers those fluids */
    static void mixerRecipes() {
        FluidType ambrosia = Fluids.fromName("AMBROSIA");
        boolean amb = ambrosia != Fluids.NONE;

        MixerRecipe stim = new MixerRecipe(500, 30).setStack1(new FluidStack(Fluids.WATER, 500)).setSolid(new ComparableStack(Items.NETHER_WART, 4));
        MixerRecipes.register(STIMPAK, amb ? new MixerRecipe[] { stim,
                new MixerRecipe(500, 30).setStack1(new FluidStack(ambrosia, 500)).setSolid(new ComparableStack(Items.NETHER_WART, 4)) } : new MixerRecipe[] { stim });

        java.util.List<MixerRecipe> sup = new java.util.ArrayList<>();
        for (FluidType base : amb ? new FluidType[] { STIMPAK, ambrosia } : new FluidType[] { STIMPAK }) {
            sup.add(new MixerRecipe(500, 40).setStack1(new FluidStack(base, 400)).setSolid(new ComparableStack(ModItems.bottle_nuka)));
            sup.add(new MixerRecipe(500, 40).setStack1(new FluidStack(base, 400)).setSolid(new ComparableStack(ModItems.bottle_cherry)));
        }
        MixerRecipes.register(SUPER_STIMPAK, sup.toArray(new MixerRecipe[0]));

        MixerRecipe medx = new MixerRecipe(500, 30).setStack1(new FluidStack(Fluids.WATER, 500)).setStack2(new FluidStack(Fluids.UNSATURATEDS, 100)).setSolid(new OreDictStack(QUARTZ.dust(), 4));
        MixerRecipes.register(MEDX, amb ? new MixerRecipe[] { medx,
                new MixerRecipe(500, 30).setStack1(new FluidStack(ambrosia, 500)).setSolid(new OreDictStack(QUARTZ.dust(), 4)) } : new MixerRecipe[] { medx });

        MixerRecipe psycho = new MixerRecipe(500, 30).setStack1(new FluidStack(Fluids.WATER, 500)).setStack2(new FluidStack(Fluids.REFORMGAS, 200)).setSolid(new ComparableStack(ModItems.powder_fire, 4));
        MixerRecipes.register(PSYCHO, amb ? new MixerRecipe[] { psycho,
                new MixerRecipe(500, 30).setStack1(new FluidStack(ambrosia, 500)).setSolid(new ComparableStack(ModItems.powder_fire, 4)) } : new MixerRecipe[] { psycho });

        FluidType milk = Fluids.fromName("MILK");
        if (milk != Fluids.NONE) MixerRecipes.register(IqFluids.ANTIDOTE, new MixerRecipe(500, 30).setStack1(new FluidStack(milk, 400)).setSolid(new ComparableStack(Items.NETHER_WART, 4)));
        if (amb) {
            MixerRecipes.register(IqFluids.AWESOME, new MixerRecipe(1000, 100).setStack1(new FluidStack(ambrosia, 500)).setStack2(new FluidStack(MEDX, 500)).setSolid(new ComparableStack(ModItems.pellet_charged)));
            MixerRecipes.register(RADAWAY, new MixerRecipe(500, 50).setStack1(new FluidStack(Fluids.BLOOD, 500)).setStack2(new FluidStack(ambrosia, 100)).setSolid(new OreDictStack(com.hbm.inventory.OreDictManager.COAL.dust())));
            MixerRecipes.register(RADX, new MixerRecipe(500, 50).setStack1(new FluidStack(Fluids.WATER, 500)).setStack2(new FluidStack(ambrosia, 100)).setSolid(new OreDictStack(com.hbm.inventory.OreDictManager.F.dust())));
        }
        // IQ's Çay recipe (curdling blood + 64 tea leaves) needs IQ's tea_leaf item, which CE lacks: skipped until it exists
    }
}
