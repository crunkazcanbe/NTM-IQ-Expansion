package com.crunkazcanbe.ntmiq.pipeline;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.OreDictManager;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * F17 from NTM: IQ Edition (irislgtm / tam69 / the IQ team, GPL-3): Industrial Pipe Anchor (100 m pipelines) and the
 * Pipe Anchor Pressurizer (fluid in/out of the anchor, runs on compressed air). Built on CE's TileEntityPipelineBase.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class IqPipeline {
    private IqPipeline() {}

    public static Block anchor, pressurizer;
    public static Item anchorItem, pressurizerItem;

    /** preInit, after IqContent.create() (needs the creative tab) */
    public static void preInit(FMLPreInitializationEvent e) {
        anchor = setup(new BlockPipeAnchorIndustrial("pipe_anchor_industrial"));
        pressurizer = setup(new BlockPipeAnchorPressurizer("pipe_anchor_pressurizer"));
        anchorItem = new ItemBlock(anchor).setRegistryName(anchor.getRegistryName());
        pressurizerItem = new ItemBlock(pressurizer).setRegistryName(pressurizer.getRegistryName());
        if (e.getSide().isClient()) IqPipelineClient.bind();
    }

    private static Block setup(Block b) {
        ModBlocks.ALL_BLOCKS.remove(b);   // BlockDummyable adds itself to CE's list; we register it under ntmiq ourselves
        b.setTranslationKey(Tags.MODID + "." + b.getRegistryName().getPath());
        b.setHardness(5.0F).setResistance(10.0F).setCreativeTab(IqContent.TAB);
        return b;
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().registerAll(anchor, pressurizer);
        GameRegistry.registerTileEntity(TileEntityPipeAnchorIndustrial.class, new ResourceLocation(Tags.MODID, "pipe_anchor_industrial"));
        GameRegistry.registerTileEntity(TileEntityPipeAnchorPressurizer.class, new ResourceLocation(Tags.MODID, "pipe_anchor_pressurizer"));
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().registerAll(anchorItem, pressurizerItem);
    }

    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> e) {
        // IQ CraftingManager: 2 anchors from 2 steel shells over a cast steel plate
        e.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(anchor, 2), "S", "S", "P",
                'S', OreDictManager.STEEL.shell(), 'P', OreDictManager.STEEL.plateCast()).setRegistryName(Tags.MODID, "pipe_anchor_industrial"));
        // IQ made this in the assembly machine (2 motors, 3 steel pipes, 2 cast steel plates, 1 analog circuit); same parts at a crafting table here
        e.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(pressurizer), "PPP", "MCM", "T T",
                'P', OreDictManager.STEEL.pipe(), 'M', ModItems.motor, 'C', new ItemStack(ModItems.circuit, 1, EnumCircuitType.ANALOG.ordinal()),
                'T', OreDictManager.STEEL.plateCast()).setRegistryName(Tags.MODID, "pipe_anchor_pressurizer"));
    }
}
