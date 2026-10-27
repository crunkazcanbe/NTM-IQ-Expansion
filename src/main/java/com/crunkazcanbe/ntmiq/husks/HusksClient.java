package com.crunkazcanbe.ntmiq.husks;

import java.nio.DoubleBuffer;
import java.util.UUID;

import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.main.client.NTMClientRegistry;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.WaveFrontObjectVAO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** F03 client side: husk renderer (owner's skin), Cloner vat with the growing clone, DIA-1000, item models. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public final class HusksClient {
    private HusksClient() {}

    static final ResourceLocation VAT_TEX = new ResourceLocation(Tags.MODID, "textures/models/machines/vat.png");
    static final ResourceLocation DIA_TEX = new ResourceLocation(Tags.MODID, "textures/models/machines/haemodialysis_machine.png");
    private static WaveFrontObjectVAO vat, dia;
    private static final ModelBiped BIPED = new ModelBiped(0F);

    static WaveFrontObjectVAO vat() {
        if (vat == null) vat = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/machines/vat.obj")).asVBO();
        return vat;
    }

    static WaveFrontObjectVAO dia() {
        if (dia == null) dia = new HFRWavefrontObject(new ResourceLocation(Tags.MODID, "models/machines/haemodialysis_machine.obj")).asVBO();
        return dia;
    }

    /**
     * Skin of a player UUID: the live skin if that player is on the server (tab list), else the UUID's default
     * Steve/Alex. ponytail: no Mojang profile lookup for offline owners (IQ's SkinCache); add one if offline genomes matter.
     */
    static ResourceLocation skin(String uuid) {
        try {
            UUID id = UUID.fromString(uuid);
            NetHandlerPlayClient net = Minecraft.getMinecraft().getConnection();
            NetworkPlayerInfo info = net == null ? null : net.getPlayerInfo(id);
            return info != null ? info.getLocationSkin() : DefaultPlayerSkin.getDefaultSkin(id);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return DefaultPlayerSkin.getDefaultSkinLegacy();
        }
    }

    /** a T-posed-free biped without an entity (ModelBiped.render needs one) */
    static void renderBiped() {
        float s = 0.0625F;
        BIPED.bipedHead.render(s);
        BIPED.bipedHeadwear.render(s);
        BIPED.bipedBody.render(s);
        BIPED.bipedRightArm.render(s);
        BIPED.bipedLeftArm.render(s);
        BIPED.bipedRightLeg.render(s);
        BIPED.bipedLeftLeg.render(s);
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (ItemHumanPart.Part p : ItemHumanPart.Part.values())
            ModelLoader.setCustomModelResourceLocation(IqHusks.humanPart, p.ordinal(), new ModelResourceLocation(Tags.MODID + ":human_part_" + p.name().toLowerCase(java.util.Locale.US), "inventory"));
        ModelLoader.setCustomModelResourceLocation(IqHusks.neuralyser, 0, new ModelResourceLocation(IqHusks.neuralyser.getRegistryName(), "inventory"));
    }

    /** preInit (client), like the other CE-style machines */
    static void bind() {
        // load OBJ models NOW: CE refuses new models after ResourceManager uploads them (lazy load crashed on first render, 10-16)
        vat(); dia();
        RenderingRegistry.registerEntityRenderingHandler(EntityHusk.class, RenderHusk::new);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCloner.class, new RenderCloner());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityHaemodialysis.class, new RenderDia());

        NTMClientRegistry.bindTeisr(IqHusks.clonerItem, new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.translate(0, -3.5, 0); GlStateManager.scale(5.25, 5.25, 5.25); }
            @Override public void renderCommon() {
                GlStateManager.scale(0.75, 0.75, 0.75);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                Minecraft.getMinecraft().getTextureManager().bindTexture(VAT_TEX);
                vat().renderPart("main");
                vat().renderPart("door");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        });
        NTMClientRegistry.bindTeisr(IqHusks.haemodialysisItem, new ItemRenderBase() {
            @Override public void renderInventory() { GlStateManager.translate(0, -2.5, 0); GlStateManager.scale(3.5, 3.5, 3.5); }
            @Override public void renderCommon() {
                GlStateManager.rotate(90, 0, 1, 0);
                GlStateManager.scale(0.75, 0.75, 0.75);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                Minecraft.getMinecraft().getTextureManager().bindTexture(DIA_TEX);
                dia().renderPart("mesh");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        });
    }

    /** IQ's RenderHusk: a player model wearing the genome owner's skin, its armor and held item */
    static class RenderHusk extends RenderLivingBase<EntityHusk> {
        RenderHusk(RenderManager m) {
            super(m, new ModelPlayer(0F, false), 0.5F);
            addLayer(new LayerBipedArmor(this));
            addLayer(new LayerHeldItem(this));
        }

        @Override protected ResourceLocation getEntityTexture(EntityHusk e) { return skin(e.getOwnerUUID()); }

        @Override protected boolean canRenderName(EntityHusk e) { return e.hasCustomName() && super.canRenderName(e); }
    }

    /** IQ's RenderCloner: the vat, its door swinging open on release, and the clone building up behind a clip plane */
    static class RenderCloner extends TileEntitySpecialRenderer<TileEntityCloner> {
        private final DoubleBuffer plane = BufferUtils.createDoubleBuffer(4);

        @Override
        public void render(TileEntityCloner te, double x, double y, double z, float interp, int destroyStage, float alpha) {
            GlStateManager.pushMatrix();
            GlStateManager.enableLighting();
            GlStateManager.disableCull();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);

            // 2x2 footprint: centre = core + rotated (E - W + 1) / 2, (S - N + 1) / 2
            int[] d = com.hbm.handler.MultiblockHandlerXR.rotate(((BlockDummyable) com.crunkazcanbe.ntmiq.husks.IqHusks.cloner).getDimensions(), te.facing().toEnumFacing());
            GlStateManager.translate(x + (d[5] - d[4] + 1) / 2.0, y - d[1], z + (d[3] - d[2] + 1) / 2.0);
            switch (te.getBlockMetadata() - BlockDummyable.offset) {
                case 4: GlStateManager.rotate(90, 0F, 1F, 0F); break;
                case 3: GlStateManager.rotate(180, 0F, 1F, 0F); break;
                case 5: GlStateManager.rotate(270, 0F, 1F, 0F); break;
                default: break;
            }

            float door = te.prevDoor + (te.door - te.prevDoor) * interp;
            bindTexture(VAT_TEX);
            vat().renderPart("main");
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.545F, 0F, 0.25F);
            GlStateManager.rotate(-90F * door, 0F, 1F, 0F);
            GlStateManager.translate(-0.545F, 0F, -0.25F);
            vat().renderPart("door");
            GlStateManager.popMatrix();

            float build = te.spawnDelay > 0 ? 1F : te.progress / (float) TileEntityCloner.PROCESS_TIME;
            if (te.spawnDelay > 0 || (te.active && build > 0F)) {
                plane.clear();
                plane.put(new double[] { 0D, -1D, 0D, 0.3D + 2.05D * build }).flip();
                GlStateManager.pushMatrix();
                GlStateManager.translate(0.31F, 0.25F, -0.31F);
                GL11.glClipPlane(GL11.GL_CLIP_PLANE0, plane);
                GL11.glEnable(GL11.GL_CLIP_PLANE0);
                GlStateManager.scale(-1F, -1F, 1F);
                GlStateManager.translate(0F, -1.8F, 0F);
                bindTexture(skin(te.clientOwner));
                renderBiped();
                GL11.glDisable(GL11.GL_CLIP_PLANE0);
                GlStateManager.popMatrix();
            }

            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        }
    }

    /** IQ's RenderHaemodialysis: the machine + its basin tinted with the recipe's blood */
    static class RenderDia extends TileEntitySpecialRenderer<TileEntityHaemodialysis> {
        @Override
        public void render(TileEntityHaemodialysis te, double x, double y, double z, float interp, int destroyStage, float alpha) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y, z + 0.5);
            GlStateManager.rotate(90, 0, 1, 0);
            GlStateManager.enableLighting();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            switch (te.getBlockMetadata() - BlockDummyable.offset) {
                case 4: GlStateManager.rotate(90, 0F, 1F, 0F); break;
                case 3: GlStateManager.rotate(180, 0F, 1F, 0F); break;
                case 5: GlStateManager.rotate(270, 0F, 1F, 0F); break;
                default: break;
            }
            bindTexture(DIA_TEX);
            dia().renderPart("mesh");

            GenericRecipe recipe = te.module.getRecipe();
            if (recipe != null && recipe.inputFluid != null && recipe.inputFluid.length > 0) {
                int c = recipe.inputFluid[0].type.getColor();
                GlStateManager.depthMask(false);
                GlStateManager.disableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.color((c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F, 0.75F);
                dia().renderPart("water");
                GlStateManager.color(1F, 1F, 1F, 1F);
                GlStateManager.disableBlend();
                GlStateManager.enableTexture2D();
                GlStateManager.depthMask(true);
            }
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.popMatrix();
        }
    }
}
