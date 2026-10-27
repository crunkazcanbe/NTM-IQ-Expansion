package com.crunkazcanbe.ntmiq.detail;

import com.crunkazcanbe.ntmiq.IqContent;
import com.crunkazcanbe.ntmiq.Tags;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** F20 from NTM: IQ Edition (irislgtm / tam69 / the IQ team, GPL-3): detail decals (ash, blood, rust, moss, oil ...). */
@Mod.EventBusSubscriber(modid = Tags.MODID)
public final class IqDetail {
    private IqDetail() {}

    public static Block detail;
    public static Item item;

    public static void preInit() {
        detail = new BlockDetail().setRegistryName(Tags.MODID, "detail").setTranslationKey("detail").setCreativeTab(IqContent.TAB);
        item = new ItemBlock(detail) {
            @Override public int getMetadata(int damage) { return damage & 15; }
            @Override public String getTranslationKey(ItemStack stack) {
                return "tile.detail." + BlockDetail.EnumDetailType.values()[stack.getMetadata() & 15].getName();
            }
        }.setHasSubtypes(true).setRegistryName(detail.getRegistryName());
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(detail);
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(item);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (BlockDetail.EnumDetailType t : BlockDetail.EnumDetailType.values())
            ModelLoader.setCustomModelResourceLocation(item, t.ordinal(), new ModelResourceLocation(Tags.MODID + ":detail_" + t.getName(), "inventory"));
    }
}
