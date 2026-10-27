package com.crunkazcanbe.ntmiq.nest;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.NtmIq;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * F05 — "The Nest" nether rework from NTM: IQ Edition (1.7.10, GPL-3; irislgtm / tam69 and the IQ team).
 * <p>
 * IQ replaces the nether WorldProvider (DimensionManager.unregisterProviderType(-1)); we never do that — the pack runs
 * NetherEx / Nether-API / Better Nether / BoP. Instead:
 * <ul>
 * <li>Nether-API installed: The Nest is a real nether biome, added through NetherAPIRegistryEvent.Nether (soft, by reflection).</li>
 * <li>No Nether-API: a populate hook turns random 8x8-chunk regions of the nether into The Nest (decoration + the
 *     chunk biome bytes, so F3, mob spawns and mods see the biome) without touching the generator.</li>
 * </ul>
 * Self-contained: blocks/items/entities/biome register from this class's own registry events (config: ntmiq-nest.cfg).
 */
@Mod.EventBusSubscriber(modid = "ntmiq")
public final class IqNest {
    private IqNest() {}

    // config
    static boolean enabled, netherHives, hellSpawns;
    static int weight, arterySpawn;

    public static Block glyphidCrevasse, glyphidGrub, glyphidOvertaken, glyphidHive, glyphidBloatwisp, spawnerNether;
    public static Block cyst, artery, bloatLog, bloatLeaves, bloatSapling, bloatfire;
    public static Block arteryPump;
    public static final List<Block> FLESH = new ArrayList<>();
    public static Item meatNether, meatNetherGrilled, eggNether;
    public static Biome theNest;

    static final List<Block> BLOCKS = new ArrayList<>();
    static final List<Item> ITEMS = new ArrayList<>();

    /** IQ EnumFleshPlantType order */
    static final String[] FLESH_PLANTS = { "tendon", "peeping", "bulb_big", "bulb_small", "fever", "fever_small", "glyphid_vent",
            "hair_short", "hair_tall", "lung", "spine_fern" };

    private static void config() {
        Configuration cfg = new Configuration(new File(Loader.instance().getConfigDir(), "ntmiq-nest.cfg"));
        String c = "the_nest";
        cfg.setCategoryComment(c, "NTM IQ Edition's 'The Nest' nether biome. Never replaces the nether provider. Existing chunks are not changed.");
        enabled = cfg.getBoolean("enable", c, true, "Generate The Nest in the nether (blocks, items and mobs always register)");
        weight = cfg.getInt("weight", c, 10, 1, 1000, "Nether-API biome weight (vanilla nether wastes = 10). Without Nether-API: % of 8x8-chunk nether regions that become The Nest (max 100)");
        netherHives = cfg.getBoolean("netherHives", c, true, "IQ: nether glyphid hives, nest fungi and spawners in The Nest");
        arterySpawn = cfg.getInt("arterySpawn", c, 10, 0, 1000, "IQ: a Satan's Blood bedrock artery in every n-th Nest chunk (0 = off)");
        hellSpawns = cfg.getBoolean("hellSpawns", c, true, "Nether glyphids (grunt + scout) also spawn rarely in the rest of the nether");
        if (cfg.hasChanged()) cfg.save();
    }

    // ---------------------------------------------------------------- registration

    private static <T extends Block> T block(T b, String name, float hardness, boolean item) {
        b.setRegistryName(new ResourceLocation("ntmiq", name)).setTranslationKey("ntmiq." + name).setCreativeTab(IqContent.TAB);
        b.setHardness(hardness);
        BLOCKS.add(b);
        if (item) ITEMS.add(new ItemBlock(b).setRegistryName(b.getRegistryName()));
        return b;
    }

