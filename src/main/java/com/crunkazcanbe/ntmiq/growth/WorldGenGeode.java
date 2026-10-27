package com.crunkazcanbe.ntmiq.growth;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Salt geodes: hollow sphere lined with salt crystal blocks (some budding, with buds), wrapped in a basalt shell.
 * IQ's MapGenGeode (GPL-3; irislgtm, tam69 and the IQ team) ran on the Moon; here it is an overworld populator with IQ's
 * numbers (1 in chance^2 chunks, hollow radius 3-5, y 15-45, at least 4 blocks of cover). The sphere stays inside the
 * 2x2 chunks being populated, so it never cascades into new chunks.
 */
final class WorldGenGeode implements IWorldGenerator {
    private final int chance, minY, maxY, minSize = 3, maxSize = 5, minCover = 4, buddingChance = 12, budChance = 3;
    private final IBlockState shell, crystal, budding;
    private final IBlockState[] buds;

    WorldGenGeode(int chance, int minY, int maxY, IBlockState shell, IBlockState crystal, IBlockState budding, IBlockState... buds) {
        this.chance = chance;
        this.minY = minY;
        this.maxY = maxY;
        this.shell = shell;
        this.crystal = crystal;
        this.budding = budding;
        this.buds = buds;
    }

    @Override
    public void generate(Random rand, int chunkX, int chunkZ, World world, IChunkGenerator gen, IChunkProvider provider) {
        if (world.provider.getDimension() != 0 || rand.nextInt(chance * chance) != 0) return;

        double hollowR = minSize + rand.nextInt(maxSize - minSize + 1);
        double crystalR = hollowR + 1.2, shellR = hollowR + 2.4;
        int r = (int) Math.ceil(shellR);
        int span = Math.max(1, 16 - 2 * r + 1);
        BlockPos c = new BlockPos(chunkX * 16 + 8 + r + rand.nextInt(span), minY + rand.nextInt(maxY - minY + 1), chunkZ * 16 + 8 + r + rand.nextInt(span));

        List<BlockPos> buddingBlocks = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            int limit = world.getHeight(c.getX() + dx, c.getZ() + dz) - minCover;
            for (int dy = -r; dy <= r; dy++) {
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                BlockPos p = c.add(dx, dy, dz);
                if (dist > shellR || p.getY() < 1 || p.getY() > limit) continue;
                IBlockState old = world.getBlockState(p);
                if (old.getBlock() == Blocks.BEDROCK || old.getBlock().hasTileEntity(old)) continue;

                if (dist <= hollowR) {
                    world.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                } else if (dist <= crystalR) {
                    if (rand.nextInt(buddingChance) == 0) {
                        world.setBlockState(p, budding, 2);
                        buddingBlocks.add(p);
                    } else {
                        world.setBlockState(p, crystal, 2);
                    }
                } else if (!world.isAirBlock(p)) {
                    world.setBlockState(p, shell, 2);
                }
            }
        }

        for (BlockPos b : buddingBlocks) for (EnumFacing dir : EnumFacing.VALUES) {
            BlockPos t = b.offset(dir);
            if (t.distanceSq(c) <= hollowR * hollowR && world.isAirBlock(t) && rand.nextInt(budChance) == 0)
                world.setBlockState(t, buds[rand.nextInt(buds.length)].withProperty(BlockBud.FACING, dir), 2);
        }
    }
}
