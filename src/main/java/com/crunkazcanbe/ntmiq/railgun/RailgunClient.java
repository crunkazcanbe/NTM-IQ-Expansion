package com.crunkazcanbe.ntmiq.railgun;

import com.hbm.main.ResourceManager;
import com.hbm.main.client.NTMClientRegistry;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.WaveFrontObjectVAO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** Client rendering for Louis: turret TESR, sabot entity and both item renderers. Ported from NTM: IQ Edition (GPL-3). */
@SideOnly(Side.CLIENT)
public final class RailgunClient {
    private RailgunClient() {}

    static final ResourceLocation TURRET_TEX = new ResourceLocation("ntmiq", "textures/models/turrets/railgun.png");

    /** OBJs load on first use (the resource manager isn't ready during preInit) through CE's wavefront loader */
    static final class Models {
        static final WaveFrontObjectVAO TURRET = new HFRWavefrontObject(new ResourceLocation("ntmiq", "models/turrets/turret_railgun.obj")).asVBO();
        static final WaveFrontObjectVAO SABOT = new HFRWavefrontObject(new ResourceLocation("ntmiq", "models/projectiles/sabot.obj")).asVBO();
    }

    public static void preInit() {
        // load OBJ models NOW: CE refuses new models after ResourceManager uploads them (lazy load crashed on first render, 10-16)
        Models.TURRET.hashCode(); Models.SABOT.hashCode();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTurretRailgun.class, new TurretRenderer());
        RenderingRegistry.registerEntityRenderingHandler(EntityRailgunProjectile.class, SabotRenderer::new);
        // CE wraps these into its TEISR model system during ModelBakeEvent
        NTMClientRegistry.bindTeisr(Item.getItemFromBlock(IqRailgun.turret), new TurretItemRenderer());
        NTMClientRegistry.bindTeisr(IqRailgun.ammo, new SabotItemRenderer());
    }

    static void renderTurretModel(double pitchDeg) {
        Models.TURRET.renderPart("Base");
        Models.TURRET.renderPart("Mount");
        GlStateManager.translate(0, 2.25, 0);
        GlStateManager.rotate((float) pitchDeg, 0, 0, 1);
        GlStateManager.translate(0, -2.25, 0);
        Models.TURRET.renderPart("Barrel");
    }

    public static class TurretRenderer extends TileEntitySpecialRenderer<TileEntityTurretRailgun> {
        @Override
        public boolean isGlobalRenderer(TileEntityTurretRailgun te) {
            return true;
        }

        @Override
        public void render(TileEntityTurretRailgun te, double x, double y, double z, float interp, int destroyStage, float alpha) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y, z + 0.5);
            GlStateManager.enableLighting();
            GlStateManager.enableCull();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            bindTexture(TURRET_TEX);

            double yaw = Math.toDegrees(te.lastRotationYaw + (te.rotationYaw - te.lastRotationYaw) * interp);
            double pitch = Math.toDegrees(te.lastRotationPitch + (te.rotationPitch - te.lastRotationPitch) * interp);

            Models.TURRET.renderPart("Base");
            GlStateManager.rotate((float) yaw, 0, 1, 0);
            Models.TURRET.renderPart("Mount");
            GlStateManager.translate(0, 2.25, 0);
            GlStateManager.rotate((float) pitch, 0, 0, 1);
            GlStateManager.translate(0, -2.25, 0);
            Models.TURRET.renderPart("Barrel");

            if (te.charge > 0L) renderChargeGlow(te);

            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.popMatrix();
        }

        /** IQ's glowing blue box inside the barrel, brighter as the capacitor bank fills */
        private static void renderChargeGlow(TileEntityTurretRailgun te) {
            GlStateManager.translate(0.09375, 0, 0);
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.disableAlpha();

            float a = (float) Math.log10((double) te.charge / (double) te.maxCharge * 9D + 1D) * (float) (Math.random() * 0.2D + 0.8D);
            double dl = 15D / 29D, ds = 0.25, dh = 0.6875, mid = 2.25;
            double lo = mid - dh, hi = mid + dh;
            double[][] quads = {
                    { -dl, lo, -ds, -dl, hi, -ds, dl, hi, -ds, dl, lo, -ds },
                    { -dl, hi, -ds, -dl, hi, ds, dl, hi, ds, dl, hi, -ds },
                    { dl, lo, -ds, dl, hi, -ds, dl, hi, ds, dl, lo, ds },
                    { -dl, lo, -ds, -dl, lo, ds, -dl, hi, ds, -dl, hi, -ds },
                    { -dl, lo, ds, dl, lo, ds, dl, hi, ds, -dl, hi, ds },
                    { -dl, lo, -ds, dl, lo, -ds, dl, lo, ds, -dl, lo, ds },
            };
            BufferBuilder buf = Tessellator.getInstance().getBuffer();
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (double[] q : quads)
                for (int i = 0; i < 12; i += 3) buf.pos(q[i], q[i + 1], q[i + 2]).color(0.4F, 0.9F, 1.0F, a).endVertex();
            Tessellator.getInstance().draw();

            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
        }
    }

    public static class TurretItemRenderer extends ItemRenderBase {
        @Override
        public void renderInventory() {
            GlStateManager.translate(0, -1.5, 0);
            GlStateManager.scale(4, 4, 4);
        }

        @Override
        public void renderCommon() {
            GlStateManager.scale(0.6, 0.6, 0.6);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            Minecraft.getMinecraft().getTextureManager().bindTexture(TURRET_TEX);
            renderTurretModel(45);
            GlStateManager.shadeModel(GL11.GL_FLAT);
        }
    }

    public static class SabotItemRenderer extends ItemRenderBase {
        @Override
        public void renderInventory() {
            GlStateManager.translate(0, 3, 0);
            GlStateManager.scale(7.5, 7.5, 7.5);
        }

        @Override
        public void renderCommon(ItemStack stack) {
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            Minecraft.getMinecraft().getTextureManager().bindTexture(ItemAmmoRailgun.type(stack.getMetadata()).texture);
            GlStateManager.rotate(-45, 1, 1, 0);
            GlStateManager.rotate(System.currentTimeMillis() / 25 % 360, 0, -1, 0);
            Models.SABOT.renderAll();
            GlStateManager.shadeModel(GL11.GL_FLAT);
        }
    }

    public static class SabotRenderer extends Render<EntityRailgunProjectile> {
        public SabotRenderer(RenderManager manager) {
            super(manager);
        }

        @Override
        public void doRender(EntityRailgunProjectile e, double x, double y, double z, float yaw, float interp) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * interp - 90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * interp - 90.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(1.5F, 1.5F, 1.5F);
            bindEntityTexture(e);
            boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
            if (fog) GlStateManager.disableFog();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            Models.SABOT.renderPart("Sabot");
            if (!e.sabotSeparated()) Models.SABOT.renderPart("Jacket");
            GlStateManager.shadeModel(GL11.GL_FLAT);
            if (fog) GlStateManager.enableFog();
            GlStateManager.popMatrix();
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityRailgunProjectile e) {
            return ResourceManager.flechette_tex;
        }
    }
}
