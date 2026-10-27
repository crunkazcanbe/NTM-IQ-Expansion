package com.crunkazcanbe.ntmiq.growth;

import com.hbm.inventory.container.ContainerBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotUpgrade;
import net.minecraft.entity.player.InventoryPlayer;

/** battery, input, 2 upgrades, output. Ported from NTM: IQ Edition (GPL-3). */
public class ContainerMachineGrowthChamber extends ContainerBase {

    public ContainerMachineGrowthChamber(InventoryPlayer invPlayer, TileEntityMachineGrowthChamber te) {
        super(invPlayer, te.getCheckedInventory());
        addSlotToContainer(new SlotNonRetarded(tile, 0, 152, 72));  // battery
        addSlotToContainer(new SlotNonRetarded(tile, 1, 44, 36));   // input
        addSlotToContainer(new SlotUpgrade(tile, 2, 89, 63));
        addSlotToContainer(new SlotUpgrade(tile, 3, 107, 63));
        addOutputSlots(invPlayer.player, tile, 4, 107, 36, 1, 1);
        playerInv(invPlayer, 8, 122);
    }
}
