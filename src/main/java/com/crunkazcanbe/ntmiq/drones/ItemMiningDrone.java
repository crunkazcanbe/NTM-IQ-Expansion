package com.crunkazcanbe.ntmiq.drones;

import com.crunkazcanbe.ntmiq.IqContent;
import com.hbm.blocks.ModBlocks;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;

import java.util.List;

/**
 * The six drones NTM: IQ Edition (1.7.10, GPL-3; irislgtm, tam69 and the IQ team) appended to HBM's ItemDrone enum.
 * A separate item here so CE's own drone enum stays untouched. Placement mirrors IQ's ItemDrone.onItemUse.
 */
public class ItemMiningDrone extends Item {

    public static final int DEMOLISHER = 0, DEMOLISHER_CHUNKLOADING = 1, DEMOLISHER_EXPRESS = 2, DEMOLISHER_EXPRESS_CHUNKLOADING = 3,
            SPELUNKER = 4, QUARRY = 5;
    public static final String[] NAMES = { "demolisher", "demolisher_chunkloading", "demolisher_express", "demolisher_express_chunkloading",
            "spelunker", "quarry" };

    public ItemMiningDrone() {
        setHasSubtypes(true);
        setRegistryName("ntmiq", "drone");
        setTranslationKey("ntmiq.drone");
        setCreativeTab(IqContent.TAB);
    }

    static String name(int meta) {
        return NAMES[Math.floorMod(meta, NAMES.length)];
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return "item.ntmiq.drone." + name(stack.getMetadata());
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (isInCreativeTab(tab)) for (int i = 0; i < NAMES.length; i++) items.add(new ItemStack(this, 1, i));
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hx, float hy, float hz) {
        if (facing != EnumFacing.UP) return EnumActionResult.FAIL;
        if (world.isRemote) return EnumActionResult.SUCCESS;

        ItemStack stack = player.getHeldItem(hand);
        int meta = stack.getMetadata();
        Entity toSpawn;

        if (meta == SPELUNKER || meta == QUARRY) {
            if (world.getBlockState(pos).getBlock() != ModBlocks.drone_crate) return EnumActionResult.FAIL;
            EntityCrateDrone d = meta == SPELUNKER ? new EntitySpelunkingDrone(world) : new EntityQuarryDrone(world);
            d.setBase(pos.getX(), pos.getY(), pos.getZ());
            toSpawn = d;
        } else {
            EntityDemolisherDrone d = new EntityDemolisherDrone(world);
            if (meta % 2 == 1) d.setChunkLoading();
            if (meta % 4 >= 2) d.setExpress();
            toSpawn = d;
        }

        toSpawn.setPosition(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        if (!world.spawnEntity(toSpawn)) {
            if (toSpawn instanceof EntityDemolisherDrone) ((EntityDemolisherDrone) toSpawn).clearChunkLoader();
            return EnumActionResult.FAIL;
        }
        if (!player.capabilities.isCreativeMode) stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            for (String s : I18nUtil.resolveKeyArray(getTranslationKey(stack) + ".desc")) list.add(TextFormatting.YELLOW + s);
        } else {
            list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "Hold <" + TextFormatting.YELLOW + "" + TextFormatting.ITALIC
                    + "LSHIFT" + TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "> to display more info");
        }
    }
}
