package com.crunkazcanbe.ntmiq.nest;

import java.util.Random;

import com.crunkazcanbe.ntmiq.IqContent;
import com.hbm.blocks.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTableList;

/**
 * The Nest — IQ's glyphid-infested nether biome (BiomeGenTheNest + NetherBiomeHelper + WorldGenBloatsprout +
 * GlyphidHiveNether from NTM: IQ Edition, GPL-3; irislgtm / tam69 and the IQ team).
 * <p>
 * {@link #decorateNest} is used both by the biome (when Nether-API generates the nether) and by IqNest's fallback
 * populate hook (plain nether). Everything stays inside the 2x2 chunks that are loaded while populating
 * (corner .. corner+31): IQ's 1.7.10 offsets reach outside that and would cascade chunk generation on 1.12.
 */
public class BiomeTheNest extends Biome {

    public BiomeTheNest() {
        super(new BiomeProperties("The Nest").setTemperature(2.0F).setRainfall(0.0F).setRainDisabled().setBaseHeight(0.0F).setHeightVariation(0.05F));
        topBlock = Blocks.NETHERRACK.getDefaultState();
        fillerBlock = Blocks.NETHERRACK.getDefaultState();
        spawnableMonsterList.clear();
        spawnableCreatureList.clear();
        spawnableWaterCreatureList.clear();
        spawnableCaveCreatureList.clear();
    }

    @Override public int getSkyColorByTemp(float temp) { return 0x4A2A1A; }

    /** no overworld decorator (it would scatter grass/ores); IQ overrides decorate the same way */
    @Override
    public void decorate(World world, Random rand, BlockPos pos) {
        decorateNest(world, rand, pos.getX(), pos.getZ());
    }

    // ---------------------------------------------------------------- decoration (IQ order and counts)

    /** a random coordinate for a feature of radius r that stays inside the populated 32x32 area */
    private static int at(int base, int r, Random rand) {
        return base + r + rand.nextInt(Math.max(1, 32 - 2 * r));
    }

