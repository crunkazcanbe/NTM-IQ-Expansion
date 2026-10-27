package com.crunkazcanbe.ntmiq.sludge;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityProxyCombo;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/** IQ's MachineSludgeProcessor: 5x5 footprint, 5 tall; the four corner-adjacent ends carry the connectors. */
public class BlockSludgeProcessor extends BlockDummyable implements ITooltipProvider {

    public BlockSludgeProcessor(String name) {
        super(Material.IRON, name);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if (meta >= 12) return new TileEntitySludgeProcessor();
        if (meta >= 6) return new TileEntityProxyCombo(true, true, true);
        return null;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return standardOpenBehavior(world, pos, player, 0);
    }

    @Override public int[] getDimensions() { return new int[] {4, 0, 2, 2, 2, 1}; }
    @Override public int getOffset() { return 2; }

    @Override
    public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        switch (dir) {
            case EAST: makeExtra(world, x, y, z - 1); makeExtra(world, x, y, z + 2); makeExtra(world, x - 4, y, z - 1); makeExtra(world, x - 4, y, z + 2); break;
            case WEST: makeExtra(world, x, y, z - 2); makeExtra(world, x, y, z + 1); makeExtra(world, x + 4, y, z - 2); makeExtra(world, x + 4, y, z + 1); break;
            case SOUTH: makeExtra(world, x - 2, y, z); makeExtra(world, x + 1, y, z); makeExtra(world, x - 2, y, z - 4); makeExtra(world, x + 1, y, z - 4); break;
            case NORTH: makeExtra(world, x - 1, y, z); makeExtra(world, x + 2, y, z); makeExtra(world, x - 1, y, z + 4); makeExtra(world, x + 2, y, z + 4); break;
            default: break;
        }
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        addStandardInfo(tooltip);
    }
}