    private static Item item(Item i, String name) {
        i.setRegistryName(new ResourceLocation("ntmiq", name)).setTranslationKey("ntmiq." + name).setCreativeTab(IqContent.TAB);
        ITEMS.add(i);
        return i;
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        config();
        glyphidCrevasse = block(new NestBlocks.NetherGlyphid(false), "nether_glyphid_crevasse", 0.5F, true);
        glyphidGrub = block(new NestBlocks.NetherGlyphid(false), "nether_glyphid_grub", 0.5F, true);
        glyphidOvertaken = block(new NestBlocks.NetherGlyphid(false), "nether_glyphid_overtaken", 0.5F, true);
        glyphidHive = block(new NestBlocks.NetherGlyphid(false), "nether_glyphid_hive", 0.5F, true);
        glyphidBloatwisp = block(new NestBlocks.NetherGlyphid(true), "nether_glyphid_bloatwisp", 0.5F, true);
        spawnerNether = block(new NestBlocks.SpawnerNether(), "glyphid_spawner_nether", 0.5F, true);
        for (String p : FLESH_PLANTS) FLESH.add(block(new NestBlocks.FleshPlant(), "plant_flesh_" + p, 0F, true));
        cyst = block(new NestBlocks.Cyst(), "cyst", 0.5F, true);
        artery = block(new NestBlocks.Artery(), "ore_bedrock_artery", -1F, true);
        arteryPump = block(new ArteryPump.PumpBlock(), "artery_pump", 5F, true);
        bloatLog = block(new NestBlocks.BloatLog(), "bloatsprout_log", 0.5F, true);
        bloatLeaves = block(new NestBlocks.BloatLeaves(), "bloatsprout_leaves", 0.5F, true);
        bloatSapling = block(new NestBlocks.BloatSapling(), "bloatsprout_sapling", 0F, true);
        bloatfire = block(new NestBlocks.Bloatfire(), "bloatfire", 0F, false);
        bloatfire.setCreativeTab(null);
        for (Block b : BLOCKS) e.getRegistry().register(b);
        GameRegistry.registerTileEntity(NestBlocks.TileSpawnerNether.class, new ResourceLocation("ntmiq", "glyphid_spawner_nether"));
        GameRegistry.registerTileEntity(ArteryPump.PumpTile.class, new ResourceLocation("ntmiq", "artery_pump"));
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        meatNether = item(new ItemFood(6, 1.0F, true), "glyphid_meat_nether");
        meatNetherGrilled = item(new ItemFood(16, 1.5F, true).setPotionEffect(new PotionEffect(MobEffects.STRENGTH, 180 * 20, 2), 1F), "glyphid_meat_nether_grilled");
        eggNether = item(new Item(), "egg_glyphid_nether");
        for (Item i : ITEMS) e.getRegistry().register(i);
        GameRegistry.addSmelting(meatNether, new ItemStack(meatNetherGrilled), 0.35F);
    }

    private static int nextId = 300;   // coordinator: this module owns mod-local entity ids 300-399

    private static EntityEntry entity(Class<? extends EntityGlyphid> c, String name, int egg) {
        return EntityEntryBuilder.create().entity(c).id(new ResourceLocation("ntmiq", "entity_" + name), nextId++).name("ntmiq." + name)
                .tracker(128, 3, true).egg(0x4A2A1A, egg).build();
    }

    @SubscribeEvent
    public static void entities(RegistryEvent.Register<EntityEntry> e) {
        e.getRegistry().registerAll(
                entity(NetherGlyphids.Grunt.class, "glyphid_nether", 0xB03A1E),
                entity(NetherGlyphids.Scout.class, "glyphid_nether_scout", 0xE0C040),
                entity(NetherGlyphids.Bombardier.class, "glyphid_nether_bombardier", 0x7A3FA0),
                entity(NetherGlyphids.Brawler.class, "glyphid_nether_brawler", 0x8C1010),
                entity(NetherGlyphids.Digger.class, "glyphid_nether_digger", 0x6A4A30),
                entity(NetherGlyphids.Blaster.class, "glyphid_nether_blaster", 0xD06020),
                entity(NetherGlyphids.Behemoth.class, "glyphid_nether_behemoth", 0x502010),
                entity(NetherGlyphids.Brenda.class, "glyphid_nether_brenda", 0xF0A0A0),
                entity(NetherGlyphids.Nuclear.class, "glyphid_nether_nuclear", 0x40FF40));
        // F03 agent: the nether blaster's bloat bomb, mod-local id 701 (700-799 range)
        e.getRegistry().register(EntityEntryBuilder.create().entity(EntityBloatBomb.class).id(new ResourceLocation("ntmiq", "entity_bloat_bomb"), 701)
                .name("ntmiq.bloat_bomb").tracker(250, 1, true).build());
    }

