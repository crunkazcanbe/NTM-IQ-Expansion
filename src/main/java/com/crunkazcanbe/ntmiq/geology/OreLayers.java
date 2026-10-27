package com.crunkazcanbe.ntmiq.geology;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.fml.common.IWorldGenerator;

/**
 * Our own copy of HBM's OreLayer3D (IQ version): big 3D-noise stone layers between y 6 and 64. CE's OreLayer3D only
 * replaces overworld stone, IQ also puts layers in the Nether (replacing netherrack), so this one takes the block to replace.
 */
final class OreLayers implements IWorldGenerator {
    private static final int MIN_Y = 6, MAX_Y = 64, HEIGHT = MAX_Y - MIN_Y + 1;

    static final List<Layer> LAYERS = new ArrayList<>();

    static void add(Block ore, int dim, Block replace, double scaleH, double scaleV, double threshold) {
        LAYERS.add(new Layer(ore.getDefaultState(), dim, BlockMatcher.forBlock(replace), scaleH, scaleV, threshold, LAYERS.size()));
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator gen, IChunkProvider provider) {
        int dim = world.provider.getDimension();
        for (Layer l : LAYERS) if (l.dim == dim) l.generate(world, chunkX * 16 + 8, chunkZ * 16 + 8);
    }

    static final class Layer {
        final IBlockState ore;
        final int dim, id;
        final BlockMatcher replace;
        final double scaleH, scaleV, threshold;
        private long seed = Long.MIN_VALUE;
        private NoiseGeneratorPerlin noiseX, noiseY, noiseZ;
        private final double[][] cacheX = new double[16][HEIGHT], cacheZ = new double[16][HEIGHT];
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        Layer(IBlockState ore, int dim, BlockMatcher replace, double scaleH, double scaleV, double threshold, int id) {
            this.ore = ore; this.dim = dim; this.replace = replace; this.scaleH = scaleH; this.scaleV = scaleV; this.threshold = threshold; this.id = id;
        }

        void generate(World world, int startX, int startZ) {
            if (noiseX == null || world.getSeed() != seed) {
                seed = world.getSeed();
                // +900 keeps our noise fields apart from CE's own OreLayer3D ids
                noiseX = new NoiseGeneratorPerlin(new Random(seed + 101L + 900 + id), 4);
                noiseY = new NoiseGeneratorPerlin(new Random(seed + 102L + 900 + id), 4);
                noiseZ = new NoiseGeneratorPerlin(new Random(seed + 103L + 900 + id), 4);
            }
            for (int o = 0; o < 16; o++) {
                for (int i = 0; i < HEIGHT; i++) {
                    int y = MAX_Y - i;
                    cacheX[o][i] = noiseX.getValue(y * scaleV, (startZ + o) * scaleH);
                    cacheZ[o][i] = noiseZ.getValue((startX + o) * scaleH, y * scaleV);
                }
            }
            for (int ox = 0; ox < 16; ox++) {
                int x = startX + ox;
                for (int oz = 0; oz < 16; oz++) {
                    int z = startZ + oz;
                    double ny = noiseY.getValue(x * scaleH, z * scaleH);
                    for (int i = 0; i < HEIGHT; i++) {
                        if (cacheX[oz][i] * ny * cacheZ[ox][i] <= threshold) continue;
                        pos.setPos(x, MAX_Y - i, z);
                        IBlockState state = world.getBlockState(pos);
                        if (state.getMaterial() == Material.ROCK && state.isNormalCube() && state.getBlock().isReplaceableOreGen(state, world, pos, replace)) {
                            world.setBlockState(pos, ore, 2 | 16);
                        }
                    }
                }
            }
        }
    }
}
