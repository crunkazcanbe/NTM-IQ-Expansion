package com.crunkazcanbe.ntmiq.magnetic;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

import static com.hbm.inventory.OreDictManager.ND;
import static com.hbm.inventory.OreDictManager.STEEL;

/**
 * F10 from NTM: IQ Edition (irislgtm / tam69 / the IQ team, GPL-3): magnetic crates. Magnetic Strip magnetizes a CE crate,
 * the Magnetic Crafter (chestplate mod or handheld, key B) is a crafting grid that pulls from magnetic crates within
 * 8 blocks, the Magnetic Stocker (special armor mod or handheld) deposits (key M, sneak = all but hotbar) and restocks (key H).
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class IqMagnetic {
    private IqMagnetic() {}

    public static final int GUI_CRAFTER = 0;
    public static Item strip, crafter, restocker;
    public static SoundEvent MAGNETIC;

    public static void preInit(FMLPreInitializationEvent e) {
        MagneticCrates.register();
        MagneticPackets.register();
        strip = new ItemMagneticStrip().setRegistryName(Tags.MODID, "magnetic_strip").setTranslationKey("magnetic_strip").setCreativeTab(IqContent.TAB);
        crafter = new ItemMagneticCrafter().setCreativeTab(IqContent.TAB);
        restocker = new ItemMagneticRestocker().setCreativeTab(IqContent.TAB);
        ResourceLocation snd = new ResourceLocation(Tags.MODID, "item.magnetic");
        MAGNETIC = new SoundEvent(snd).setRegistryName(snd);
        MinecraftForge.EVENT_BUS.register(new MagneticCrates.Events());
        // ponytail: the add-on's only IGuiHandler; if another ntmiq feature needs one, merge it here (FML keeps one per mod)
        NetworkRegistry.INSTANCE.registerGuiHandler(Tags.MODID, new IGuiHandler() {
            @Override public Object getServerGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
                return id == GUI_CRAFTER ? new ContainerMagneticCrafter(p.inventory, w) : null;
            }
            @Override public Object getClientGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
                return id == GUI_CRAFTER ? new GUIMagneticCrafter(p.inventory, w) : null;
            }
        });
        if (e.getSide().isClient()) MagneticClient.preInit();
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().registerAll(strip, crafter, restocker);
    }

    @SubscribeEvent
    public static void sounds(RegistryEvent.Register<SoundEvent> e) {
        e.getRegistry().register(MAGNETIC);
    }

    /** IQ CraftingManager recipes */
    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> e) {
        e.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(strip, 4), "N", "S", "N",
                'N', ND.dust(), 'S', STEEL.plate()).setRegistryName(Tags.MODID, "magnetic_strip"));
        e.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(crafter), "NPN", "SCS", "NSN",
                'N', ND.dust(), 'S', STEEL.plate(), 'P', STEEL.plateCast(), 'C', Blocks.CRAFTING_TABLE).setRegistryName(Tags.MODID, "magnetic_crafter"));
        e.getRegistry().register(new ShapedOreRecipe(null, new ItemStack(restocker), "NDN", "SCS", "SPS",
                'N', ND.dust(), 'S', STEEL.plate(), 'D', new ItemStack(ModItems.circuit, 1, EnumCircuitType.VACUUM_TUBE.ordinal()),
                'C', Blocks.HOPPER, 'P', STEEL.plateCast()).setRegistryName(Tags.MODID, "magnetic_restocker"));
    }
}
