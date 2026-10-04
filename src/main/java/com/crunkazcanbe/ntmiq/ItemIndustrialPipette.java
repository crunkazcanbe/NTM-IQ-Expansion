package com.crunkazcanbe.ntmiq;

import java.util.List;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemArmorMod;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * IQ's Industrial Pipette: a helmet mod holding up to 16 000 mB of one HBM fluid. With an empty hand, punch an HBM
 * fluid machine/tank to pour the pipette into it; sneak-punch to suck fluid out. The action bar shows what it holds.
 * (IQ: ItemModIndustrialPipette + EventHandlerIndustrialPipette, ported to CE 1.12.2.)
 */
public class ItemIndustrialPipette extends ItemArmorMod implements IFillableItem {
    public static final int MAX_FLUID = 16000;

    public ItemIndustrialPipette(String name) {
        super(ArmorModHandler.helmet_only, true, false, false, false, name);
        ModItems.ALL_ITEMS.remove(this); // CE's base registers into CE's list; we register it ourselves (Leafia's pattern)
    }

    // ---- stored fluid (our own NBT; CE has no static helpers for this) ----
    static FluidType type(ItemStack s) {
        return s.hasTagCompound() && s.getTagCompound().hasKey("iqFluid") ? Fluids.fromID(s.getTagCompound().getInteger("iqFluid")) : Fluids.NONE;
    }

    static int fill(ItemStack s) {
        return s.hasTagCompound() ? s.getTagCompound().getInteger("iqFill") : 0;
    }

    static void set(ItemStack s, FluidType t, int amount) {
        if (amount <= 0 || t == Fluids.NONE) {
            if (s.hasTagCompound()) { s.getTagCompound().removeTag("iqFluid"); s.getTagCompound().removeTag("iqFill"); }
            return;
        }
        if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
        s.getTagCompound().setInteger("iqFluid", t.getID());
        s.getTagCompound().setInteger("iqFill", amount);
    }

    @Override public boolean acceptsFluid(FluidType t, ItemStack s) { return fill(s) == 0 || type(s) == t; }

    @Override public int tryFill(FluidType t, int amount, ItemStack s) {
        int f = fill(s);
        if (f > 0 && type(s) != t) return amount;
        int add = Math.min(amount, MAX_FLUID - f);
        set(s, t, f + add);
        return amount - add;
    }

    @Override public boolean providesFluid(FluidType t, ItemStack s) { return type(s) == t && fill(s) > 0; }

    @Override public int tryEmpty(FluidType t, int amount, ItemStack s) {
        if (!providesFluid(t, s)) return 0;
        int take = Math.min(amount, fill(s));
        set(s, t, fill(s) - take);
        return take;
    }

    @Override public FluidType getFirstFluidType(ItemStack s) { FluidType t = type(s); return t == Fluids.NONE ? null : t; }

    @Override public int getFill(ItemStack s) { return fill(s); }

    @Override
    public void addInformation(ItemStack s, World w, List<String> list, net.minecraft.client.util.ITooltipFlag flag) {
        list.add(TextFormatting.LIGHT_PURPLE + I18n.translateToLocalFormatted("item.industrial_pipette.fill", type(s).getLocalizedName(), fill(s), MAX_FLUID));
        list.add(TextFormatting.GREEN + I18n.translateToLocal("item.industrial_pipette.desc"));
        list.add(TextFormatting.GREEN + I18n.translateToLocal("item.industrial_pipette.desc2"));
        list.add("");
        super.addInformation(s, w, list, flag);
    }

    @Override
    public void addDesc(List<String> list, ItemStack s, ItemStack armor) {
        list.add(TextFormatting.LIGHT_PURPLE + "  " + s.getDisplayName() + " (" + type(s).getLocalizedName() + ": " + fill(s) + " mB)");
    }

