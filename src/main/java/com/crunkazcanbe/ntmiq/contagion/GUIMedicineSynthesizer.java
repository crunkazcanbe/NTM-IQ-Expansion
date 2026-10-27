package com.crunkazcanbe.ntmiq.contagion;

import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.util.I18nUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

/**
 * The Dial-A-Dose screen, with IQ's own layout and texture (GPL-3). Cure mode and disease mode share the panel; in
 * disease mode the extra red plates from the texture sheet are drawn over it, as in IQ.
 */
public class GUIMedicineSynthesizer extends GuiInfoContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation("ntmiq", "textures/gui/machine/gui_medicine_synthesizer.png");
    private static final float TEX = 320F;

    private final TileEntityMedicineSynthesizer synth;

    public GUIMedicineSynthesizer(InventoryPlayer invPlayer, TileEntityMedicineSynthesizer te) {
        super(new ContainerMedicineSynthesizer(invPlayer, te));
        this.synth = te;
        this.xSize = 268;
        this.ySize = 216;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        super.drawScreen(mouseX, mouseY, f);
        super.renderHoveredToolTip(mouseX, mouseY);
        drawElectricityInfo(this, mouseX, mouseY, guiLeft + 247, guiTop + 19, 16, 63, synth.power, synth.maxPower);
        synth.tank.renderTankInfo(this, mouseX, mouseY, guiLeft + 7, guiTop + 24, 16, 75);

        if (synth.active) drawCustomInfoStat(mouseX, mouseY, guiLeft + 85, guiTop + 31, 34, 34, mouseX, mouseY,
                new String[] { I18nUtil.resolveKey("gui.ntmiq.dial.time", formatTime(synth.maxProgress - synth.progress)) });
    }

    private static String formatTime(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        int hours = seconds / 3600;
        seconds %= 3600;
        int minutes = seconds / 60;
        seconds %= 60;
        if (hours > 0) return hours + "h " + minutes + "m " + seconds + "s";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y) {
        fontRenderer.drawString(I18n.format("container.inventory"), 48, 122, 4210752);
        if (!synth.displayGenome.isEmpty()) {
            String genome = colorGenome(synth.displayGenome);
            fontRenderer.drawString(genome, 143 - fontRenderer.getStringWidth(genome) / 4, 98, 0x00FF00);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawModalRectWithCustomSizedTexture(guiLeft, guiTop, 0, 0, xSize, ySize, TEX, TEX);

        boolean disease = synth.isDiseaseMode();
        if (disease) {   // IQ's "Dial-A-Disease" overlay plates
            drawModalRectWithCustomSizedTexture(guiLeft + 39, guiTop + 5, 286, 126, 33, 9, TEX, TEX);
            drawModalRectWithCustomSizedTexture(guiLeft + 145, guiTop + 20, 290, 35, 26, 56, TEX, TEX);
            drawModalRectWithCustomSizedTexture(guiLeft + 94, guiTop + 81, 303, 136, 16, 16, TEX, TEX);
            drawModalRectWithCustomSizedTexture(guiLeft + 43, guiTop + 40, 304, 157, 16, 16, TEX, TEX);
            drawModalRectWithCustomSizedTexture(guiLeft + 78, guiTop + 5, 0, 220, 93, 10, TEX, TEX);
        }

        int p = synth.getProgressScaled(34);
        if (p > 0) drawModalRectWithCustomSizedTexture(guiLeft + 85, guiTop + 31 + 34 - p, 285, (disease ? 92 : 0) + 34 - p, 34, p, TEX, TEX);

        int k = (int) synth.getPowerScaled(63);
        if (k > 0) drawModalRectWithCustomSizedTexture(guiLeft + 247, guiTop + 19 + 63 - k, 269, 63 - k, 16, k, TEX, TEX);
        if (synth.power >= TileEntityMedicineSynthesizer.POWER_PER_TICK)
            drawModalRectWithCustomSizedTexture(guiLeft + 251, guiTop + 5, 269, 63, 9, 12, TEX, TEX);

        synth.tank.renderTank(guiLeft + 7, guiTop + 99, zLevel, 16, 75, 0);

        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawModalRectWithCustomSizedTexture(guiLeft + 227, guiTop + 5, disease ? 312 : 316, 153, 3, 3, TEX, TEX);
    }

    /** severity red, resistance aqua, transmission green — the three genome segments */
    private static String colorGenome(String genome) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < genome.length(); i++)
            sb.append(i < 8 ? TextFormatting.RED : i < 16 ? TextFormatting.AQUA : TextFormatting.GREEN).append(genome.charAt(i));
        return sb.toString();
    }
}