    @SubscribeEvent
    public static void biomes(RegistryEvent.Register<Biome> e) {
        theNest = new BiomeTheNest().setRegistryName(new ResourceLocation("ntmiq", "the_nest"));
        e.getRegistry().register(theNest);
        BiomeDictionary.addTypes(theNest, BiomeDictionary.Type.NETHER, BiomeDictionary.Type.HOT, BiomeDictionary.Type.DRY,
                BiomeDictionary.Type.DEAD, BiomeDictionary.Type.SPOOKY);
        // natural spawns in The Nest (IQ relies on spawner blocks alone; a few walkers make the biome feel alive)
        EntityRegistry.addSpawn(NetherGlyphids.Grunt.class, 40, 2, 4, EnumCreatureType.MONSTER, theNest);
        EntityRegistry.addSpawn(NetherGlyphids.Scout.class, 10, 1, 1, EnumCreatureType.MONSTER, theNest);
        EntityRegistry.addSpawn(NetherGlyphids.Bombardier.class, 12, 1, 2, EnumCreatureType.MONSTER, theNest);
        EntityRegistry.addSpawn(NetherGlyphids.Brawler.class, 8, 1, 2, EnumCreatureType.MONSTER, theNest);
        EntityRegistry.addSpawn(NetherGlyphids.Digger.class, 6, 1, 1, EnumCreatureType.MONSTER, theNest);
        EntityRegistry.addSpawn(NetherGlyphids.Blaster.class, 6, 1, 1, EnumCreatureType.MONSTER, theNest);
        if (hellSpawns) {
            EntityRegistry.addSpawn(NetherGlyphids.Grunt.class, 3, 1, 3, EnumCreatureType.MONSTER, Biomes.HELL);
            EntityRegistry.addSpawn(NetherGlyphids.Scout.class, 1, 1, 1, EnumCreatureType.MONSTER, Biomes.HELL);
        }
        if (!enabled) return;
        if (Loader.isModLoaded("nether_api")) hookNetherApi();
        else MinecraftForge.EVENT_BUS.register(Fallback.class);
    }

    // ---------------------------------------------------------------- Nether-API (soft dependency, no compile-time jar)

    /**
     * Subscribes {@link NetherApiListener#onRegistry} to git.jbredwards.nether_api.api.event.NetherAPIRegistryEvent$Nether.
     * EVENT_BUS.register(Object) needs the event type at compile time, so we call its private register(Class, Object,
     * Method, ModContainer) — present in both Forge 14.23.5 and Cleanroom.
     */
    private static void hookNetherApi() {
        try {
            Class<?> event = Class.forName("git.jbredwards.nether_api.api.event.NetherAPIRegistryEvent$Nether");
            Method register = EventBus.class.getDeclaredMethod("register", Class.class, Object.class, Method.class, ModContainer.class);
            register.setAccessible(true);
            ModContainer owner = Loader.instance().getIndexedModList().get("ntmiq");
            register.invoke(MinecraftForge.EVENT_BUS, event, new NetherApiListener(), NetherApiListener.class.getMethod("onRegistry", Event.class), owner);
            NtmIq.LOG.info("The Nest: registered as a Nether-API nether biome (weight {})", weight);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            NtmIq.LOG.error("The Nest: Nether-API hook failed, using the populate fallback instead", ex);
            MinecraftForge.EVENT_BUS.register(Fallback.class);
        }
    }

    public static class NetherApiListener {
        /** registered by reflection only (never via EVENT_BUS.register, which would subscribe it to every Event) */
        @SubscribeEvent
        public void onRegistry(Event e) {
            try {
                Object registry = e.getClass().getField("registry").get(e);
                Class.forName("git.jbredwards.nether_api.api.registry.INetherAPIRegistry")
                        .getMethod("registerBiome", Biome.class, int.class).invoke(registry, theNest, weight);
            } catch (ReflectiveOperationException ex) {
                NtmIq.LOG.error("The Nest: could not add the biome to Nether-API's registry", ex);
            }
        }
    }

    // ---------------------------------------------------------------- fallback for a plain nether

    public static final class Fallback {
        private Fallback() {}

        /** a region is 8x8 chunks; 'weight' % of regions are The Nest, picked from the world seed */
        static boolean isNestRegion(World w, int chunkX, int chunkZ) {
            long seed = w.getSeed() ^ ((long) (chunkX >> 3) * 341873128712L + (long) (chunkZ >> 3) * 132897987541L) ^ 0x4E455354L;
            return new Random(seed).nextInt(100) < Math.min(weight, 100);
        }

        @SubscribeEvent
        public static void populate(PopulateChunkEvent.Post e) {
            World w = e.getWorld();
            if (w.isRemote || w.provider.getDimension() != -1 || !isNestRegion(w, e.getChunkX(), e.getChunkZ())) return;
            Chunk chunk = w.getChunk(e.getChunkX(), e.getChunkZ());
            Arrays.fill(chunk.getBiomeArray(), (byte) Biome.getIdForBiome(theNest));
            BiomeTheNest.decorateNest(w, e.getRand(), e.getChunkX() * 16, e.getChunkZ() * 16);   // features stay in corner..corner+31
            chunk.markDirty();
        }
    }
}
