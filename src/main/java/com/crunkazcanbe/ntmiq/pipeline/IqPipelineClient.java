package com.crunkazcanbe.ntmiq.pipeline;

import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.blocks.BlockDummyable;
import com.hbm.main.client.NTMClientRegistry;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.WaveFrontObjectVAO;
import com.hbm.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** Client side of F17: IQ's RenderPipeAnchorIndustrial / RenderPipeAnchorPressurizer (models + textures from IQ). */
@SideOnly(Side.CLIENT)
public final class IqPipelineClient {
    private IqPipelineClient() {}

    static final ResourceLocation ANCHOR_TEX = new ResourceLocation(Tags.MODID, "textures/models/network/large_pipe_anchor.png");
    static final ResourceLocation PRESS_TEX = new ResourceLocation(Tags.MODID, "textures/models/network/pressurizer.png");
    private static WaveFrontObjectVAO anchorModel, pressModel;

    // loaded on first draw: the resource manager has no mod assets yet during preInit
    static WaveFrontObjectVAO anchor() {
        if (anchorModel == null) anchorModel = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/network/large_pipe_anchor.obj")).asVBO();
        return anchorModel;
    }

    static WaveFrontObjectVAO press() {
        if (pressModel == null) pressModel = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/network/pressurizer.obj")).asVBO();
        return pressModel;
    }

    /** called in preInit, before CE's ModelRegistryEvent handler wires the TEISR model locations */
    static void bind() {
        // load OBJ models NOW: CE refuses new models after ResourceManager uploads them (lazy load crashed on first render, 10-16)
        anchor(); press();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPipeAnchorIndustrial.class, new AnchorRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPipeAnchorPressurizer.class, new PressRenderer());
        NTMClientRegistry.bindTeisr(IqPipeline.anchorItem, new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.translate(0, -8.0, 0); GlStateManager.scale(6, 6, 6); }
            @Override public void renderCommon() {
                GlStateManager.rotate(90, 0, 1, 0);
                GlStateManager.scale(0.75, 0.75, 0.75);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                Minecraft.getMinecraft().getTextureManager().bindTexture(ANCHOR_TEX);
                anchor().renderPart("mesh");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        });
        NTMClientRegistry.bindTeisr(IqPipeline.pressurizerItem, new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.translate(0, -3.0, 0); GlStateManager.scale(6, 6, 6); }
            @Override public void renderCommon() {
                GlStateManager.rotate(90, 0, 1, 0);
                GlStateManager.scale(0.75, 0.75, 0.75);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                Minecraft.getMinecraft().getTextureManager().bindTexture(PRESS_TEX);
                press().renderAll();
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        });
    }

    static int facing(TileEntity te) {
        return te.getBlockMetadata() - BlockDummyable.offset;
    }

    static class AnchorRenderer extends TileEntitySpecialRenderer<TileEntityPipeAnchorIndustrial> {
        @Override
        public void render(TileEntityPipeAnchorIndustrial anchor, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
            Vec3d mount = anchor.getMountPos();
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5D + mount.x, y + mount.y, z + 0.5D + mount.z);
            GlStateManager.enableLighting();
            GlStateManager.disableCull();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);

            GlStateManager.pushMatrix();
            switch (facing(anchor)) {
                case 3: GlStateManager.rotate(180, 0, 1, 0); break;
                case 4: GlStateManager.rotate(90, 0, 1, 0); break;
                case 5: GlStateManager.rotate(270, 0, 1, 0); break;
                default: break;
            }
            GlStateManager.translate(0, -2.0D, 0);
            bindTexture(ANCHOR_TEX);
            anchor().renderPart("mesh");
            GlStateManager.popMatrix();

            for (int[] c : anchor.getConnected()) {
                TileEntity tile = anchor.getWorld().getTileEntity(new net.minecraft.util.math.BlockPos(c[0], c[1], c[2]));
                if (!(tile instanceof TileEntityPipeAnchorIndustrial other) || anchor.getType() != other.getType()) continue;
                Vec3d a = anchor.getConnectionPoint();
                Vec3d b = other.getConnectionPoint();
                if (!isDominant(a, b)) continue;   // only one of the two anchors draws the pipe

                double dX = b.x - a.x, dY = b.y - a.y, dZ = b.z - a.z;
                double hyp = Math.sqrt(dX * dX + dZ * dZ);
                double yaw = Math.toDegrees(Math.atan2(dX, dZ));
                double pitch = Math.toDegrees(Math.atan2(dY, hyp));
                double length = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

                GlStateManager.pushMatrix();
                GlStateManager.translate(0, 0.25D, 0);
                GlStateManager.rotate((float) yaw, 0, 1, 0);
                GlStateManager.rotate((float) (90 - pitch), 1, 0, 0);

                GlStateManager.pushMatrix();
                GlStateManager.scale(1, length / 2.75D, 1);
                GlStateManager.translate(0, -3, 0);
                int color = ColorUtil.lightenColor(anchor.getType().getColor(), 0.25D);
                GlStateManager.color(ColorUtil.fr(color), ColorUtil.fg(color), ColorUtil.fb(color));
                anchor().renderPart("pipe");
                GlStateManager.color(1F, 1F, 1F);
                GlStateManager.popMatrix();

                GlStateManager.pushMatrix();
                GlStateManager.translate(0, length / 2D - 5.875D, 0);
                anchor().renderPart("ring");
                GlStateManager.popMatrix();

                GlStateManager.popMatrix();
            }

            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        }

        static boolean isDominant(Vec3d first, Vec3d second) {
            if (first.x != second.x) return first.x < second.x;
            if (first.y != second.y) return first.y < second.y;
            return first.z < second.z;
        }
    }

    static class PressRenderer extends TileEntitySpecialRenderer<TileEntityPipeAnchorPressurizer> {
        @Override
        public void render(TileEntityPipeAnchorPressurizer te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
            GlStateManager.pushMatrix();
            int f = facing(te);
            switch (f) {
                case 3: GlStateManager.translate(x, y, z); break;
                case 4: GlStateManager.translate(x + 1.0D, y, z); break;
                case 5: GlStateManager.translate(x, y, z + 1.0D); break;
                default: GlStateManager.translate(x + 1.0D, y, z + 1.0D); break;
            }
            GlStateManager.enableLighting();
            GlStateManager.disableCull();
            switch (f) {
                case 3: GlStateManager.rotate(180, 0, 1, 0); break;
                case 4: GlStateManager.rotate(90, 0, 1, 0); break;
                case 5: GlStateManager.rotate(270, 0, 1, 0); break;
                default: break;
            }
            bindTexture(PRESS_TEX);
            press().renderAll();
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        }
    }
}
