package com.crunkazcanbe.ntmiq.magsep;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.modules.machine.ModuleMachineBase;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * 1 item + 1 fluid in, 6 items + 1 fluid out, with IQ's "parallels": the disc tier runs N recipes at once, scaling
 * inputs and outputs (IQ added this to its ModuleMachineBase; CE's base has no parallels, so it lives here).
 * Ported from NTM: IQ Edition (GPL-3).
 */
public class ModuleMachineMagneticSeparator extends ModuleMachineBase {
    public int parallels = 1;

    public ModuleMachineMagneticSeparator(int index, IEnergyHandlerMK2 battery, ItemStackHandler inv, int in, int outStart, FluidTankNTM inTank, FluidTankNTM outTank) {
        super(index, battery, inv);
        this.inputSlots = new int[] { in };
        this.outputSlots = new int[6];
        for (int i = 0; i < 6; i++) outputSlots[i] = outStart + i;
        this.inputTanks = new FluidTankNTM[] { inTank };
        this.outputTanks = new FluidTankNTM[] { outTank };
    }

    @Override
    public GenericRecipes getRecipeSet() {
        return MagneticSeparatorRecipes.INSTANCE;
    }

    @Override
    protected boolean hasInput(GenericRecipe recipe) {
        if (!super.hasInput(recipe)) return false;
        if (recipe.inputItem != null) for (int i = 0; i < Math.min(recipe.inputItem.length, inputSlots.length); i++)
            if (inventory.getStackInSlot(inputSlots[i]).getCount() < recipe.inputItem[i].stacksize * parallels) return false;
        if (recipe.inputFluid != null) for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++)
            if (inputTanks[i].getFill() < recipe.inputFluid[i].fill * parallels) return false;
        return true;
    }

    @Override
    protected boolean canFitOutput(GenericRecipe recipe) {
        if (recipe.outputItem != null) for (int i = 0; i < Math.min(recipe.outputItem.length, outputSlots.length); i++) {
            ItemStack stack = inventory.getStackInSlot(outputSlots[i]);
            if (stack.isEmpty()) continue;
            if (recipe.outputItem[i].possibleMultiOutput()) return false;
            ItemStack single = recipe.outputItem[i].getSingle();
            if (single == null || single.isEmpty() || stack.getItem() != single.getItem() || stack.getItemDamage() != single.getItemDamage()) return false;
            if (stack.getCount() + single.getCount() * parallels > stack.getMaxStackSize()) return false;
        }
        if (recipe.outputFluid != null) for (int i = 0; i < Math.min(recipe.outputFluid.length, outputTanks.length); i++)
            if (recipe.outputFluid[i].fill * parallels + outputTanks[i].getFill() > outputTanks[i].getMaxFill()) return false;
        return true;
    }

    @Override
    protected void consumeInput(GenericRecipe recipe) {
        for (int p = 0; p < parallels; p++) super.consumeInput(recipe);
    }

    @Override
    protected void produceItem(GenericRecipe recipe) {
        for (int p = 0; p < parallels; p++) super.produceItem(recipe);
    }
}
