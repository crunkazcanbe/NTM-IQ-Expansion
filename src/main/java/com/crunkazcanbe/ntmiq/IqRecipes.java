package com.crunkazcanbe.ntmiq;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.IForgeRegistry;

/** Ore dictionary, crafting, smelting and IQ's "HBM ores drop raw ore". */
public final class IqRecipes {
    private IqRecipes() {}

    static String cap(String s) {
        StringBuilder b = new StringBuilder();
        for (String p : s.split("_")) b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        return b.toString();
    }

    static void oreDict() {
        OreDictionary.registerOre("ingotTin", IqContent.ingotTin);
        OreDictionary.registerOre("ingotBronze", IqContent.ingotBronze);
        OreDictionary.registerOre("ingotVanadium", IqContent.ingotVanadium);
        OreDictionary.registerOre("nuggetVanadium", IqContent.nuggetVanadium);
        OreDictionary.registerOre("dustVanadium", IqContent.powderVanadium);
        OreDictionary.registerOre("gemAnthracite", IqContent.chunkAnthracite);
        OreDictionary.registerOre("gemFlamingAnthracite", IqContent.chunkFlamingAnthracite);
        OreDictionary.registerOre("fuelCoke", IqContent.cokeHemp);
        OreDictionary.registerOre("fuelCoke", IqContent.cokeHaemacoke);
        OreDictionary.registerOre("dustCoke", IqContent.powderCokeHemp);
        OreDictionary.registerOre("dustCoke", IqContent.powderCokeHaemacoke);
        OreDictionary.registerOre("blockSheetmetal", IqContent.sheetmetal);
        // IQ also registered raw ores as "ore<Metal>"; in this pack Industrial Upgrade builds recipes for every ore* entry and
        // died during its startup scan with them (10-03), so here they are only raw<Metal>
        for (Map.Entry<String, Item> e : IqContent.RAW.entrySet()) OreDictionary.registerOre("raw" + cap(e.getKey()), e.getValue());
        for (Map.Entry<String, Block> e : IqContent.RAW_BLOCK.entrySet()) OreDictionary.registerOre("blockRaw" + cap(e.getKey()), e.getValue());
        for (Map.Entry<String, Block> e : IqContent.CRYSTAL_BLOCK.entrySet()) OreDictionary.registerOre("crystalBlock" + cap(e.getKey()), e.getValue());
    }

    /** IQ's SmeltingRecipes: raw ore -> its ingot (or HBM chunk), plus nugget/powder/stone smelts */
    static void smelting() {
        Object[][] raw = {
                { "iron", "ingotIron", 0.7F }, { "gold", "ingotGold", 1.0F }, { "copper", "ingotCopper", 2.5F }, { "titanium", "ingotTitanium", 3.0F },
                { "thorium", "ingotThorium232|ingotThorium", 3.0F }, { "zinc", "ingotZinc", 2.5F }, { "niobium", "ingotNiobium", 4.0F },
                { "tungsten", "ingotTungsten", 6.0F }, { "lead", "ingotLead", 3.0F }, { "beryllium", "ingotBeryllium", 2.0F },
                { "australium", "nuggetAustralium", 2.5F }, { "lanthanium", "ingotLanthanium|ingotLanthanum", 4.0F }, { "uranium", "ingotUranium", 6.0F },
                { "schrabidium", "ingotSchrabidium", 128.0F }, { "nickel", "@hbm:chunk_ore:pentlandite", 2.5F }, { "aluminium", "@hbm:chunk_ore:cryolite", 2.5F },
                { "lithium", "ingotLithium", 2.0F }, { "cadmium", "ingotCadmium", 2.5F }, { "arsenic", "ingotArsenic", 2.5F } };
        int n = 0;
        for (Object[] r : raw) {
            ItemStack out = find((String) r[1]);
            if (out.isEmpty()) continue;
            GameRegistry.addSmelting(IqContent.RAW.get((String) r[0]), out, (Float) r[2]);
            n++;
        }
        GameRegistry.addSmelting(IqContent.powderVanadium, new ItemStack(IqContent.ingotVanadium), 1.0F);
        NtmIq.LOG.info("NTM IQ Expansion: {} raw ore smelting recipes", n);
    }

