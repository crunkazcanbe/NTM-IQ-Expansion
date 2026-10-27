package com.crunkazcanbe.ntmiq.railgun;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Louis' fire-control screen: target X/Z entry, status read-out, orientation compass or ballistic-arc graph, 3-stage charge
 * gauge and a two-click "dump charge" button. Ported from NTM: IQ Edition (1.7.10, GPL-3) — irislgtm, Alexmaster75 and the IQ team.
 * Typed commands (Enter to send): "x:z" target, "fire", "abort", "pitch!deg", "yaw!deg".
 */
public class GUITurretRailgun extends GuiInfoContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation("ntmiq", "textures/gui/weapon/gui_turret_railgun.png");
    private static final int GREEN = 0x08ff00;

    private final TileEntityTurretRailgun railgun;
    private GuiTextField field;
    private byte timer = 0;
    private boolean yeSure = false; // dump charge sanity check

    public GUITurretRailgun(InventoryPlayer invPlayer, TileEntityTurretRailgun te) {
        super(new ContainerTurretRailgun(invPlayer, te));
        this.railgun = te;
        this.xSize = 176;
        this.ySize = 222;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        this.field = new GuiTextField(0, this.fontRenderer, guiLeft + 96, guiTop + 79, 50, 14);
        this.field.setTextColor(-1);
        this.field.setDisabledTextColour(-1);
        this.field.setEnableBackgroundDrawing(false);
        this.field.setMaxStringLength(25);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    private void send(NBTTagCompound data) {
        PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, railgun.getPos()));
    }

    private void click() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void drawScreen(int x, int y, float interp) {
        super.drawScreen(x, y, interp);

        drawElectricityInfo(this, x, y, guiLeft + 153, guiTop + 34, 16, 42, railgun.getPower(), railgun.getMaxPower());
        drawElectricityInfo(this, x, y, guiLeft + 7, guiTop + 34, 16, 60, railgun.charge, railgun.maxCharge);
        drawCustomInfoStat(x, y, guiLeft + 7, guiTop + 100, 10, 24, x, y, new String[] {
                "Switch to " + (railgun.graphMode ? TextFormatting.GREEN + "ballistics" : TextFormatting.YELLOW + "orientation") + TextFormatting.RESET + " graph" });
        drawCustomInfoStat(x, y, guiLeft + 24, guiTop + 105, 27, 14, x, y, new String[] {
                yeSure ? "Are you " + (BobMathUtil.getBlink() ? TextFormatting.RED : TextFormatting.YELLOW) + "SURE" + TextFormatting.RESET + "?" : TextFormatting.RED + "Dump currently stored charge" });

        // accepted ammo, shown when hovering the empty sabot slots
        Slot hovered = getSlotUnderMouse();
        if (mc.player.inventory.getItemStack().isEmpty() && guiLeft + 61 <= x && guiLeft + 61 + 54 > x && guiTop + 103 < y && guiTop + 103 + 18 >= y
                && (hovered == null || !hovered.getHasStack())) {
            ItemStack[] list = new ItemStack[ItemAmmoRailgun.TYPES.length];
            for (int i = 0; i < list.length; i++) list[i] = new ItemStack(IqRailgun.ammo, 1, i);
            int cycle = (int) ((System.currentTimeMillis() % (1000L * list.length)) / 1000);
            String name = list[cycle].getDisplayName();
            list[cycle].setCount(0);
            List<Object[]> lines = new ArrayList<>();
            lines.add(list);
            lines.add(new Object[] { I18nUtil.resolveKey(name) });
            drawStackText(lines, x, y, this.fontRenderer, 0);
        }
        renderHoveredToolTip(x, y);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        super.mouseClicked(x, y, button);
        field.mouseClicked(x, y, button);

        if (guiLeft + 132 <= x && guiLeft + 132 + 29 > x && guiTop + 99 < y && guiTop + 99 + 17 >= y) { // on/off
            NBTTagCompound data = new NBTTagCompound();
            data.setBoolean("activeChange", !railgun.active);
            send(data);
            click();
            return;
        }
        if (guiLeft + 7 <= x && guiLeft + 7 + 10 > x && guiTop + 100 < y && guiTop + 100 + 24 >= y) { // graph mode
            NBTTagCompound data = new NBTTagCompound();
            data.setBoolean("graphModeChange", !railgun.graphMode);
            send(data);
            click();
            return;
        }
        if (guiLeft + 24 <= x && guiLeft + 24 + 27 > x && guiTop + 105 < y && guiTop + 105 + 14 >= y) { // dump charge (click twice)
            if (!yeSure) {
                yeSure = true;
                timer = 5;
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(IqRailgun.buttonIncorrect, 1.0F));
            } else {
                yeSure = false;
                NBTTagCompound data = new NBTTagCompound();
                data.setBoolean("dumpCharge", true);
                send(data);
                click();
            }
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (field.isFocused() && (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)) {
            if (railgun.active && railgun.getPower() > 0 && !field.getText().isEmpty()) {
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(HBMSoundHandler.techBoop, 1.0F));
                NBTTagCompound data = new NBTTagCompound();
                data.setString("dataInput", field.getText());
                send(data);
                field.setText("");
            }
            return;
        }
        if (field.textboxKeyTyped(c, key)) return;
        super.keyTyped(c, key);
    }

    private void text(String s, double px, double py, int color) {
        fontRenderer.drawString(s, (int) (px * 2), (int) (py * 2), color);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        String name = railgun.hasCustomName() ? railgun.getName() : I18n.format(railgun.getName());
        fontRenderer.drawString(name, xSize / 2 - fontRenderer.getStringWidth(name) / 2, 9, 4210752);

        if (!(railgun.active && railgun.getPower() > 0)) return;

        String t = field.getText();
        if (field.isFocused()) {
            String cursor = System.currentTimeMillis() % 1000 < 500 ? " " : "||";
            t = t.substring(0, field.getCursorPosition()) + cursor + t.substring(field.getCursorPosition());
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5, 0.5, 1);
        text("Target:", 97, 36, GREEN);
        if (railgun.targetVector != null) {
            text("X: " + num(railgun.targetVector.x), 97, 41, GREEN);
            text("Z: " + num(railgun.targetVector.y), 97, 46, GREEN);
        }
        text("Loaded: " + TileEntityTurretRailgun.sabotName(railgun.sabotType), 97, 53, GREEN);
        text("V0: " + BobMathUtil.getShortNumber((long) (railgun.v0 * Math.sqrt((double) railgun.charge / (double) railgun.maxCharge))) + "m/s", 97, 58, GREEN);
        text("Charging: " + (int) (railgun.charge * 100 / railgun.maxCharge) + "%", 97, 65, 0x00ff00);
        text("Status: " + railgun.status, 97, 70, GREEN);
        if (railgun.graphMode) {
            text("X", 55, 36, GREEN);
            text("Z", 84, 66, GREEN);
        } else {
            text("> The graph may not", 31, 48, GREEN);
            text("accurately represent", 31, 52, GREEN);
            text("the actual path.", 31, 58, GREEN);
            text(String.format("e: %.2f s", railgun.iterations / 20F), 61, 89, GREEN);
        }
        text("P: " + (int) Math.toDegrees(railgun.rotationPitch) + "°", 31, 36, GREEN);
        text("Y: " + (int) Math.toDegrees(railgun.rotationYaw) + "°", 31, 42, GREEN);
        text(t, 97, 85, GREEN);
        GlStateManager.popMatrix();

        if (railgun.graphMode) {
            arrow(59, 92, 59, 36, 1F, GREEN);   // ordinate
            arrow(31, 64, 87, 64, 1F, GREEN);   // abscissa
            circle(59, 64, 20F, 48, GREEN);
            arrow(59, 64, (float) (59 + 15 * -Math.cos(railgun.rotationYaw)), (float) (64 + 15 * Math.sin(railgun.rotationYaw)), 1F, 0xfafa00); // barrel
            if (railgun.targetDirectionVector != null)
                arrow(59, 64, (float) (59 + 10 * -railgun.targetDirectionVector.x), (float) (64 + 10 * -railgun.targetDirectionVector.y), 1F, 0xff0000);
        } else {
            // ballistic arc of the current solver angle
            List<float[]> points = new ArrayList<>();
            float maxY = 0F;
            for (double T = 0D; T < railgun.t_max && points.size() < 20_000; T += 0.01D) {
                float py = (float) railgun.y_i(T, railgun.m, railgun.v0 * Math.sin(railgun.theta), railgun.bc, railgun.g);
                if (py > maxY) maxY = py;
                points.add(new float[] { (float) (35 + railgun.x_i(T, railgun.m, railgun.v0 * Math.cos(railgun.theta), railgun.bc) / railgun.x_target * 50), py });
            }
            if (!points.isEmpty() && maxY != 0F) {
                begin(GL11.GL_LINE_STRIP);
                BufferBuilder buf = Tessellator.getInstance().getBuffer();
                for (float[] p : points) {
                    float py = 88 - p[1] / maxY * 24;
                    buf.pos(MathHelper.clamp(p[0], 35, 85), MathHelper.clamp(py, 64, 88), zLevel).color(255, 0, 0, 255).endVertex();
                }
                end();
            }
            arrow(35, 92, 35, 64, 1F, GREEN);
            arrow(31, 88, 87, 88, 1F, GREEN);
        }
    }

    private static String num(double v) {
        return Math.abs(v) > 1000000000 ? BobMathUtil.getShortNumber((long) v) : String.valueOf((int) v);
    }

    private static void begin(int mode) {
        GlStateManager.disableTexture2D();
        Tessellator.getInstance().getBuffer().begin(mode, DefaultVertexFormats.POSITION_COLOR);
    }

    private static void end() {
        Tessellator.getInstance().draw();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
    }

    private void vertex(double x, double y, int color) {
        Tessellator.getInstance().getBuffer().pos(x, y, zLevel).color(color >> 16 & 255, color >> 8 & 255, color & 255, 255).endVertex();
    }

    /** IQ's GUIElements.drawHollowCircle */
    private void circle(int x, int y, float r, int segments, int color) {
        begin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            double a = 2D * Math.PI * i / segments;
            vertex(x + r * Math.cos(a), y + r * Math.sin(a), color);
        }
        end();
    }

    /** IQ's GUIElements.drawArrowVector: line from (x,y) to (tx,ty) with an arrowhead */
    private void arrow(int x, int y, float tx, float ty, float minDist, int color) {
        float dx = tx - x, dy = ty - y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float sx, sy;
        if (len * 0.1F > minDist) { sx = minDist * dx / len; sy = minDist * dy / len; }
        else { sx = 0.1F * dx; sy = 0.1F * dy; }
        begin(GL11.GL_LINE_LOOP);
        vertex(x, y, color);
        vertex(x + dx - sx, y + dy - sy, color);
        vertex(x + dx - sx - sy, y + dy - sy + sx, color);
        vertex(x + dx, y + dy, color);
        vertex(x + dx - sx + sy, y + dy - sy - sx, color);
        vertex(x + dx - sx, y + dy - sy, color);
        end();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mx, int my) {
        drawDefaultBackground();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        int scale = (int) (railgun.getPower() * 42 / railgun.getMaxPower());
        drawTexturedModalRect(guiLeft + 153, guiTop + 76 - scale, 176, 59 - scale, 16, scale);

        if (railgun.active) drawTexturedModalRect(guiLeft + 132, guiTop + 99, 176, 0, 29, 17);
        if (!railgun.graphMode) drawTexturedModalRect(guiLeft + 7, guiTop + 100, 205, 0, 10, 24);
        if (timer > 0 && guiLeft + 24 <= mx && guiLeft + 24 + 27 > mx && guiTop + 105 < my && guiTop + 105 + 14 >= my)
            drawTexturedModalRect(guiLeft + 24, guiTop + 105, 215, 0, 27, 14);
        if (yeSure) drawTexturedModalRect(guiLeft + 24, guiTop + 105, 215, 14, 27, 14);
        if (timer > 0) timer--;

        // one charge gauge drawn across three 60px bars
        scale = (int) (railgun.charge * 3 * 60 / railgun.maxCharge);
        for (int i = 0; i < 3; i++) {
            int s = MathHelper.clamp(scale - 60 * i, 0, 60);
            drawTexturedModalRect(guiLeft + 7 + 6 * i, guiTop + 94 - s, 176, 119 - s, 4, s);
        }
    }
}
