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

    public NtmIq() {
        com.crunkazcanbe.ntmiq.fluids.IqFluids.register();       // F24: must hook CE's Fluids.init(), which runs in CE's preInit
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        IqContent.create();
        com.crunkazcanbe.ntmiq.geology.IqGeology.preInit(e);     // F22: resource stone layers + soldering alloy
        com.crunkazcanbe.ntmiq.pipeline.IqPipeline.preInit(e);   // F17: industrial pipe anchor + pressurizer
        com.crunkazcanbe.ntmiq.railgun.IqRailgun.preInit(e);     // F08: railgun turret "Louis" + sabots
        com.crunkazcanbe.ntmiq.conveyor.IqConveyor.preInit(e);   // F16: conveyor combinator + spraypainter
        com.crunkazcanbe.ntmiq.fluids.IqChem.register();         // F24: IQ chem plant / mixer / electrolyser recipes
        com.crunkazcanbe.ntmiq.drones.IqDrones.preInit(e);       // F09: quarry / spelunking / demolisher drones
        com.crunkazcanbe.ntmiq.growth.IqGrowth.preInit(e);       // F13: growth chamber, budding salt/certus, salt geodes
        com.crunkazcanbe.ntmiq.magsep.IqMagSep.preInit(e);       // F11: magnetic separator + magnetic discs
        com.crunkazcanbe.ntmiq.blood.IqBlood.preInit(e);         // F01: bloodstream capability, drug fluids, syringes, auto drip, blood monitor
        com.crunkazcanbe.ntmiq.contagion.IqContagion.preInit(e); // F02: diseases, genomes, symptoms, Dial-A-Dose, contagion gloves (needs F01)
        com.crunkazcanbe.ntmiq.contagion.ContagionLoot.register();
        com.crunkazcanbe.ntmiq.sludge.IqSludge.preInit(e);       // F12: mass centrifuge (sludge processor)
        com.crunkazcanbe.ntmiq.magnetic.IqMagnetic.preInit(e);   // F10: magnetic strip / crafter / stocker
        com.crunkazcanbe.ntmiq.foam.IqFoam.preInit(e);           // F19: construction foam + quikrete
        com.crunkazcanbe.ntmiq.detail.IqDetail.preInit();        // F20: detail decals
        com.crunkazcanbe.ntmiq.husks.IqHusks.preInit(e);         // F03: human parts, cloner, husks, neuralysers, DIA-1000 (needs F01)
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
