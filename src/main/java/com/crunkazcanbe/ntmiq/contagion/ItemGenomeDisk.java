package com.crunkazcanbe.ntmiq.contagion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm.items.special.ItemCustomLore;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * Floppy disk (IQ: items/tool/ItemFloppyDisk, GPL-3): the lab notebook of this system. Genome samples get written onto
 * it one section at a time; once the five core sections are there the disk describes a whole disease and the
 * Dial-A-Dose can brew against it. All of it lives under the stack's "pathogen" tag, exactly as in IQ, so a disk
 * written by a later Genome Sequencer or Gene Splicer reads the same.
 */
@SuppressWarnings("deprecation")
public class ItemGenomeDisk extends ItemCustomLore {

    public ItemGenomeDisk(String name) {
        super(name);                                     // CE's ItemBase names it ntmiq:<name> / item.<name>
        setMaxStackSize(1);
        com.hbm.items.ModItems.ALL_ITEMS.remove(this);   // CE's base self-registers; the add-on registers it itself
    }

    private static NBTTagCompound pathogen(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().hasKey("pathogen")
                ? stack.getTagCompound().getCompoundTag("pathogen") : null;
    }

    private static NBTTagCompound ensurePathogen(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        if (!stack.getTagCompound().hasKey("pathogen")) stack.getTagCompound().setTag("pathogen", new NBTTagCompound());
        return stack.getTagCompound().getCompoundTag("pathogen");
    }

    public static String getFrameId(ItemStack stack) {
        NBTTagCompound p = pathogen(stack);
        if (p == null) return null;
        if (p.hasKey("frame")) return p.getString("frame");
        String id = getSections(stack).get("id");
        return id == null || id.isEmpty() ? null : id;
    }

    /** the genome the disk spells out; segments it does not have yet read "????????" */
    public static String getGenome(ItemStack stack) {
        NBTTagCompound p = pathogen(stack);
        if (p != null && p.hasKey("genome")) return p.getString("genome");
        Map<String, String> sections = getSections(stack);
        return sections.isEmpty() ? "" : GenomeSample.buildGenome(sections);
    }

    public static boolean isGenomeComplete(String genome) {
        return genome != null && !genome.isEmpty() && genome.indexOf('?') < 0;
    }

    public static DiseaseDefinition getStoredDef(ItemStack stack) {
        NBTTagCompound p = pathogen(stack);
        if (p == null || !p.hasKey("stats")) return null;
        DiseaseDefinition def = DiseaseDefinition.readFromNBT(p.getCompoundTag("stats"));
        if (def != null && getFrameId(stack) != null) def.id = getFrameId(stack);
        return def;
    }

    public static Map<String, String> getSections(ItemStack stack) {
        Map<String, String> sections = new HashMap<>();
        NBTTagCompound p = pathogen(stack);
        if (p == null || !p.hasKey("sections")) return sections;
        NBTTagCompound tag = p.getCompoundTag("sections");
        for (String key : tag.getKeySet()) sections.put(key, tag.getString(key));
        return sections;
    }

    public static void putSection(ItemStack stack, String key, String value) {
        if (stack.isEmpty() || key == null || value == null) return;
        NBTTagCompound p = ensurePathogen(stack);
        NBTTagCompound sections = p.hasKey("sections") ? p.getCompoundTag("sections") : new NBTTagCompound();
        sections.setString(key, value);
        p.setTag("sections", sections);
        if ("id".equals(key)) p.setString("frame", value);
    }

    /** write a whole frame + genome at once (a sequencer or a splicer does this) */
    public static void writeGenome(ItemStack stack, DiseaseDefinition def, String genome) {
        NBTTagCompound p = ensurePathogen(stack);
        if (genome != null && !genome.isEmpty()) p.setString("genome", genome);
        if (def == null) return;
        if (def.id != null && !def.id.isEmpty()) p.setString("frame", def.id);
        p.setTag("stats", def.toNBT());
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.addAll(getAnalysisLines(stack));
        super.addInformation(stack, world, list, flag);
    }

    public static List<String> getAnalysisLines(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        String frameId = getFrameId(stack);
        String genome = getGenome(stack);
        Map<String, String> sections = getSections(stack);
        if (frameId == null && genome.isEmpty() && sections.isEmpty()) {
            lines.add(TextFormatting.DARK_GRAY + I18n.translateToLocal("item.floppy_disk.blank"));
            return lines;
        }

        DiseaseDefinition def = frameId == null ? null : DiseaseRegistry.get(frameId);
        if (def == null) def = getStoredDef(stack);
        if (def == null) def = GenomeSample.buildDefinition(sections);

        if (def != null) {
            lines.add(TextFormatting.RED + (def.displayName == null || def.displayName.isEmpty() ? def.id : def.displayName)
                    + TextFormatting.GRAY + (def.type == null ? "" : " (" + def.type.name().toLowerCase(java.util.Locale.US) + ")"));
        } else if (frameId != null) {
            lines.add(TextFormatting.RED + frameId);
        }
        if (!genome.isEmpty()) lines.add(TextFormatting.GREEN + genome);
        if (!sections.isEmpty()) lines.add(TextFormatting.AQUA + I18n.translateToLocalFormatted("item.floppy_disk.sections", sections.size(), GenomeSample.KEYS.length));

        String missing = GenomeSample.getMissing(sections);
        if (def == null && !missing.isEmpty()) lines.add(TextFormatting.YELLOW + I18n.translateToLocalFormatted("item.floppy_disk.missing", missing));
        return lines;
    }
}
