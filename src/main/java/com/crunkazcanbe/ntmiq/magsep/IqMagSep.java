package com.crunkazcanbe.ntmiq.magsep;

import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.SerializableRecipe;
import com.hbm.items.ModItems;
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
 * F11 — Magnetic Separator + magnetic discs (iron / neodymium / vanadium / nickel) from NTM: IQ Edition (1.7.10, GPL-3;
 * irislgtm, tam69 and the IQ team), rebuilt on HBM NTM CE 1.12.2. IQ ships no recipes for the machine or the discs
 * (creative-only there); the assembler + crafting recipes below are the add-on's own so they can be made in survival.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class IqMagSep {
    private IqMagSep() {}

    public static Block separator;
    public static ItemMagneticDisc disc;
    public static SoundEvent magOpen, magClose, magLoop;

    public static void preInit(FMLPreInitializationEvent e) {
        separator = new MachineMagneticSeparator();
        disc = new ItemMagneticDisc();
        magOpen = sound("machine.magnetic_open");
        magClose = sound("machine.magnetic_close");
        magLoop = sound("machine.magnetic_loop");
        SerializableRecipe.recipeHandlers.add(MagneticSeparatorRecipes.INSTANCE); // CE fills its list in postInit, after this
        SerializableRecipe.additionalListeners.add(name -> {
            if (AssemblyMachineRecipes.class.getSimpleName().equals(name)) assembler();
        });
        if (e.getSide().isClient()) MagSepClient.preInit();
    }

    private static SoundEvent sound(String name) {
        ResourceLocation rl = new ResourceLocation(Tags.MODID, name);
        return new SoundEvent(rl).setRegistryName(rl);
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(separator);
        GameRegistry.registerTileEntity(TileEntityMachineMagneticSeparator.class, new ResourceLocation(Tags.MODID, "tileentity_magnetic_separator"));
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(new ItemBlock(separator).setRegistryName(separator.getRegistryName()));
        e.getRegistry().register(disc);
    }

    @SubscribeEvent
    public static void sounds(RegistryEvent.Register<SoundEvent> e) {
        e.getRegistry().registerAll(magOpen, magClose, magLoop);
    }

    /** add-on recipes: a ring of the disc metal around a circular magnet */
    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> e) {
        String[] metals = { "ingotIron", "ingotNeodymium", "ingotVanadium", "ingotNickel" };
        for (ItemMagneticDisc.EnumMagneticDisc t : ItemMagneticDisc.EnumMagneticDisc.values()) {
            IRecipe r = new ShapedOreRecipe(null, new ItemStack(disc, 1, t.ordinal()), " X ", "XMX", " X ", 'X', metals[t.ordinal()], 'M', ModItems.magnet_circular);
            e.getRegistry().register(r.setRegistryName(new ResourceLocation(Tags.MODID, "magnetic_disc_" + t.name().toLowerCase(java.util.Locale.US))));
        }
    }

    private static void assembler() {
        AssemblyMachineRecipes.INSTANCE.register(new GenericRecipe("ass.magnetic_separator").setup(400, 100).outputItems(new ItemStack(separator))
                .inputItems(new OreDictStack(STEEL.plateCast(), 8), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.coil_copper, 8),
                        new ComparableStack(ModItems.magnet_circular, 4), new ComparableStack(ModBlocks.machine_transformer)));
    }
}
