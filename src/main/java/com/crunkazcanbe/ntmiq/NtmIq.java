package com.crunkazcanbe.ntmiq;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * NTM IQ Expansion — the content NTM: IQ Edition (1.7.10, GPL-3, irislgtm / tam69 / Siepert123 / NotMega and the IQ
 * team) adds on top of HBM's Nuclear Tech, ported to 1.12.2 as an add-on for HBM NTM Community Edition.
 */
@Mod(modid = Tags.MODID, name = Tags.MODNAME, version = Tags.VERSION, dependencies = "required-after:hbm")
@Mod.EventBusSubscriber(modid = Tags.MODID)
public class NtmIq {
    public static final Logger LOG = LogManager.getLogger(Tags.MODNAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        IqContent.create();
        MinecraftForge.EVENT_BUS.register(new ItemIndustrialPipette.Handler());
        MinecraftForge.EVENT_BUS.register(new IqRecipes.RawOreDrops());
        LOG.info("NTM IQ Expansion: {} blocks, {} items", IqContent.BLOCKS.size(), IqContent.ITEMS.size());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        IqRecipes.oreDict();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        IqRecipes.smelting();        // after CE has filled the ore dictionary with its ingots
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        for (Block b : IqContent.BLOCKS) e.getRegistry().register(b);
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        for (Item i : IqContent.ITEMS) e.getRegistry().register(i);
    }

    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<net.minecraft.item.crafting.IRecipe> e) {
        IqRecipes.crafting(e.getRegistry());
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (Item i : IqContent.ITEMS) {
            if (i == IqContent.industrialPipette) continue;   // CE's baked-item system draws it (hbm:items/industrial_pipette)
            ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        }
    }
}
