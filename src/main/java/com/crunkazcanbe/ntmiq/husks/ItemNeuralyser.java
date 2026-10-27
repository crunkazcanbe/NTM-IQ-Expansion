package com.crunkazcanbe.ntmiq.husks;

import java.util.List;
import java.util.UUID;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * IQ's ItemNeuralyser (NTM: IQ Edition, GPL-3; irislgtm / tam69 / IQ team). Sneak-right-click a husk to link, then
 * right-click (anywhere, husk within {@link #LINK_RANGE}) to move your mind into it. While held, the action bar shows
 * the linked husk's distance.
 */
public class ItemNeuralyser extends Item {

    public static final double LINK_RANGE = 100D;

    public ItemNeuralyser() {
        setMaxStackSize(1);
        setFull3D();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) return new ActionResult<>(EnumActionResult.PASS, stack);
        if (!world.isRemote) {
            EntityHusk husk = getLinkedHusk(stack, world);
            if (husk != null && husk.isAdopted() && player.getDistance(husk) <= LINK_RANGE) {
                if (!IqHusks.bodySwap) player.sendStatusMessage(new TextComponentTranslation("item.neuralyser.disabled"), true);
                else husk.beginTransfer(player);
            } else {
                player.sendStatusMessage(new TextComponentTranslation(getLinkedUUID(stack) != null ? "item.neuralyser.no_target" : "item.neuralyser.not_linked"), true);
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean inHand) {
        if (world.isRemote || !inHand || !(entity instanceof EntityPlayer) || world.getTotalWorldTime() % 10 != 0) return;
        hud((EntityPlayer) entity, stack);
    }

    static void hud(EntityPlayer player, ItemStack stack) {
        if (getLinkedUUID(stack) == null) return;
        EntityHusk husk = getLinkedHusk(stack, player.world);
        player.sendStatusMessage(husk != null
                ? new TextComponentTranslation("item.neuralyser.hud", husk.hasCustomName() ? husk.getCustomNameTag() : husk.getOwnerName(), (int) player.getDistance(husk))
                : new TextComponentTranslation("item.neuralyser.hud.lost"), true);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        tooltip(stack, list);
    }

    static void tooltip(ItemStack stack, List<String> list) {
        if (getLinkedUUID(stack) != null) list.add(TextFormatting.GRAY + I18nUtil.resolveKey("item.neuralyser.linked_to", stack.getTagCompound().getString("linkedName")));
        else list.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("item.neuralyser.hint"));
    }

    // ---------------------------------------------------------------- shared with the Dead Man's Neuralyser

    public static boolean isNeuralyser(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() == IqHusks.neuralyser || stack.getItem() == IqHusks.deadmansNeuralyser);
    }

    static void linkTo(ItemStack stack, EntityHusk husk) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setString("linkedUUID", husk.getUniqueID().toString());
        stack.getTagCompound().setString("linkedName", husk.hasCustomName() ? husk.getCustomNameTag() : husk.getOwnerName());
    }

    static String getLinkedUUID(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().hasKey("linkedUUID") ? stack.getTagCompound().getString("linkedUUID") : null;
    }

    /** the linked husk if it is loaded in this world (server only) */
    static EntityHusk getLinkedHusk(ItemStack stack, World world) {
        String id = getLinkedUUID(stack);
        if (id == null || !(world instanceof WorldServer)) return null;
        try {
            Entity e = ((WorldServer) world).getEntityFromUuid(UUID.fromString(id));
            return e instanceof EntityHusk && e.isEntityAlive() ? (EntityHusk) e : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** IQ's ItemModDeadMansNeuralyser: chestplate special-slot mod; on death you wake up in the linked husk */
    public static class DeadMans extends ItemArmorMod {
        public DeadMans() {
            super(ArmorModHandler.extra, false, true, false, false, "deadmans_neuralyser");
            ModItems.ALL_ITEMS.remove(this);
        }

        @Override
        public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
            tooltip(stack, list);
            list.add(TextFormatting.YELLOW + I18nUtil.resolveKey("item.deadmans_neuralyser.desc"));
            super.addInformation(stack, world, list, flag);
        }

        @Override
        public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean inHand) {
            if (!world.isRemote && inHand && entity instanceof EntityPlayer && world.getTotalWorldTime() % 10 == 0) hud((EntityPlayer) entity, stack);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void addDesc(List<String> list, ItemStack stack, ItemStack armor) {
            list.add(TextFormatting.GRAY + "  " + stack.getDisplayName());
        }
    }
}
