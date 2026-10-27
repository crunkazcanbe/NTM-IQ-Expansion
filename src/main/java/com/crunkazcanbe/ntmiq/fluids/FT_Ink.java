package com.crunkazcanbe.ntmiq.fluids;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.util.I18nUtil;
import net.minecraft.util.text.TextFormatting;

import java.io.IOException;
import java.util.List;

/** IQ's FT_Ink (irislgtm / tam69 / the IQ team, GPL-3): marks a fluid as spraypainter ink of one dye colour (0 = white .. 15 = black). */
public class FT_Ink extends FluidTrait {

    private int color;

    public FT_Ink() {}

    public FT_Ink(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    @Override
    public void addInfo(List<String> info) {
        info.add(TextFormatting.LIGHT_PURPLE + "[" + I18nUtil.resolveKey("hbmfluid.trait.ink") + "]");
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("color").value(color);
    }

    @Override
    public void deserializeJSON(JsonObject obj) {
        if (obj.has("color")) this.color = obj.get("color").getAsInt();
    }
}
