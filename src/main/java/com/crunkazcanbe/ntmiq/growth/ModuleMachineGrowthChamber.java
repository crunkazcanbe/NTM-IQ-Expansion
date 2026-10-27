package com.crunkazcanbe.ntmiq.growth;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.modules.machine.ModuleMachineBase;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Random;

/** 1 item + 1 fluid in, 1 item out; each cycle takes a random 1-2x the recipe time. Ported from NTM: IQ Edition (GPL-3). */
public class ModuleMachineGrowthChamber extends ModuleMachineBase {
    private static final Random RAND = new Random();

    public ModuleMachineGrowthChamber(int index, IEnergyHandlerMK2 battery, ItemStackHandler inv, int in, int out, FluidTankNTM tank) {
        super(index, battery, inv);
        this.inputSlots = new int[] { in };
        this.outputSlots = new int[] { out };
        this.inputTanks = new FluidTankNTM[] { tank };
        this.outputTanks = new FluidTankNTM[0];
    }

    @Override
    public void process(GenericRecipe recipe, double speed, double power) {
        this.battery.setPower(this.battery.getPower() - (power == 1 ? recipe.power : (long) (recipe.power * power)));
        int d = Math.max(1, recipe.duration);
        this.progress += Math.min(speed / (d + RAND.nextInt(d)), 1D);

        if (this.progress >= 1D) {
            consumeInput(recipe);
            produceItem(recipe);
            if (this.canProcess(recipe, speed, power)) this.progress -= 1D;
            else this.progress = 0D;
        }
    }

    @Override
    public GenericRecipes getRecipeSet() {
        return GrowthChamberRecipes.INSTANCE;
    }
}
