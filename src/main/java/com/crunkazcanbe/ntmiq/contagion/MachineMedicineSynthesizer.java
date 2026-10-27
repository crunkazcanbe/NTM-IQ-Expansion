package com.crunkazcanbe.ntmiq.contagion;

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

/**
 * The Dial-A-Dose block (IQ: blocks/machine/BlockMedicineSynthesizer, GPL-3): a 2-wide, 2-tall lab unit with its power
 * and fluid ports on the sides of the core.
 */
public class MachineMedicineSynthesizer extends BlockDummyable {

    public MachineMedicineSynthesizer() {
        super(Material.IRON, "machine_medicine_synthesizer");
        ModBlocks.ALL_BLOCKS.remove(this);   // CE would register it a second time
        setTranslationKey("ntmiq.machine_medicine_synthesizer");
        setHardness(10.0F);
        setResistance(20.0F);
        setCreativeTab(IqContent.TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if (meta >= 12) return new TileEntityMedicineSynthesizer();
        if (meta >= 6) return new TileEntityProxyCombo(true, true, false);
        return null;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        return standardOpenBehavior(world, pos, player, 0);
    }

    @Override public int[] getDimensions() { return new int[] { 1, 0, 0, 1, 0, 1 }; }
    @Override public int getOffset() { return 0; }

    @Override
    protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        makeExtra(world, x + 2, y, z);
        makeExtra(world, x - 2, y, z);
        makeExtra(world, x, y, z + 2);
        makeExtra(world, x, y, z - 2);
    }
}
