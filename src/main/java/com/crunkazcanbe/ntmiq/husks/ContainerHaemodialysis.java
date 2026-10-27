package com.crunkazcanbe.ntmiq.husks;

import com.hbm.inventory.container.ContainerBase;
import com.hbm.inventory.slot.SlotNonRetarded;
import com.hbm.inventory.slot.SlotUpgrade;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraftforge.items.IItemHandler;

/** IQ's ContainerMachineHaemodialysis: battery + 2 upgrades. */
public class ContainerHaemodialysis extends ContainerBase {
    public ContainerHaemodialysis(InventoryPlayer inv, IItemHandler te) {
        super(inv, te);
        addSlotToContainer(new SlotNonRetarded(te, 0, 152, 81));
        addSlotToContainer(new SlotUpgrade(te, 1, 152, 108));
        addSlotToContainer(new SlotUpgrade(te, 2, 152, 126));
        playerInv(inv, 8, 174);
    }
}
