package com.crunkazcanbe.ntmiq.husks;

import java.util.List;

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

/** IQ's MachineHaemodialysis (NTM: IQ Edition, GPL-3): 3x3 base, 4 tall, the four corners carry the connectors. */
public class BlockHaemodialysis extends BlockDummyable implements ITooltipProvider {

    public BlockHaemodialysis(String name) { super(Material.IRON, name); }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if (meta >= 12) return new TileEntityHaemodialysis();
        if (meta >= 6) return new TileEntityProxyCombo(true, true, true);
        return null;
    }

    @Override public int[] getDimensions() { return new int[] { 3, 0, 1, 1, 1, 1 }; }
    @Override public int getOffset() { return 1; }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return standardOpenBehavior(world, pos, player, 0);
    }

    @Override
    public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        x += dir.offsetX * o;
        z += dir.offsetZ * o;
        makeExtra(world, x + 1, y, z + 1);
        makeExtra(world, x + 1, y, z - 1);
        makeExtra(world, x - 1, y, z + 1);
        makeExtra(world, x - 1, y, z - 1);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        addStandardInfo(tooltip);
    }
}
