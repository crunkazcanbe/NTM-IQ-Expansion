package com.crunkazcanbe.ntmiq.husks;

import com.crunkazcanbe.ntmiq.blood.Bloodstream;
import com.crunkazcanbe.ntmiq.blood.BloodstreamAPI;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The body swap (IQ: ItemNeuralyser.transfer / transferOnDeath + PlayerNBTNeuralyserPacket), rebuilt to be
 * multiplayer-safe: IQ re-read the WHOLE player NBT on both sides through a custom packet; here only the body moves —
 * inventory, health, body stats/traits, blood type and position — all server side, synced by vanilla (inventory
 * container, health packet, attribute packet, teleport). Mind stuff (XP, advancements, spawn point, capabilities) stays.
 *
 * <p>Inventory safety: both inventories are serialized before anything changes and simply exchanged, so every item is
 * always either on the player or in the husk. On death the dying body's items drop where it died (keepInventory: the
 * player keeps them and the husk's items drop at the husk instead). Config "bodySwap=false" turns all of this off.
 */
public final class HuskSwap {

    /** player's mind goes into the husk, the player's old body stays behind as the husk */
    static void swap(EntityPlayerMP p, EntityHusk h) {
        if (!IqHusks.bodySwap || h.isDead || h.world != p.world) return;
        p.closeScreen();   // puts any cursor-held stack back into the inventory first

        NBTTagList oldInv = p.inventory.writeToNBT(new NBTTagList());
        int oldSel = p.inventory.currentItem;
        float oldHp = p.getHealth();
        int[] oldStats = HuskBody.stats(p);
        String oldTraits = HuskBody.traits(p), oldName = HuskBody.ownerName(p), oldUuid = HuskBody.ownerUuid(p);
        FluidType oldBlood = BloodstreamAPI.get(p).getBloodType();
        double px = p.posX, py = p.posY, pz = p.posZ;
        float yaw = p.rotationYaw, pitch = p.rotationPitch;

        takeBody(p, h, false);

        h.setBody(oldInv, oldSel, oldStats, oldTraits, oldHp);
        h.setOwner(oldName, oldUuid);
        BloodstreamAPI.get(h).setBloodType(oldBlood);
        h.setLocationAndAngles(px, py, pz, yaw, pitch);
    }

    /** move the player into the husk's place and body; the husk keeps nothing of value afterwards unless swap() refills it */
    private static void takeBody(EntityPlayerMP p, EntityHusk h, boolean keepOwnItems) {
        if (!keepOwnItems) {
            p.inventory.readFromNBT(h.inv);   // clears first, then loads the husk's items
            p.inventory.currentItem = h.selected;
        }
        h.inv = new NBTTagList();             // the items now belong to the player: never leave a second copy behind
        Bloodstream hb = BloodstreamAPI.get(h);
        BloodstreamAPI.get(p).setBloodType(hb.getBloodType());
        HuskBody.set(p, h.stats.clone(), h.traits, h.getOwnerName(), h.getOwnerUUID());
        p.setHealth(Math.max(1F, Math.min(h.getHealth(), p.getMaxHealth())));
        p.fallDistance = 0F;
        p.extinguish();
        p.connection.setPlayerLocation(h.posX, h.posY, h.posZ, h.rotationYaw, h.rotationPitch);
        p.inventoryContainer.detectAndSendChanges();
        p.world.playSound(null, h.posX, h.posY, h.posZ, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 1F, 1.2F);
    }

    // ---------------------------------------------------------------- Dead Man's Neuralyser

    /** HIGH so it runs before grave/death-chest mods take the items */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onDeath(LivingDeathEvent e) {
        if (!(e.getEntityLiving() instanceof EntityPlayerMP) || !IqHusks.bodySwap || !IqHusks.deathSwap) return;
        EntityPlayerMP p = (EntityPlayerMP) e.getEntityLiving();
        ItemStack chest = p.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (chest.isEmpty() || !ArmorModHandler.hasMods(chest)) return;
        ItemStack mod = ArmorModHandler.pryMod(chest, ArmorModHandler.extra);
        if (mod.getItem() != IqHusks.deadmansNeuralyser) return;
        EntityHusk h = ItemNeuralyser.getLinkedHusk(mod, p.world);
        if (h == null || !h.isAdopted()) return;

        e.setCanceled(true);
        p.closeScreen();
        boolean keep = p.world.getGameRules().getBoolean("keepInventory");
        if (keep) h.dropStoredInventory();    // you keep your stuff, the husk's stuff falls out where it stood
        else p.inventory.dropAllItems();      // the dead body's stuff stays where it died
        p.clearActivePotions();
        takeBody(p, h, keep);
        h.setDead();
    }
}
