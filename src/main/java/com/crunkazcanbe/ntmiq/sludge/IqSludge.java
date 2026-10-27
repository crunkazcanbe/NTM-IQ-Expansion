package com.crunkazcanbe.ntmiq.sludge;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import static com.hbm.inventory.OreDictManager.ANY_RESISTANTALLOY;
import static com.hbm.inventory.OreDictManager.STEEL;

/**
 * F12 from NTM: IQ Edition (irislgtm / tam69 / the IQ team, GPL-3): the Mass Centrifuge (sludge processor), a 5x5
 * GenericRecipe machine that splits heavy sludges. Recipes live in config/hbmRecipes/hbmSludgeProcessor.json like CE's.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class IqSludge {
    private IqSludge() {}

    public static Block block;
    public static Item item;

    /** preInit, after IqContent.create() (creative tab) */
    public static void preInit(FMLPreInitializationEvent e) {
        block = new BlockSludgeProcessor("sludge_processor");
        ModBlocks.ALL_BLOCKS.remove(block);   // BlockDummyable self-registers into CE's list; we register it under ntmiq
        block.setTranslationKey(Tags.MODID + ".sludge_processor");
        block.setHardness(5.0F).setResistance(10.0F).setCreativeTab(IqContent.TAB);
        item = new ItemBlock(block).setRegistryName(block.getRegistryName());
        // CE's postInit runs registerAllHandlers() + initialize() over this list, so adding here is enough
        SerializableRecipe.recipeHandlers.add(SludgeProcessorRecipes.INSTANCE);
        SerializableRecipe.additionalListeners.add(name -> {
            if (AssemblyMachineRecipes.class.getSimpleName().equals(name)) assemblerRecipe();
        });
        if (e.getSide().isClient()) SludgeClient.bind();
    }

    /** IQ: "ass.mass_centrifige" */
    private static void assemblerRecipe() {
        AssemblyMachineRecipes.INSTANCE.register(new GenericRecipe("ass.mass_centrifige").setup(1_200, 100).outputItems(new ItemStack(block, 1))
                .inputItems(new ComparableStack(ModItems.centrifuge_element, 8), new OreDictStack(STEEL.plateWelded(), 8),
                        new OreDictStack(ANY_RESISTANTALLOY.plateCast(), 4), new OreDictStack(STEEL.shell(), 3), new OreDictStack(STEEL.pipe(), 16)));
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(block);
        GameRegistry.registerTileEntity(TileEntitySludgeProcessor.class, new ResourceLocation(Tags.MODID, "tileentity_sludge_processor"));
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(item);
    }
}
