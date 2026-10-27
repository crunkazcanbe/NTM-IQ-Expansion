package com.crunkazcanbe.ntmiq.pipeline;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.network.TileEntityPipelineBase;
import net.minecraft.util.math.Vec3d;

/** IQ's TileEntityPipeAnchorIndustrial: a 100 m pipeline anchor that takes fluid from the Pipe Anchor Pressurizer under it. */
public class TileEntityPipeAnchorIndustrial extends TileEntityPipelineBase {

    /**
     * IQ added ConnectionType.LARGE; CE's enum only has SMALL and an enum can't be extended without a mixin.
     * CE's canConnect compares the types by reference, so null works as "large": industrial links to industrial, never to a small anchor.
     */
    @Override
    public ConnectionType getConnectionType() {
        return null;
    }

    @Override
    public Vec3d getMountPos() {
        switch (this.getBlockMetadata() - BlockDummyable.offset) {
            case 3: return new Vec3d(-0.5, 1.0, -0.5);
            case 4: return new Vec3d(0.5, 1.0, -0.5);
            case 5: return new Vec3d(-0.5, 1.0, 0.5);
            default: return new Vec3d(0.5, 1.0, 0.5);
        }
    }

    @Override
    public double getMaxPipeLength() {
        return 100;
    }

    @Override
    public FluidNode createNode(FluidType type) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        FluidNode node = new FluidNode(type.getNetworkProvider(), pos).setConnections(
                new DirPos(x, y, z, ForgeDirection.UNKNOWN),
                new DirPos(x + 1, y, z, ForgeDirection.EAST),
                new DirPos(x - 1, y, z, ForgeDirection.WEST),
                new DirPos(x, y, z + 1, ForgeDirection.SOUTH),
                new DirPos(x, y, z - 1, ForgeDirection.NORTH));
        for (int[] c : this.connected) node.addConnection(new DirPos(c[0], c[1], c[2], ForgeDirection.UNKNOWN));
        return node;
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        if (dir == ForgeDirection.UP) return false;
        if (type != this.type) return false;
        if (dir == ForgeDirection.DOWN) return world.getBlockState(pos.down()).getBlock() instanceof BlockPipeAnchorPressurizer;
        return true;
    }
}
