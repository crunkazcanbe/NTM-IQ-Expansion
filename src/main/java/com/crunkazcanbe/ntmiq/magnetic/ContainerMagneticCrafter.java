package com.crunkazcanbe.ntmiq.magnetic;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** IQ's ContainerMagneticCrafter: a crafting table that needs no table. */
public class ContainerMagneticCrafter extends ContainerWorkbench {

    public ContainerMagneticCrafter(InventoryPlayer inv, World world) {
        super(inv, world, BlockPos.ORIGIN);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
