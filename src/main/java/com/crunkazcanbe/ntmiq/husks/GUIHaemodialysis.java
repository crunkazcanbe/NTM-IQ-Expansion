package com.crunkazcanbe.ntmiq.husks;

import java.io.IOException;

import com.crunkazcanbe.ntmiq.Tags;
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

/** IQ's GUIMachineHaemodialysis (texture from IQ). */
public class GUIHaemodialysis extends GuiInfoContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/processing/gui_haemodialysis_machine.png");
    private final TileEntityHaemodialysis dia;

    public GUIHaemodialysis(InventoryPlayer inv, TileEntityHaemodialysis te) {
        super(new ContainerHaemodialysis(inv, te.getCheckedInventory()));
        dia = te;
        xSize = 176;
        ySize = 256;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        super.drawScreen(mouseX, mouseY, f);
        super.renderHoveredToolTip(mouseX, mouseY);
        for (int i = 0; i < 3; i++) dia.inputTanks[i].renderTankInfo(this, mouseX, mouseY, guiLeft + 8 + i * 18, guiTop + 18, 16, 96);
        for (int i = 0; i < 2; i++) dia.outputTanks[i].renderTankInfo(this, mouseX, mouseY, guiLeft + 98 + i * 18, guiTop + 18, 16, 52);
        drawElectricityInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 18, 16, 61, dia.power, dia.maxPower);

        if (guiLeft + 7 <= mouseX && guiLeft + 7 + 18 > mouseX && guiTop + 125 < mouseY && guiTop + 125 + 18 >= mouseY) {
            GenericRecipe recipe = dia.module.getRecipe();
            if (recipe != null) GUIElements.drawHoveringTextRecipe(recipe.print(), mouseX, mouseY, fontRenderer, itemRender, width, height);
            else drawHoveringText(TextFormatting.YELLOW + I18nUtil.resolveKey("gui.recipe.setRecipe"), mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        if (checkClick(x, y, 7, 125, 18, 18)) GUIScreenRecipeSelector.openSelector(HaemodialysisRecipes.INSTANCE, dia, dia.module.recipe, 0, null, this);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int i, int j) {
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mx, int my) {
        drawDefaultBackground();
        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        int p = (int) (dia.power * 61 / Math.max(1, dia.maxPower));
        drawTexturedModalRect(guiLeft + 152, guiTop + 79 - p, 176, 61 - p, 16, p);
        if (dia.module.progress > 0) drawTexturedModalRect(guiLeft + 62, guiTop + 126, 176, 61, (int) Math.ceil(70 * dia.module.progress), 16);

        GenericRecipe recipe = dia.module.getRecipe();
        renderItem(recipe != null ? recipe.getIcon() : TEMPLATE_FOLDER, 8, 126);
        for (int i = 0; i < 3; i++) dia.inputTanks[i].renderTank(guiLeft + 8 + i * 18, guiTop + 114, zLevel, 16, 96);
        for (int i = 0; i < 2; i++) dia.outputTanks[i].renderTank(guiLeft + 98 + i * 18, guiTop + 70, zLevel, 16, 52);
    }
}
