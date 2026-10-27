package com.crunkazcanbe.ntmiq.drones;

import com.hbm.entity.item.EntityDroneBase;
import com.hbm.main.ResourceManager;
import com.hbm.render.entity.item.RenderDeliveryDrone;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Renderers for the F09 drones (NTM: IQ Edition RenderQuarryDrone / RenderSpelunkingDrone, GPL-3): CE's delivery-drone
 * OBJ with IQ's textures. Quarry drones draw at 2x size. Demolishers look like CE transport drones (as in IQ).
 */
@SideOnly(Side.CLIENT)
public final class DronesClient {
    private DronesClient() {}

    static final ResourceLocation QUARRY_TEX = new ResourceLocation("ntmiq", "textures/models/drones/drone_quarry.png");
    static final ResourceLocation SPELUNKER_TEX = new ResourceLocation("ntmiq", "textures/models/drones/drone_spelunker.png");

    public static void preInit() {
        RenderingRegistry.registerEntityRenderingHandler(EntityQuarryDrone.class, m -> new MiningDroneRenderer<>(m, QUARRY_TEX, 2.0));
        RenderingRegistry.registerEntityRenderingHandler(EntitySpelunkingDrone.class, m -> new MiningDroneRenderer<>(m, SPELUNKER_TEX, 1.0));
        RenderingRegistry.registerEntityRenderingHandler(EntityDemolisherDrone.class, RenderDeliveryDrone.FACTORY_DELIVERY);
    }

    public static class MiningDroneRenderer<T extends EntityDroneBase> extends Render<T> {
        private final ResourceLocation tex;
        private final double scale;

        MiningDroneRenderer(RenderManager m, ResourceLocation tex, double scale) {
            super(m);
            this.tex = tex;
            this.scale = scale;
        }

        @Override
        public void doRender(T e, double x, double y, double z, float yaw, float interp) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.disableCull();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            GlStateManager.scale(scale, scale, scale);
            bindTexture(tex);
            ResourceManager.delivery_drone.renderPart("Drone");
            if (e.getAppearance() == 1) ResourceManager.delivery_drone.renderPart("Crate");
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        }

        @Override
        protected ResourceLocation getEntityTexture(T e) {
            return tex;
        }
    }
}
