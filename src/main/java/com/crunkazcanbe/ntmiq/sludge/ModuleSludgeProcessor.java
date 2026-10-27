package com.crunkazcanbe.ntmiq.sludge;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.modules.machine.ModuleMachineBase;
import net.minecraftforge.items.ItemStackHandler;

/** IQ's ModuleMachineSludgeProcessor: 1 item + 1 fluid in, 4 items + 4 fluids out. */
public class ModuleSludgeProcessor extends ModuleMachineBase {

    public ModuleSludgeProcessor(int index, IEnergyHandlerMK2 battery, ItemStackHandler slots) {
        super(index, battery, slots);
        this.inputSlots = new int[1];
        this.outputSlots = new int[4];
        this.inputTanks = new FluidTankNTM[1];
        this.outputTanks = new FluidTankNTM[4];
    }

    @Override
    public GenericRecipes getRecipeSet() {
        return SludgeProcessorRecipes.INSTANCE;
    }

    public ModuleSludgeProcessor itemInput(int start) { inputSlots[0] = start; return this; }
    public ModuleSludgeProcessor itemOutput(int start) { for (int i = 0; i < 4; i++) outputSlots[i] = start + i; return this; }
    public ModuleSludgeProcessor fluidInput(FluidTankNTM a) { inputTanks[0] = a; return this; }
    public ModuleSludgeProcessor fluidOutput(FluidTankNTM a, FluidTankNTM b, FluidTankNTM c, FluidTankNTM d) {
        outputTanks[0] = a; outputTanks[1] = b; outputTanks[2] = c; outputTanks[3] = d; return this;
    }
}
