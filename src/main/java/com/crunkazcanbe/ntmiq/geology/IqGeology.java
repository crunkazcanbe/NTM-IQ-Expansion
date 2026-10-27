package com.crunkazcanbe.ntmiq.geology;

import java.io.File;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * F22 leftovers from NTM: IQ Edition (irislgtm / tam69 / the IQ team, GPL-3): cassiterite / rutile / cobaltite resource
 * stones, the soldering alloy ingot, and IQ's stone-layer worldgen (also for round 1's anthracite + flaming anthracite).
 * IQ removed overworld/nether cobalt ore and nether coal for these layers; we only ADD the layers, CE's ores stay.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class IqGeology {
    private IqGeology() {}

    public static Block stoneCassiterite, stoneRutile, stoneCobaltite;
    public static Item ingotSolderingAlloy;

    public static void preInit(FMLPreInitializationEvent e) {
        stoneCassiterite = stone("stone_resource_cassiterite");
        stoneRutile = stone("stone_resource_rutile");
        stoneCobaltite = stone("stone_resource_cobaltite");

        ingotSolderingAlloy = new Item().setRegistryName(new ResourceLocation(Tags.MODID, "ingot_soldering_alloy"))
                .setTranslationKey(Tags.MODID + ".ingot_soldering_alloy").setCreativeTab(IqContent.TAB);
        IqContent.ITEMS.add(ingotSolderingAlloy);

        Configuration cfg = new Configuration(new File(e.getModConfigurationDirectory(), "ntmiq-worldgen.cfg"));
        String c = "stone_layers";
        cfg.setCategoryComment(c, "NTM IQ Edition resource stone layers (big 3D-noise blobs between y 6 and 64). Existing chunks are not changed.");
        boolean all = cfg.getBoolean("enableAll", c, true, "Master switch: false = no IQ stone layers at all");
        boolean cassiterite = cfg.getBoolean("enableCassiterite", c, true, "Overworld cassiterite (tin) layers");
        boolean anthracite = cfg.getBoolean("enableAnthracite", c, true, "Overworld anthracite layers");
        boolean rutile = cfg.getBoolean("enableRutile", c, true, "Nether rutile layers");
        boolean flaming = cfg.getBoolean("enableFlamingAnthracite", c, true, "Nether flaming anthracite layers");
        boolean cobaltite = cfg.getBoolean("enableCobaltite", c, true, "Nether cobaltite layers");
        if (cfg.hasChanged()) cfg.save();

        if (all) {
            // numbers straight from IQ's MainRegistry (threshold: higher = rarer)
            if (cassiterite) OreLayers.add(stoneCassiterite, 0, Blocks.STONE, 0.04D, 0.25D, 230);
            if (anthracite) OreLayers.add(IqContent.stoneAnthracite, 0, Blocks.STONE, 0.04D, 0.25D, 240);
            if (rutile) OreLayers.add(stoneRutile, -1, Blocks.NETHERRACK, 0.03D, 0.15D, 300);
            if (flaming) OreLayers.add(IqContent.stoneFlamingAnthracite, -1, Blocks.NETHERRACK, 0.04D, 0.25D, 240);
            if (cobaltite) OreLayers.add(stoneCobaltite, -1, Blocks.NETHERRACK, 0.04D, 0.25D, 285);
            if (!OreLayers.LAYERS.isEmpty()) GameRegistry.registerWorldGenerator(new OreLayers(), 0);
        }
    }

    private static Block stone(String name) {
        Block b = new Block(Material.ROCK).setHardness(1.5F).setResistance(10F);
        b.setRegistryName(new ResourceLocation(Tags.MODID, name)).setTranslationKey(Tags.MODID + "." + name).setCreativeTab(IqContent.TAB);
        IqContent.BLOCKS.add(b);
        IqContent.ITEMS.add(new ItemBlock(b).setRegistryName(b.getRegistryName()));
        return b;
    }

    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> e) {
        // no "ore*" names: Industrial Upgrade in this pack builds recipes for every ore* entry (see IqRecipes)
        OreDictionary.registerOre("stoneCassiterite", stoneCassiterite);
        OreDictionary.registerOre("gemRutile", stoneRutile);
        OreDictionary.registerOre("stoneCobaltite", stoneCobaltite);
        OreDictionary.registerOre("ingotSolderingAlloy", ingotSolderingAlloy);

        GameRegistry.addSmelting(stoneCassiterite, new ItemStack(IqContent.ingotTin), 0.5F);   // IQ SmeltingRecipes
        // rutile's only use, as in IQ: chem plant gemRutile + chlorine -> TiCl4, electrolysed to titanium (fluids/IqChem)

        // IQ makes soldering alloy in the crucible from equal tin + lead plus flux (3n + 3n + 1n -> 18n); same ratio by hand
        e.getRegistry().register(new ShapelessOreRecipe(null, new ItemStack(ingotSolderingAlloy, 2), "ingotTin", "ingotLead", ModItems.powder_flux)
                .setRegistryName(Tags.MODID, "ingot_soldering_alloy"));
    }

    /** IQ BlockResourceStone drops: cobaltite -> 2..3+fortune cobalt fragments, (flaming) anthracite -> 1..2+fortune chunks */
    @SubscribeEvent
    public static void drops(BlockEvent.HarvestDropsEvent e) {
        if (e.isSilkTouching() || e.getWorld().isRemote) return;
        Block b = e.getState().getBlock();
        ItemStack drop;
        int fortune = e.getFortuneLevel();
        if (b == stoneCobaltite) drop = new ItemStack(ModItems.fragment_cobalt, 2 + e.getWorld().rand.nextInt(2 + fortune));
        else if (b == IqContent.stoneAnthracite) drop = new ItemStack(IqContent.chunkAnthracite, 1 + e.getWorld().rand.nextInt(2 + fortune));
        else if (b == IqContent.stoneFlamingAnthracite) drop = new ItemStack(IqContent.chunkFlamingAnthracite, 1 + e.getWorld().rand.nextInt(2 + fortune));
        else return;
        e.getDrops().clear();
        e.getDrops().add(drop);
        e.setDropChance(1.0F);
    }
}
