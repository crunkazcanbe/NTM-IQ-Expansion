package com.crunkazcanbe.ntmiq;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Round 1 of the IQ port: materials, raw ores, storage/decor blocks, fuels and the Industrial Pipette.
 * Every 1.7.10 metadata variant is its own block/item here (raw_ore_iron, sheetmetal_red ...), so models are plain JSON.
 */
public final class IqContent {
    private IqContent() {}

    public static final List<Item> ITEMS = new ArrayList<>();
    public static final List<Block> BLOCKS = new ArrayList<>();

    /** IQ's raw ore list, in IQ's order */
    public static final String[] RAW_ORES = { "iron", "gold", "copper", "titanium", "thorium", "morkite", "nickel", "mineral", "zinc",
            "lithium", "niobium", "palladium", "iodine", "arsenic", "cadmium", "tungsten", "aluminium", "lead", "beryllium", "silicon",
            "australium", "lanthanium", "uranium", "schrabidium" };

    /** IQ's crystal storage blocks (EnumCrystalBlockType) */
    public static final String[] CRYSTALS = { "coal", "iron", "gold", "redstone", "lapis", "diamond", "uranium", "thorium", "plutonium",
            "titanium", "sulfur", "niter", "copper", "tungsten", "aluminium", "fluorite", "beryllium", "lead", "schraranium", "schrabidium",
            "rare", "phosphorus", "trixite", "lithium", "cobalt", "mineral", "nickel", "niobium", "zinc", "osmiridium", "cinnebar",
            "starmetal", "salt", "certus" };

    public static final String[] COLORS = { "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
            "cyan", "purple", "blue", "brown", "green", "red", "black" };

    /** item name -> burn time (ticks) for the fuels */
    public static final Map<String, Integer> FUEL = new LinkedHashMap<>();

    public static final Map<String, Item> RAW = new LinkedHashMap<>();
    public static final Map<String, Block> RAW_BLOCK = new LinkedHashMap<>();
    public static final Map<String, Block> CRYSTAL_BLOCK = new LinkedHashMap<>();
    public static final Map<String, Block> SHEET_COLORED = new LinkedHashMap<>();
    public static Item ingotTin, ingotBronze, ingotVanadium, nuggetVanadium, powderVanadium;
    public static Item chunkAnthracite, chunkFlamingAnthracite, cokeHemp, cokeHaemacoke, powderCokeHemp, powderCokeHaemacoke;
    public static Item industrialPipette;
    public static Block sheetmetal, plasticCrate, stoneAnthracite, stoneFlamingAnthracite, blockCokeHemp, blockCokeHaemacoke;

    public static CreativeTabs TAB;

    static void create() {
        TAB = new CreativeTabs("ntmiq") {
            @Override public ItemStack createIcon() { return new ItemStack(ingotVanadium); }
        };

        ingotTin = item("ingot_tin");
        ingotBronze = item("ingot_bronze");
        ingotVanadium = item("ingot_vanadium");
        nuggetVanadium = item("nugget_vanadium");
        powderVanadium = item("powder_vanadium");
        for (String o : RAW_ORES) RAW.put(o, item("raw_ore_" + o));

        chunkAnthracite = fuel("chunk_ore_anthracite", 2400);
        chunkFlamingAnthracite = fuel("chunk_ore_flaming_anthracite", 4800);
        cokeHemp = fuel("coke_hemp", 3200);
        cokeHaemacoke = fuel("coke_haemacoke", 3200);
        powderCokeHemp = fuel("powder_coke_hemp", 3200);
        powderCokeHaemacoke = fuel("powder_coke_haemacoke", 3200);

        for (String o : RAW_ORES) RAW_BLOCK.put(o, block(new Simple(Material.ROCK, SoundType.STONE), "block_raw_ore_" + o, 5F, 10F));
        for (String c : CRYSTALS) CRYSTAL_BLOCK.put(c, block(new Simple(Material.ROCK, SoundType.GLASS), "crystal_block_" + c, 5F, 10F));
        sheetmetal = block(new Simple(Material.IRON, SoundType.METAL), "sheetmetal", 5F, 10F);
        for (String c : COLORS) SHEET_COLORED.put(c, block(new Simple(Material.IRON, SoundType.METAL), "sheetmetal_colored_" + c, 5F, 10F));
        plasticCrate = block(new Simple(Material.WOOD, SoundType.WOOD), "plastic_crate", 0.6F, 3F);
        stoneAnthracite = block(new Simple(Material.ROCK, SoundType.STONE), "stone_resource_anthracite", 1.5F, 10F);
        stoneFlamingAnthracite = block(new Simple(Material.ROCK, SoundType.STONE).light(10), "stone_resource_flaming_anthracite", 1.5F, 10F);
        blockCokeHemp = block(new Simple(Material.ROCK, SoundType.STONE), "block_coke_hemp", 5F, 10F);
        blockCokeHaemacoke = block(new Simple(Material.ROCK, SoundType.STONE), "block_coke_haemacoke", 5F, 10F);
        FUEL.put("block_coke_hemp", 32000);
        FUEL.put("block_coke_haemacoke", 32000);

        industrialPipette = new ItemIndustrialPipette("industrial_pipette");
        industrialPipette.setCreativeTab(TAB);
        ITEMS.add(industrialPipette);
    }

    private static Item item(String name) {
        Item i = new Item() {
            @Override public int getItemBurnTime(ItemStack s) { return FUEL.getOrDefault(name, -1); }
        };
        i.setRegistryName(new ResourceLocation(Tags.MODID, name)).setTranslationKey(Tags.MODID + "." + name).setCreativeTab(TAB);
        ITEMS.add(i);
        return i;
    }

    private static Item fuel(String name, int ticks) {
        FUEL.put(name, ticks);
        return item(name);
    }

    private static Block block(Block b, String name, float hardness, float resistance) {
        b.setRegistryName(new ResourceLocation(Tags.MODID, name)).setTranslationKey(Tags.MODID + "." + name).setCreativeTab(TAB);
        b.setHardness(hardness).setResistance(resistance);
        BLOCKS.add(b);
        Item ib = new ItemBlock(b) {
            @Override public int getItemBurnTime(ItemStack s) { return FUEL.getOrDefault(name, -1); }
        }.setRegistryName(b.getRegistryName());
        ITEMS.add(ib);
        return b;
    }

    /** a plain full block with the sound and light we need */
    static class Simple extends Block {
        private int light;
        Simple(Material m, SoundType s) { super(m); setSoundType(s); }
        Simple light(int l) { this.light = l; setLightLevel(l / 15F); return this; }
        @Override public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) { return light; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.SOLID; }
    }
}
