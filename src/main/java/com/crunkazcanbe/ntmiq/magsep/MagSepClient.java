package com.crunkazcanbe.ntmiq.magsep;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.client.NTMClientRegistry;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.WaveFrontObjectVAO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** Magnetic separator TESR (drum close/spin/open animation), item renderer and the tinted disc. Ported from NTM: IQ Edition (GPL-3). */
@SideOnly(Side.CLIENT)
public final class MagSepClient {
    private MagSepClient() {}

    static final ResourceLocation TEX = new ResourceLocation("ntmiq", "textures/models/machines/magnetic_separator.png");

    static final class Models {
        static final WaveFrontObjectVAO SEP = new HFRWavefrontObject(new ResourceLocation("ntmiq", "models/machines/magnetic_separator.obj")).asVBO();
    }

    public static void preInit() {
        // load OBJ models NOW: CE refuses new models after ResourceManager uploads them (lazy load crashed on first render, 10-16)
        Models.SEP.hashCode();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityMachineMagneticSeparator.class, new Renderer());
        NTMClientRegistry.bindTeisr(Item.getItemFromBlock(IqMagSep.separator), new ItemRenderBase() {
            @Override public void renderInventory() {
                GlStateManager.translate(0, -1.5, 0);
                GlStateManager.scale(2.5, 2.5, 2.5);
            }

            @Override public void renderCommon() {
                GlStateManager.scale(0.9, 0.9, 0.9);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                Minecraft.getMinecraft().getTextureManager().bindTexture(TEX);
                Models.SEP.renderPart("base");
                Models.SEP.renderPart("hull");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        });
        MinecraftForge.EVENT_BUS.register(MagSepClient.class);
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (ItemMagneticDisc.EnumMagneticDisc t : ItemMagneticDisc.EnumMagneticDisc.values())
            ModelLoader.setCustomModelResourceLocation(IqMagSep.disc, t.ordinal(), new ModelResourceLocation(IqMagSep.disc.getRegistryName(), "inventory"));
    }

    @SubscribeEvent
    public static void colors(ColorHandlerEvent.Item e) {
        e.getItemColors().registerItemColorHandler((stack, tint) -> tint == 1 ? ItemMagneticDisc.getType(stack).color : 0xFFFFFF, IqMagSep.disc);
    }

    static class Renderer extends TileEntitySpecialRenderer<TileEntityMachineMagneticSeparator> {
        private static final int[] OPEN = TileEntityMachineMagneticSeparator.OPENING_ANIMATION_TICKS;
        private static final int[] CLOSE = TileEntityMachineMagneticSeparator.CLOSING_ANIMATION_TICKS;
        private static final float MULT = TileEntityMachineMagneticSeparator.ROTATION_MULTIPLIER;

        @Override
        public boolean isGlobalRenderer(TileEntityMachineMagneticSeparator te) {
            return true;
        }

        @Override
        public void render(TileEntityMachineMagneticSeparator sep, double x, double y, double z, float pt, int destroyStage, float alpha) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
            GlStateManager.enableLighting();
            GlStateManager.disableCull();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            switch (sep.getBlockMetadata() - BlockDummyable.offset) {
                case 2: GlStateManager.rotate(90, 0F, 1F, 0F); break;
                case 4: GlStateManager.rotate(180, 0F, 1F, 0F); break;
                case 3: GlStateManager.rotate(270, 0F, 1F, 0F); break;
                default: break;
            }
            bindTexture(TEX);
            Models.SEP.renderPart("base");
            Models.SEP.renderPart("innercover");

            float sink = 0F, spin = 0F;
            if (sep.open) {
                if (sep.animationTicks == 0) {
                    sink = 180F;
                } else if (sep.animRotation > 0) {
                    float a = sep.animRotation < OPEN[2] ? sep.animRotation + pt : sep.animRotation;
                    sink = inExpo(a / OPEN[2]) * 180F;
                    spin = sep.rotation * MULT;
                } else {
                    float a = sep.animAcceleration < OPEN[0] ? sep.animAcceleration + pt : sep.animAcceleration;
                    spin = outQuart(a / OPEN[0]) * 360F + sep.rotation * MULT;
                }
            } else {
                if (sep.animationTicks == 0) {
                    spin = (sep.rotation + pt) * MULT;
                } else if (sep.animAcceleration > 0) {
                    float a = sep.animAcceleration < CLOSE[2] ? sep.animAcceleration + pt : sep.animAcceleration;
                    float extra = sep.animAcceleration < CLOSE[2] ? sep.rotation : sep.rotation + pt;
                    spin = inQuart(a / CLOSE[2]) * 360F + extra * MULT;
                } else if (sep.animRotation > 0) {
                    float a = sep.animRotation < CLOSE[1] ? sep.animRotation + pt : sep.animRotation;
                    sink = 180F - inExpo(a / CLOSE[1]) * 180F;
                    spin = sep.rotation * MULT;
                } else {
                    sink = 180F;
                }
            }

            GlStateManager.pushMatrix();
            if (sink != 0F) GlStateManager.rotate(sink, 1F, 0F, 0F);
            if (sink > 0F) Models.SEP.renderPart("outer_cover");
            if (sink < 180F) {
                if (spin != 0F) GlStateManager.rotate(spin, 0F, 1F, 0F);
                Models.SEP.renderPart("hull");
            }
            GlStateManager.popMatrix();

            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        }

        private static float inExpo(float x) { return x == 0 ? 0 : (float) Math.pow(2, 10 * x - 10); }
        private static float outQuart(float x) { return (float) (1 - Math.pow(1 - x, 4)); }
        private static float inQuart(float x) { return x * x * x * x; }
    }
}
