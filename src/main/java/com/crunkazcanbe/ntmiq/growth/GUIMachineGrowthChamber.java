package com.crunkazcanbe.ntmiq.growth;

import com.hbm.inventory.gui.GUIScreenRecipeSelector;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.gui.element.GUIElements;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.util.I18nUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;

/** Growth chamber screen. Ported from NTM: IQ Edition (GPL-3; irislgtm, tam69 and the IQ team). */
public class GUIMachineGrowthChamber extends GuiInfoContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("ntmiq", "textures/gui/processing/gui_growth_chamber.png");
    private final TileEntityMachineGrowthChamber chamber;

    public GUIMachineGrowthChamber(InventoryPlayer inv, TileEntityMachineGrowthChamber te) {
        super(new ContainerMachineGrowthChamber(inv, te));
        this.chamber = te;
        this.xSize = 176;
        this.ySize = 204;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        super.drawScreen(mouseX, mouseY, f);
        super.renderHoveredToolTip(mouseX, mouseY);
        chamber.tank.renderTankInfo(this, mouseX, mouseY, guiLeft + 35, guiTop + 63, 34, 16);
        drawElectricityInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 18, 16, 52, chamber.power, chamber.maxPower);

        if (guiLeft + 7 <= mouseX && guiLeft + 25 > mouseX && guiTop + 10 < mouseY && guiTop + 28 >= mouseY) {
            GenericRecipe recipe = chamber.module.getRecipe();
            if (recipe != null) GUIElements.drawHoveringTextRecipe(recipe.print(), mouseX, mouseY, fontRenderer, itemRender, width, height);
            else drawHoveringText(TextFormatting.YELLOW + I18nUtil.resolveKey("gui.recipe.setRecipe"), mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        if (checkClick(x, y, 7, 10, 18, 18)) GUIScreenRecipeSelector.openSelector(GrowthChamberRecipes.INSTANCE, chamber, chamber.module.recipe, 0, null, this);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int i, int j) {
        fontRenderer.drawString(I18n.format("container.inventory"), 8, 110, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mx, int my) {
        super.drawDefaultBackground();
        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        if (chamber.power > 0) {
            int p = (int) (chamber.power * 52 / chamber.maxPower);
            drawTexturedModalRect(guiLeft + 152, guiTop + 70 - p, 176, 52 - p, 16, p);
            drawTexturedModalRect(guiLeft + 156, guiTop + 4, 176, 52, 9, 12);
        }
        if (chamber.module.progress > 0) {
            int w = (int) Math.ceil(38 * chamber.module.progress);
            drawTexturedModalRect(guiLeft + 66, guiTop + 37, 204, 0, w, 14);
        }

        GenericRecipe recipe = chamber.module.getRecipe();
        renderItem(recipe != null ? recipe.getIcon() : TEMPLATE_FOLDER, 9, 10);

        if (recipe != null && recipe.inputItem != null) {
            // ghost the expected input in the empty input slot
            Slot slot = inventorySlots.inventorySlots.get(chamber.module.inputSlots[0]);
            if (!slot.getHasStack()) {
                renderItem(recipe.inputItem[0].extractForCyclingDisplay(20), slot.xPos, slot.yPos, 10F);
                Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.color(1F, 1F, 1F, 0.5F);
                GlStateManager.enableBlend();
                zLevel = 300F;
                drawTexturedModalRect(guiLeft + slot.xPos, guiTop + slot.yPos, slot.xPos, slot.yPos, 16, 16);
                zLevel = 0F;
                GlStateManager.color(1F, 1F, 1F, 1F);
                GlStateManager.disableBlend();
            }
        }

        chamber.tank.renderTank(guiLeft + 35, guiTop + 79, zLevel, 34, 16, 1);
    }
}
