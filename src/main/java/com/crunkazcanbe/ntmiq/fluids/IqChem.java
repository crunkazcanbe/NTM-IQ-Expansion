package com.crunkazcanbe.ntmiq.fluids;

import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.FluidContainerRegistry.FluidContainer;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes.ElectrolysisRecipe;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.MixerRecipes.MixerRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ModItems;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import static com.crunkazcanbe.ntmiq.fluids.IqFluids.*;

/**
 * F24 chemistry from NTM: IQ Edition (irislgtm / tam69 / the IQ team, GPL-3): IQ's ChemicalPlantRecipes / MixerRecipes /
 * ElectrolyserFluidRecipes additions whose inputs exist in CE + this add-on. Left out until their items are ported:
 * bloodclot dyes, brimtumen asphalt, haematite, buurmium chain, brine (CE-Space fluid), sodium carbonate, drugs (F01), foam (F19).
 */
public final class IqChem {
    private IqChem() {}

    /** preInit */
    public static void register() {
        SerializableRecipe.additionalListeners.add(name -> {
            if (ChemicalPlantRecipes.class.getSimpleName().equals(name)) chemPlant();
            else if (MixerRecipes.class.getSimpleName().equals(name)) mixer();
            else if (ElectrolyserFluidRecipes.class.getSimpleName().equals(name)) electrolyser();
        });
        // IQ FluidContainerRegistry: the piss rag holds 100 mB
        FluidContainerRegistry.registerContainer(new FluidContainer(new ItemStack(ModItems.rag_piss), new ItemStack(ModItems.rag), PISS, 100));
    }

    private static void chemPlant() {
        ChemicalPlantRecipes r = ChemicalPlantRecipes.INSTANCE;
        // rutile (round 2's resource stone, ore-dicted gemRutile) + chlorine -> titanium tetrachloride; electrolysis makes titanium
        r.register(new GenericRecipe("chem.ticl4").setup(100, 100)
                .inputItems(new OreDictStack("gemRutile"))
                .inputFluids(new FluidStack(Fluids.CHLORINE, 1000))
                .outputFluids(new FluidStack(TICL4, 1000), new FluidStack(HEAVY_SAND_SLOP, 500)));
        r.register(new GenericRecipe("chem.sulfuricacidalt").setupNamed(20, 400).setIcon(ModItems.gas_full, SULFUR_DIOXIDE.getID())
                .inputFluids(new FluidStack(SULFUR_DIOXIDE, 1_000), new FluidStack(Fluids.OXYGEN, 5_000))
                .outputFluids(new FluidStack(Fluids.SULFURIC_ACID, 1_000)));
        r.register(new GenericRecipe("chem.rubber_vanadium").setup(100, 200)
                .inputItems(new OreDictStack("dustSulfur"), new OreDictStack("dustVanadium"))
                .inputFluids(new FluidStack(Fluids.UNSATURATEDS, 500))
                .outputItems(new ItemStack(ModItems.ingot_rubber, 4)));
        r.register(new GenericRecipe("chem.hydrapiss_fluid").setupNamed(250, 1_000).setIcon(ModItems.canister_full, Fluids.HYDRAZINE.getID())
                .inputFluids(new FluidStack(Fluids.NITRIC_ACID, 2000), new FluidStack(PISS, 250))
                .outputFluids(new FluidStack(Fluids.HYDRAZINE, 800)));
        r.register(new GenericRecipe("chem.nutrientslop").setupNamed(40, 100).setIcon(ModItems.fluid_barrel_full, NUTRIENT_SLOP.getID())
                .inputItems(new ComparableStack(Items.BREAD, 1))
                .inputFluids(new FluidStack(Fluids.WATER, 100))
                .outputItems(new ItemStack(ModItems.biomass, 4))
                .outputFluids(new FluidStack(NUTRIENT_SLOP, 10)));
        r.register(new GenericRecipe("chem.uncleslop").setupNamed(120, 200).setIcon(ModItems.fluid_barrel_full, TAMSLOP.getID())
                .inputFluids(new FluidStack(NUTRIENT_SLOP, 1_000), new FluidStack(Fluids.SALIENT, 500), new FluidStack(Fluids.BLOOD, 500))
                .outputFluids(new FluidStack(TAMSLOP, 1_000)));
        // IQ also returns 100 mB ammonia, which only exists with CE-Space
        r.register(new GenericRecipe("chem.pissreprocessing").setupNamed(300, 1000).setIcon(ModItems.fluid_icon, PISS.getID())
                .inputFluids(new FluidStack(PISS_DEPLETED, 1000))
                .outputFluids(new FluidStack(Fluids.WATER, 800), new FluidStack(PISS, 100)));
        // ponytail: stand-in until F05 ports bloodclot + gall (IQ's only base-ink source); 1 dye + water -> 1000 mB of the base inks (IQ has no brown source at all)
        String[] dyes = { "dyeWhite", "dyeYellow", "dyeBlue", "dyeGreen", "dyeRed", "dyeBlack", "dyeBrown" };
        FluidType[] inks = { INK_WHITE, INK_YELLOW, INK_BLUE, INK_GREEN, INK_RED, INK_BLACK, INK_BROWN };
        for (int i = 0; i < dyes.length; i++) {
            r.register(new GenericRecipe("chem.ink_" + dyes[i].substring(3).toLowerCase()).setup(100, 100)
                    .inputItems(new OreDictStack(dyes[i]))
                    .inputFluids(new FluidStack(Fluids.WATER, 1000))
                    .outputFluids(new FluidStack(inks[i], 1000)));
        }
    }

