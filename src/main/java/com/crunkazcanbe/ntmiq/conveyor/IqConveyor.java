package com.crunkazcanbe.ntmiq.conveyor;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemConveyorWand.ConveyorType;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

import static com.hbm.inventory.OreDictManager.STEEL;

/**
 * F16 from NTM: IQ Edition (irislgtm / tam69 / the IQ team, GPL-3): Conveyor Combinator (crafts 2x2 / 3x3 item sets on
 * the belt) and Conveyor Spraypainter (paints items with IQ's inks, peroxide bleaches). Runs on CE's conveyor belts.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class IqConveyor {
    private IqConveyor() {}

    public static Block combinator, spraypainter;
    public static Item combinatorItem, spraypainterItem;
    public static SoundEvent spraypaint;

    /** preInit, after IqContent.create() (creative tab) */
    public static void preInit(FMLPreInitializationEvent e) {
        combinator = setup(new BlockConveyorCombinator("machine_conveyor_combinator"));
        spraypainter = setup(new BlockConveyorSpraypainter("machine_conveyor_spraypainter"));
        combinatorItem = new ItemBlock(combinator).setRegistryName(combinator.getRegistryName());
        spraypainterItem = new ItemBlock(spraypainter).setRegistryName(spraypainter.getRegistryName());
        ResourceLocation rl = new ResourceLocation(Tags.MODID, "block.spraypaint");
        spraypaint = new SoundEvent(rl).setRegistryName(rl);
        SerializableRecipe.additionalListeners.add(name -> {
            if (AssemblyMachineRecipes.class.getSimpleName().equals(name)) assembler();
        });
        if (e.getSide().isClient()) IqConveyorClient.bind();
    }

    private static Block setup(Block b) {
        ModBlocks.ALL_BLOCKS.remove(b);   // BlockDummyable adds itself to CE's list; we register it under ntmiq ourselves
        b.setTranslationKey(Tags.MODID + "." + b.getRegistryName().getPath());
        b.setHardness(5.0F).setResistance(10.0F).setCreativeTab(IqContent.TAB);
        return b;
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().registerAll(combinator, spraypainter);
        GameRegistry.registerTileEntity(TileEntityConveyorCombinator.class, new ResourceLocation(Tags.MODID, "conveyor_combinator"));
        GameRegistry.registerTileEntity(TileEntityConveyorSpraypainter.class, new ResourceLocation(Tags.MODID, "conveyor_spraypainter"));
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().registerAll(combinatorItem, spraypainterItem);
    }

    @SubscribeEvent
    public static void sounds(RegistryEvent.Register<SoundEvent> e) {
        e.getRegistry().register(spraypaint);
    }

    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> e) {
        // IQ CraftingManager
        e.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(combinator), " S ", "MCM", " S ",
                'S', STEEL.ingot(), 'M', ModItems.motor, 'C', new ItemStack(ModItems.conveyor_wand, 1, ConveyorType.REGULAR.ordinal()))
                .setRegistryName(Tags.MODID, "machine_conveyor_combinator"));
    }

    private static void assembler() {
        AssemblyMachineRecipes.INSTANCE.register(new GenericRecipe("ass.conveyor_spraypainter").setup(200, 100)
                .outputItems(new ItemStack(spraypainter, 1))
                .inputItems(new OreDictStack(STEEL.plate(), 6), new OreDictStack(STEEL.pipe(), 2), new ComparableStack(ModItems.motor, 2),
                        new ComparableStack(ModItems.conveyor_wand, 1, ConveyorType.REGULAR.ordinal())));
    }
}
