package com.crunkazcanbe.ntmiq.drones;

import com.crunkazcanbe.ntmiq.IqContent;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.DroneWaypointRequest;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Quarry Marker (NTM: IQ Edition, GPL-3; irislgtm, tam69 and the IQ team): the small standing logistics-waypoint block
 * with its own tile entity. CE's block constructor adds itself to ModBlocks.ALL_BLOCKS, so take it back out and
 * register it ourselves (as ntmiq:quarry_marker).
 */
public class BlockQuarryMarker extends DroneWaypointRequest {

    public BlockQuarryMarker() {
        super("quarry_marker");
        ModBlocks.ALL_BLOCKS.remove(this);
        setTranslationKey("ntmiq.quarry_marker");
        setHardness(0.1F);
        setResistance(10.0F);
        setCreativeTab(IqContent.TAB);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityQuarryMarker();
    }
}
