package com.crunkazcanbe.ntmiq.husks;

import com.hbm.inventory.container.ContainerBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.items.IItemHandler;

/** IQ's ContainerCloner: syringe, five part slots, battery (same layout as IQ's gui_cloner.png). */
public class ContainerCloner extends ContainerBase {
    public ContainerCloner(InventoryPlayer inv, IItemHandler te) {
        super(inv, te);
        addSlotToContainer(new SlotNonRetarded(te, TileEntityCloner.SLOT_SYRINGE, 24, 108));
        addSlots(te, TileEntityCloner.SLOT_PARTS, 62, 108, 1, 5, 19);
        addSlotToContainer(new SlotNonRetarded(te, TileEntityCloner.SLOT_BATTERY, 188, 55));
        playerInv(inv, 20, 154, 212);
    }
}
