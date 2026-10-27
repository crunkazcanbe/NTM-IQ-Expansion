package com.crunkazcanbe.ntmiq.magsep;

import com.crunkazcanbe.ntmiq.IqContent;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityProxyCombo;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** Magnetic Separator — 5x5x3, the whole outer ring takes pipes/power/items. Ported from NTM: IQ Edition (GPL-3). */
public class MachineMagneticSeparator extends BlockDummyable {

    public MachineMagneticSeparator() {
        super(Material.IRON, "machine_magnetic_separator");
        ModBlocks.ALL_BLOCKS.remove(this); // CE would register it a second time
        setTranslationKey("ntmiq.machine_magnetic_separator");
        setHardness(5.0F);
        setResistance(30.0F);
        setCreativeTab(IqContent.TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if (meta >= 12) return new TileEntityMachineMagneticSeparator();
        if (meta >= extra) return new TileEntityProxyCombo(true, true, true);
        return null;
    }

    @Override public int[] getDimensions() { return new int[] { 2, 0, 2, 2, 2, 2 }; }
    @Override public int getOffset() { return 2; }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return standardOpenBehavior(world, pos, player, 0);
    }

    @Override
    protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        x += dir.offsetX * o;
        z += dir.offsetZ * o;
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++)
            if (Math.abs(i) == 2 || Math.abs(j) == 2) makeExtra(world, x + i, y, z + j);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        for (String line : I18n.format("tile.ntmiq.machine_magnetic_separator.desc").split("\\$")) list.add(TextFormatting.YELLOW + line);
    }
}
