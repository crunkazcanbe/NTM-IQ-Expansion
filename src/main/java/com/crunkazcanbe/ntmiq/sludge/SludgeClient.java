package com.crunkazcanbe.ntmiq.sludge;

import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.blocks.BlockDummyable;
import com.hbm.main.client.NTMClientRegistry;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.WaveFrontObjectVAO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** IQ's RenderSludgeProcessor. IQ's OBJ only has the "Base" part (its "Fan" part call drew nothing), so only Base is drawn. */
@SideOnly(Side.CLIENT)
public final class SludgeClient {
    private SludgeClient() {}

    static final ResourceLocation TEX = new ResourceLocation(Tags.MODID, "textures/models/machines/sludge_processor.png");
    private static WaveFrontObjectVAO model;

    static WaveFrontObjectVAO model() {
        if (model == null) model = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/machines/sludge_processor.obj")).asVBO();
        return model;
    }

    static void bind() {
        // load OBJ models NOW: CE refuses new models after ResourceManager uploads them (lazy load crashed on first render, 10-16)
        model();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySludgeProcessor.class, new Renderer());
        NTMClientRegistry.bindTeisr(IqSludge.item, new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.translate(0, -2.5, 0); GlStateManager.scale(2.5, 2.5, 2.5); }
            @Override public void renderCommon() {
                GlStateManager.scale(0.75, 0.75, 0.75);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                Minecraft.getMinecraft().getTextureManager().bindTexture(TEX);
                model().renderPart("Base");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        });
    }

    static class Renderer extends TileEntitySpecialRenderer<TileEntitySludgeProcessor> {
        @Override
        public void render(TileEntitySludgeProcessor te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y, z + 0.5);
            GlStateManager.rotate(90, 0, 1, 0);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GlStateManager.disableCull();
            GlStateManager.enableLighting();
            switch (te.getBlockMetadata() - BlockDummyable.offset) {
                case 4: GlStateManager.rotate(90, 0, 1, 0); break;
                case 3: GlStateManager.rotate(180, 0, 1, 0); break;
                case 5: GlStateManager.rotate(270, 0, 1, 0); break;
                default: break;
            }
            bindTexture(TEX);
            model().renderPart("Base");
            GlStateManager.enableCull();
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.popMatrix();
        }
    }
}
