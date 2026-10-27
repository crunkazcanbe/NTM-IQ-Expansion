package com.crunkazcanbe.ntmiq.foam;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/** IQ's BlockConstructionFoam: soft, walk-through foam that hardens into its material 200 ticks after it is placed. */
public class BlockConstructionFoam extends Block {
    public static final PropertyInteger TYPE = PropertyInteger.create("type", 0, 15);

    /** what each foam type hardens into (unset = hardened construction foam) */
    public static final IBlockState[] HARDENS_TO = new IBlockState[16];

    public BlockConstructionFoam() {
        super(Material.CLOTH);
    }

    public static IBlockState getResult(int type) {
        IBlockState s = HARDENS_TO[type & 15];
        return s != null ? s : IqFoam.hardened.getDefaultState();
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, TYPE); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(TYPE, meta & 15); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(TYPE); }

    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return NULL_AABB; }
    @Override public Item getItemDropped(IBlockState state, Random rand, int fortune) { return null; }
    @Override public int quantityDropped(Random rand) { return 0; }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) world.scheduleUpdate(pos, this, 200);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!world.isRemote && state.getBlock() == this) world.setBlockState(pos, getResult(state.getValue(TYPE)), 3);
    }
}
