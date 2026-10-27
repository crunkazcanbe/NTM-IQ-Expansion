package com.crunkazcanbe.ntmiq.contagion;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

/**
 * Contaminated items (IQ: handler/contagion/ItemContagion, GPL-3): a carrier of a fomite-capable disease leaves it on
 * single-stack items in its inventory and on items lying nearby, and anyone who carries such an item without
 * {@link ItemContagionGloves} catches it.
 */
public final class ItemContagion {
    private ItemContagion() {}

    public static final String KEY = "ntmiqContagion";

    public static void setFrames(ItemStack stack, List<String> frames) {
        if (stack.isEmpty() || frames == null || frames.isEmpty()) return;
        NBTTagList list = new NBTTagList();
        for (String frame : frames) if (frame != null && !frame.isEmpty()) list.appendTag(new NBTTagString(frame));
        if (list.tagCount() == 0) return;
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setTag(KEY, list);
    }

    public static List<String> getFrames(ItemStack stack) {
        List<String> frames = new ArrayList<>();
        if (stack.isEmpty() || !stack.hasTagCompound()) return frames;
        NBTTagList list = stack.getTagCompound().getTagList(KEY, Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) frames.add(list.getStringTagAt(i));
        return frames;
    }

    public static boolean hasFrames(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().getTagList(KEY, Constants.NBT.TAG_STRING).tagCount() > 0;
    }

    /** only single-stack items hold a film of anything, as in IQ */
    public static boolean isContaminable(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() == 1;
    }

    /** @param fomiteFrames the carrier's fomite-capable diseases (may be empty: the inventory check still runs) */
    public static void tick(EntityLivingBase host, List<String> fomiteFrames) {
        if (!ContagionConfig.enabled || !ContagionConfig.fomites) return;
        checkInventory(host);
        if (!fomiteFrames.isEmpty() && host.world.getTotalWorldTime() % 100 == 0) {
            tagInventoryItem(host, fomiteFrames);
            tagNearbyItems(host, fomiteFrames);
        }
    }

    public static void tagInventoryItem(EntityLivingBase host, List<String> frames) {
        if (!(host instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) host;
        Random rand = player.getRNG();
        ItemStack stack = rand.nextInt(100) == 0
                ? player.inventory.armorInventory.get(rand.nextInt(4))
                : player.inventory.mainInventory.get(rand.nextInt(player.inventory.mainInventory.size()));
        if (isContaminable(stack)) setFrames(stack, frames);
    }

    public static void tagNearbyItems(EntityLivingBase host, List<String> frames) {
        double range = host.isWet() ? 16D : 2D;
        for (EntityItem item : host.world.getEntitiesWithinAABB(EntityItem.class, host.getEntityBoundingBox().grow(range)))
            tagEntityItem(item, frames);
    }

    public static void tagEntityItem(EntityItem item, List<String> frames) {
        if (item == null || frames == null || frames.isEmpty()) return;
        if (isContaminable(item.getItem())) setFrames(item.getItem(), frames);
    }

    /** carrying a contaminated item barehanded infects you; gloves take the hit instead */
    public static void checkInventory(EntityLivingBase host) {
        if (!(host instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) host;

        boolean carrying = false;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++)
            if (hasFrames(player.inventory.getStackInSlot(i))) { carrying = true; break; }
        if (!carrying || ItemContagionGloves.wear(player)) return;

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!hasFrames(stack)) continue;
            for (String frameId : getFrames(stack)) {
                DiseaseDefinition def = DiseaseRegistry.get(frameId);
                if (def == null || DiseaseHandler.isImmuneOrInfected(player, frameId, null)) continue;
                DiseaseHandler.infect(player, def, 10F + def.baseSeverity * 10F, def.getReferenceGenome());
            }
        }
    }

    /** what a carrier can leave on items: its fomite-capable diseases */
    public static List<String> fomiteFramesOf(EntityLivingBase host) {
        List<String> frames = new ArrayList<>();
        for (com.crunkazcanbe.ntmiq.blood.BloodEntry entry : com.crunkazcanbe.ntmiq.blood.BloodstreamAPI.get(host).getEntries()) {
            if (!entry.isPathogen()) continue;
            DiseaseDefinition def = DiseaseRegistry.resolve(entry.frameId, entry.data == null ? null : entry.data.getCompoundTag("def"));
            if (def != null && def.fomite) frames.add(def.id);
        }
        return frames;
    }
}
