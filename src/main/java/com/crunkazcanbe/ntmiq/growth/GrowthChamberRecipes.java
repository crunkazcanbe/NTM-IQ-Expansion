package com.crunkazcanbe.ntmiq.growth;

import com.crunkazcanbe.ntmiq.IqContent;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.PlantEnums.EnumFlowerPlantType;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import net.minecraft.item.ItemStack;

/**
 * IQ's growth chamber recipes (config/hbmRecipes/hbmGrowthChamber.json). Fluids CE lacks (SALT_WATER, BRINE, FERTILIZER,
 * COMPOST) are looked up by IQ's names, so they light up once the add-on's fluid port (IqFluids) registers them; until then
 * those recipes are skipped. Certus quartz recipes only exist with AE2 and use AE2's dust via the ore dictionary.
 * Flowers CE doesn't have (strawberry, mint, CD0, mustard willow from CD0) are skipped.
 */
public class GrowthChamberRecipes extends GenericRecipes<GenericRecipe> {
    public static final GrowthChamberRecipes INSTANCE = new GrowthChamberRecipes();

    @Override public int inputItemLimit() { return 1; }
    @Override public int inputFluidLimit() { return 1; }
    @Override public int outputItemLimit() { return 1; }
    @Override public int outputFluidLimit() { return 0; }
    @Override public String getFileName() { return "hbmGrowthChamber.json"; }
    @Override public GenericRecipe instantiateRecipe(String name) { return new GenericRecipe(name); }

    @Override
    public void registerDefaults() {
        ComparableStack shard = new ComparableStack(IqGrowth.saltShard);
        add("growth.salt.salt_water", 500, 200, shard, "SALT_WATER", 4_000, 0, new ItemStack(IqGrowth.saltCluster, 1));
        add("growth.salt.brine", 500, 200, shard, "BRINE", 1_000, 0, new ItemStack(IqGrowth.saltCluster, 3));
        add("growth.salt.water", 250, 100, shard, "WATER", 16_000, 0, new ItemStack(IqGrowth.saltBudLarge, 1));
        add("growth.budding_salt", 1000, 100, new ComparableStack(IqContent.CRYSTAL_BLOCK.get("salt"), 8), "BRINE", 16_000, 2, new ItemStack(IqGrowth.buddingSalt));

        if (IqGrowth.buddingCertus != null) {
            // IQ left these without setup() (0 ticks); given the salt timings so the chamber can run them
            OreDictStack dust = new OreDictStack("dustCertusQuartz");
            add("growth.certus_quartz.water", 500, 200, dust, "WATER", 8_000, 0, new ItemStack(IqGrowth.certusBudLarge));
            add("growth.certus_quartz.heavywater", 500, 200, dust, "HEAVYWATER", 8_000, 0, new ItemStack(IqGrowth.certusCluster));
            add("growth.budding_certus_quartz", 1000, 100, new ComparableStack(IqContent.CRYSTAL_BLOCK.get("certus"), 8), "HEAVYWATER", 16_000, 4, new ItemStack(IqGrowth.buddingCertus));
        }

        // plants (IQ: "weed" = CE hemp; IQ's non-hemp compost recipes really take 2000 mB fertilizer)
        plant("hemp", EnumFlowerPlantType.HEMP, "COMPOST");
        plant("nightshade", EnumFlowerPlantType.NIGHTSHADE, "FERTILIZER");
        plant("foxglove", EnumFlowerPlantType.FOXGLOVE, "FERTILIZER");
        plant("tabacco", EnumFlowerPlantType.TOBACCO, "FERTILIZER");
    }

    private void plant(String name, EnumFlowerPlantType type, String secondFluid) {
        ComparableStack in = new ComparableStack(ModBlocks.plant_flower, 1, type.ordinal());
        add("growth." + name + ".fertilizer", 100, 50, in, "FERTILIZER", 250, 0, new ItemStack(ModBlocks.plant_flower, 5, type.ordinal()));
        add("growth." + name + ".compost", 100, 50, in, secondFluid, 2_000, 0, new ItemStack(ModBlocks.plant_flower, 2, type.ordinal()));
    }

    private void add(String name, int duration, long power, AStack in, String fluid, int mb, int pressure, ItemStack out) {
        FluidType type = Fluids.fromName(fluid);
        if (type == null || type == Fluids.NONE) return;
        register(new GenericRecipe(name).setup(duration, power).inputItems(in)
                .inputFluids(new FluidStack(type, mb, pressure)).outputItems(out));
    }
}
