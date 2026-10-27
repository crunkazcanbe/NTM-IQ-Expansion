package com.crunkazcanbe.ntmiq.sludge;

import com.crunkazcanbe.ntmiq.IqContent;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

/**
 * IQ's SludgeProcessorRecipes (Mass Centrifuge), irislgtm / tam69 / IQ team, GPL-3. Fluids IQ adds (natcrete chain = F07
 * Czechoslowakia/Space, sand slop + SO2 + metavanadate = F24) are looked up by name: a recipe only loads when every fluid
 * it needs exists, so concrete recycling works on plain CE and the others appear once their fluids are registered.
 */
public class SludgeProcessorRecipes extends GenericRecipes<GenericRecipe> {
    public static final SludgeProcessorRecipes INSTANCE = new SludgeProcessorRecipes();

    @Override public int inputItemLimit() { return 1; }
    @Override public int inputFluidLimit() { return 1; }
    @Override public int outputItemLimit() { return 4; }
    @Override public int outputFluidLimit() { return 4; }
    @Override public String getFileName() { return "hbmSludgeProcessor.json"; }
    @Override public GenericRecipe instantiateRecipe(String name) { return new GenericRecipe(name); }

    private static FluidStack f(String name, int amount) {
        FluidType t = Fluids.fromName(name);
        return t == Fluids.NONE ? null : new FluidStack(t, amount);
    }

    private static boolean all(FluidStack... s) {
        for (FluidStack x : s) if (x == null) return false;
        return true;
    }

    @Override
    public void registerDefaults() {
        // natcrete = F07 (Czechoslowakia planet, needs CE-Space): only if something registers those fluids
        FluidStack natcrete = f("NATCRETE", 2000), conc = f("CONCRETE", 1000), buur = f("BUURCRETE", 500), scrap = f("SCRAPCRETE", 500), lava = f("LAVA", 500);
        if (all(natcrete, conc, buur, scrap, lava))
            register(new GenericRecipe("sludge.natcrete").setup(100, 10_000).setNameWrapper("sludge.natcrete")
                    .inputFluids(natcrete).outputFluids(conc, buur, scrap, lava).setIconToFirstIngredient());

        // IQ left "sludge.buurmium" commented out (needs Space buurmium) - same here

        register(new GenericRecipe("sludge.concrete_recycling").setup(40, 5_000).setNameWrapper("sludge.concrete_recycling")
                .inputFluids(new FluidStack(Fluids.CONCRETE, 16000))
                .outputItems(new ItemStack(ModItems.powder_cement))
                .outputFluids(new FluidStack(Fluids.WATER, 2000)));

        Block ironCrystal = IqContent.CRYSTAL_BLOCK.get("iron");   // IQ's block_crystal:IRON = our round-1 crystal block
        FluidStack acid = f("SULFURIC_ACID", 1000), so2 = f("SULFUR_DIOXIDE", 1000), vanadate = f("SODIUM_METAVANADATE", 100);
        if (ironCrystal != null && all(acid, so2, vanadate))
            register(new GenericRecipe("sludge.iron_crystal").setup(100, 20_000).setNameWrapper("sludge.iron_crystal")
                    .inputItems(new ComparableStack(ironCrystal, 1))
                    .inputFluids(acid)
                    .outputItems(new ItemStack(ModItems.powder_iron, 32))
                    .outputFluids(so2, vanadate));

        FluidStack slop = f("HEAVY_SAND_SLOP", 1000), monazite = f("MONAZITE_SLOP", 500), cl = f("CHLORINE", 500);
        if (all(slop, monazite, cl))
            register(new GenericRecipe("sludge.sand_slop_processing").setup(50, 5_000).setNameWrapper("sludge.sand_slop_processing")
                    .inputFluids(slop).outputItems(new ItemStack(ModItems.powder_zirconium, 2)).outputFluids(monazite, cl)
                    .setIconToFirstIngredient());
    }
}
