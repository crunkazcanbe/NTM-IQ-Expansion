package com.crunkazcanbe.ntmiq.contagion;

import java.util.Random;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.Tags;
import com.crunkazcanbe.ntmiq.blood.BloodstreamAPI;
import com.crunkazcanbe.ntmiq.blood.IqBlood;
import com.crunkazcanbe.ntmiq.blood.ItemMedicalSyringe;
import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
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
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import static com.hbm.inventory.OreDictManager.ANY_PLASTIC;
import static com.hbm.inventory.OreDictManager.ANY_RUBBER;
import static com.hbm.inventory.OreDictManager.DESH;
import static com.hbm.inventory.OreDictManager.GOLD;
import static com.hbm.inventory.OreDictManager.STEEL;

/**
 * F02 — Contagion / genetics from NTM: IQ Edition (1.7.10, GPL-3; irislgtm, tam69 and the IQ team), rebuilt on HBM NTM
 * CE 1.12.2 as an add-on. Diseases of five kinds, each with a mutating genome, run inside F01's bloodstream and spread
 * by air, touch, blows, proximity, contaminated items and needles. You fight them by sampling blood, separating the
 * genome in the Magnetic Separator, writing the pieces to a floppy disk and brewing a matched antiserum in the
 * Dial-A-Dose — which, loaded with blood instead of antiserum, grows the disease for you instead.
 *
 * <p>Every spread path has a config switch ({@link ContagionConfig}) and a carrier stops looking for new hosts once
 * enough infected bodies stand around it, so a server never turns into a world-wide plague.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class IqContagion {
    private IqContagion() {}

    public static ItemGenomeVial vial;
    public static ItemGenomeDisk floppyDisk;
    public static ItemPharmaUnit pharmaUnit;
    public static ItemContagionGloves glovesRubber, glovesDesh;
    public static Block synthesizer;

    public static void preInit(FMLPreInitializationEvent e) {
        ContagionConfig.load(e);
        DiseaseRegistry.seedDefaults();

        IqFluids.registerTrait("ntmiq_pathogen", FT_Pathogen.class);   // CE writes trait JSON by these names
        IqFluids.registerTrait("ntmiq_pharma", FT_Pharma.class);
        traits();
        Fluids.additionalListeners.add(IqContagion::traits);           // CE's reloadFluids re-reads trait JSON: put ours back

        vial = new ItemGenomeVial("vial");
        floppyDisk = new ItemGenomeDisk("floppy_disk");
        pharmaUnit = new ItemPharmaUnit("pharma_computing_unit");
        glovesRubber = new ItemContagionGloves("gloves_rubber", 64);
        glovesDesh = new ItemContagionGloves("gloves_desh", 512);
        for (Item i : items()) i.setCreativeTab(IqContent.TAB);
        synthesizer = new MachineMedicineSynthesizer();

        // F01 plug points: pathogens tick with the blood, syringes carry diseases and cures
        BloodstreamAPI.registerTicker(DiseaseHandler::tick);
        BloodstreamAPI.registerPayload(ItemMedicalSyringe.KEY_PATHOGENS, (stack, target, intensity) ->
                FT_Pathogen.applyPayload(stack.getTagCompound().getTagList(ItemMedicalSyringe.KEY_PATHOGENS, Constants.NBT.TAG_COMPOUND), target, intensity));
        BloodstreamAPI.registerPayload(PharmaProfile.KEY, (stack, target, intensity) ->
                FT_Pharma.apply(PharmaProfile.from(stack), target, intensity));

        SerializableRecipe.additionalListeners.add(name -> {
            if (AssemblyMachineRecipes.class.getSimpleName().equals(name)) assembler();
        });
        if (e.getSide().isClient()) ContagionClient.preInit();
    }

    static Item[] items() { return new Item[] { vial, floppyDisk, pharmaUnit, glovesRubber, glovesDesh }; }

    /** blood carries diseases, antiserum cures them (idempotent: CE keys traits by class) */
    static void traits() {
        if (IqFluids.HUMAN_BLOOD != null) IqFluids.HUMAN_BLOOD.addTraits(new FT_Pathogen());
        if (IqFluids.ANTISERUM != null) IqFluids.ANTISERUM.addTraits(new FT_Pharma());
    }

    // ---------------- registries ----------------

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().registerAll(items());
        e.getRegistry().register(new ItemBlock(synthesizer).setRegistryName(synthesizer.getRegistryName()));
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(synthesizer);
        GameRegistry.registerTileEntity(TileEntityMedicineSynthesizer.class, new ResourceLocation(Tags.MODID, "tileentity_medicine_synthesizer"));
    }

    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> e) {
        ItemStack basic = new ItemStack(ModItems.circuit, 1, EnumCircuitType.BASIC.ordinal());

        add(e, "vial", new ShapedOreRecipe(null, new ItemStack(vial, 4), "G G", " G ", 'G', "blockGlassColorless"));
        add(e, "floppy_disk", new ShapedOreRecipe(null, new ItemStack(floppyDisk), "PPP", "PRP", 'P', ANY_PLASTIC.any(), 'R', Items.REDSTONE));
        add(e, "pharma_computing_unit", new ShapedOreRecipe(null, new ItemStack(pharmaUnit), " G ", "GCG", " G ", 'G', GOLD.wireFine(), 'C', basic));
        add(e, "gloves_rubber", new ShapedOreRecipe(null, new ItemStack(glovesRubber), "R R", "R R", 'R', ANY_RUBBER.ingot()));
        add(e, "gloves_desh", new ShapelessOreRecipe(null, new ItemStack(glovesDesh), glovesRubber, DESH.plate(), DESH.plate()));

        // the lab loop that does not need the machines IQ runs it on: draw a vial off a filled syringe, write a sample to a disk
        add(e, "blood_vial", new ContagionRecipes.BloodVialDraw());
        add(e, "disk_write", new ContagionRecipes.DiskWrite());
    }

    private static void add(RegistryEvent.Register<IRecipe> e, String name, IRecipe r) {
        e.getRegistry().register(r.setRegistryName(new ResourceLocation(Tags.MODID, name)));
    }

    private static void assembler() {
        AssemblyMachineRecipes.INSTANCE.register(new GenericRecipe("ass.ntmiq_medicine_synthesizer").setup(600, 200)
                .outputItems(new ItemStack(synthesizer))
                .inputItems(new OreDictStack(STEEL.plateCast(), 8), new ComparableStack(ModItems.pipes_steel, 4),
                        new ComparableStack(ModItems.circuit, 4, EnumCircuitType.ADVANCED.ordinal()),
                        new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.tank_steel, 2)));
    }

    // ---------------- events ----------------

    /** IQ's melee transmission axis */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        if (e.getSource().isProjectile()) return;
        if (e.getSource().getTrueSource() instanceof EntityLivingBase)
            DiseaseHandler.handleHit((EntityLivingBase) e.getSource().getTrueSource(), e.getEntityLiving());
    }

    /** a carrier's drops come out contaminated; zombies very rarely drop a wild disease syringe */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        EntityLivingBase dead = e.getEntityLiving();
        if (dead.world.isRemote || !ContagionConfig.enabled) return;

        if (ContagionConfig.fomites) {
            java.util.List<String> frames = ItemContagion.fomiteFramesOf(dead);
            if (!frames.isEmpty()) for (EntityItem item : e.getDrops()) ItemContagion.tagEntityItem(item, frames);
        }

        if (ContagionConfig.wildSyringeLoot && dead instanceof EntityZombie && dead.getRNG().nextInt(500) == 0)
            e.getDrops().add(new EntityItem(dead.world, dead.posX, dead.posY, dead.posZ, WildDisease.makeSyringe(dead.getRNG())));
    }

    /** a creative-tab / command shortcut: a syringe of MKU and one wild disease, so the system is reachable without loot */
    public static ItemStack wildSyringe(Random rand) { return WildDisease.makeSyringe(rand); }

    /** a filled medical syringe's pathogen list, or an empty list */
    public static NBTTagList pathogensOf(ItemStack syringe) {
        if (syringe.isEmpty() || !syringe.hasTagCompound()) return new NBTTagList();
        return syringe.getTagCompound().getTagList(ItemMedicalSyringe.KEY_PATHOGENS, Constants.NBT.TAG_COMPOUND);
    }

    /** F01's syringe item, for the recipes above */
    static Item medicalSyringe() { return IqBlood.medicalSyringe; }
}