    /** "ingotA|ingotB" = first ore-dict name that has an item; "@mod:item:subname" = HBM chunk by its enum name */
    static ItemStack find(String spec) {
        if (spec.startsWith("@")) {
            String[] p = spec.substring(1).split(":");
            Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(p[0], p[1]));
            if (it == null) return ItemStack.EMPTY;
            for (int meta = 0; meta < 64; meta++) {
                ItemStack s = new ItemStack(it, 1, meta);
                if (s.getTranslationKey().toLowerCase(Locale.ROOT).contains(p[2])) return s;
            }
            return ItemStack.EMPTY;
        }
        for (String name : spec.split("\\|")) {
            List<ItemStack> l = OreDictionary.getOres(name);
            if (!l.isEmpty()) { ItemStack s = l.get(0).copy(); s.setCount(1); if (s.getMetadata() == OreDictionary.WILDCARD_VALUE) s.setItemDamage(0); return s; }
        }
        return ItemStack.EMPTY;
    }

    static void crafting(IForgeRegistry<IRecipe> reg) {
        // raw ore <-> raw ore block
        for (Map.Entry<String, Item> e : IqContent.RAW.entrySet()) {
            Block blk = IqContent.RAW_BLOCK.get(e.getKey());
            add(reg, "raw_block_" + e.getKey(), new ShapedOreRecipe(null, new ItemStack(blk), "XXX", "XXX", "XXX", 'X', e.getValue()));
            add(reg, "raw_unblock_" + e.getKey(), new ShapelessOreRecipe(null, new ItemStack(e.getValue(), 9), blk));
        }
        // crystal blocks <-> 9 HBM crystals (only where CE has that crystal)
        for (Map.Entry<String, Block> e : IqContent.CRYSTAL_BLOCK.entrySet()) {
            Item crystal = ForgeRegistries.ITEMS.getValue(new ResourceLocation("hbm", "crystal_" + e.getKey()));
            if (crystal == null) continue;
            add(reg, "crystal_block_" + e.getKey(), new ShapedOreRecipe(null, new ItemStack(e.getValue()), "XXX", "XXX", "XXX", 'X', crystal));
            add(reg, "crystal_unblock_" + e.getKey(), new ShapelessOreRecipe(null, new ItemStack(crystal, 9), e.getValue()));
        }
        // vanadium nugget <-> ingot
        add(reg, "vanadium_ingot", new ShapedOreRecipe(null, new ItemStack(IqContent.ingotVanadium), "XXX", "XXX", "XXX", 'X', "nuggetVanadium"));
        add(reg, "vanadium_nuggets", new ShapelessOreRecipe(null, new ItemStack(IqContent.nuggetVanadium, 9), "ingotVanadium"));
        // bronze: 3 copper + 1 tin (any mod's tin/copper)
        add(reg, "bronze", new ShapelessOreRecipe(null, new ItemStack(IqContent.ingotBronze, 4), "ingotCopper", "ingotCopper", "ingotCopper", "ingotTin"));
        // sheetmetal: 4 steel plates -> 4; 8 sheetmetal + dye -> 8 coloured
        add(reg, "sheetmetal", new ShapedOreRecipe(null, new ItemStack(IqContent.sheetmetal, 4), "PP", "PP", 'P', "plateSteel"));
        String[] dyes = { "dyeWhite", "dyeOrange", "dyeMagenta", "dyeLightBlue", "dyeYellow", "dyeLime", "dyePink", "dyeGray", "dyeLightGray",
                "dyeCyan", "dyePurple", "dyeBlue", "dyeBrown", "dyeGreen", "dyeRed", "dyeBlack" };
        for (int i = 0; i < IqContent.COLORS.length; i++) {
            Block colored = IqContent.SHEET_COLORED.get(IqContent.COLORS[i]);
            add(reg, "sheetmetal_" + IqContent.COLORS[i], new ShapedOreRecipe(null, new ItemStack(colored, 8), "SSS", "SDS", "SSS", 'S', "blockSheetmetal", 'D', dyes[i]));
        }
        // plastic crate: polymer around a chest
        add(reg, "plastic_crate", new ShapedOreRecipe(null, new ItemStack(IqContent.plasticCrate), "PPP", "P P", "PPP", 'P', "ingotPolymer"));
        // coke blocks + powders
        add(reg, "coke_hemp_block", new ShapedOreRecipe(null, new ItemStack(IqContent.blockCokeHemp), "XXX", "XXX", "XXX", 'X', IqContent.cokeHemp));
        add(reg, "coke_haema_block", new ShapedOreRecipe(null, new ItemStack(IqContent.blockCokeHaemacoke), "XXX", "XXX", "XXX", 'X', IqContent.cokeHaemacoke));
        add(reg, "coke_hemp_unblock", new ShapelessOreRecipe(null, new ItemStack(IqContent.cokeHemp, 9), IqContent.blockCokeHemp));
        add(reg, "coke_haema_unblock", new ShapelessOreRecipe(null, new ItemStack(IqContent.cokeHaemacoke, 9), IqContent.blockCokeHaemacoke));
        // industrial pipette: steel plates around CE's boron pipette and a bucket
        Item boron = ForgeRegistries.ITEMS.getValue(new ResourceLocation("hbm", "pipette_boron"));
        if (boron != null)
            add(reg, "industrial_pipette", new ShapedOreRecipe(null, new ItemStack(IqContent.industrialPipette), "PXP", "PBP", 'P', "plateSteel", 'X', boron, 'B', Items.BUCKET));
    }

    private static void add(IForgeRegistry<IRecipe> reg, String name, IRecipe r) {
        reg.register(r.setRegistryName(new ResourceLocation(Tags.MODID, name)));
    }

    /** IQ change to HBM's BlockOre: HBM metal ores drop raw ore instead of themselves (silk touch still gives the ore). */
    public static class RawOreDrops {
        private static final Pattern ORE = Pattern.compile("^ore_(?:gneiss_|nether_|meteor_)?([a-z]+)$");

        @SubscribeEvent
        public void onDrops(BlockEvent.HarvestDropsEvent e) {
            if (e.isSilkTouching() || e.getWorld().isRemote) return;
            ResourceLocation id = e.getState().getBlock().getRegistryName();
            if (id == null || !"hbm".equals(id.getNamespace())) return;
            Matcher m = ORE.matcher(id.getPath());
            if (!m.matches()) return;
            Item raw = IqContent.RAW.get(m.group(1));
            if (raw == null) return;
            Item self = Item.getItemFromBlock(e.getState().getBlock());
            boolean replaced = false;
            for (int i = 0; i < e.getDrops().size(); i++) {
                ItemStack d = e.getDrops().get(i);
                if (d.getItem() == self) {
                    int bonus = e.getFortuneLevel() > 0 ? e.getWorld().rand.nextInt(e.getFortuneLevel() + 1) : 0;
                    e.getDrops().set(i, new ItemStack(raw, d.getCount() + bonus));
                    replaced = true;
                }
            }
            if (replaced) e.setDropChance(1.0F);
        }
    }
}
