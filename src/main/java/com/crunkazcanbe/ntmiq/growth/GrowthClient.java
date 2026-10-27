package com.crunkazcanbe.ntmiq.growth;

import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.main.MainRegistry;
import com.hbm.main.client.NTMClientRegistry;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.WaveFrontObjectVAO;
import com.hbm.render.util.BeamPronter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/** Growth chamber TESR (base, recipe item, fluid column, glass, arcs) + item renderer. Ported from NTM: IQ Edition (GPL-3). */
@SideOnly(Side.CLIENT)
public final class GrowthClient {
    private GrowthClient() {}

    static final ResourceLocation TEX = new ResourceLocation("ntmiq", "textures/models/machines/growth_chamber.png");

    static final class Models {
        static final WaveFrontObjectVAO CHAMBER = new HFRWavefrontObject(new ResourceLocation("ntmiq", "models/machines/growth_chamber.obj")).asVBO();
    }

    public static void preInit() {
        // load OBJ models NOW: CE refuses new models after ResourceManager uploads them (lazy load crashed on first render, 10-16)
        Models.CHAMBER.hashCode();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityMachineGrowthChamber.class, new Renderer());
        NTMClientRegistry.bindTeisr(Item.getItemFromBlock(IqGrowth.growthChamber), new ItemRenderBase() {
            @Override public void renderInventory() {
                GlStateManager.translate(0, -2, 0);
                GlStateManager.scale(3, 3, 3);
            }

            @Override public void renderCommon() {
                Minecraft.getMinecraft().getTextureManager().bindTexture(TEX);
                Models.CHAMBER.renderAll();
            }
        });
    }

    static class Renderer extends TileEntitySpecialRenderer<TileEntityMachineGrowthChamber> {
        private static final Random RAND = new Random();
        private static final double[][] GLASS_WALLS = {
                { 1.375, 0.9375 }, { 1.375, -0.9375 }, { 0.9375, -1.375 }, { -0.9375, -1.375 },
                { -1.375, -0.9375 }, { -1.375, 0.9375 }, { -0.9375, 1.375 }, { 0.9375, 1.375 } };
        private static final double[][] PRONGS = {
                { 0.5, 1.0, 0 }, { -0.5, 1.0, 0 }, { 0, 1.0, 0.5 }, { 0, 1.0, -0.5 },
                { 0.5, 2.0, 0 }, { -0.5, 2.0, 0 }, { 0, 2.0, 0.5 }, { 0, 2.0, -0.5 } };

        @Override
        public boolean isGlobalRenderer(TileEntityMachineGrowthChamber te) {
            return true;
        }

        @Override
        public void render(TileEntityMachineGrowthChamber te, double x, double y, double z, float interp, int destroyStage, float alpha) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y, z + 0.5);
            GlStateManager.enableLighting();
            GlStateManager.enableCull();

            GlStateManager.pushMatrix();
            rotate(te.getBlockMetadata() - BlockDummyable.offset);
            bindTexture(TEX);
            Models.CHAMBER.renderPart("Base");
            GlStateManager.popMatrix();

            GenericRecipe recipe = te.module.getRecipe();
            if (recipe != null && MainRegistry.proxy.me().getDistanceSq(te.getPos()) < 35 * 35) renderRecipeItem(recipe.getIcon());

            renderFluid(te.tank);
            if (te.didProcess) renderLightning(te);

            // glass last, translucent, no depth write
            GlStateManager.pushMatrix();
            rotate(te.getBlockMetadata() - BlockDummyable.offset);
            bindTexture(TEX);
            GlStateManager.enableBlend();
            GlStateManager.disableCull();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.depthMask(false);
            Models.CHAMBER.renderPart("Glass");
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.popMatrix();

            GlStateManager.popMatrix();
        }

        private static void rotate(int dir) {
            switch (dir) {
                case 2: GlStateManager.rotate(90, 0, 1, 0); break;
                case 3: GlStateManager.rotate(270, 0, 1, 0); break;
                case 4: GlStateManager.rotate(180, 0, 1, 0); break;
                default: break;
            }
        }

        private void renderRecipeItem(ItemStack icon) {
            if (icon == null || icon.isEmpty()) return;
            ItemStack stack = icon.copy();
            stack.setCount(1);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 1.5, 0);
            GlStateManager.rotate(-rendererDispatcher.entityYaw, 0, 1, 0);
            GlStateManager.scale(0.8, 0.8, 0.8);
            Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
            GlStateManager.popMatrix();
        }

        private void renderFluid(FluidTankNTM tank) {
            if (tank.getTankType() == Fluids.NONE || tank.getFill() <= 0) return;
            double inset = 0.95, radius = 1.375 * inset, bottom = 0.5, top = 2.49;
            double height = bottom + (top - bottom) * tank.getFill() / tank.getMaxFill();
            int color = tank.getTankType().getColor();
            float r = ((color >> 16) & 0xFF) / 255F, g = ((color >> 8) & 0xFF) / 255F, b = (color & 0xFF) / 255F;

            bindTexture(tank.getTankType().getTexture());
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.depthMask(false);

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            int n = GLASS_WALLS.length;
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (int i = 0; i < n; i++) {
                double[] a = GLASS_WALLS[i], c = GLASS_WALLS[(i + 1) % n];
                double ax = a[0] * inset, az = a[1] * inset, cx = c[0] * inset, cz = c[1] * inset;
                buf.pos(ax, bottom, az).tex(0, 1).color(r, g, b, 0.75F).endVertex();
                buf.pos(cx, bottom, cz).tex(1, 1).color(r, g, b, 0.75F).endVertex();
                buf.pos(cx, height, cz).tex(1, 0).color(r, g, b, 0.75F).endVertex();
                buf.pos(ax, height, az).tex(0, 0).color(r, g, b, 0.75F).endVertex();
            }
            tess.draw();
            buf.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (double[] p : GLASS_WALLS) {
                double px = p[0] * inset, pz = p[1] * inset;
                buf.pos(px, height, pz).tex((px + radius) / (2 * radius), (pz + radius) / (2 * radius)).color(r, g, b, 0.75F).endVertex();
            }
            tess.draw();

            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.color(1, 1, 1, 1);
        }

        private void renderLightning(TileEntityMachineGrowthChamber te) {
            long time = System.currentTimeMillis();
            int seed = (int) (time / 50);
            for (int i = 0; i < PRONGS.length; i++) {
                RAND.setSeed(time / 200 * 31 + i + te.getPos().getX() * 7L + te.getPos().getZ() * 13L);
                if (RAND.nextInt(3) != 0) continue;
                double[] p = PRONGS[i];
                Vec3d toItem = new Vec3d(-p[0], 1.5 - p[1], -p[2]);
                GlStateManager.pushMatrix();
                GlStateManager.disableLighting();
                GlStateManager.translate(p[0], p[1], p[2]);
                BeamPronter.prontBeam(toItem, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x404040, 0x002040, seed + i, 8, 0.0625F, 3, 0.025F);
                BeamPronter.prontBeam(toItem, BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID, 0x404040, 0x002040, seed + i, 1, 0F, 3, 0.025F);
                GlStateManager.enableLighting();
                GlStateManager.popMatrix();
            }
            GlStateManager.enableCull();
            GlStateManager.color(1, 1, 1, 1);
        }
    }
}
