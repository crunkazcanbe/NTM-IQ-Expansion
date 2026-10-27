package com.crunkazcanbe.ntmiq.magnetic;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;

/** Replaces IQ's NEI MagneticCrafterOverlayHandler: JEI's "+" on a crafting recipe fills the grid from inventory + magnetic crates. */
@JEIPlugin
public class MagneticJei implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(new IRecipeTransferHandler<ContainerMagneticCrafter>() {
            @Override public Class<ContainerMagneticCrafter> getContainerClass() { return ContainerMagneticCrafter.class; }

            @Override
            public IRecipeTransferError transferRecipe(ContainerMagneticCrafter container, IRecipeLayout layout, EntityPlayer player, boolean maxTransfer, boolean doTransfer) {
                if (!doTransfer) return null;
                ItemStack[][] items = new ItemStack[9][];
                for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> e : layout.getItemStacks().getGuiIngredients().entrySet()) {
                    IGuiIngredient<ItemStack> ing = e.getValue();
                    int cell = e.getKey() - 1;   // crafting category: slot 0 = output, 1..9 = grid
                    if (!ing.isInput() || cell < 0 || cell > 8) continue;
                    List<ItemStack> all = ing.getAllIngredients();
                    if (!all.isEmpty()) items[cell] = all.toArray(new ItemStack[0]);
                }
                MagneticPackets.NET.sendToServer(new MagneticPackets.RecipeMessage(items));
                return null;
            }
        }, VanillaRecipeCategoryUid.CRAFTING);
    }
}
