package com.crunkazcanbe.ntmiq.foam;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.Tags;
import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.hbm.blocks.BlockEnums.EnumStoneType;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapelessOreRecipe;

/**
 * F19 from NTM: IQ Edition (irislgtm / tam69 / the IQ team, GPL-3): construction foam. Spray one of the 9 foam fluids
 * (IqFluids) from CE's chemical thrower and it sets as soft foam that hardens into its material after 10 s. IQ edited
 * EntityChemical for this; CE fires Forge's ProjectileImpactEvent for its chemical projectiles, so no mixin is needed.
 * Quikrete bags mix the material foams (mixer) and set into 4 blocks in the crystallizer.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class IqFoam {
    private IqFoam() {}

    /** IQ quikrete order = foam type 1..8 */
    public static final String[] QUIKRETE = { "concrete", "reinforced_concrete", "ducrete", "reinforced_ducrete", "limestone", "netherrack", "depthrock", "nether_depthrock" };

    public static Block foam, hardened;
    public static final Item[] quikrete = new Item[QUIKRETE.length];

    public static void preInit(FMLPreInitializationEvent e) {
        foam = new BlockConstructionFoam().setHardness(0.5F).setResistance(0.0F);
        name(foam, "construction_foam");
        foam.setSoundType(SoundType.CLOTH);
        hardened = new Block(Material.ROCK).setHardness(5.0F).setResistance(20.0F);
        name(hardened, "hardened_construction_foam");
        for (int i = 0; i < QUIKRETE.length; i++) {
            String n = "quikrete_" + QUIKRETE[i];
            quikrete[i] = new Item().setRegistryName(Tags.MODID, n).setTranslationKey(n).setCreativeTab(IqContent.TAB);
        }

        // trait class must be known to CE's trait-config writer before it is attached (see IqFluids)
        IqFluids.registerTrait("constructionFoam", FT_ConstructionFoam.class);
        FluidType[] fluids = foams();
        for (int i = 0; i < fluids.length; i++)
            if (fluids[i] != null && !fluids[i].hasTrait(FT_ConstructionFoam.class)) fluids[i].addTraits(new FT_ConstructionFoam(i));

        SerializableRecipe.additionalListeners.add(name -> {
            if (MixerRecipes.class.getSimpleName().equals(name)) mixer();
            else if (CrystallizerRecipes.class.getSimpleName().equals(name)) crystallizer();
        });
    }

    private static void name(Block b, String n) {
        b.setRegistryName(Tags.MODID, n).setTranslationKey(n).setCreativeTab(IqContent.TAB);
    }

    static FluidType[] foams() {
        return new FluidType[] { IqFluids.CONSTRUCTION_FOAM, IqFluids.CONSTRUCTION_FOAM_CONCRETE, IqFluids.CONSTRUCTION_FOAM_REINFORCED_CONCRETE,
                IqFluids.CONSTRUCTION_FOAM_DUCRETE, IqFluids.CONSTRUCTION_FOAM_REINFORCED_DUCRETE, IqFluids.CONSTRUCTION_FOAM_LIMESTONE,
                IqFluids.CONSTRUCTION_FOAM_NETHERRACK, IqFluids.CONSTRUCTION_FOAM_DEPTHROCK, IqFluids.CONSTRUCTION_FOAM_NETHER_DEPTHROCK };
    }

    /** IQ ModBlocks: BlockConstructionFoam.registerResult(1..8, ...) */
    private static IBlockState[] results() {
        return new IBlockState[] { null, ModBlocks.concrete.getDefaultState(), ModBlocks.concrete_rebar.getDefaultState(), ModBlocks.ducrete.getDefaultState(),
                ModBlocks.reinforced_ducrete.getDefaultState(), ModBlocks.stone_resource.getStateFromMeta(EnumStoneType.LIMESTONE.ordinal()),
                Blocks.NETHERRACK.getDefaultState(), ModBlocks.stone_depth.getDefaultState(), ModBlocks.stone_depth_nether.getDefaultState() };
    }

    /** IQ MixerRecipes: colloid + water (+ quikrete bag) -> 1000 mB foam */
    private static void mixer() {
        FluidType[] f = foams();
        if (f[0] == null) return;
        MixerRecipes.register(f[0], new MixerRecipes.MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.COLLOID, 1000)).setStack2(new FluidStack(Fluids.WATER, 1000)));
        for (int i = 0; i < quikrete.length; i++)
            if (f[i + 1] != null) MixerRecipes.register(f[i + 1], new MixerRecipes.MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.COLLOID, 1000))
                    .setStack2(new FluidStack(Fluids.WATER, 1000)).setSolid(new ComparableStack(quikrete[i], 1)));
    }

    /** IQ CrystallizerRecipes: quikrete + 250 mB water -> 4 blocks, 100 ticks */
    private static void crystallizer() {
        IBlockState[] r = results();
        for (int i = 0; i < quikrete.length; i++) {
            IBlockState s = r[i + 1];
            CrystallizerRecipes.registerRecipe(new ComparableStack(quikrete[i]),
                    new CrystallizerRecipes.CrystallizerRecipe(new ItemStack(s.getBlock(), 4, s.getBlock().getMetaFromState(s)), 100), new FluidStack(Fluids.WATER, 250));
        }
    }

    /** CE's chemthrower projectile hit a block with a foam fluid: place foam (IQ's EntityChemical edit) */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent e) {
        if (!(e.getEntity() instanceof EntityChemical chem) || chem.world.isRemote) return;
        RayTraceResult mop = e.getRayTraceResult();
        if (mop == null || mop.typeOfHit != RayTraceResult.Type.BLOCK) return;
        FluidType type = chem.getType();
        if (type.hasTrait(FT_ConstructionFoam.class)) type.getTrait(FT_ConstructionFoam.class).applyFoam(chem.world, mop);
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        IBlockState[] r = results();
        for (int i = 1; i < r.length; i++) BlockConstructionFoam.HARDENS_TO[i] = r[i];
        e.getRegistry().registerAll(foam, hardened);
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(new ItemBlock(foam).setRegistryName(foam.getRegistryName()));
        e.getRegistry().register(new ItemBlock(hardened).setRegistryName(hardened.getRegistryName()));
        e.getRegistry().registerAll(quikrete);
    }

    /**
     * ponytail: our addition, not IQ. IQ got quikrete only from Czechoslowakia bedrock ore (F07, needs CE-Space), so without
     * Space the bags were unobtainable: 4 blocks + paper -> 1 bag (the crystallizer turns 1 bag back into 4 blocks).
     */
    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> e) {
        IBlockState[] r = results();
        for (int i = 0; i < quikrete.length; i++) {
            IBlockState s = r[i + 1];
            ItemStack block = new ItemStack(s.getBlock(), 1, s.getBlock().getMetaFromState(s));
            e.getRegistry().register(new ShapelessOreRecipe(null, new ItemStack(quikrete[i]), block, block, block, block, Items.PAPER)
                    .setRegistryName(Tags.MODID, "quikrete_" + QUIKRETE[i]));
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (Item i : quikrete) ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        for (Block b : new Block[] { foam, hardened })
            ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(b), 0, new ModelResourceLocation(b.getRegistryName(), "inventory"));
    }
}
