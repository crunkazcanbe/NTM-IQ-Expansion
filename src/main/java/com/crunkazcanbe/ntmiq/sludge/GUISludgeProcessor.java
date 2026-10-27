package com.crunkazcanbe.ntmiq.sludge;

import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.inventory.gui.GUIScreenRecipeSelector;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.gui.element.GUIElements;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.util.I18nUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;

/** IQ's GUIMachineSludgeProcessor (texture from IQ). */
public class GUISludgeProcessor extends GuiInfoContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/processing/gui_sludge_processor.png");
    private final TileEntitySludgeProcessor te;

    public GUISludgeProcessor(InventoryPlayer inv, TileEntitySludgeProcessor te) {
        super(new ContainerSludgeProcessor(inv, te.getCheckedInventory()));
        this.te = te;
        this.xSize = 176;
        this.ySize = 256;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        super.drawScreen(mouseX, mouseY, f);
        super.renderHoveredToolTip(mouseX, mouseY);

        te.inputTanks[0].renderTankInfo(this, mouseX, mouseY, guiLeft + 8, guiTop + 18, 16, 52);
        for (int i = 0; i < 4; i++) te.outputTanks[i].renderTankInfo(this, mouseX, mouseY, guiLeft + 62 + i * 18, guiTop + 18, 16, 52);
        drawElectricityInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 18, 16, 61, te.power, te.maxPower);

        if (guiLeft + 7 <= mouseX && guiLeft + 7 + 18 > mouseX && guiTop + 125 < mouseY && guiTop + 125 + 18 >= mouseY) {
            GenericRecipe recipe = te.sludgeProcessorModule.getRecipe();
            if (recipe != null) GUIElements.drawHoveringTextRecipe(recipe.print(), mouseX, mouseY, fontRenderer, itemRender, width, height);
            else drawHoveringText(TextFormatting.YELLOW + I18nUtil.resolveKey("gui.recipe.setRecipe"), mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        if (checkClick(x, y, 7, 125, 18, 18))
            GUIScreenRecipeSelector.openSelector(SludgeProcessorRecipes.INSTANCE, te, te.sludgeProcessorModule.recipe, 0, ItemBlueprints.grabPool(te.inventory.getStackInSlot(1)), this);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int i, int j) {
        String name = te.hasCustomName() ? te.getName() : I18n.format(te.getName());
        fontRenderer.drawString(name, 70 - fontRenderer.getStringWidth(name) / 2, 6, 4210752);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partial, int mx, int my) {
        super.drawDefaultBackground();
        GlStateManager.color(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        int p = (int) (te.power * 61 / Math.max(1, te.maxPower));
        drawTexturedModalRect(guiLeft + 152, guiTop + 79 - p, 176, 61 - p, 16, p);

        if (te.sludgeProcessorModule.progress > 0) {
            int j = (int) Math.ceil(70 * te.sludgeProcessorModule.progress);
            drawTexturedModalRect(guiLeft + 62, guiTop + 126, 176, 61, j, 16);
        }

        GenericRecipe recipe = te.sludgeProcessorModule.getRecipe();
        if (te.didProcess) drawTexturedModalRect(guiLeft + 51, guiTop + 121, 195, 0, 3, 6);
        else if (recipe != null) drawTexturedModalRect(guiLeft + 51, guiTop + 121, 192, 0, 3, 6);
        if (te.didProcess) drawTexturedModalRect(guiLeft + 56, guiTop + 121, 195, 0, 3, 6);
        else if (recipe != null && te.power >= recipe.power) drawTexturedModalRect(guiLeft + 56, guiTop + 121, 192, 0, 3, 6);

        renderItem(recipe != null ? recipe.getIcon() : TEMPLATE_FOLDER, 8, 126);

        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < recipe.inputItem.length; i++) {
                Slot slot = inventorySlots.inventorySlots.get(te.sludgeProcessorModule.inputSlots[i]);
                if (!slot.getHasStack()) renderItem(recipe.inputItem[i].extractForCyclingDisplay(20), slot.xPos, slot.yPos, 10F);
            }
            Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(1F, 1F, 1F, 0.5F);
            GlStateManager.enableBlend();
            zLevel = 300F;
            for (int i = 0; i < recipe.inputItem.length; i++) {
                Slot slot = inventorySlots.inventorySlots.get(te.sludgeProcessorModule.inputSlots[i]);
                if (!slot.getHasStack()) drawTexturedModalRect(guiLeft + slot.xPos, guiTop + slot.yPos, slot.xPos, slot.yPos, 16, 16);
            }
            zLevel = 0F;
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.disableBlend();
        }

        te.inputTanks[0].renderTank(guiLeft + 8, guiTop + 70, zLevel, 16, 52);
        for (int i = 0; i < 4; i++) te.outputTanks[i].renderTank(guiLeft + 62 + i * 18, guiTop + 70, zLevel, 16, 52);
    }
}
