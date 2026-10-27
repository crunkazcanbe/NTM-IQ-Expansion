package com.crunkazcanbe.ntmiq.drones;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import static com.hbm.inventory.OreDictManager.TI;

/**
 * F09 — mining drones from NTM: IQ Edition (1.7.10, GPL-3; irislgtm, tam69 and the IQ team) on CE's drone system:
 * Quarry Drone + Quarry Marker, Spelunking Drone, and 4 Demolisher drones. Recipes are IQ's CraftingManager ones.
 */
@Mod.EventBusSubscriber(modid = "ntmiq")
public final class IqDrones {
    private IqDrones() {}

    public static ItemMiningDrone drone;
    public static Block quarryMarker;

    /** call from the mod's preInit (after IqContent.create, which makes the creative tab) */
    public static void preInit(FMLPreInitializationEvent e) {
        drone = new ItemMiningDrone();
        quarryMarker = new BlockQuarryMarker();
        if (e.getSide().isClient()) DronesClient.preInit();
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(quarryMarker);
        GameRegistry.registerTileEntity(TileEntityQuarryMarker.class, new ResourceLocation("ntmiq", "tileentity_quarry_marker"));
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(new ItemBlock(quarryMarker).setRegistryName(quarryMarker.getRegistryName()));
        e.getRegistry().register(drone);
    }

    @SubscribeEvent
    public static void entities(RegistryEvent.Register<EntityEntry> e) {
        e.getRegistry().registerAll(
                entity(EntityQuarryDrone.class, "entity_quarry_drone", 200),
                entity(EntitySpelunkingDrone.class, "entity_spelunking_drone", 201),
                entity(EntityDemolisherDrone.class, "entity_demolisher_drone", 202));
    }

    private static EntityEntry entity(Class<? extends net.minecraft.entity.Entity> c, String name, int id) {
        return EntityEntryBuilder.create().entity(c).id(new ResourceLocation("ntmiq", name), id).name("ntmiq." + name)
                .tracker(250, 1, false).build();
    }

    @SubscribeEvent
    public static void recipes(RegistryEvent.Register<IRecipe> e) {
        ItemStack patrol = new ItemStack(ModItems.drone, 1, 0);
        ItemStack dem = stack(ItemMiningDrone.DEMOLISHER), demCl = stack(ItemMiningDrone.DEMOLISHER_CHUNKLOADING);
        ItemStack demEx = stack(ItemMiningDrone.DEMOLISHER_EXPRESS), demExCl = stack(ItemMiningDrone.DEMOLISHER_EXPRESS_CHUNKLOADING);
        String kerosene = Fluids.KEROSENE.getDict(1_000);

        add(e, "drone_demolisher", new ShapelessOreRecipe(null, dem, patrol, ModItems.drillbit));
        add(e, "drone_demolisher_chunkloading", new ShapedOreRecipe(null, demCl, "E", "D", 'E', Items.ENDER_PEARL, 'D', dem));
        add(e, "drone_demolisher_express", new ShapedOreRecipe(null, demEx, " P ", "KDK", " P ", 'P', TI.plateWelded(), 'K', kerosene, 'D', dem));
        add(e, "drone_demolisher_express_chunkloading", new ShapedOreRecipe(null, demExCl, "E", "D", 'E', Items.ENDER_PEARL, 'D', demEx));
        add(e, "drone_demolisher_express_chunkloading_alt", new ShapedOreRecipe(null, demExCl, " P ", "KDK", " P ", 'P', TI.plateWelded(), 'K', kerosene, 'D', demCl));
        add(e, "drone_demolisher_unload", new ShapelessOreRecipe(null, dem, demCl));
        add(e, "drone_demolisher_express_unload", new ShapelessOreRecipe(null, demEx, demExCl));
        add(e, "drone_spelunker", new ShapelessOreRecipe(null, stack(ItemMiningDrone.SPELUNKER), patrol, Items.DIAMOND_PICKAXE));
        add(e, "drone_quarry", new ShapelessOreRecipe(null, stack(ItemMiningDrone.QUARRY), patrol, ModItems.drill_titanium));
        add(e, "quarry_marker", new ShapedOreRecipe(null, new ItemStack(quarryMarker, 4), "T", "C", 'T', ModBlocks.drone_waypoint_request, 'C', "dustRedstone"));
    }

    private static ItemStack stack(int meta) {
        return new ItemStack(drone, 1, meta);
    }

    private static void add(RegistryEvent.Register<IRecipe> e, String name, IRecipe r) {
        e.getRegistry().register(r.setRegistryName(new ResourceLocation("ntmiq", name)));
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (int i = 0; i < ItemMiningDrone.NAMES.length; i++)
            ModelLoader.setCustomModelResourceLocation(drone, i, new ModelResourceLocation("ntmiq:drone_" + ItemMiningDrone.NAMES[i], "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(quarryMarker), 0, new ModelResourceLocation(quarryMarker.getRegistryName(), "inventory"));
    }
}
