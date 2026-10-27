package com.crunkazcanbe.ntmiq.husks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.crunkazcanbe.ntmiq.Tags;
import com.crunkazcanbe.ntmiq.blood.ItemMedicalSyringe;
import com.crunkazcanbe.ntmiq.husks.ItemHumanPart.Stat;
import com.crunkazcanbe.ntmiq.husks.ItemHumanPart.Trait;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.util.I18nUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

/** IQ's GUICloner: spinning preview of the genome owner, three info pages (stats / traits / genome), power, blood tank, on/off lever. */
public class GUICloner extends GuiInfoContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/machine/gui_cloner.png");
    private static final ResourceLocation[] TABS = {
            new ResourceLocation(Tags.MODID, "textures/gui/cloner_stats.png"),
            new ResourceLocation(Tags.MODID, "textures/gui/cloner_traits.png"),
            new ResourceLocation(Tags.MODID, "textures/gui/cloner_genome.png") };
    private static final int TABS_Y = 20, TAB_SIZE = 12;

    private final TileEntityCloner cloner;
    private float spin;
    private int page;

    public GUICloner(InventoryPlayer inv, TileEntityCloner te) {
        super(new ContainerCloner(inv, te.getCheckedInventory()));
        cloner = te;
        xSize = 212;
        ySize = 236;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        spin += 2F;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        super.drawScreen(mouseX, mouseY, f);
        super.renderHoveredToolTip(mouseX, mouseY);
        drawElectricityInfo(this, mouseX, mouseY, guiLeft + 188, guiTop + 19, 16, 34, cloner.getPower(), cloner.getMaxPower());
        cloner.tank.renderTankInfo(this, mouseX, mouseY, guiLeft + 188, guiTop + 89, 16, 35);
    }

    private static int tabX(int i) {
        int section = 96 / TABS.length;
        return 60 + i * section + (section - TAB_SIZE) / 2;
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        for (int i = 0; i < TABS.length; i++) if (checkClick(x, y, tabX(i), TABS_Y, TAB_SIZE, TAB_SIZE)) { page = i; click(); return; }
        if (checkClick(x, y, 161, 105, 9, 20)) {
            NBTTagCompound data = new NBTTagCompound();
            data.setBoolean("toggle", true);
            PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, cloner.getPos()));
            click();
        }
    }

    private void click() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int[] totals = new int[Stat.values().length];
        List<String> traits = new ArrayList<>();
        for (ItemStack part : cloner.getDistinctParts()) {
            for (Stat s : Stat.values()) totals[s.ordinal()] += ItemHumanPart.getStat(part, s);
            Trait t = ItemHumanPart.getTrait(part);
            if (t != null) traits.add(I18nUtil.resolveKey("item.human_part.trait." + t.name().toLowerCase(Locale.US)));
        }

        int y = 41;
        if (page == 0) {
            for (Stat s : Stat.values()) { text(I18nUtil.resolveKey("item.human_part.stat." + s.name().toLowerCase(Locale.US)) + ": " + totals[s.ordinal()], 64, y, 0x00FF00); y += 8; }
        } else if (page == 1) {
            if (traits.isEmpty()) text(I18nUtil.resolveKey("gui.cloner.empty"), 64, y, 0x808080);
            for (String t : traits) { text(t, 64, y, 0xFF5555); y += 8; }   // max 5 (one per part) fits the screen
        } else {
            ItemStack syringe = cloner.inventory.getStackInSlot(TileEntityCloner.SLOT_SYRINGE);
            String donor = syringe.hasTagCompound() ? syringe.getTagCompound().getString(ItemMedicalSyringe.KEY_OWNER_NAME) : "";
            text(I18nUtil.resolveKey("gui.cloner.genome", donor.isEmpty() ? "?" : donor), 64, y, 0x00A0FF);
            if (cloner.tank.getTankType() != Fluids.NONE) text(cloner.tank.getTankType().getLocalizedName(), 64, y + 8, 0x00FF00);
        }
        text(cloner.getDistinctParts().size() + "/5", 146, 84, 0x808080);
        fontRenderer.drawString(I18n.format("container.inventory"), 20, ySize - 96 + 2, 4210752);
    }

    private void text(String s, int x, int y, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.6, 0.6, 1);
        while (s.length() > 3 && fontRenderer.getStringWidth(s) * 0.6F > 92) s = s.substring(0, s.length() - 4) + "..";
        fontRenderer.drawStringWithShadow(s, Math.round(x / 0.6F), Math.round(y / 0.6F), color);
        GlStateManager.popMatrix();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mx, int my) {
        drawDefaultBackground();
        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        int energy = (int) (cloner.getPower() * 34 / Math.max(cloner.getMaxPower(), 1));
        if (energy > 0) {
            drawTexturedModalRect(guiLeft + 188, guiTop + 53 - energy, 235, 50 - energy, 16, energy);
            drawTexturedModalRect(guiLeft + 192, guiTop + 5, 235, 50, 8, 11);
        }
        int time = cloner.progress * 79 / TileEntityCloner.PROCESS_TIME;
        if (time > 0) drawTexturedModalRect(guiLeft + 163, guiTop + 97 - time, 251, 95 - time, 5, time);
        if (cloner.active) drawTexturedModalRect(guiLeft + 163, guiTop + 106, 250, 106, 5, 18);

        for (int i = 0; i < TABS.length; i++) {
            float tint = i == page ? 1F : 0.35F;
            GlStateManager.color(tint, tint, tint, 1F);
            Minecraft.getMinecraft().getTextureManager().bindTexture(TABS[i]);
            drawModalRectWithCustomSizedTexture(guiLeft + tabX(i), guiTop + TABS_Y, 0, 0, TAB_SIZE, TAB_SIZE, TAB_SIZE, TAB_SIZE);
        }
        GlStateManager.color(1F, 1F, 1F, 1F);

        cloner.tank.renderTank(guiLeft + 188, guiTop + 124, zLevel, 16, 35);
        renderClone();
    }

    private void renderClone() {
        GlStateManager.enableDepth();
        GlStateManager.disableCull();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate(guiLeft + 31F, guiTop + 40F, 50F);
        GlStateManager.scale(-35F, 35F, 35F);
        GlStateManager.rotate(180F + spin, 0F, 1F, 0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(HusksClient.skin(cloner.clientOwner));
        HusksClient.renderBiped();
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableCull();
        GlStateManager.disableDepth();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }
}
