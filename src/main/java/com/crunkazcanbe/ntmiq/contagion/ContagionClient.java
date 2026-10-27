package com.crunkazcanbe.ntmiq.contagion;

import com.crunkazcanbe.ntmiq.blood.ItemFluidSyringe;
import com.crunkazcanbe.ntmiq.blood.IqPotions;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.main.client.NTMClientRegistry;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.WaveFrontObjectVAO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Client side of F02: the Dial-A-Dose model, item models and tints, and IQ's symptom screen overlays — the full-screen
 * washes that tell you something is wrong with you (RenderScreenOverlay.renderSymptomOverlay, GPL-3).
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = com.crunkazcanbe.ntmiq.Tags.MODID)
public final class ContagionClient {
    private ContagionClient() {}

    static final ResourceLocation TEX = new ResourceLocation("ntmiq", "textures/models/machines/dial_a_drug.png");

    static final class Models {
        static final WaveFrontObjectVAO DIAL = new HFRWavefrontObject(new ResourceLocation("ntmiq", "models/machines/dial_a_drug.obj")).asVBO();
    }

    public static void preInit() {
        // load OBJ models NOW: CE refuses new models after ResourceManager uploads them (lazy load crashed on first render, 10-16)
        Models.DIAL.hashCode();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityMedicineSynthesizer.class, new Renderer());
        NTMClientRegistry.bindTeisr(Item.getItemFromBlock(IqContagion.synthesizer), new ItemRenderBase() {
            @Override public void renderInventory() {
                GlStateManager.translate(0, -1.5, 0);
                GlStateManager.scale(2.5, 2.5, 2.5);
            }

            @Override public void renderCommon() {
                Minecraft.getMinecraft().getTextureManager().bindTexture(TEX);
                Models.DIAL.renderAll();
            }
        });
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (Item i : IqContagion.items())
            ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
    }

    /** layer 1 of the vial is the fluid inside it */
    @SubscribeEvent
    public static void colors(ColorHandlerEvent.Item e) {
        e.getItemColors().registerItemColorHandler((stack, tint) -> {
            if (tint != 1 || ItemFluidSyringe.fill(stack) <= 0) return 0xFFFFFF;
            FluidType t = ItemFluidSyringe.type(stack);
            return t.getColor() < 0 ? 0xFFFFFF : t.getColor();
        }, IqContagion.vial);
    }

    // ---------------- symptom overlays ----------------

    @SubscribeEvent
    public static void overlay(RenderGameOverlayEvent.Post e) {
        if (e.getType() != ElementType.HOTBAR) return;
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        float pulse = 0.5F + 0.5F * (float) Math.sin(player.ticksExisted / 10.0D);
        draw(e.getResolution(), player, IqPotions.symptomRash, "overlay_symptom_hemorrhage", 0.2F + 0.1F * pulse);
        draw(e.getResolution(), player, IqPotions.symptomHemorrhage, "overlay_symptom_hemorrhage", 0.35F);
        draw(e.getResolution(), player, IqPotions.symptomSeptic, "overlay_symptom_septic", 0.3F + 0.2F * pulse);
        draw(e.getResolution(), player, IqPotions.symptomAortic, "overlay_symptom_aortic", 0.4F + 0.4F * pulse);
        draw(e.getResolution(), player, IqPotions.symptomComa, "overlay_symptom_blackout", 0.75F);
        draw(e.getResolution(), player, IqPotions.symptomCardiac, "overlay_symptom_blackout", 0.75F);
        draw(e.getResolution(), player, IqPotions.symptomFever, "overlay_symptom_fever", 0.2F);
        draw(e.getResolution(), player, IqPotions.symptomVomit, "overlay_symptom_vomit", 0.25F);
        draw(e.getResolution(), player, IqPotions.symptomSchizophrenia, "overlay_symptom_schizophrenia", 0.3F);
        draw(e.getResolution(), player, IqPotions.symptomSeizure, "overlay_symptom_schizophrenia", 0.3F);
    }

    private static void draw(ScaledResolution res, EntityPlayer player, Potion potion, String texture, float alpha) {
        if (potion == null || !player.isPotionActive(potion)) return;

        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("ntmiq", "textures/misc/" + texture + ".png"));
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlpha();
        GlStateManager.color(1F, 1F, 1F, alpha);

        int w = res.getScaledWidth(), h = res.getScaledHeight();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buf.pos(0, h, -90).tex(0, 1).endVertex();
        buf.pos(w, h, -90).tex(1, 1).endVertex();
        buf.pos(w, 0, -90).tex(1, 0).endVertex();
        buf.pos(0, 0, -90).tex(0, 0).endVertex();
        tess.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    static class Renderer extends TileEntitySpecialRenderer<TileEntityMedicineSynthesizer> {

        @Override
        public void render(TileEntityMedicineSynthesizer te, double x, double y, double z, float interp, int destroyStage, float alpha) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y, z + 0.5);
            GlStateManager.enableLighting();
            GlStateManager.disableCull();
            switch (te.getBlockMetadata() - BlockDummyable.offset) {
                case 2: GlStateManager.rotate(90, 0, 1, 0); break;
                case 3: GlStateManager.rotate(270, 0, 1, 0); break;
                case 4: GlStateManager.rotate(180, 0, 1, 0); break;
                default: break;
            }
            bindTexture(TEX);
            Models.DIAL.renderAll();
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        }
    }
}
