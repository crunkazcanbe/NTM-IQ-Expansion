package com.crunkazcanbe.ntmiq.foam;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.inventory.fluid.trait.FluidTrait;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.io.IOException;

/** IQ's FT_ConstructionFoam (irislgtm / tam69 / IQ team, GPL-3): a chemthrower hit places a 2x2 patch of foam on the hit face. */
public class FT_ConstructionFoam extends FluidTrait {

    public int foamMeta;

    public FT_ConstructionFoam() {}

    public FT_ConstructionFoam(int foamMeta) {
        this.foamMeta = foamMeta;
    }

    public void applyFoam(World world, RayTraceResult mop) {
        EnumFacing side = mop.sideHit;
        BlockPos hit = mop.getBlockPos();
        int dx = side.getXOffset(), dy = side.getYOffset(), dz = side.getZOffset();
        int bx = hit.getX() + dx, by = hit.getY() + dy, bz = hit.getZ() + dz;
        // which quarter of the face was hit decides where the 2x2 patch grows
        int ox = mop.hitVec.x - hit.getX() < 0.5 ? -1 : 0;
        int oy = mop.hitVec.y - hit.getY() < 0.5 ? -1 : 0;
        int oz = mop.hitVec.z - hit.getZ() < 0.5 ? -1 : 0;

        if (dx != 0) {
            place(world, bx, by + oy, bz + oz); place(world, bx, by + oy + 1, bz + oz);
            place(world, bx, by + oy, bz + oz + 1); place(world, bx, by + oy + 1, bz + oz + 1);
        } else if (dy != 0) {
            place(world, bx + ox, by, bz + oz); place(world, bx + ox + 1, by, bz + oz);
            place(world, bx + ox, by, bz + oz + 1); place(world, bx + ox + 1, by, bz + oz + 1);
        } else {
            place(world, bx + ox, by + oy, bz); place(world, bx + ox + 1, by + oy, bz);
            place(world, bx + ox, by + oy + 1, bz); place(world, bx + ox + 1, by + oy + 1, bz);
        }
    }

    private void place(World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock().isAir(state, world, pos) || state.getBlock().isReplaceable(world, pos))
            world.setBlockState(pos, IqFoam.foam.getDefaultState().withProperty(BlockConstructionFoam.TYPE, foamMeta & 15), 3);
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("foamMeta").value(foamMeta);
    }

    @Override
    public void deserializeJSON(JsonObject obj) {
        if (obj.has("foamMeta")) foamMeta = obj.get("foamMeta").getAsInt();
    }
}
