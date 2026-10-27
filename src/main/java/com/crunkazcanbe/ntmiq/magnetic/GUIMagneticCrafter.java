package com.crunkazcanbe.ntmiq.magnetic;

import com.crunkazcanbe.ntmiq.Tags;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/** IQ's GUIMagneticCrafter (texture from IQ). */
public class GUIMagneticCrafter extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/tool/magnetic_crafting_table.png");

    public GUIMagneticCrafter(InventoryPlayer inv, World world) {
        super(new ContainerMagneticCrafter(inv, world));
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRenderer.drawString(I18n.format("item.magnetic_crafter.name"), 8, 6, 4210752);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mx, int my) {
        mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect((width - xSize) / 2, (height - ySize) / 2, 0, 0, xSize, ySize);
    }
}
