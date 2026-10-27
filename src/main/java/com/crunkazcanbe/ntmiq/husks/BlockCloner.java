package com.crunkazcanbe.ntmiq.husks;

import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityProxyCombo;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * IQ's BlockCloner (NTM: IQ Edition, GPL-3): a 2x2x3 vat. Sneak-click with a fluid identifier sets the blood fluid,
 * sneak-click empty-handed toggles it, plain click opens the GUI. The bottom layer's other three blocks connect pipes/power.
 */
public class BlockCloner extends BlockDummyable implements ITooltipProvider {

    public BlockCloner(String name) { super(Material.IRON, name); }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if (meta >= 12) return new TileEntityCloner();
        if (meta >= 6) return new TileEntityProxyCombo(true, true, true);
        return null;
    }

    @Override public int[] getDimensions() { return new int[] { 2, 0, 0, 1, 0, 1 }; }
    @Override public int getOffset() { return 0; }

    @Override
    public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        BlockPos core = new BlockPos(x, y, z);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos p = core.add(dx, 0, dz);
            if ((dx != 0 || dz != 0) && world.getBlockState(p).getBlock() == this && core.equals(findCore(world, p))) makeExtra(world, p.getX(), p.getY(), p.getZ());
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) return standardOpenBehavior(world, pos, player, 0);
        BlockPos core = findCore(world, pos);
        TileEntity te = core == null ? null : world.getTileEntity(core);
        if (!(te instanceof TileEntityCloner)) return false;
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() instanceof IItemFluidIdentifier) {
            if (!world.isRemote) ((TileEntityCloner) te).setTankType(((IItemFluidIdentifier) held.getItem()).getType(world, core.getX(), core.getY(), core.getZ(), held));
            return true;
        }
        if (held.isEmpty()) {
            if (!world.isRemote) {
                ((TileEntityCloner) te).toggle();
                world.playSound(null, core, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 1F, 1F);
            }
            return true;
        }
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        addStandardInfo(tooltip);
    }
}
