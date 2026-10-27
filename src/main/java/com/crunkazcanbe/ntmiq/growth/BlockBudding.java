package com.crunkazcanbe.ntmiq.growth;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Budding salt / budding certus quartz: random ticks grow a new bud on a free side or push an attached bud one stage on.
 * Drops nothing and can't be silk-touched or pushed — like AE2/amethyst. Ported from NTM: IQ Edition's BlockBuddingBase
 * (GPL-3; irislgtm, tam69 and the IQ team).
 */
public class BlockBudding extends Block {
    private final float growthChance;
    BlockBud firstStage;

    public BlockBudding(Material mat, SoundType sound, float growthChance) {
        super(mat);
        this.growthChance = growthChance;
        setSoundType(sound);
        setHardness(1.5F);
        setResistance(5F);
        setTickRandomly(true);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.isRemote || rand.nextFloat() > growthChance) return;
        EnumFacing dir = EnumFacing.byIndex(rand.nextInt(6));
        BlockPos target = pos.offset(dir);
        IBlockState t = world.getBlockState(target);

        if (t.getBlock().isAir(t, world, target)) {
            world.setBlockState(target, firstStage.getDefaultState().withProperty(BlockBud.FACING, dir), 3);
        } else if (t.getBlock() instanceof BlockBud && t.getValue(BlockBud.FACING) == dir) {
            Block next = ((BlockBud) t.getBlock()).nextStage;
            if (next != null) world.setBlockState(target, next.getDefaultState().withProperty(BlockBud.FACING, dir), 3);
        }
    }

    @Override public Item getItemDropped(IBlockState state, Random rand, int fortune) { return net.minecraft.init.Items.AIR; }
    @Override protected boolean canSilkHarvest() { return false; }
    @Override public EnumPushReaction getPushReaction(IBlockState state) { return EnumPushReaction.BLOCK; }
}
