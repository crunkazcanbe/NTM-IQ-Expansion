package com.crunkazcanbe.ntmiq.magsep;

import com.hbm.inventory.gui.GUIScreenRecipeSelector;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.gui.element.GUIElements;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.util.I18nUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;

/** Magnetic separator screen. Ported from NTM: IQ Edition (GPL-3; irislgtm, tam69 and the IQ team). */
public class GUIMachineMagneticSeparator extends GuiInfoContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("ntmiq", "textures/gui/processing/gui_magnetic_separator.png");
    private final TileEntityMachineMagneticSeparator machine;

    public GUIMachineMagneticSeparator(InventoryPlayer inv, TileEntityMachineMagneticSeparator te) {
        super(new ContainerMachineMagneticSeparator(inv, te));
        this.machine = te;
        this.xSize = 176;
        this.ySize = 256;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        super.drawScreen(mouseX, mouseY, f);
        super.renderHoveredToolTip(mouseX, mouseY);
        machine.inputTank.renderTankInfo(this, mouseX, mouseY, guiLeft + 8, guiTop + 18, 16, 52);
        machine.outputTank.renderTankInfo(this, mouseX, mouseY, guiLeft + 116, guiTop + 36, 16, 52);
        drawElectricityInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 18, 16, 61, machine.power, machine.maxPower);

        if (guiLeft + 7 <= mouseX && guiLeft + 25 > mouseX && guiTop + 125 < mouseY && guiTop + 143 >= mouseY) {
            GenericRecipe recipe = machine.module.getRecipe();
            if (recipe != null) GUIElements.drawHoveringTextRecipe(recipe.print(), mouseX, mouseY, fontRenderer, itemRender, width, height);
            else drawHoveringText(TextFormatting.YELLOW + I18nUtil.resolveKey("gui.recipe.setRecipe"), mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        if (checkClick(x, y, 7, 125, 18, 18)) GUIScreenRecipeSelector.openSelector(MagneticSeparatorRecipes.INSTANCE, machine, machine.module.recipe, 0, null, this);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int i, int j) {
        String name = machine.hasCustomName() ? machine.getName() : I18n.format(machine.getName());
        fontRenderer.drawString(name, 70 - fontRenderer.getStringWidth(name) / 2, 6, 4210752);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mx, int my) {
        super.drawDefaultBackground();
        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        int p = (int) (machine.power * 61 / machine.maxPower);
        drawTexturedModalRect(guiLeft + 152, guiTop + 79 - p, 176, 61 - p, 16, p);
        if (machine.module.progress > 0) {
            int w = (int) Math.ceil(70 * machine.module.progress);
            drawTexturedModalRect(guiLeft + 62, guiTop + 126, 176, 61, w, 16);
        }

        GenericRecipe recipe = machine.module.getRecipe();
        renderItem(recipe != null ? recipe.getIcon() : TEMPLATE_FOLDER, 8, 126);
        machine.inputTank.renderTank(guiLeft + 8, guiTop + 70, zLevel, 16, 52);
        machine.outputTank.renderTank(guiLeft + 116, guiTop + 88, zLevel, 16, 52);
    }
}
