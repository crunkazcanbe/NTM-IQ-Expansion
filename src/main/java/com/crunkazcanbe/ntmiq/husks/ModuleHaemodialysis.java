package com.crunkazcanbe.ntmiq.husks;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.modules.machine.ModuleMachineBase;
import net.minecraftforge.items.ItemStackHandler;

/** IQ's ModuleMachineHaemodialysis: 3 fluids in, 2 out, no items. */
public class ModuleHaemodialysis extends ModuleMachineBase {

    public ModuleHaemodialysis(int index, IEnergyHandlerMK2 battery, ItemStackHandler slots, FluidTankNTM[] in, FluidTankNTM[] out) {
        super(index, battery, slots);
        inputSlots = new int[0];
        outputSlots = new int[0];
        inputTanks = in;
        outputTanks = out;
    }

    @Override public GenericRecipes getRecipeSet() { return HaemodialysisRecipes.INSTANCE; }
}
