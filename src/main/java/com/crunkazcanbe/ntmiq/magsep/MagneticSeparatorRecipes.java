package com.crunkazcanbe.ntmiq.magsep;

import com.crunkazcanbe.ntmiq.contagion.GenomeSample;
import com.crunkazcanbe.ntmiq.contagion.ItemGenomeVial;
import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.NbtComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import net.minecraft.item.ItemStack;

/**
 * IQ's magnetic separator recipes (config/hbmRecipes/hbmMagneticSeparator.json), including "magsep.genomeSample":
 * a blood vial spun with heparin breaks into genome samples (F02). The real work is in the tile entity, which picks
 * random sample keys off the disease in the vial; this entry is what NEI/JEI shows and what the recipe file keeps.
 */
public class MagneticSeparatorRecipes extends GenericRecipes<GenericRecipe> {
    public static final MagneticSeparatorRecipes INSTANCE = new MagneticSeparatorRecipes();

    @Override public int inputItemLimit() { return 2; }
    @Override public int inputFluidLimit() { return 1; }
    @Override public int outputItemLimit() { return 6; }
    @Override public int outputFluidLimit() { return 1; }
    @Override public String getFileName() { return "hbmMagneticSeparator.json"; }
    @Override public GenericRecipe instantiateRecipe(String name) { return new GenericRecipe(name); }

    @Override
    public void registerDefaults() {
        register(new GenericRecipe("magsep.iron").setup(200, 1_000)
                .inputItems(new ComparableStack(ModItems.powder_iron))
                .inputFluids(new FluidStack(Fluids.WATER, 250))
                .outputItems(new ItemStack(ModItems.powder_iron), new ItemStack(ModItems.dust))
                .outputFluids(new FluidStack(Fluids.SPENTSTEAM, 250))
                .setIconToFirstIngredient());

        register(new GenericRecipe("magsep.genomeSample").setup(TileEntityMachineMagneticSeparator.GENOME_DURATION, 1_000)
                .inputItems(new NbtComparableStack(bloodVial()))
                .inputFluids(new FluidStack(IqFluids.HEPARIN, TileEntityMachineMagneticSeparator.HEPARIN_PER_SAMPLE))
                .outputItems(
                        GenomeSample.make("severity", "01234567"),
                        GenomeSample.make("resistance", "89ABCDEF"),
                        GenomeSample.make("transmission", "01234567"),
                        GenomeSample.make("mutationRate", "0.05"),
                        GenomeSample.make("antigenMutability", "0.2"),
                        GenomeSample.make("incubation", "144000"))
                .setIconToFirstIngredient());
    }

    private static ItemStack bloodVial() {
        return ItemGenomeVial.filled(IqFluids.HUMAN_BLOOD, ItemGenomeVial.MAX_FLUID);
    }
}
