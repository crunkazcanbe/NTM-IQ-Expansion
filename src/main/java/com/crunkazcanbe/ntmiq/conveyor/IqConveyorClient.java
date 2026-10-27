package com.crunkazcanbe.ntmiq.conveyor;

import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.blocks.BlockDummyable;
import com.hbm.main.client.NTMClientRegistry;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.WaveFrontObjectVAO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** IQ's RenderConveyorCombinator / RenderConveyorSpraypainter (models + textures from IQ), scrolling belt like CE's conveyor press. */
@SideOnly(Side.CLIENT)
public final class IqConveyorClient {
    private IqConveyorClient() {}

    static final ResourceLocation COMB_TEX = tex("conveyor_combinator"), COMB_BELT = tex("conveyor_combinator_belt");
    static final ResourceLocation SPRAY_TEX = tex("conveyor_spraypainter"), SPRAY_BELT = tex("conveyor_spraypainter_belt");
    private static WaveFrontObjectVAO combModel, sprayModel;

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(Tags.MODID, "textures/models/machines/" + name + ".png");
    }

    // loaded on first draw: the resource manager has no mod assets yet during preInit
    static WaveFrontObjectVAO comb() {
        if (combModel == null) combModel = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/machines/conveyor_combinator.obj")).asVBO();
        return combModel;
    }

    static WaveFrontObjectVAO spray() {
        if (sprayModel == null) sprayModel = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/machines/conveyor_spraypainter.obj")).asVBO();
        return sprayModel;
    }

    static void bind() {
        // load OBJ models NOW: CE refuses new models after ResourceManager uploads them (lazy load crashed on first render, 10-16)
        comb(); spray();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityConveyorCombinator.class, new CombRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityConveyorSpraypainter.class, new SprayRenderer());
        NTMClientRegistry.bindTeisr(IqConveyor.combinatorItem, new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.translate(0, -2.1, 0); GlStateManager.scale(4.5, 4.5, 4.5); }
            @Override public void renderCommon() {
                GlStateManager.scale(1.5, 1.5, 1.5);
                bindTex(COMB_TEX); comb().renderPart("body"); comb().renderPart("press_head");
                bindTex(COMB_BELT); comb().renderPart("conveyor");
            }
        });
        NTMClientRegistry.bindTeisr(IqConveyor.spraypainterItem, new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.translate(0, -4, 0); GlStateManager.scale(4.5, 4.5, 4.5); }
            @Override public void renderCommon() {
                bindTex(SPRAY_TEX); spray().renderPart("mesh");
                bindTex(SPRAY_BELT); spray().renderPart("conveyor");
            }
        });
    }

    private static void bindTex(ResourceLocation tex) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
    }

    static void rotate(TileEntity te) {
        switch (te.getBlockMetadata() - BlockDummyable.offset) {
            case 2: GlStateManager.rotate(90, 0F, 1F, 0F); break;
            case 4: GlStateManager.rotate(180, 0F, 1F, 0F); break;
            case 3: GlStateManager.rotate(270, 0F, 1F, 0F); break;
            default: break;
        }
    }

    /** draws a belt part with its texture scrolled by world time */
    static void belt(TileEntity te, WaveFrontObjectVAO model) {
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.loadIdentity();
        int ticks = (int) (te.getWorld().getTotalWorldTime() % 16) - 2;
        GlStateManager.translate(0, ticks / 16D, 0);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        model.renderPart("conveyor");
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.loadIdentity();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    }

    static class CombRenderer extends TileEntitySpecialRenderer<TileEntityConveyorCombinator> {
        @Override
        public void render(TileEntityConveyorCombinator te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y, z + 0.5);
            GlStateManager.enableLighting();
            GlStateManager.enableCull();
            rotate(te);
            bindTexture(COMB_TEX);
            comb().renderPart("body");
            GlStateManager.pushMatrix();
            double piston = te.lastHammer + (te.renderHammer - te.lastHammer) * partialTicks;
            GlStateManager.translate(0, -piston * 0.25, 0);
            comb().renderPart("press_head");
            GlStateManager.popMatrix();
            bindTexture(COMB_BELT);
            belt(te, comb());
            GlStateManager.popMatrix();
        }
    }

    static class SprayRenderer extends TileEntitySpecialRenderer<TileEntityConveyorSpraypainter> {
        @Override
        public void render(TileEntityConveyorSpraypainter te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y, z + 0.5);
            GlStateManager.enableLighting();
            GlStateManager.enableCull();
            rotate(te);
            bindTexture(SPRAY_TEX);
            spray().renderPart("mesh");
            bindTexture(SPRAY_BELT);
            belt(te, spray());
            GlStateManager.popMatrix();
        }
    }
}
