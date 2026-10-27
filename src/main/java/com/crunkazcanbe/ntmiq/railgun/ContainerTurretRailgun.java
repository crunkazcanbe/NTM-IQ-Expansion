package com.crunkazcanbe.ntmiq.railgun;

import com.hbm.inventory.container.ContainerBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.entity.player.InventoryPlayer;

/** Louis: battery slot + 3 sabot slots. Ported from NTM: IQ Edition (GPL-3). */
public class ContainerTurretRailgun extends ContainerBase {

    public ContainerTurretRailgun(InventoryPlayer invPlayer, TileEntityTurretRailgun te) {
        super(invPlayer, te.getCheckedInventory());
        addSlotToContainer(new SlotNonRetarded(tile, 0, 153, 78)); // battery
        addSlots(tile, 1, 62, 104, 1, 3);                          // sabots
        playerInv(invPlayer, 8, 140);
    }
}