    public static void decorateNest(World w, Random rand, int cx, int cz) {
        // the blobs
        for (int i = 0; i < 10; i++) fleshBlob(w, at(cx, 3, rand), at(cz, 3, rand), 1 + rand.nextInt(3), 2 + rand.nextInt(3));
        for (int i = 0; i < 8; i++) metaBlob(w, at(cx, 3, rand), at(cz, 3, rand), 1 + rand.nextInt(3), IqNest.glyphidOvertaken);
        for (int i = 0; i < 5; i++) metaBlob(w, at(cx, 2, rand), at(cz, 2, rand), 1 + rand.nextInt(2), IqNest.glyphidCrevasse);
        for (int i = 0; i < 5; i++) metaBlob(w, at(cx, 2, rand), at(cz, 2, rand), 1 + rand.nextInt(2), IqNest.glyphidHive);
        for (int i = 0; i < 20; i++) {
            BlockPos p = floor(w, at(cx, 0, rand), at(cz, 0, rand)).down();
            Block s = w.getBlockState(p).getBlock();
            if (s == Blocks.NETHERRACK || s instanceof NestBlocks.NetherGlyphid) {
                int r = rand.nextInt(3);
                set(w, p, r == 0 ? IqNest.glyphidCrevasse : r == 1 ? IqNest.glyphidHive : IqNest.glyphidGrub);
            }
        }

        // bloatwisp hives
        for (int i = 0; i < 6; i++) {
            BlockPos p = floor(w, at(cx, 0, rand), at(cz, 0, rand)).down();
            Block s = w.getBlockState(p).getBlock();
            if (s == Blocks.NETHERRACK || s == ModBlocks.glyphid_base || s instanceof NestBlocks.NetherGlyphid) {
                set(w, p, IqNest.glyphidBloatwisp);
                if (w.isAirBlock(p.up())) set(w, p.up(), IqNest.bloatfire);
            }
        }

        // sulfur / phosphorus crystal pillars (round 1's IQ crystal blocks), floor then ceiling
        for (int i = 0; i < 2; i++) {
            int x = at(cx, 1, rand), z = at(cz, 1, rand);
            BlockPos f = floor(w, x, z);
            if (f.getY() <= 30 || f.getY() >= 100) continue;
            boolean lava = w.getBlockState(f.down()).getBlock() == Blocks.LAVA;
            if (!lava && !isSolidFloor(w, x, z) || !isFlatArea(w, x, z, 2)) continue;
            Block cb = crystal(rand);
            int h = 3 + rand.nextInt(3);
            for (int dy = 0; dy < h; dy++) plus(w, f.up(dy), cb, true);
        }
        for (int i = 0; i < 2; i++) {
            int x = at(cx, 1, rand), z = at(cz, 1, rand);
            Block cb = crystal(rand);
            for (int y = 126; y > 40; y--) {
                if (w.isAirBlock(new BlockPos(x, y, z)) && isSolidMass(w, new BlockPos(x, y + 1, z))) {
                    int h = 3 + rand.nextInt(3);
                    for (int dy = 0; dy < h && y - dy > 32; dy++) plus(w, new BlockPos(x, y - dy, z), cb, false);
                    break;
                }
            }
        }

        // trees
        {
            int x = at(cx, 4, rand), z = at(cz, 4, rand);
            BlockPos f = floor(w, x, z);
            if (f.getY() > 30 && f.getY() < 100 && isSolidFloor(w, x, z) && isFlatArea(w, x, z, 2)) growBloatsprout(w, rand, f);
        }

        // fungi
        for (int i = 0; i < 8; i++) {
            int x = at(cx, 4, rand), z = at(cz, 4, rand);
            BlockPos f = floor(w, x, z);
            if (f.getY() <= 30 || f.getY() >= 90) continue;
            if (!isThickFloor(w, x, z) && !isLavaNearby(w, f)) continue;
            fungus(w, rand, f);
        }
        if (IqNest.netherHives && rand.nextInt(2) == 0) {
            int x = at(cx, 15, rand), z = at(cz, 15, rand);   // nest fungus cap is up to r15 + a hive on top
            BlockPos f = floor(w, x, z);
            if (f.getY() > 30 && f.getY() < 90 && (isThickFloor(w, x, z) || isLavaNearby(w, f))) nestFungus(w, rand, f);
        }
        for (int i = 0; i < 2; i++) {
            int x = at(cx, 9, rand), z = at(cz, 9, rand);
            BlockPos f = floor(w, x, z);
            if (f.getY() <= 30 || f.getY() >= 90) continue;
            if (!isThickFloor(w, x, z) && !isLavaNearby(w, f)) continue;
            grandFungus(w, rand, f);
        }

        // tentacles from the roof
        for (int i = 0; i < 8; i++) roofTentacle(w, rand, at(cx, 2, rand), at(cz, 2, rand));

        // roof (vent) fungus
        if (rand.nextInt(2) == 0) {
            int x = at(cx, 6, rand), z = at(cz, 6, rand);
            for (int y = 126; y > 90; y--) {
                BlockPos p = new BlockPos(x, y, z);
                if (w.isAirBlock(p) && isSolidMass(w, p.up())) { ventFungus(w, rand, p); break; }
            }
        }

        // cysts
        for (int i = 0; i < 8; i++) {
            int x = at(cx, 2, rand), z = at(cz, 2, rand);
            BlockPos f = floor(w, x, z);
            if (f.getY() <= 30 || f.getY() >= 100 || !isSolidFloor(w, x, z) || !isFlatArea(w, x, z, 2)) continue;
            cyst(w, rand, f.down());
        }
        if (rand.nextInt(8) == 0) {
            int x = at(cx, 1, rand), z = at(cz, 1, rand);
            if (isSolidFloor(w, x, z)) bloathive(w, rand, floor(w, x, z));
        }

        // hive
        if (IqNest.netherHives && rand.nextInt(2) == 0) hive(w, rand, at(cx, 7, rand), at(cz, 7, rand));

        // Satan's Blood bedrock artery
        if (IqNest.arterySpawn > 0 && rand.nextInt(IqNest.arterySpawn) == 0) {
            int x = at(cx, 5, rand), z = at(cz, 5, rand);
            for (int bx = -4; bx <= 4; bx++)
                for (int bz = -4; bz <= 4; bz++)
                    if (Math.abs(bx) + Math.abs(bz) <= 6)
                        for (int y = 0; y < 6; y++) {
                            BlockPos p = new BlockPos(x + bx, y, z + bz);
                            if (w.getBlockState(p).getBlock() == Blocks.BEDROCK) set(w, p, IqNest.artery);
                        }
        }

        // plants
        for (int i = 0; i < 80; i++) {
            BlockPos p = floor(w, at(cx, 0, rand), at(cz, 0, rand));
            if (NestBlocks.isNestSoil(w.getBlockState(p.down()).getBlock()) && w.isAirBlock(p)) set(w, p, fleshPlant(rand));
        }

        // skulls
        for (int i = 0; i < 6; i++) {
            int x = at(cx, 0, rand), z = at(cz, 0, rand);
            BlockPos f = floor(w, x, z);
            if (f.getY() <= 30 || f.getY() >= 100 || !isSolidFloor(w, x, z) || !w.isAirBlock(f)) continue;
            skull(w, rand, f);
        }

        for (int i = 0; i < 5; i++) metaBlob(w, at(cx, 2, rand), at(cz, 2, rand), 1 + rand.nextInt(2), IqNest.glyphidGrub);

        // "the saac": lone nether spawners on flesh
        if (IqNest.netherHives) {
            for (int i = 0; i < 2; i++) {
                int x = at(cx, 1, rand), z = at(cz, 1, rand);
                BlockPos f = floor(w, x, z);
                if (f.getY() <= 30 || f.getY() >= 100 || !isSolidFloor(w, x, z) || !isFlatArea(w, x, z, 1)) continue;
                if (w.getBlockState(f.down()).getBlock() == ModBlocks.glyphid_base && w.isAirBlock(f)) set(w, f, IqNest.spawnerNether);
            }
        }
    }

