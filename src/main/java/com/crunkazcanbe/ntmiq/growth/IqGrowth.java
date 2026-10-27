package com.crunkazcanbe.ntmiq.growth;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

import static com.hbm.inventory.OreDictManager.NB;
import static com.hbm.inventory.OreDictManager.STEEL;

/**
 * F13 — Growth Chamber, budding salt / budding certus quartz with their buds, salt geodes, salt items and crystal block
 * sounds from NTM: IQ Edition (1.7.10, GPL-3; irislgtm, tam69 and the IQ team), rebuilt on HBM NTM CE 1.12.2.
 * Certus content only exists when AE2 is loaded (like IQ) and uses AE2's crystal/dust through the ore dictionary
 * (crystalCertusQuartz / dustCertusQuartz) instead of adding a second certus item.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class IqGrowth {
    private IqGrowth() {}

    public static Item salt, saltShard;
    public static Block blockSalt, buddingSalt, buddingCertus, growthChamber;
    public static BlockBud saltBudSmall, saltBudMedium, saltBudLarge, saltCluster;
    public static BlockBud certusBudSmall, certusBudMedium, certusBudLarge, certusCluster;
    public static SoundEvent crystalPlace, crystalBreak, crystalStep, crystalResonate, crystalShimmer, growthLoop;
    public static SoundType crystalSound;

    public static void preInit(FMLPreInitializationEvent e) {
        crystalPlace = sound("block.crystalBlockPlace");
        crystalBreak = sound("block.crystalBlockBreak");
        crystalStep = sound("step.crystalBlock");
        crystalResonate = sound("block.crystalBlockResonate");
        crystalShimmer = sound("block.crystalBlockShimmer");
        growthLoop = sound("block.growthChamberOperation");
        crystalSound = new SoundType(1F, 1F, crystalBreak, crystalStep, crystalPlace, crystalStep, crystalStep);

        salt = item("salt");
        saltShard = item("salt_shard");
        blockSalt = block(new Block(Material.ROCK) {{ setSoundType(crystalSound); }}, "block_salt", 1.5F, 2F);

        saltCluster = bud(new BlockBud(Material.ROCK, crystalSound, 3, 0.3125F, 0.4375F, () -> new ItemStack(saltShard)), "salt_cluster", null);
        saltBudLarge = bud(new BlockBud(Material.ROCK, crystalSound, 2, 0.3125F, 0.3125F, () -> new ItemStack(saltShard)), "salt_bud_large", saltCluster);
        saltBudMedium = bud(new BlockBud(Material.ROCK, crystalSound, 1, 0.3125F, 0.25F, () -> new ItemStack(saltShard)), "salt_bud_medium", saltBudLarge);
        saltBudSmall = bud(new BlockBud(Material.ROCK, crystalSound, 0, 0.25F, 0.1875F, () -> new ItemStack(saltShard)), "salt_bud_small", saltBudMedium);
        BlockBudding bs = new BlockBudding(Material.ROCK, crystalSound, 0.5F);
        bs.firstStage = saltBudSmall;
        buddingSalt = block(bs, "budding_salt", 1.5F, 5F);

        if (Loader.isModLoaded("appliedenergistics2")) {
            Supplier<ItemStack> certus = () -> firstOre("crystalCertusQuartz");
            certusCluster = bud(new BlockBud(Material.GLASS, SoundType.GLASS, 3, 0.3125F, 0.4375F, certus), "certus_quartz_cluster", null);
            certusBudLarge = bud(new BlockBud(Material.GLASS, SoundType.GLASS, 2, 0.3125F, 0.3125F, certus), "certus_quartz_bud_large", certusCluster);
            certusBudMedium = bud(new BlockBud(Material.GLASS, SoundType.GLASS, 1, 0.3125F, 0.25F, certus), "certus_quartz_bud_medium", certusBudLarge);
            certusBudSmall = bud(new BlockBud(Material.GLASS, SoundType.GLASS, 0, 0.25F, 0.1875F, certus), "certus_quartz_bud_small", certusBudMedium);
            BlockBudding bc = new BlockBudding(Material.GLASS, SoundType.GLASS, 0.5F);
            bc.firstStage = certusBudSmall;
            buddingCertus = block(bc, "budding_certus_quartz", 1.5F, 5F);
        }

        growthChamber = new MachineGrowthChamber();

        Configuration cfg = new Configuration(new File(e.getModConfigurationDirectory(), "ntmiq-worldgen.cfg"));
        String c = "geodes";
        cfg.setCategoryComment(c, "NTM IQ Edition salt geodes (basalt shell, salt crystal lining, budding salt + buds). Existing chunks are not changed.");
        boolean geodes = cfg.getBoolean("enableSaltGeodes", c, true, "Generate salt geodes in the overworld");
        int chance = cfg.getInt("saltGeodeChance", c, 32, 1, 1000, "One geode per chance*chance chunks (IQ: 32 = 1 in 1024)");
        int minY = cfg.getInt("saltGeodeMinY", c, 15, 5, 200, "Lowest centre height");
        int maxY = cfg.getInt("saltGeodeMaxY", c, 45, 5, 240, "Highest centre height");
        if (cfg.hasChanged()) cfg.save();
        if (geodes) GameRegistry.registerWorldGenerator(new WorldGenGeode(chance, minY, Math.max(minY, maxY), ModBlocks.basalt.getDefaultState(),
                IqContent.CRYSTAL_BLOCK.get("salt").getDefaultState(), buddingSalt.getDefaultState(),
                saltBudSmall.getDefaultState(), saltBudMedium.getDefaultState(), saltBudLarge.getDefaultState()), 10);

        SerializableRecipe.recipeHandlers.add(GrowthChamberRecipes.INSTANCE); // CE fills its list in postInit, after this
        SerializableRecipe.additionalListeners.add(name -> {
            if (name.equals(AssemblyMachineRecipes.class.getSimpleName())) assembler();
            else if (name.equals(ShredderRecipes.class.getSimpleName())) shredder();
            else if (name.equals(CentrifugeRecipes.class.getSimpleName())) centrifuge();
        });
        if (e.getSide().isClient()) GrowthClient.preInit();
    }

    static ItemStack firstOre(String name) {
        List<ItemStack> ores = OreDictionary.getOres(name, false);
        return ores.isEmpty() ? ItemStack.EMPTY : ores.get(0).copy();
    }

    private static SoundEvent sound(String name) {
        ResourceLocation rl = new ResourceLocation(Tags.MODID, name);
        return new SoundEvent(rl).setRegistryName(rl);
    }

    private static Item item(String name) {
        Item i = new Item().setRegistryName(new ResourceLocation(Tags.MODID, name)).setTranslationKey(Tags.MODID + "." + name).setCreativeTab(IqContent.TAB);
        IqContent.ITEMS.add(i);
        return i;
    }

    private static Block block(Block b, String name, float hardness, float resistance) {
        b.setRegistryName(new ResourceLocation(Tags.MODID, name)).setTranslationKey(Tags.MODID + "." + name).setCreativeTab(IqContent.TAB);
        b.setHardness(hardness).setResistance(resistance);
        IqContent.BLOCKS.add(b);
        IqContent.ITEMS.add(new ItemBlock(b).setRegistryName(b.getRegistryName()));
        return b;
    }

    private static BlockBud bud(BlockBud b, String name, Block next) {
        block(b, name, 1.5F, 2.5F); // = vanilla default for hardness 1.5
        return b.next(next);
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(growthChamber);
        GameRegistry.registerTileEntity(TileEntityMachineGrowthChamber.class, new ResourceLocation(Tags.MODID, "tileentity_growth_chamber"));
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(new ItemBlock(growthChamber).setRegistryName(growthChamber.getRegistryName()));
    }

    @SubscribeEvent
    public static void sounds(RegistryEvent.Register<SoundEvent> e) {
        e.getRegistry().registerAll(crystalPlace, crystalBreak, crystalStep, crystalResonate, crystalShimmer, growthLoop);
    }

    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> e) {
        OreDictionary.registerOre("dustSalt", salt);
        OreDictionary.registerOre("itemSalt", salt);
        OreDictionary.registerOre("ingotSalt", saltShard);    // IQ: SALT .ingot(salt_shard)
        OreDictionary.registerOre("crystalSalt", saltCluster); // IQ: SALT .crystal(salt_cluster)
        OreDictionary.registerOre("blockSalt", blockSalt);

        add(e, "block_salt", new ShapedOreRecipe(null, new ItemStack(blockSalt), "XXX", "XXX", "XXX", 'X', salt));
        add(e, "block_salt_unpack", new ShapelessOreRecipe(null, new ItemStack(salt, 9), blockSalt));
        // IQ's crystal block recipes for the two crystals CE has no item for
        add(e, "crystal_block_salt", new ShapedOreRecipe(null, new ItemStack(IqContent.CRYSTAL_BLOCK.get("salt")), "XXX", "XXX", "XXX", 'X', saltCluster));
        add(e, "crystal_unblock_salt", new ShapelessOreRecipe(null, new ItemStack(saltCluster, 9), IqContent.CRYSTAL_BLOCK.get("salt")));
        if (buddingCertus != null)
            add(e, "crystal_block_certus", new ShapedOreRecipe(null, new ItemStack(IqContent.CRYSTAL_BLOCK.get("certus")), "XXX", "XXX", "XXX", 'X', "crystalCertusQuartz"));
    }

    private static void add(RegistryEvent.Register<IRecipe> e, String name, IRecipe r) {
        e.getRegistry().register(r.setRegistryName(new ResourceLocation(Tags.MODID, name)));
    }

    /** IQ: "ass.growth_chamber" */
    private static void assembler() {
        AssemblyMachineRecipes.INSTANCE.register(new GenericRecipe("ass.growth_chamber").setup(400, 100).outputItems(new ItemStack(growthChamber))
                .inputItems(new OreDictStack(STEEL.plateCast(), 4), new ComparableStack(ModBlocks.glass_boron, 32),
                        new ComparableStack(ModItems.motor, 2), new OreDictStack(NB.ingot(), 6), new ComparableStack(ModBlocks.machine_transformer)));
    }

    private static void shredder() {
        ShredderRecipes.setRecipe(saltBudLarge, new ItemStack(salt));
        ShredderRecipes.setRecipe(saltCluster, new ItemStack(salt, 3));
        if (buddingCertus == null) return;
        ItemStack dust = firstOre("dustCertusQuartz");
        if (dust.isEmpty()) return;
        ItemStack crystal = firstOre("crystalCertusQuartz");
        if (!crystal.isEmpty()) ShredderRecipes.setRecipe(crystal, copy(dust, 1));
        ShredderRecipes.setRecipe(new ItemStack(IqContent.CRYSTAL_BLOCK.get("certus")), copy(dust, 4));
        ShredderRecipes.setRecipe(certusCluster, copy(dust, 2));
        for (Block b : new Block[] { certusBudLarge, certusBudMedium, certusBudSmall }) ShredderRecipes.setRecipe(b, copy(dust, 1));
    }

    private static void centrifuge() {
        CentrifugeRecipes.addRecipe(new ComparableStack(saltBudLarge), new ItemStack[] { new ItemStack(saltShard), new ItemStack(saltShard) });
        CentrifugeRecipes.addRecipe(new ComparableStack(saltCluster), new ItemStack[] { new ItemStack(saltShard, 3), new ItemStack(saltShard, 3), new ItemStack(salt, 5) });
    }

    private static ItemStack copy(ItemStack s, int n) {
        ItemStack c = s.copy();
        c.setCount(n);
        return c;
    }
}
