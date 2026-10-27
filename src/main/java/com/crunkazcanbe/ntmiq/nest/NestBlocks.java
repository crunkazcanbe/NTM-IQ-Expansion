package com.crunkazcanbe.ntmiq.nest;

import java.util.Random;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockGlyphidSpawner;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The Nest's blocks, ported from NTM: IQ Edition (1.7.10, GPL-3; irislgtm / tam69 and the IQ team).
 * IQ's metadata blocks (nether_glyphid, plant_flesh) are one block per variant here, like the rest of this add-on.
 */
public final class NestBlocks {
    private NestBlocks() {}

    /** IQ's soil check for everything that grows in The Nest */
    static boolean isNestSoil(Block b) {
        return b == Blocks.NETHERRACK || b == Blocks.SOUL_SAND || b == ModBlocks.glyphid_base || b instanceof NetherGlyphid;
    }

    /** nether_glyphid (crevasse / grub / overtaken / hive / bloatwisp): fleshy, drops nothing */
    public static class NetherGlyphid extends Block {
        final boolean bloatwisp;

        NetherGlyphid(boolean bloatwisp) {
            super(Material.CORAL);
            this.bloatwisp = bloatwisp;
            setSoundType(SoundType.SLIME);
        }

        @Override public Item getItemDropped(IBlockState state, Random rand, int fortune) { return Items.AIR; }

        /** bloatwisp nests keep their bloatfire burning forever, like netherrack does for fire */
        @Override public boolean isFireSource(World world, BlockPos pos, EnumFacing side) {
            return bloatwisp && side == EnumFacing.UP;
        }

        /** IQ: plain fire lit on a bloatwisp nest turns into bloatfire */
        @SuppressWarnings("deprecation")
        @Override public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
            if (bloatwisp && world.getBlockState(pos.up()).getBlock() == Blocks.FIRE)
                world.setBlockState(pos.up(), IqNest.bloatfire.getDefaultState(), 2);
        }
    }

    /** shared bush base: grows on netherrack / soul sand / glyphid flesh (vanilla bushes would pop off netherrack) */
    public static class NestBush extends BlockBush {
        NestBush() { setSoundType(SoundType.SLIME); }
        @Override protected boolean canSustainBush(IBlockState state) { return isNestSoil(state.getBlock()); }
        @Override public boolean canBlockStay(World world, BlockPos pos, IBlockState state) { return canSustainBush(world.getBlockState(pos.down())); }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    }

    /** plant_flesh: tendonweed, bulb plants, fevershrooms ... only shears keep them */
    public static class FleshPlant extends NestBush {
        @Override public Item getItemDropped(IBlockState state, Random rand, int fortune) { return Items.AIR; }

        @Override
        public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack tool) {
            if (!world.isRemote && tool.getItem() instanceof ItemShears) spawnAsEntity(world, pos, new ItemStack(this));
            else super.harvestBlock(world, player, pos, state, te, tool);
        }
    }

    /** cyst ("or boils, you tell me"): spurts blood. IQ uses a geyser TE; client particles do the same job here */
    public static class Cyst extends Block {
        Cyst() { super(Material.CORAL); setSoundType(SoundType.CLOTH); }

        @SideOnly(Side.CLIENT)
        @Override
        public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
            int n = rand.nextInt(12) == 0 ? 20 : rand.nextInt(3) == 0 ? 1 : 0;   // the odd big spurt
            for (int i = 0; i < n; i++)
                world.spawnParticle(EnumParticleTypes.REDSTONE, pos.getX() + 0.3 + rand.nextDouble() * 0.4,
                        pos.getY() + 1.0625 + (n > 1 ? rand.nextDouble() * 1.5 : 0), pos.getZ() + 0.3 + rand.nextDouble() * 0.4, 0.7, 0.05, 0.05);
        }
    }

    /**
     * ore_bedrock_artery: Satan's Blood bedrock vein (unbreakable, infinite). IQ makes it a fluid reserve for derricks;
     * CE's drills only accept CE's own oil blocks, so an {@link ArteryPump} anywhere above it pumps IqFluids.SATANS_BLOOD.
     */
    public static class Artery extends Block {
        Artery() {
            super(Material.ROCK);
            setBlockUnbreakable();
            setResistance(1_000_000F);
            setSoundType(SoundType.STONE);
        }
        @Override public Item getItemDropped(IBlockState state, Random rand, int fortune) { return Items.AIR; }
    }

    public static class BloatLog extends BlockRotatedPillar {
        BloatLog() { super(Material.WOOD); setSoundType(SoundType.SLIME); }
        @Override public boolean isWood(IBlockAccess world, BlockPos pos) { return true; }
    }

    /** IQ's leaves never decay (worldgen meta 8), so a plain non-opaque block is enough */
    public static class BloatLeaves extends Block {
        BloatLeaves() { super(Material.LEAVES); setSoundType(SoundType.SLIME); setLightOpacity(1); }
        @Override public Item getItemDropped(IBlockState state, Random rand, int fortune) { return Item.getItemFromBlock(IqNest.bloatSapling); }
        @Override public int quantityDropped(Random rand) { return rand.nextInt(10) == 0 ? 1 : 0; }   // leaves 1/20 + IQ's extra 5%
        @SuppressWarnings("deprecation") @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT_MIPPED; }
        @Override public boolean isLeaves(IBlockState state, IBlockAccess world, BlockPos pos) { return true; }
    }

    public static class BloatSapling extends NestBush implements IGrowable {
        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
            super.updateTick(world, pos, state, rand);
            if (!world.isRemote && world.getLightFromNeighbors(pos.up()) >= 9 && rand.nextInt(7) == 0) grow(world, rand, pos, state);
        }

        @Override public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) { return true; }
        @Override public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) { return true; }

        @Override
        public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
            world.setBlockToAir(pos);
            if (!BiomeTheNest.growBloatsprout(world, rand, pos)) world.setBlockState(pos, state, 2);
        }
    }

    /** bloatfire: IQ's yellow nest fire. Vanilla fire logic spreads it as itself (tryCatchFire uses this.getDefaultState) */
    public static class Bloatfire extends BlockFire {
        Bloatfire() { setLightLevel(1F); setSoundType(SoundType.CLOTH); }
    }

    /** glyphid_spawner_nether: CE's glyphid hive spawner, swapping every glyphid it makes for its nether variant */
    public static class SpawnerNether extends BlockContainer {
        SpawnerNether() { super(Material.CORAL); setSoundType(SoundType.SLIME); }
        @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileSpawnerNether(); }
        @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }
        @Override public Item getItemDropped(IBlockState state, Random rand, int fortune) { return IqNest.eggNether; }
    }

    public static class TileSpawnerNether extends BlockGlyphidSpawner.TileEntityGlyphidSpawner {
        @Override public void trySpawnEntity(EntityGlyphid glyphid) { super.trySpawnEntity(NetherGlyphids.toNether(glyphid)); }
    }
}