    @Override
    public void modUpdate(EntityLivingBase e, ItemStack armor) {
        if (e.world.isRemote || !(e instanceof EntityPlayerMP) || e.world.getTotalWorldTime() % 5 != 0) return;
        ItemStack mod = ArmorModHandler.pryMods(armor)[ArmorModHandler.helmet_only];
        if (mod == null || mod.getItem() != this) return;
        String msg = fill(mod) <= 0 ? TextFormatting.GRAY + I18n.translateToLocal("item.industrial_pipette.empty")
                : TextFormatting.AQUA + type(mod).getLocalizedName() + TextFormatting.WHITE + ": " + fill(mod) + " / " + MAX_FLUID + " mB";
        ((EntityPlayerMP) e).sendStatusMessage(new TextComponentString(msg), true);
    }

    /** punch handler (IQ's EventHandlerIndustrialPipette) */
    public static class Handler {
        @SubscribeEvent
        public void onPunch(PlayerInteractEvent.LeftClickBlock event) {
            EntityPlayer player = event.getEntityPlayer();
            World world = player.world;
            if (world.isRemote || !player.getHeldItemMainhand().isEmpty()) return;
            ItemStack helmet = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
            if (helmet.isEmpty()) return;
            ItemStack mod = ArmorModHandler.pryMods(helmet)[ArmorModHandler.helmet_only];
            if (mod == null || mod.isEmpty() || !(mod.getItem() instanceof ItemIndustrialPipette)) return;

            BlockPos pos = event.getPos();
            TileEntity te = world.getTileEntity(pos);
            if (!(te instanceof IFluidStandardSenderMK2) && !(te instanceof IFluidStandardReceiverMK2)) {
                Block block = world.getBlockState(pos).getBlock();
                if (block instanceof BlockDummyable) te = ((BlockDummyable) block).findCoreTE(world, pos);
            }
            if (!(te instanceof IFluidStandardSenderMK2) && !(te instanceof IFluidStandardReceiverMK2)) return;

            FluidType ptype = type(mod);
            int pfill = fill(mod);
            boolean changed = false;
            if (player.isSneaking()) {                       // suck fluid OUT of the machine
                FluidTankNTM[] tanks = te instanceof IFluidStandardSenderMK2 ? ((IFluidStandardSenderMK2) te).getSendingTanks() : null;
                if ((tanks == null || tanks.length == 0) && te instanceof IFluidStandardTransceiverMK2) tanks = ((IFluidStandardTransceiverMK2) te).getAllTanks();
                if (tanks == null) return;
                for (FluidTankNTM tank : tanks) {
                    if (tank.getFill() <= 0 || tank.getTankType() == Fluids.NONE) continue;
                    if (pfill > 0 && ptype != Fluids.NONE && ptype != tank.getTankType()) continue;
                    int take = Math.min(MAX_FLUID - pfill, tank.getFill());
                    if (take <= 0) continue;
                    set(mod, tank.getTankType(), pfill + take);
                    tank.setFill(tank.getFill() - take);
                    changed = true;
                    break;
                }
            } else {                                         // pour the pipette INTO the machine
                if (pfill <= 0 || ptype == Fluids.NONE) return;
                FluidTankNTM[] tanks = te instanceof IFluidStandardReceiverMK2 ? ((IFluidStandardReceiverMK2) te).getReceivingTanks() : null;
                if ((tanks == null || tanks.length == 0) && te instanceof IFluidStandardTransceiverMK2) tanks = ((IFluidStandardTransceiverMK2) te).getAllTanks();
                if (tanks == null) return;
                for (FluidTankNTM tank : tanks) {
                    if (tank.getTankType() != Fluids.NONE && tank.getTankType() != ptype) continue;
                    if (tank.getTankType() == Fluids.NONE) tank.setTankType(ptype);
                    int pour = Math.min(tank.getMaxFill() - tank.getFill(), pfill);
                    if (pour <= 0) continue;
                    tank.setFill(tank.getFill() + pour);
                    set(mod, ptype, pfill - pour);
                    changed = true;
                    break;
                }
            }
            if (!changed) return;
            ArmorModHandler.applyMod(helmet, mod);           // write the updated pipette back into the helmet
            te.markDirty();
            world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5F, 1.0F);
            event.setCanceled(true);
        }
    }
}
