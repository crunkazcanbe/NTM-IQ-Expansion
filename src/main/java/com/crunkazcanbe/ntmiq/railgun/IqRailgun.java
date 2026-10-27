package com.crunkazcanbe.ntmiq.railgun;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

import static com.hbm.inventory.OreDictManager.*;

/**
 * F08 — Railgun turret "Louis" from NTM: IQ Edition (1.7.10, GPL-3; irislgtm, Alexmaster75 / tam69 and the IQ team),
 * rebuilt on HBM NTM CE 1.12.2. Registers the turret, the 6 sabots, the sabot entity, 2 IQ sounds and IQ's assembler recipes.
 */
@Mod.EventBusSubscriber(modid = "ntmiq")
public final class IqRailgun {
    private IqRailgun() {}

    public static Block turret;
    public static ItemAmmoRailgun ammo;
    public static SoundEvent nukeCharge, buttonIncorrect;

    /** call from the mod's preInit (after IqContent.create, which makes the creative tab) */
    public static void preInit(FMLPreInitializationEvent e) {
        turret = new BlockTurretRailgun();
        ammo = new ItemAmmoRailgun();
        nukeCharge = sound("block.nukeCharge");
        buttonIncorrect = sound("block.buttonIncorrect");
        SerializableRecipe.additionalListeners.add(name -> {
            if (AssemblyMachineRecipes.class.getSimpleName().equals(name)) recipes();
        });
        if (e.getSide().isClient()) RailgunClient.preInit();
    }

    private static SoundEvent sound(String name) {
        ResourceLocation rl = new ResourceLocation("ntmiq", name);
        return new SoundEvent(rl).setRegistryName(rl);
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(turret);
        GameRegistry.registerTileEntity(TileEntityTurretRailgun.class, new ResourceLocation("ntmiq", "tileentity_turret_railgun"));
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(new ItemBlock(turret).setRegistryName(turret.getRegistryName()));
        e.getRegistry().register(ammo);
    }

    @SubscribeEvent
    public static void entities(RegistryEvent.Register<EntityEntry> e) {
        e.getRegistry().register(EntityEntryBuilder.create().entity(EntityRailgunProjectile.class)
                .id(new ResourceLocation("ntmiq", "entity_railgun_sabot"), 100).name("ntmiq.railgun_sabot")
                .tracker(1000, 1, true).build());
    }

    @SubscribeEvent
    public static void sounds(RegistryEvent.Register<SoundEvent> e) {
        e.getRegistry().registerAll(nukeCharge, buttonIncorrect);
    }

    /** IQ uses stainless steel, which CE doesn't have: use it if any mod provides it, else steel. */
    private static String stainless(String shape) {
        String name = shape + "StainlessSteel";
        return OreDictionary.getOres(name, false).isEmpty() ? shape + "Steel" : name;
    }

    /** IQ's assembler recipes (AssemblyMachineRecipes, "ass.railgun*"); SAS3 cell -> 1000 mB SAS3 fluid input */
    private static void recipes() {
        AssemblyMachineRecipes r = AssemblyMachineRecipes.INSTANCE;
        r.register(new GenericRecipe("ass.railgun").setup(1_600, 100).outputItems(new ItemStack(turret, 1))
                .inputItems(new ComparableStack(ModItems.battery_pack, 1, EnumBatteryPack.BATTERY_LITHIUM),
                        new ComparableStack(ModItems.battery_pack, 1, EnumBatteryPack.CAPACITOR_BISMUTH),
                        new OreDictStack(stainless("ingot"), 64), new ComparableStack(ModBlocks.concrete, 16),
                        new ComparableStack(ModItems.motor_bismuth, 3), new ComparableStack(ModItems.circuit, 8, EnumCircuitType.ADVANCED),
                        new OreDictStack(WEAPONSTEEL.mechanism(), 20), new ComparableStack(ModBlocks.machine_radar, 1),
                        new ComparableStack(ModItems.crt_display, 2), new ComparableStack(ModItems.coil_copper, 16))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "arty"));

        String plate = stainless("plate");
        sabot(r, "ass.railguntungsten", ItemAmmoRailgun.TUNGSTEN, 200, plate, new OreDictStack(W.block(), 2));
        sabot(r, "ass.railgundu", ItemAmmoRailgun.DU, 200, plate, new OreDictStack(U238.block(), 1), new OreDictStack(W.block(), 1));
        r.register(new GenericRecipe("ass.railgunnuke").setup(800, 100).outputItems(new ItemStack(ammo, 1, ItemAmmoRailgun.NUKE))
                .inputItems(new OreDictStack(plate, 3), new ComparableStack(ModItems.warhead_nuclear, 1), new OreDictStack(W.block(), 2), new OreDictStack(W.bolt(), 16))
                .inputFluids(new FluidStack(Fluids.SAS3, 1_000)));
        sabot(r, "ass.railgundesh", ItemAmmoRailgun.DESH, 200, plate, new OreDictStack(DESH.block(), 2));
        sabot(r, "ass.railgunstarmetal", ItemAmmoRailgun.STARMETAL, 200, plate, new OreDictStack(STAR.block(), 2));
        sabot(r, "ass.railgunfluid", ItemAmmoRailgun.FLUID, 200, plate, new OreDictStack(W.block(), 2), new ComparableStack(ModItems.fluid_tank_lead_empty, 1));
    }

    /** plate x3 + extras + tungsten bolts x16 */
    private static void sabot(AssemblyMachineRecipes r, String name, int meta, int time, String plate, AStack... extra) {
        AStack[] in = new AStack[extra.length + 2];
        in[0] = new OreDictStack(plate, 3);
        System.arraycopy(extra, 0, in, 1, extra.length);
        in[in.length - 1] = new OreDictStack(W.bolt(), 16);
        r.register(new GenericRecipe(name).setup(time, 100).outputItems(new ItemStack(ammo, 1, meta)).inputItems(in));
    }
}
