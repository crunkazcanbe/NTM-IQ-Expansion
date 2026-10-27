package com.crunkazcanbe.ntmiq.detail;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Locale;

/**
 * IQ's BlockDetail (irislgtm / tam69 / IQ team, GPL-3): 16 decal types drawn on every solid face next to the block.
 * IQ used an ISBRH with 16 random texture variants per face; here the multipart blockstate (generated) picks a
 * position-random variant per face.
 */
public class BlockDetail extends Block {

    public enum EnumDetailType implements IStringSerializable {
        ASH, BLOOD, CORROSION, CRACKS, DUST, FROST, GRIME, MOLD, MOSS, MUD, OIL, RUST, SCRATCH, SLIME, SOOT, STAIN;
        @Override public String getName() { return name().toLowerCase(Locale.US); }
    }

    public static final PropertyEnum<EnumDetailType> TYPE = PropertyEnum.create("type", EnumDetailType.class);
    public static final PropertyBool[] ATTACHED = new PropertyBool[6];   // indexed by EnumFacing.getIndex()
    static {
        for (EnumFacing f : EnumFacing.VALUES) ATTACHED[f.getIndex()] = PropertyBool.create(f.getName());
    }

    public BlockDetail() {
        super(Material.VINE);
        setSoundType(SoundType.PLANT);
        setHardness(0.0F);
        setLightOpacity(0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TYPE, ATTACHED[0], ATTACHED[1], ATTACHED[2], ATTACHED[3], ATTACHED[4], ATTACHED[5]);
    }

    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(TYPE, EnumDetailType.values()[meta & 15]); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(TYPE).ordinal(); }
    @Override public int damageDropped(IBlockState state) { return getMetaFromState(state); }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        for (EnumFacing f : EnumFacing.VALUES) state = state.withProperty(ATTACHED[f.getIndex()], isAttached(world, pos, f));
        return state;
    }

    static boolean isAttached(IBlockAccess world, BlockPos pos, EnumFacing dir) {
        BlockPos n = pos.offset(dir);
        IBlockState s = world.getBlockState(n);
        return s.isSideSolid(world, n, dir.getOpposite()) || s.isOpaqueCube() || (s.isFullCube() && !s.getBlock().isAir(s, world, n));
    }

    static boolean hasAttachment(IBlockAccess world, BlockPos pos) {
        for (EnumFacing f : EnumFacing.VALUES) if (isAttached(world, pos, f)) return true;
        return false;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && hasAttachment(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!world.isRemote && !hasAttachment(world, pos)) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        double f = 0.0625D;
        return new AxisAlignedBB(isAttached(world, pos, EnumFacing.WEST) ? f : 0, isAttached(world, pos, EnumFacing.DOWN) ? f : 0,
                isAttached(world, pos, EnumFacing.NORTH) ? f : 0, isAttached(world, pos, EnumFacing.EAST) ? 1 - f : 1,
                isAttached(world, pos, EnumFacing.UP) ? 1 - f : 1, isAttached(world, pos, EnumFacing.SOUTH) ? 1 - f : 1);
    }

    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return NULL_AABB; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public boolean isPassable(IBlockAccess world, BlockPos pos) { return true; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) { return BlockFaceShape.UNDEFINED; }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
        for (EnumDetailType t : EnumDetailType.values()) items.add(new ItemStack(this, 1, t.ordinal()));
    }
}
