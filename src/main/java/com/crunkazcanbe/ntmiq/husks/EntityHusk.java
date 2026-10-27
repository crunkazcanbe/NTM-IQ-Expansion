package com.crunkazcanbe.ntmiq.husks;

import java.util.UUID;

import com.crunkazcanbe.ntmiq.blood.BloodBehavior;
import com.crunkazcanbe.ntmiq.blood.BloodstreamAPI;
import com.crunkazcanbe.ntmiq.husks.ItemHumanPart.Stat;
import com.hbm.inventory.fluid.FluidType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * IQ's EntityHusk (NTM: IQ Edition, GPL-3; irislgtm / tam69 / IQ team): a body without a mind, grown by the Cloner.
 * It stores a player-format inventory ({@link net.minecraft.entity.player.InventoryPlayer#writeToNBT}), body stats and
 * traits, and looks like its genome owner. A neuralyser moves a player's mind into it ({@link HuskSwap}).
 *
 * <p>Multiplayer: the client only ever sees the owner name/UUID (data manager) and the worn/held items (vanilla
 * equipment sync, display copies with drop chance 0). Items live in {@link #inv} on the server only.
 * Unlike IQ, a spawn-egg husk adopts the nearest player's looks only, never a copy of his inventory (IQ duplicated it).
 */
public class EntityHusk extends EntityLiving {

    private static final DataParameter<String> OWNER_NAME = EntityDataManager.createKey(EntityHusk.class, DataSerializers.STRING);
    private static final DataParameter<String> OWNER_UUID = EntityDataManager.createKey(EntityHusk.class, DataSerializers.STRING);

    static final int TRANSFER_DELAY = 26;

    NBTTagList inv = new NBTTagList();
    int selected;
    int[] stats = new int[Stat.values().length];
    String traits = "";
    boolean adopted;

    private int transferDelay;
    private UUID transferPlayer;

    public EntityHusk(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        experienceValue = 0;
        for (EntityEquipmentSlot s : EntityEquipmentSlot.values()) setDropChance(s, 0F);
        enablePersistence();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(OWNER_NAME, "");
        dataManager.register(OWNER_UUID, "");
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20D);
    }

    public String getOwnerName() { return dataManager.get(OWNER_NAME); }

    public String getOwnerUUID() { return dataManager.get(OWNER_UUID); }

    public boolean isAdopted() { return adopted; }

    void setOwner(String name, String uuid) {
        dataManager.set(OWNER_NAME, name == null ? "" : name);
        dataManager.set(OWNER_UUID, uuid == null ? "" : uuid);
    }

    /** Cloner output: genome owner, the five parts' summed stats/traits, and the tank's fluid as blood */
    void setupClone(String uuid, String name, int[] totals, String traitList, FluidType blood) {
        setOwner(name, uuid);
        if (blood != null) BloodstreamAPI.get(this).setBloodType(blood);
        BloodBehavior b = BloodstreamAPI.getBehavior(BloodstreamAPI.get(this).getBloodType());
        if (b != null) b.applyStats(totals);   // StatBonus bloods (Med-X, stimpak, psycho ...) add their bonus
        setBody(new NBTTagList(), 0, totals, traitList, -1F);
        adopted = true;
    }

    /** body the husk now wears; health < 0 = full */
    void setBody(NBTTagList inventory, int selectedSlot, int[] bodyStats, String bodyTraits, float health) {
        inv = inventory == null ? new NBTTagList() : inventory;
        selected = Math.max(0, Math.min(8, selectedSlot));
        stats = bodyStats != null && bodyStats.length == Stat.values().length ? bodyStats : new int[Stat.values().length];
        traits = bodyTraits == null ? "" : bodyTraits;
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20D + HuskBody.stat(stats, Stat.VITALITY) / 20D);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(1D + HuskBody.stat(stats, Stat.STRENGTH) / 100D);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.1D + HuskBody.stat(stats, Stat.AGILITY) / 20000D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(HuskBody.stat(stats, Stat.TOUGHNESS) / 1000D);
        setHealth(health < 0 ? getMaxHealth() : Math.max(1F, Math.min(health, getMaxHealth())));
        refreshEquipment();
    }

    /** mirror armor + selected hotbar item as display-only equipment */
    private void refreshEquipment() {
        for (EntityEquipmentSlot s : EntityEquipmentSlot.values()) setItemStackToSlot(s, ItemStack.EMPTY);
        for (int i = 0; i < inv.tagCount(); i++) {
            NBTTagCompound t = inv.getCompoundTagAt(i);
            int slot = t.getByte("Slot") & 255;
            ItemStack s = new ItemStack(t);
            if (s.isEmpty()) continue;
            if (slot == selected) setItemStackToSlot(EntityEquipmentSlot.MAINHAND, s);
            else if (slot == 150) setItemStackToSlot(EntityEquipmentSlot.OFFHAND, s);
            else if (slot >= 100 && slot < 104) setItemStackToSlot(EntityEquipmentSlot.values()[2 + slot - 100], s);   // FEET, LEGS, CHEST, HEAD
        }
    }

    void beginTransfer(EntityPlayer player) {
        if (transferDelay > 0) return;
        transferDelay = TRANSFER_DELAY;
        transferPlayer = player.getUniqueID();
        // IQ plays a glitch-shader fade; vanilla nausea + blindness give the same "losing yourself" moment with zero netcode
        player.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, TRANSFER_DELAY + 40, 0, false, false));
        player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, TRANSFER_DELAY + 10, 0, false, false));
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 1F, 2F);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) return;

        if (!adopted) {   // spawn egg: look like the nearest player, nothing else
            EntityPlayer p = world.getClosestPlayerToEntity(this, 16D);
            if (p != null) {
                setOwner(p.getName(), p.getUniqueID().toString());
                adopted = true;
            }
        }

        if (transferDelay > 0 && --transferDelay == 0) {
            EntityPlayer p = transferPlayer == null ? null : world.getPlayerEntityByUUID(transferPlayer);
            transferPlayer = null;
            if (p instanceof EntityPlayerMP && p.isEntityAlive() && getDistance(p) <= ItemNeuralyser.LINK_RANGE) HuskSwap.swap((EntityPlayerMP) p, this);
        }
    }

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (!ItemNeuralyser.isNeuralyser(held)) return super.processInteract(player, hand);
        if (world.isRemote) return true;

        if (player.isSneaking()) {
            ItemNeuralyser.linkTo(held, this);
            world.playSound(null, posX, posY, posZ, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS, 1F, 1.5F);
            player.sendStatusMessage(new TextComponentTranslation("item.neuralyser.linked"), true);
        } else if (held.getItem() == IqHusks.neuralyser && adopted && getUniqueID().toString().equals(ItemNeuralyser.getLinkedUUID(held))) {
            beginTransfer(player);
        }
        return true;
    }

    /** the stored body inventory falls out; the display equipment never drops */
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!world.isRemote) dropStoredInventory();
    }

    void dropStoredInventory() {
        for (int i = 0; i < inv.tagCount(); i++) {
            ItemStack s = new ItemStack(inv.getCompoundTagAt(i));
            if (!s.isEmpty()) entityDropItem(s, 0F);
        }
        inv = new NBTTagList();
        refreshEquipment();
    }

    @Override protected void dropEquipment(boolean wasRecentlyHit, int lootingModifier) { }

    @Override protected boolean canDespawn() { return false; }

    @Override public boolean canPickUpLoot() { return false; }


    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setTag("huskInventory", inv);
        tag.setInteger("huskSelected", selected);
        tag.setIntArray("huskStats", stats);
        tag.setString("huskTraits", traits);
        tag.setString("huskOwner", getOwnerName());
        tag.setString("huskOwnerUUID", getOwnerUUID());
        tag.setBoolean("huskAdopted", adopted);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        setOwner(tag.getString("huskOwner"), tag.getString("huskOwnerUUID"));
        adopted = tag.getBoolean("huskAdopted");
        float hp = getHealth();
        setBody(tag.getTagList("huskInventory", 10), tag.getInteger("huskSelected"), tag.getIntArray("huskStats"), tag.getString("huskTraits"), hp);
    }
}