    private static void mixer() {
        mix(INK_ORANGE, INK_RED, INK_YELLOW);
        mix(INK_LIME, INK_GREEN, INK_WHITE);
        mix(INK_LIGHT_BLUE, INK_BLUE, INK_WHITE);
        mix(INK_CYAN, INK_BLUE, INK_GREEN);
        mix(INK_PURPLE, INK_BLUE, INK_RED);
        mix(INK_PINK, INK_RED, INK_WHITE);
        mix(INK_GRAY, INK_BLACK, INK_WHITE);
        mix(INK_LIGHT_GRAY, INK_GRAY, INK_WHITE);
        mix(INK_MAGENTA, INK_PURPLE, INK_PINK);

        MixerRecipes.register(SLAKED_LIME, new MixerRecipe(1000, 30).setStack1(new FluidStack(Fluids.WATER, 1000)).setSolid(new ComparableStack(ModItems.powder_limestone)));
        MixerRecipes.register(SALT_WATER, new MixerRecipe(1000, 15).setStack1(new FluidStack(Fluids.WATER, 1000)).setSolid(new OreDictStack("dustSalt")));
        MixerRecipes.register(FERTILIZER, new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.WATER, 1000)).setSolid(new ComparableStack(ModItems.powder_fertilizer)));
        MixerRecipes.register(COMPOST, new MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.BIOGAS, 2000)).setSolid(new ComparableStack(ModItems.biomass)));
        MixerRecipes.register(DIALYSATE, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.WATER, 500)).setSolid(new ComparableStack(ModItems.powder_calcium)));
        MixerRecipes.register(POISON, new MixerRecipe(500, 30).setStack1(new FluidStack(Fluids.WATER, 400)).setSolid(new ComparableStack(Items.SPIDER_EYE, 4)));
    }

    private static void mix(FluidType out, FluidType a, FluidType b) {
        MixerRecipes.register(out, new MixerRecipe(1_000, 20).setStack1(new FluidStack(a, 500)).setStack2(new FluidStack(b, 500)));
    }

    private static void electrolyser() {
        ElectrolyserFluidRecipes.recipes.put(TICL4, new ElectrolysisRecipe(1000, new FluidStack(Fluids.NONE, 0), new FluidStack(Fluids.CHLORINE, 500),
                new ItemStack(ModItems.powder_titanium, 4)));
        ElectrolyserFluidRecipes.recipes.put(MONAZITE_SLOP, new ElectrolysisRecipe(250, new FluidStack(Fluids.NONE, 0), new FluidStack(Fluids.CHLORINE, 250),
                new ItemStack(ModItems.powder_thorium, 1), new ItemStack(ModItems.powder_desh_mix, 1)));
    }
}
