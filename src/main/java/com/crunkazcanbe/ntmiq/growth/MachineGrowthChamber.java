package com.crunkazcanbe.ntmiq.growth;

import com.crunkazcanbe.ntmiq.IqContent;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityProxyCombo;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Growth Chamber — 3x3x3 with pipe/power ports on the four side centres, top and bottom. Ported from NTM: IQ Edition (GPL-3). */
public class MachineGrowthChamber extends BlockDummyable {

    public MachineGrowthChamber() {
        super(Material.IRON, "machine_growth_chamber");
        ModBlocks.ALL_BLOCKS.remove(this); // CE would register it a second time
        setTranslationKey("ntmiq.machine_growth_chamber");
        setHardness(10.0F);
        setResistance(20.0F);
        setCreativeTab(IqContent.TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if (meta >= 12) return new TileEntityMachineGrowthChamber();
        if (meta >= 6) return new TileEntityProxyCombo(true, true, true);
        return null;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return standardOpenBehavior(world, pos, player, 0);
    }

    @Override public int[] getDimensions() { return new int[] { 2, 0, 1, 1, 1, 1 }; }
    @Override public int getOffset() { return 1; }

    @Override
    protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        x -= dir.offsetX;
        z -= dir.offsetZ;
        for (int h : new int[] { 0, 2 }) {
            makeExtra(world, x + 1, y + h, z);
            makeExtra(world, x - 1, y + h, z);
            makeExtra(world, x, y + h, z + 1);
            makeExtra(world, x, y + h, z - 1);
        }
    }
}
