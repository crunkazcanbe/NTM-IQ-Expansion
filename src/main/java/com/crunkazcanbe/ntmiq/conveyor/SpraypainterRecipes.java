package com.crunkazcanbe.ntmiq.conveyor;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * IQ's SpraypainterRecipes: dye a block with an ink, bleach it back with hydrogen peroxide. 5 mB per item.
 * IQ's platemetal pair is left out (CE has no platemetal); sheetmetal uses this add-on's F25 blocks.
 */
public final class SpraypainterRecipes {
    private SpraypainterRecipes() {}

    public static final int AMOUNT = 5;

    /** {input AStack, FluidType, output ItemStack} */
    public static final List<Object[]> RECIPES = new ArrayList<>();

    private static void init() {
        if (!RECIPES.isEmpty()) return;
        FluidType[] inks = IqFluids.inks();
        for (int c = 0; c < 16; c++) {
            pair(Blocks.GLASS, 0, Blocks.STAINED_GLASS, c, inks[c]);
            pair(Blocks.GLASS_PANE, 0, Blocks.STAINED_GLASS_PANE, c, inks[c]);
            pair(Blocks.HARDENED_CLAY, 0, Blocks.STAINED_HARDENED_CLAY, c, inks[c]);
            pair(ModBlocks.concrete_smooth, 0, ModBlocks.concrete_colored, c, inks[c]);
            pair(IqContent.sheetmetal, 0, IqContent.SHEET_COLORED.get(IqContent.COLORS[c]), 0, inks[c]);
            if (c != 0) {
                pair(Blocks.WOOL, 0, Blocks.WOOL, c, inks[c]);
                pair(Blocks.CARPET, 0, Blocks.CARPET, c, inks[c]);
            }
        }
    }

    private static void pair(Block base, int baseMeta, Block dyed, int dyedMeta, FluidType ink) {
        RECIPES.add(new Object[] { new ComparableStack(base, 1, baseMeta), ink, new ItemStack(dyed, 1, dyedMeta) });
        RECIPES.add(new Object[] { new ComparableStack(dyed, 1, dyedMeta), Fluids.PEROXIDE, new ItemStack(base, 1, baseMeta) });
    }

    /** @return the painted stack (count 1) or EMPTY */
    public static ItemStack getOutput(ItemStack input, FluidType fluid) {
        init();
        if (input.isEmpty() || fluid == null || fluid == Fluids.NONE) return ItemStack.EMPTY;
        for (Object[] recipe : RECIPES) {
            if (recipe[1] == fluid && ((AStack) recipe[0]).matchesRecipe(input, true)) return ((ItemStack) recipe[2]).copy();
        }
        return ItemStack.EMPTY;
    }
}
