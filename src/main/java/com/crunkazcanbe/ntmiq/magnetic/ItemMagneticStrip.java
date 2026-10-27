package com.crunkazcanbe.ntmiq.magnetic;

import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.List;

/** IQ's ItemMagneticStrip: use on any CE crate to make it magnetic. */
public class ItemMagneticStrip extends Item {

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityCrateBase)) return EnumActionResult.PASS;
        if (world.isRemote) return EnumActionResult.SUCCESS;

        if (MagneticCrates.isMagnetic(te)) {
            TextComponentTranslation msg = new TextComponentTranslation("item.magnetic_strip.already");
            msg.getStyle().setColor(TextFormatting.YELLOW);
            player.sendMessage(msg);
            return EnumActionResult.SUCCESS;
        }

        MagneticCrates.setMagnetic(te, true);
        if (!player.capabilities.isCreativeMode) player.getHeldItem(hand).shrink(1);
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        list.add(TextFormatting.GRAY + I18n.translateToLocal("item.magnetic_strip.desc"));
    }
}
