package com.crunkazcanbe.ntmiq.husks;

import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;

/**
 * IQ's HaemodialysisRecipes (NTM: IQ Edition, GPL-3): the DIA-1000 cleans The Nest's bloods with heparin + dialysate.
 * Satan's Blood (artery pump) -> curdling blood + blood vapors -> lung fuel; curdling blood -> wormwood + morningstar's fire.
 * Editable in config/hbmRecipes/hbmHaemodialysis.json like CE's own machines.
 */
public class HaemodialysisRecipes extends GenericRecipes<GenericRecipe> {
    public static final HaemodialysisRecipes INSTANCE = new HaemodialysisRecipes();

    @Override public int inputItemLimit() { return 0; }
    @Override public int inputFluidLimit() { return 3; }
    @Override public int outputItemLimit() { return 0; }
    @Override public int outputFluidLimit() { return 2; }
    @Override public String getFileName() { return "hbmHaemodialysis.json"; }
    @Override public GenericRecipe instantiateRecipe(String name) { return new GenericRecipe(name); }

    @Override
    public void registerDefaults() {
        add("haemodialysis.martyr", new FluidStack(IqFluids.BLOOD_OF_THE_MARTYR, 1_000), new FluidStack(IqFluids.HAEMOGLOBIN, 3_000), new FluidStack(IqFluids.AMBROSIA, 2_000));
        add("haemodialysis.curdling", new FluidStack(IqFluids.CURDLING_BLOOD, 1_000), new FluidStack(IqFluids.WORMWOOD, 3_000), new FluidStack(IqFluids.MORNINGSTARS_FIRE, 1_000));
        add("haemodialysis.satansblood", new FluidStack(IqFluids.SATANS_BLOOD, 1_000), new FluidStack(IqFluids.CURDLING_BLOOD, 1_000), new FluidStack(IqFluids.BLOOD_VAPORS, 100));
        add("haemodialysis.lungfuel", new FluidStack(IqFluids.BLOOD_VAPORS, 1_000), new FluidStack(IqFluids.LUNG_FUEL, 1_000), new FluidStack(IqFluids.HAEMOGLOBIN, 500));
    }

    private void add(String name, FluidStack blood, FluidStack outA, FluidStack outB) {
        register(new GenericRecipe(name).setup(200, 1_000).setNameWrapper(name)
                .inputFluids(blood, new FluidStack(IqFluids.HEPARIN, 250), new FluidStack(IqFluids.DIALYSATE, 250))
                .outputFluids(outA, outB));
    }
}
