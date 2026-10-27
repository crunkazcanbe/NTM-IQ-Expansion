package com.crunkazcanbe.ntmiq.contagion;

import com.hbm.inventory.container.ContainerBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotTakeOnly;
import net.minecraft.entity.player.InventoryPlayer;

/** Dial-A-Dose slots, laid out as in NTM: IQ Edition (GPL-3): syringe, vial, floppy disk, computing unit, battery, output. */
public class ContainerMedicineSynthesizer extends ContainerBase {

    public ContainerMedicineSynthesizer(InventoryPlayer invPlayer, TileEntityMedicineSynthesizer te) {
        super(invPlayer, te.getCheckedInventory());
        addSlotToContainer(new SlotNonRetarded(tile, 0, 43, 22));   // empty medical syringe
        addSlotToContainer(new SlotNonRetarded(tile, 1, 43, 40));   // blood vial of the target disease (cure mode)
        addSlotToContainer(new SlotNonRetarded(tile, 2, 43, 58));   // floppy disk
        addSlotToContainer(new SlotNonRetarded(tile, 3, 94, 81));   // pharmaceutical computing unit
        addSlotToContainer(new SlotNonRetarded(tile, 4, 247, 84));  // battery
        addSlotToContainer(new SlotTakeOnly(tile, 5, 150, 40));     // finished syringe
        addSlotToContainer(new SlotNonRetarded(tile, 6, 7, 105));   // fluid identifier: picks what the tank holds
        playerInv(invPlayer, 48, 135);
    }
}
