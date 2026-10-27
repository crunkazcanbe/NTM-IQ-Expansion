package com.crunkazcanbe.ntmiq.growth;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;
import java.util.function.Supplier;

/**
 * A crystal bud growing out of a budding block (AE2 / amethyst style), stage 0-3. FACING = the direction it grows towards.
 * Ported from NTM: IQ Edition's BlockBudBase / BlockBudSalt / BlockBudCertusQuartz (GPL-3; irislgtm, tam69 and the IQ team).
 * IQ drew buds with an ISBRH (two crossed quads); here that is a rotated vanilla "block/cross" baked model.
 */
public class BlockBud extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    final int stage;
    private final float halfWidth, height;
    private final Supplier<ItemStack> drop;
    Block nextStage;

    public BlockBud(Material mat, SoundType sound, int stage, float halfWidth, float height, Supplier<ItemStack> drop) {
        super(mat);
        this.stage = stage;
        this.halfWidth = halfWidth;
        this.height = height;
        this.drop = drop;
        setSoundType(sound);
        setHardness(1.5F);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
    }

    BlockBud next(Block next) {
        this.nextStage = next;
        return this;
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta)); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getIndex(); }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float hx, float hy, float hz, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, side);
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return canStay(world, pos, side);
    }

    static boolean canStay(World world, BlockPos pos, EnumFacing dir) {
        return world.isSideSolid(pos.offset(dir.getOpposite()), dir, false);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!canStay(world, pos, state.getValue(FACING))) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        EnumFacing d = state.getValue(FACING);
        return new AxisAlignedBB(min(d.getXOffset()), min(d.getYOffset()), min(d.getZOffset()), max(d.getXOffset()), max(d.getYOffset()), max(d.getZOffset()));
    }

    private float min(int o) { return o == 1 ? 0 : o == -1 ? 1 - height : 0.5F - halfWidth; }
    private float max(int o) { return o == 1 ? height : o == -1 ? 1 : 0.5F + halfWidth; }

    /** stage 2+ drops shards (1 for a large bud, 2-4 for a cluster); smaller buds drop nothing without silk touch */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (stage < 2) return;
        ItemStack s = drop.get();
        if (s.isEmpty()) return;
        Random rand = world instanceof World ? ((World) world).rand : RANDOM;
        s.setCount(stage == 2 ? 1 : 2 + rand.nextInt(3));
        drops.add(s);
    }

    @Override protected boolean canSilkHarvest() { return true; }
    @Override public EnumPushReaction getPushReaction(IBlockState state) { return EnumPushReaction.DESTROY; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) { return BlockFaceShape.UNDEFINED; }
    @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
}
