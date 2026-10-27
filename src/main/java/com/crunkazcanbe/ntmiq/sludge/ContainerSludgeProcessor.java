package com.crunkazcanbe.ntmiq.sludge;

import com.hbm.inventory.container.ContainerBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotUpgrade;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.items.IItemHandler;

/** IQ's ContainerMachineSludgeProcessor (same slot layout). */
public class ContainerSludgeProcessor extends ContainerBase {

    public ContainerSludgeProcessor(InventoryPlayer inv, IItemHandler te) {
        super(inv, te);
        addSlotToContainer(new SlotNonRetarded(te, 0, 152, 81));   // battery
        addSlotToContainer(new SlotNonRetarded(te, 1, 35, 126));   // blueprint
        addSlotToContainer(new SlotUpgrade(te, 2, 152, 108));
        addSlotToContainer(new SlotUpgrade(te, 3, 152, 126));
        addSlotToContainer(new SlotNonRetarded(te, 4, 8, 90));     // item in
        addOutputSlots(inv.player, te, 5, 62, 90, 1, 4);           // items out
        playerInv(inv, 8, 174);
    }
}
