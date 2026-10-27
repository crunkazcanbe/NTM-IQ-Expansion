package com.crunkazcanbe.ntmiq.contagion;

import java.util.Random;

import com.crunkazcanbe.ntmiq.Tags;
import com.crunkazcanbe.ntmiq.blood.IqBlood;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Wild disease syringes in dungeon and mineshaft chests (IQ seeds them through ChestGenHooks; 1.12.2 uses loot tables).
 * Each one rolls its own disease when the chest is opened, so no two finds are the same.
 */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class ContagionLoot {
    private ContagionLoot() {}

    private static final ResourceLocation FUNCTION = new ResourceLocation(Tags.MODID, "wild_disease");
    private static final ResourceLocation DUNGEON = new ResourceLocation("minecraft", "chests/simple_dungeon");
    private static final ResourceLocation MINESHAFT = new ResourceLocation("minecraft", "chests/abandoned_mineshaft");

    public static void register() {
        LootFunctionManager.registerFunction(new WildDiseaseFunction.Serializer());
    }

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent e) {
        if (!ContagionConfig.wildSyringeLoot) return;
        if (!DUNGEON.equals(e.getName()) && !MINESHAFT.equals(e.getName())) return;

        LootEntry entry = new LootEntryItem(IqBlood.medicalSyringe, 1, 0,
                new LootFunction[] { new WildDiseaseFunction(new LootCondition[0]) }, new LootCondition[0], Tags.MODID + ":wild_disease");
        e.getTable().addPool(new LootPool(new LootEntry[] { entry }, new LootCondition[0],
                new RandomValueRange(1), new RandomValueRange(0), Tags.MODID + "_wild_disease"));
    }

    /** turns the drawn syringe into a syringe of one freshly rolled disease */
    public static class WildDiseaseFunction extends LootFunction {

        public WildDiseaseFunction(LootCondition[] conditions) { super(conditions); }

        @Override
        public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
            return WildDisease.makeSyringe(rand);
        }

        public static class Serializer extends LootFunction.Serializer<WildDiseaseFunction> {
            public Serializer() { super(FUNCTION, WildDiseaseFunction.class); }

            @Override public void serialize(JsonObject json, WildDiseaseFunction value, JsonSerializationContext ctx) {}

            @Override
            public WildDiseaseFunction deserialize(JsonObject json, JsonDeserializationContext ctx, LootCondition[] conditions) {
                return new WildDiseaseFunction(conditions);
            }
        }
    }
}
