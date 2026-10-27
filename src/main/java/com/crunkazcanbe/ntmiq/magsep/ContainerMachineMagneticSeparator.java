package com.crunkazcanbe.ntmiq.magsep;

import com.hbm.inventory.container.ContainerBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.entity.player.InventoryPlayer;

/** battery, input, disc, 6 outputs. Ported from NTM: IQ Edition (GPL-3). */
public class ContainerMachineMagneticSeparator extends ContainerBase {

    public ContainerMachineMagneticSeparator(InventoryPlayer invPlayer, TileEntityMachineMagneticSeparator te) {
        super(invPlayer, te.getCheckedInventory());
        addSlotToContainer(new SlotNonRetarded(tile, 0, 152, 81)); // battery
        addSlotToContainer(new SlotNonRetarded(tile, 1, 8, 90));   // input
        addSlotToContainer(new SlotNonRetarded(tile, 2, 46, 51));  // magnetic disc
        addOutputSlots(invPlayer.player, tile, 3, 80, 36, 3, 2);
        playerInv(invPlayer, 8, 174);
    }
}
