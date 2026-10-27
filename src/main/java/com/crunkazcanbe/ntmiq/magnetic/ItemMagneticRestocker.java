package com.crunkazcanbe.ntmiq.magnetic;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

/** IQ's ItemMagneticRestocker ("Magnetic Stocker"): hotkeys deposit into / restock from magnetic crates. */
public class ItemMagneticRestocker extends ItemArmorMod {

    public ItemMagneticRestocker() {
        super(ArmorModHandler.extra, true, false, false, false, "magnetic_restocker");
        ModItems.ALL_ITEMS.remove(this);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(TextFormatting.YELLOW + I18nUtil.resolveKey("item.magnetic_restocker.desc"));
        list.add(TextFormatting.YELLOW + I18nUtil.resolveKey("item.magnetic_restocker.desc2"));
        list.add(TextFormatting.LIGHT_PURPLE + I18nUtil.resolveKey("item.magnetic_restocker.desc3"));
        list.add("");
        super.addInformation(stack, world, list, flag);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addDesc(List<String> list, ItemStack stack, ItemStack armor) {
        list.add(TextFormatting.YELLOW + "  " + stack.getDisplayName() + " (" + I18nUtil.resolveKey("item.magnetic_restocker.shortDesc") + ")");
    }

    /** put stacks into crates already holding that item; sneaking skips the hotbar and also fills empty slots */
    public static void deposit(EntityPlayer player, boolean shift) {
        boolean moved = false;
        List<ItemStackHandler> crates = MagneticCrates.getAllCrates(player);
        for (int i = shift ? 9 : 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (stack.isEmpty() || !stack.isStackable() || MagneticCrates.isMagneticCrateItem(stack)) continue;
            int before = stack.getCount();
            for (ItemStackHandler crate : crates) {
                stack = depositToCrate(crate, stack, shift);
                if (stack.isEmpty()) break;
            }
            if (stack.getCount() < before) {
                player.inventory.mainInventory.set(i, stack);
                moved = true;
            }
        }
        player.inventoryContainer.detectAndSendChanges();
        if (moved) playSound(player);
    }

    /** top every inventory stack back up to a full stack from the crates */
    public static void restock(EntityPlayer player) {
        boolean refilled = false;
        List<ItemStackHandler> crates = MagneticCrates.getAllCrates(player);
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.isEmpty() || MagneticCrates.isMagneticCrateItem(stack)) continue;
            int need = stack.getMaxStackSize() - stack.getCount();
            for (ItemStackHandler crate : crates) {
                for (int s = 0; s < crate.getSlots() && need > 0; s++) {
                    ItemStack existing = crate.getStackInSlot(s);
                    if (existing.isEmpty() || !canStack(existing, stack)) continue;
                    int take = Math.min(need, existing.getCount());
                    ItemStack rest = existing.copy();
                    rest.shrink(take);
                    crate.setStackInSlot(s, rest);
                    stack.grow(take);
                    need -= take;
                    refilled = true;
                }
                if (need <= 0) break;
            }
        }
        player.inventoryContainer.detectAndSendChanges();
        if (refilled) playSound(player);
    }

    private static ItemStack depositToCrate(ItemStackHandler crate, ItemStack stack, boolean shift) {
        boolean hasType = false;
        for (int i = 0; i < crate.getSlots(); i++) {
            ItemStack existing = crate.getStackInSlot(i);
            if (existing.isEmpty() || !canStack(existing, stack)) continue;
            hasType = true;
            int limit = Math.min(existing.getMaxStackSize(), crate.getSlotLimit(i));
            if (existing.getCount() >= limit) continue;
            int add = Math.min(limit - existing.getCount(), stack.getCount());
            ItemStack grown = existing.copy();
            grown.grow(add);
            crate.setStackInSlot(i, grown);
            stack.shrink(add);
            if (stack.isEmpty()) return ItemStack.EMPTY;
        }
        if (shift || hasType) {
            for (int i = 0; i < crate.getSlots(); i++) {
                if (crate.getStackInSlot(i).isEmpty() && crate.isItemValid(i, stack)) {
                    crate.setStackInSlot(i, stack.copy());
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    static boolean canStack(ItemStack a, ItemStack b) {
        return a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    static void playSound(EntityPlayer player) {
        player.world.playSound(null, player.posX, player.posY, player.posZ, IqMagnetic.MAGNETIC, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }
}