    // ---------------------------------------------------------------- NetherBiomeHelper (iris-lgtm)

    /** first free block above the floor, scanning down from y 100 (bloatsprout leaves don't count); y -1 if none */
    static BlockPos floor(World w, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, 100, z);
        for (int y = 100; y > 0; y--) {
            p.setY(y);
            IBlockState s = w.getBlockState(p);
            if (s.getBlock() == IqNest.bloatLeaves) continue;
            if (s.getBlock() != Blocks.AIR) return new BlockPos(x, y + 1, z);
        }
        return new BlockPos(x, -1, z);
    }

    static boolean isSolidFloor(World w, int x, int z) {
        BlockPos f = floor(w, x, z);
        return f.getY() > 0 && NestBlocks.isNestSoil(w.getBlockState(f.down()).getBlock());
    }

    static boolean isSolidMass(World w, BlockPos p) { return !w.isAirBlock(p) && !w.isAirBlock(p.up()); }

    static boolean isThickFloor(World w, int x, int z) {
        BlockPos f = floor(w, x, z);
        if (f.getY() <= 2) return false;
        for (int dy = 1; dy <= 3; dy++) if (!NestBlocks.isNestSoil(w.getBlockState(f.down(dy)).getBlock())) return false;
        return true;
    }

    static boolean isLavaNearby(World w, BlockPos f) {
        for (int dy = 1; dy <= 5; dy++) {
            Block b = w.getBlockState(f.down(dy)).getBlock();
            if (b == Blocks.LAVA) return true;
            if (!NestBlocks.isNestSoil(b)) return false;
        }
        return false;
    }

    static boolean isFlatArea(World w, int x, int z, int r) {
        int h = floor(w, x, z).getY();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                if (Math.abs(floor(w, x + dx, z + dz).getY() - h) > 1) return false;
        return true;
    }

    // ---------------------------------------------------------------- helpers

    private static void set(World w, BlockPos p, Block b) {
        if (p.getY() > 0 && p.getY() < 256) w.setBlockState(p, b.getDefaultState(), 2);
    }

    private static Block crystal(Random rand) {
        return IqContent.CRYSTAL_BLOCK.get(rand.nextBoolean() ? "phosphorus" : "sulfur");
    }

    private static Block fleshPlant(Random rand) { return IqNest.FLESH.get(rand.nextInt(IqNest.FLESH.size())); }

    /** a plus-shaped crystal layer; floor pillars may also replace lava */
    private static void plus(World w, BlockPos c, Block b, boolean overLava) {
        for (EnumFacing f : new EnumFacing[] { null, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST }) {
            BlockPos p = f == null ? c : c.offset(f);
            if (w.isAirBlock(p) || overLava && w.getBlockState(p).getBlock() == Blocks.LAVA) set(w, p, b);
        }
    }

    /** IQ's "box": 5/8 glyphid flesh, 2/8 overtaken netherrack, 1/8 grub */
    private static void flesh(World w, BlockPos p, Random rand) {
        int r = rand.nextInt(8);
        if (r < 5) set(w, p, ModBlocks.glyphid_base);
        else set(w, p, r < 7 ? IqNest.glyphidOvertaken : IqNest.glyphidGrub);
    }

    private static void fungus(World w, Random rand, BlockPos f) {
        rootInLava(w, f, 1);
        int stalkH = 4 + rand.nextInt(7), capR = 2 + rand.nextInt(3);
        for (int dy = 0; dy < stalkH; dy++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    if (dx == 0 || dz == 0) flesh(w, f.add(dx, dy, dz), rand);
        cap(w, rand, f.up(stalkH), capR);
    }

    private static void cap(World w, Random rand, BlockPos c, int capR) {
        for (int dx = -capR; dx <= capR; dx++)
            for (int dz = -capR; dz <= capR; dz++) {
                int d = dx * dx + dz * dz;
                if (d > capR * capR) continue;
                flesh(w, c.add(dx, 0, dz), rand);
                if (d >= (capR - 1) * (capR - 1)) set(w, c.add(dx, -1, dz), ModBlocks.glyphid_base);
            }
    }

    private static void ventFungus(World w, Random rand, BlockPos top) {
        int stalkH = 18 + rand.nextInt(10), capR = 4 + rand.nextInt(3);
        for (int dy = 0; dy < stalkH; dy++) {
            int r = 2 - (int) ((double) dy / stalkH);
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++)
                    if (Math.abs(dx) + Math.abs(dz) <= r) flesh(w, top.add(dx, -dy, dz), rand);
        }
        BlockPos c = top.down(stalkH);
        for (int dx = -capR; dx <= capR; dx++)
            for (int dz = -capR; dz <= capR; dz++) {
                int d = dx * dx + dz * dz;
                if (d > capR * capR) continue;
                flesh(w, c.add(dx, 0, dz), rand);
                if (d >= (capR - 1) * (capR - 1))
                    for (int dw = 1; dw <= 2; dw++) if (w.isAirBlock(c.add(dx, -dw, dz))) set(w, c.add(dx, -dw, dz), ModBlocks.glyphid_base);
            }
    }

    private static void grandFungus(World w, Random rand, BlockPos f) {
        rootInLava(w, f, 2);
        int stalkH = 9 + rand.nextInt(7), capR = 6 + rand.nextInt(4);
        stalk(w, rand, f, stalkH, 3, 1.0D, 2);
        BlockPos c = f.up(stalkH);
        cap(w, rand, c, capR);
        int capR2 = Math.max(2, capR / 2);
        for (int dx = -capR2; dx <= capR2; dx++)
            for (int dz = -capR2; dz <= capR2; dz++)
                if (dx * dx + dz * dz <= capR2 * capR2 && w.isAirBlock(c.add(dx, 1, dz))) flesh(w, c.add(dx, 1, dz), rand);
    }

    private static void nestFungus(World w, Random rand, BlockPos f) {
        rootInLava(w, f, 2);
        int stalkH = 10 + rand.nextInt(7), capR = 12 + rand.nextInt(4);
        stalk(w, rand, f, stalkH, 4, 2.0D, 3);
        BlockPos c = f.up(stalkH);
        for (int dx = -capR; dx <= capR; dx++)
            for (int dz = -capR; dz <= capR; dz++)
                if (dx * dx + dz * dz <= capR * capR) flesh(w, c.add(dx, 0, dz), rand);
        hiveStructure(w, c.up(), rand);
    }

    /** diamond-section stalk that tapers from r0 by 'taper' over its height, never below rMin */
    private static void stalk(World w, Random rand, BlockPos f, int h, int r0, double taper, int rMin) {
        for (int dy = 0; dy < h; dy++) {
            int r = Math.max(rMin, r0 - (int) ((double) dy / h * taper));
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++)
                    if (Math.abs(dx) + Math.abs(dz) <= r) flesh(w, f.add(dx, dy, dz), rand);
        }
    }

    private static void rootInLava(World w, BlockPos f, int padR) {
        for (int dy = 1; dy <= 5; dy++) {
            Block b = w.getBlockState(f.down(dy)).getBlock();
            if (b == Blocks.LAVA) {
                for (int d2 = 1; d2 <= dy; d2++)
                    for (int dx = -padR; dx <= padR; dx++)
                        for (int dz = -padR; dz <= padR; dz++) {
                            BlockPos p = f.add(dx, -d2, dz);
                            Block cur = w.getBlockState(p).getBlock();
                            if (cur == Blocks.LAVA || cur == Blocks.NETHERRACK || cur == ModBlocks.glyphid_base || cur instanceof NestBlocks.NetherGlyphid)
                                set(w, p, ModBlocks.glyphid_base);
                        }
                return;
            }
            if (!NestBlocks.isNestSoil(b)) return;
        }
    }

    private static void roofTentacle(World w, Random rand, int x, int z) {
        for (int y = 126; y > 60; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (w.isAirBlock(p) && !w.isAirBlock(p.up())) {
                int len = 6 + rand.nextInt(9);
                for (int dy = 0; dy < len; dy++) {
                    int ty = y - dy;
                    if (ty <= 32) break;
                    int r = dy == len - 1 ? 0 : dy < 3 ? 2 : 1;
                    for (int dx = -r; dx <= r; dx++)
                        for (int dz = -r; dz <= r; dz++) {
                            if (r > 0 && dx * dx + dz * dz > r * r) continue;
                            BlockPos q = new BlockPos(x + dx, ty, z + dz);
                            if (w.isAirBlock(q)) flesh(w, q, rand);
                        }
                    if (r == 0) break;
                }
                return;
            }
        }
    }

    private static void fleshBlob(World w, int bx, int bz, int radius, int depth) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                BlockPos p = floor(w, bx + dx, bz + dz).down();
                Block s = w.getBlockState(p).getBlock();
                if (s != Blocks.NETHERRACK && !(s instanceof NestBlocks.NetherGlyphid)) continue;
                set(w, p, ModBlocks.glyphid_base);
                for (int dy = 1; dy <= depth; dy++) if (w.getBlockState(p.down(dy)).getBlock() == Blocks.NETHERRACK) set(w, p.down(dy), ModBlocks.glyphid_base);
            }
    }

    private static void metaBlob(World w, int bx, int bz, int radius, Block b) {
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                BlockPos p = floor(w, bx + dx, bz + dz).down();
                Block s = w.getBlockState(p).getBlock();
                if (s == Blocks.NETHERRACK || s == ModBlocks.glyphid_base) set(w, p, b);
            }
    }

    private static void cyst(World w, Random rand, BlockPos base) {
        int h = 1 + rand.nextInt(2);
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                for (int dy = 0; dy <= h; dy++) {
                    BlockPos p = base.add(dx, 1 + dy, dz);
                    Block cur = w.getBlockState(p).getBlock();
                    if (cur == Blocks.AIR || NestBlocks.isNestSoil(cur) && cur != Blocks.SOUL_SAND) set(w, p, ModBlocks.glyphid_base);
                }
            }
        set(w, base.up(), IqNest.cyst);
    }

    private static void bloathive(World w, Random rand, BlockPos f) {
        int h = 1 + rand.nextInt(2);
        for (int dy = -1; dy < h; dy++)
            for (int dx = -1; dx <= 0; dx++)
                for (int dz = -1; dz <= 0; dz++) set(w, f.add(dx, dy, dz), ModBlocks.glyphid_base);
        set(w, f.up(h), IqNest.glyphidBloatwisp);
        set(w, f.up(h + 1), IqNest.bloatfire);
        if (rand.nextBoolean()) set(w, f.add(0, h + 1, -1), Blocks.FIRE);
    }

    private static void skull(World w, Random rand, BlockPos p) {
        w.setBlockState(p, Blocks.SKULL.getDefaultState().withProperty(BlockSkull.FACING, EnumFacing.UP), 2);
        TileEntity te = w.getTileEntity(p);
        if (te instanceof TileEntitySkull) {
            ((TileEntitySkull) te).setType(1);   // wither skeleton
            ((TileEntitySkull) te).setSkullRotation(rand.nextInt(16));
        }
    }

    /** IQ's floor hive: needs a flat 12x12x5 air pocket above solid ground */
    private static void hive(World w, Random rand, int x, int z) {
        BlockPos f = floor(w, x, z);
        if (!isSolidFloor(w, x, z) || !isFlatArea(w, x, z, 2)) return;
        int base = f.getY() - 1;
        for (int dy = 0; dy <= 4; dy++)
            for (int dx = -6; dx <= 5; dx++)
                for (int dz = -6; dz <= 5; dz++)
                    if (!w.isAirBlock(new BlockPos(x + dx, base + dy, z + dz))) return;
        for (int k = 3; k >= -1; k--) {
            if (w.getBlockState(new BlockPos(x, base + k, z)).isNormalCube()) {
                hiveStructure(w, new BlockPos(x, base + k + 1, z), rand);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- GlyphidHiveNether

    /** [layer from top][x][z]: 1 wall, 2 wall or spawner, 3 skull / loot, 4 cyst */
    private static final int[][][] HIVE = {
            { {0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0},
              {0,0,0,0,0,4,1,0,0,0,0,0},{0,0,0,0,1,1,1,1,0,0,0,0},{0,0,0,0,1,1,1,1,0,0,0,0},{0,0,0,0,0,1,4,0,0,0,0,0},
              {0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0} },
            { {0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,1,1,1,1,0,0,0,0},
              {0,0,0,1,4,4,0,1,1,0,0,0},{0,0,0,1,0,0,0,0,1,0,0,0},{0,0,0,1,0,0,0,0,1,0,0,0},{0,0,0,1,1,0,4,4,1,0,0,0},
              {0,0,0,0,1,1,1,1,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0} },
            { {0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,1,2,1,1,0,0,0,0},{0,0,0,2,1,0,0,1,2,0,0,0},
              {0,0,1,1,0,0,0,0,1,1,0,0},{0,0,2,0,0,0,0,0,0,2,0,0},{0,0,1,0,0,0,0,0,0,1,0,0},{0,0,1,1,0,0,0,0,1,1,0,0},
              {0,0,0,2,1,0,0,1,2,0,0,0},{0,0,0,0,1,2,1,1,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0} },
            { {0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,0,1,1,1,1,0,0,0,0},{0,0,2,1,2,0,0,2,1,2,0,0},{0,0,1,0,0,0,0,0,0,1,0,0},
              {0,1,1,0,0,0,0,0,0,1,1,0},{0,1,0,0,0,0,0,0,0,0,1,0},{0,1,0,0,0,0,0,0,0,0,1,0},{0,1,1,0,0,0,0,0,0,1,1,0},
              {0,0,1,0,0,0,0,0,0,1,0,0},{0,0,2,1,2,0,0,2,1,2,0,0},{0,0,0,0,1,1,1,1,0,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0} },
            { {0,0,0,0,0,0,0,0,0,0,0,0},{0,0,0,1,1,1,1,1,1,0,0,0},{0,0,1,1,3,3,3,3,1,1,0,0},{0,1,1,3,3,3,3,3,3,1,1,0},
              {0,1,3,3,3,3,3,3,3,3,1,0},{0,1,3,3,3,3,3,3,3,3,1,0},{0,1,3,3,3,3,3,3,3,3,1,0},{0,1,3,3,3,3,3,3,3,3,1,0},
              {0,1,1,3,3,3,3,3,3,1,1,0},{0,0,1,1,3,3,3,3,1,1,0,0},{0,0,0,1,1,1,1,1,1,0,0,0},{0,0,0,0,0,0,0,0,0,0,0,0} },
            { {0,0,0,1,1,1,1,1,1,0,0,0},{0,1,1,1,1,1,1,1,1,1,1,0},{0,1,1,1,1,1,1,1,1,1,1,0},{1,1,1,1,1,1,1,1,1,1,1,1},
              {1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1},{1,1,1,1,1,1,1,1,1,1,1,1},
              {1,1,1,1,1,1,1,1,1,1,1,1},{0,1,1,1,1,1,1,1,1,1,1,0},{0,1,1,1,1,1,1,1,1,1,1,0},{0,0,0,1,1,1,1,1,1,0,0,0} },
    };

    static void hiveStructure(World w, BlockPos c, Random rand) {
        for (int i = 0; i < 12; i++)
            for (int j = 0; j < 6; j++)
                for (int k = 0; k < 12; k++) {
                    BlockPos p = c.add(i - 6, j - 2, k - 6);
                    switch (HIVE[5 - j][i][k]) {
                        case 1: hiveWall(w, p, rand); break;
                        case 2: if (rand.nextInt(3) == 0) set(w, p, IqNest.spawnerNether); else hiveWall(w, p, rand); break;
                        case 3:
                            int r = rand.nextInt(3);
                            if (r == 0) skull(w, rand, p);
                            else {
                                // IQ fills deco_loot piles (bones / nether hive loot); a nether-fortress chest stands in for both
                                w.setBlockState(p, Blocks.CHEST.getDefaultState(), 2);
                                TileEntity te = w.getTileEntity(p);
                                if (te instanceof TileEntityChest) ((TileEntityChest) te).setLootTable(LootTableList.CHESTS_NETHER_BRIDGE, rand.nextLong());
                            }
                            break;
                        case 4: set(w, p, IqNest.cyst); break;
                        default: break;
                    }
                }
        for (int i = 0; i < 12; i++)
            for (int j = 0; j < 6; j++)
                for (int k = 0; k < 12; k++) {
                    if (HIVE[5 - j][i][k] == 0 || rand.nextInt(4) != 0) continue;
                    BlockPos p = c.add(i - 6, j - 2, k - 6);
                    Block below = w.getBlockState(p).getBlock();
                    if ((below instanceof NestBlocks.NetherGlyphid || below == ModBlocks.glyphid_base) && w.isAirBlock(p.up())) set(w, p.up(), fleshPlant(rand));
                }
    }

    private static void hiveWall(World w, BlockPos p, Random rand) {
        int r = rand.nextInt(60);
        set(w, p, r < 25 ? IqNest.glyphidHive : r < 40 ? IqNest.glyphidOvertaken : r < 50 ? IqNest.glyphidCrevasse : IqNest.glyphidGrub);
    }

    // ---------------------------------------------------------------- WorldGenBloatsprout (2, 5, 7, 4, 3)

    /** trunk 7..11 tall, an ellipsoid bulb of leaves r4 x 3 around 2 below the top. false if the soil is wrong */
    static boolean growBloatsprout(World w, Random rand, BlockPos pos) {
        int height = rand.nextInt(5) + 7;
        if (pos.getY() < 1 || pos.getY() + height + 1 > 256) return false;
        if (!NestBlocks.isNestSoil(w.getBlockState(pos.down()).getBlock())) return false;
        for (int i = 0; i < height; i++) set(w, pos.up(i), IqNest.bloatLog);
        BlockPos bulb = pos.up(height - 2);
        int rxz = 4, ry = 3;
        for (int dy = -ry; dy <= ry; dy++)
            for (int dx = -rxz; dx <= rxz; dx++)
                for (int dz = -rxz; dz <= rxz; dz++) {
                    double d = Math.pow(dx / (double) rxz, 2) + Math.pow(dy / (double) ry, 2) + Math.pow(dz / (double) rxz, 2);
                    BlockPos p = bulb.add(dx, dy, dz);
                    if (d <= 1 && w.isAirBlock(p)) set(w, p, IqNest.bloatLeaves);
                }
        return true;
    }
}
