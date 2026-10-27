package com.crunkazcanbe.ntmiq.nest;

import com.hbm.render.entity.mob.RenderGlyphid;
import com.hbm.render.entity.mob.RenderGlyphidNuclear;
import net.minecraft.block.BlockFire;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** The Nest's item models, bloatfire state mapper and the nether glyphid renderers (CE's, with IQ's nether skins). */
@Mod.EventBusSubscriber(modid = "ntmiq", value = Side.CLIENT)
public final class NestClient {
    private NestClient() {}

    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (Item i : IqNest.ITEMS) ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        ModelLoader.setCustomStateMapper(IqNest.bloatfire, new StateMap.Builder()
                .ignore(BlockFire.AGE, BlockFire.NORTH, BlockFire.EAST, BlockFire.SOUTH, BlockFire.WEST, BlockFire.UPPER).build());

        RenderingRegistry.registerEntityRenderingHandler(NetherGlyphids.Grunt.class, RenderGlyphid.FACTORY);
        RenderingRegistry.registerEntityRenderingHandler(NetherGlyphids.Scout.class, RenderGlyphid.FACTORY);
        RenderingRegistry.registerEntityRenderingHandler(NetherGlyphids.Bombardier.class, RenderGlyphid.FACTORY);
        RenderingRegistry.registerEntityRenderingHandler(NetherGlyphids.Brawler.class, RenderGlyphid.FACTORY);
        RenderingRegistry.registerEntityRenderingHandler(NetherGlyphids.Digger.class, RenderGlyphid.FACTORY);
        RenderingRegistry.registerEntityRenderingHandler(NetherGlyphids.Blaster.class, RenderGlyphid.FACTORY);
        RenderingRegistry.registerEntityRenderingHandler(NetherGlyphids.Behemoth.class, RenderGlyphid.FACTORY);
        RenderingRegistry.registerEntityRenderingHandler(NetherGlyphids.Brenda.class, RenderGlyphid.FACTORY);
        RenderingRegistry.registerEntityRenderingHandler(NetherGlyphids.Nuclear.class, RenderGlyphidNuclear.FACTORY);
    }
}
