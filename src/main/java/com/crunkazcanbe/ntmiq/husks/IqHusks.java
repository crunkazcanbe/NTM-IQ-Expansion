package com.crunkazcanbe.ntmiq.husks;

import java.io.File;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.Tags;
import com.crunkazcanbe.ntmiq.blood.BloodBehavior;
import com.crunkazcanbe.ntmiq.blood.BloodstreamAPI;
import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.crunkazcanbe.ntmiq.husks.ItemHumanPart.Part;
import com.crunkazcanbe.ntmiq.husks.ItemHumanPart.Stat;
import com.crunkazcanbe.ntmiq.nest.IqNest;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.LiquefactionRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

import static com.hbm.inventory.OreDictManager.ANY_CONCRETE;
import static com.hbm.inventory.OreDictManager.RUBBER;
import static com.hbm.inventory.OreDictManager.CU;
import static com.hbm.inventory.OreDictManager.STEEL;

/**
 * F03 "Husks / cloner / neuralyser" from NTM: IQ Edition (1.7.10, GPL-3; irislgtm / tam69 / iamnotmega and the IQ team),
 * plus the DIA-1000 haemodialysis machine that cleans The Nest's bloods.
 * <ul>
 * <li>Human parts (head/torso/arms/legs/heart) carry five stats and maybe a trait; zombies drop them.</li>
 * <li>Cloner: medical syringe with a blood sample + one of each part + a blood fluid -> a husk of the sample's owner.</li>
 * <li>Neuralyser: link a husk, then swap bodies with it. Dead Man's Neuralyser (chestplate mod): wake up in it on death.</li>
 * </ul>
 * Not ported: IQ's Robotic Head / Cybernetic Head husk automation (needs IQ's ROR radio net) and the glitch shader.
 * Config: config/ntmiq-husks.cfg ("bodySwap=false" disables neuralyser + death swaps; cloner and husks still work).
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class IqHusks {
    private IqHusks() {}

    static boolean bodySwap, deathSwap;
    static float partDropChance;

    public static Item humanPart, neuralyser, deadmansNeuralyser;
    public static Block cloner, haemodialysis;
    static Item clonerItem, haemodialysisItem;

    /** preInit, after IqContent.create() and IqBlood.preInit() */
    public static void preInit(FMLPreInitializationEvent e) {
        Configuration cfg = new Configuration(new File(Loader.instance().getConfigDir(), "ntmiq-husks.cfg"));
        String c = "husks";
        bodySwap = cfg.getBoolean("bodySwap", c, true, "Neuralyser body swaps with husks. Off = husks are just clones you can't enter");
        deathSwap = cfg.getBoolean("deathSwap", c, true, "Dead Man's Neuralyser: on death, wake up in the linked husk (needs bodySwap)");
        partDropChance = cfg.getFloat("partDropChance", c, 0.02F, 0F, 1F, "Chance a zombie killed by a player drops a human part");
        if (cfg.hasChanged()) cfg.save();

        humanPart = item(new ItemHumanPart(), "human_part");
        neuralyser = item(new ItemNeuralyser(), "neuralyser");
        deadmansNeuralyser = new ItemNeuralyser.DeadMans().setTranslationKey("deadmans_neuralyser").setCreativeTab(IqContent.TAB);   // CE baked item: hbm:items/deadmans_neuralyser

        cloner = machine(new BlockCloner("machine_cloner"));
        haemodialysis = machine(new BlockHaemodialysis("machine_haemodialysis"));
        clonerItem = new ItemBlock(cloner).setRegistryName(cloner.getRegistryName());
        haemodialysisItem = new ItemBlock(haemodialysis).setRegistryName(haemodialysis.getRegistryName());

        SerializableRecipe.recipeHandlers.add(HaemodialysisRecipes.INSTANCE);
        SerializableRecipe.additionalListeners.add(name -> {
            if (AssemblyMachineRecipes.class.getSimpleName().equals(name)) assemblerRecipes();
            else if (LiquefactionRecipes.class.getSimpleName().equals(name)) liquefaction();
        });

        bloodStats();
        MinecraftForge.EVENT_BUS.register(new HuskBody.Events());
        MinecraftForge.EVENT_BUS.register(new HuskSwap());
        if (e.getSide().isClient()) HusksClient.bind();
    }

    private static Item item(Item i, String name) {
        return i.setRegistryName(new ResourceLocation(Tags.MODID, name)).setTranslationKey(Tags.MODID + "." + name).setCreativeTab(IqContent.TAB);
    }

    private static Block machine(Block b) {
        ModBlocks.ALL_BLOCKS.remove(b);   // BlockDummyable self-registers into CE's list; we register it under ntmiq
        return b.setTranslationKey(Tags.MODID + "." + b.getRegistryName().getPath()).setHardness(5F).setResistance(10F).setCreativeTab(IqContent.TAB);
    }

    /** IQ BloodBehavior.registerAll's StatBonus half: a husk grown with these as blood gets a stat bonus */
    private static void bloodStats() {
        stat(IqFluids.MEDX, Stat.VITALITY, 60);
        stat(IqFluids.STIMPAK, Stat.STRENGTH, 60);
        stat(IqFluids.SUPER_STIMPAK, Stat.STRENGTH, 90);
        stat(IqFluids.PSYCHO, Stat.AGILITY, 80);
        stat(IqFluids.RADAWAY, Stat.RECOVERY, 60);
        stat(Fluids.BLOOD, Stat.AGILITY, 40);
        stat(Fluids.fromName("SCUTTERBLOOD"), Stat.TOUGHNESS, 60);   // NONE on CE builds without it: ignored by registerBehavior
    }

    private static void stat(com.hbm.inventory.fluid.FluidType blood, Stat stat, int amount) {
        BloodstreamAPI.registerBehavior(blood, new BloodBehavior() {
            @Override public void applyStats(int[] totals) { totals[stat.ordinal()] += amount; }
        });
    }

    /** IQ AssemblyMachineRecipes "ass.cloner" / "ass.haemodialysis" (stainless steel + pharmaceutical chips don't exist in CE: steel / advanced chips) */
    private static void assemblerRecipes() {
        AssemblyMachineRecipes.INSTANCE.register(new GenericRecipe("ass.haemodialysis").setup(300, 100).outputItems(new ItemStack(haemodialysis))
                .inputItems(new ComparableStack(ModItems.circuit, 3, EnumCircuitType.ANALOG.ordinal()), new OreDictStack(STEEL.shell(), 4),
                        new OreDictStack(STEEL.plateCast(), 2), new OreDictStack(ANY_CONCRETE.any(), 16), new OreDictStack(CU.pipe(), 3)));
        AssemblyMachineRecipes.INSTANCE.register(new GenericRecipe("ass.cloner").setup(600, 100).outputItems(new ItemStack(cloner))
                .inputItems(new ComparableStack(ModItems.circuit, 12, EnumCircuitType.ADVANCED.ordinal()), new OreDictStack(STEEL.plateWelded(), 3),
                        new OreDictStack(STEEL.ingot(), 24), new OreDictStack(RUBBER.pipe(), 8), new ComparableStack(ModItems.motor_desh, 4)));
    }

    /** IQ LiquefactionRecipes: heparin from netherglyphid eggs and meat (the DIA-1000 needs it) */
    private static void liquefaction() {
        LiquefactionRecipes.recipes.put(new ComparableStack(IqNest.eggNether), new FluidStack(100, IqFluids.HEPARIN));
        LiquefactionRecipes.recipes.put(new ComparableStack(IqNest.meatNether), new FluidStack(10, IqFluids.HEPARIN));
    }

    // ---------------------------------------------------------------- registries

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().registerAll(cloner, haemodialysis);
        GameRegistry.registerTileEntity(TileEntityCloner.class, new ResourceLocation(Tags.MODID, "tileentity_cloner"));
        GameRegistry.registerTileEntity(TileEntityHaemodialysis.class, new ResourceLocation(Tags.MODID, "tileentity_haemodialysis"));
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().registerAll(humanPart, neuralyser, deadmansNeuralyser, clonerItem, haemodialysisItem);
    }

    /** mod-local entity id 700 (F03 owns 700-799) */
    @SubscribeEvent
    public static void entities(RegistryEvent.Register<EntityEntry> e) {
        e.getRegistry().register(EntityEntryBuilder.create().entity(EntityHusk.class).id(new ResourceLocation(Tags.MODID, "husk"), 700)
                .name(Tags.MODID + ".husk").tracker(80, 3, true).egg(0xC9A07E, 0x3B2A1E).build());
    }

    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> e) {
        e.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(neuralyser), " G ", "CRC", " S ",
                'G', "blockGlass", 'C', new ItemStack(ModItems.circuit, 1, EnumCircuitType.ADVANCED.ordinal()), 'R', Items.ENDER_EYE,
                'S', STEEL.plate()).setRegistryName(Tags.MODID, "neuralyser"));
        e.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(deadmansNeuralyser), "N", "T",
                'N', neuralyser, 'T', Items.TOTEM_OF_UNDYING).setRegistryName(Tags.MODID, "deadmans_neuralyser"));
    }

    /** IQ puts parts in bone piles / nether hive loot (structures not ported); here zombies killed by players drop them */
    @SubscribeEvent
    public static void drops(LivingDropsEvent e) {
        EntityLivingBase ent = e.getEntityLiving();
        if (ent.world.isRemote || !(ent instanceof EntityZombie) || !e.isRecentlyHit() || ent.getRNG().nextFloat() >= partDropChance) return;
        Part p = Part.values()[ent.getRNG().nextInt(Part.values().length)];
        e.getDrops().add(new EntityItem(ent.world, ent.posX, ent.posY, ent.posZ, ItemHumanPart.make(p, 8, 45)));
    }
}
