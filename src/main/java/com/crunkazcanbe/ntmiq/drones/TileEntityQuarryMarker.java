package com.crunkazcanbe.ntmiq.drones;

import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.network.IDroneLinkable;
import com.hbm.util.ParticleUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

/**
 * Quarry marker from NTM: IQ Edition (1.7.10, GPL-3; irislgtm, tam69 and the IQ team). Link markers to each other with
 * CE's drone linker (up to 3 links each); the Quarry Drone digs the box between the linked markers. Red lines show links.
 */
public class TileEntityQuarryMarker extends TileEntityLoadedBase implements IDroneLinkable, ITickable {

    public int[][] links = new int[3][];

    @Override
    public void update() {
        if (world.isRemote) {
            if (world.getTotalWorldTime() % 8 == 0) {
                for (int[] l : links) {
                    if (l != null) ParticleUtil.spawnDroneLine(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            l[0] - pos.getX(), l[1] - pos.getY(), l[2] - pos.getZ(), 0xff0000);
                }
            }
        } else {
            networkPackNT(50);
        }
    }

    @Override
    public BlockPos getPoint() {
        return pos;
    }

    @Override
    public void setNextTarget(int x, int y, int z) {
        if (x == pos.getX() && y == pos.getY() && z == pos.getZ()) return;
        for (int[] l : links) if (l != null && l[0] == x && l[1] == y && l[2] == z) return;
        for (int i = 0; i < 3; i++) {
            if (links[i] == null) {
                links[i] = new int[] { x, y, z };
                markDirty();
                return;
            }
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        for (int[] l : links) {
            buf.writeBoolean(l != null);
            if (l != null) {
                buf.writeInt(l[0]);
                buf.writeInt(l[1]);
                buf.writeInt(l[2]);
            }
        }
    }

    @Override
    public void deserialize(ByteBuf buf) {
        for (int i = 0; i < 3; i++) links[i] = buf.readBoolean() ? new int[] { buf.readInt(), buf.readInt(), buf.readInt() } : null;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        for (int i = 0; i < 3; i++) {
            int[] p = nbt.getIntArray("link" + i);
            links[i] = p.length == 3 ? p : null;
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        for (int i = 0; i < 3; i++) if (links[i] != null) nbt.setIntArray("link" + i, links[i]);
        return nbt;
    }
}
