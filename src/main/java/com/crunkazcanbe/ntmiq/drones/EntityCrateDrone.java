package com.crunkazcanbe.ntmiq.drones;

import com.hbm.entity.item.EntityDroneBase;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.tileentity.network.TileEntityDroneCrate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;

/**
 * The part the Quarry and Spelunking drones of NTM: IQ Edition (1.7.10, GPL-3; irislgtm, tam69 and the IQ team) share:
 * a 27-slot hold, a home drone crate they were placed on, and unloading into that crate. Built on CE's EntityDroneBase.
 */
public abstract class EntityCrateDrone extends EntityDroneBase {

    public ItemStack[] slots = emptySlots();
    protected int baseX, baseY, baseZ;

    protected EntityCrateDrone(World world) {
        super(world);
    }

    private static ItemStack[] emptySlots() {
        ItemStack[] s = new ItemStack[27];
        java.util.Arrays.fill(s, ItemStack.EMPTY);
        return s;
    }

    public void setBase(int x, int y, int z) {
        baseX = x;
        baseY = y;
        baseZ = z;
    }

    /** the ItemMiningDrone meta this drone drops back as */
    protected abstract int itemMeta();

    /** IQ added a canClimbWalls() hook to EntityDroneBase; CE has none, so clear the flag CE checks before it moves us */
    protected boolean canClimbWalls() {
        return true;
    }

    @Override
    public void onUpdate() {
        if (!world.isRemote && !canClimbWalls()) collidedHorizontally = false;
        super.onUpdate();
    }

    protected boolean isFull() {
        for (ItemStack s : slots) if (s.isEmpty()) return false;
        return true;
    }

    protected boolean isEmpty() {
        for (ItemStack s : slots) if (!s.isEmpty()) return false;
        return true;
    }

    protected void store(ItemStack drop) {
        if (!DroneMining.addToStorage(slots, drop)) entityDropItem(drop, 0F);
        if (!isEmpty()) setAppearance(1);
    }

    /**
     * Called once the drone hovers over its base. If the crate is gone the drone drops its load and itself (IQ).
     * @return true when the hold is now empty
     */
    protected boolean unloadAtBase() {
        TileEntity te = world.getTileEntity(new BlockPos(baseX, baseY, baseZ));
        if (!(te instanceof TileEntityDroneCrate)) {
            dropEverything(0F);
            return false;
        }
        unloadToCrate((TileEntityDroneCrate) te);
        if (isEmpty()) {
            setAppearance(0);
            return true;
        }
        return false;
    }

    /** IQ: only into a crate set to receive items; crate slots 0-17 */
    protected void unloadToCrate(TileEntityDroneCrate crate) {
        if (crate.sendingMode || !crate.itemType) return;
        ItemStackHandler inv = crate.inventory;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].isEmpty()) continue;
            for (int j = 0; j < 18 && !slots[i].isEmpty(); j++) {
                ItemStack c = inv.getStackInSlot(j);
                if (!c.isEmpty() && ItemStack.areItemsEqual(c, slots[i]) && ItemStack.areItemStackTagsEqual(c, slots[i])) {
                    int add = Math.min(slots[i].getCount(), c.getMaxStackSize() - c.getCount());
                    if (add > 0) {
                        c.grow(add);
                        slots[i].shrink(add);
                    }
                }
            }
            if (slots[i].isEmpty()) {
                slots[i] = ItemStack.EMPTY;
                continue;
            }
            for (int j = 0; j < 18; j++) {
                if (inv.getStackInSlot(j).isEmpty()) {
                    inv.setStackInSlot(j, slots[i].copy());
                    slots[i] = ItemStack.EMPTY;
                    break;
                }
            }
        }
        crate.markDirty();
        world.playSound(null, baseX + 0.5, baseY + 0.5, baseZ + 0.5, HBMSoundHandler.storageClose, SoundCategory.BLOCKS, 2.0F, 1.0F);
    }

    protected void dropEverything(float yOff) {
        setDead();
        for (int i = 0; i < slots.length; i++) {
            if (!slots[i].isEmpty()) entityDropItem(slots[i], yOff);
            slots[i] = ItemStack.EMPTY;
        }
        entityDropItem(new ItemStack(IqDrones.drone, 1, itemMeta()), yOff);
    }

    @Override
    public boolean hitByEntity(Entity attacker) {
        if (isDead) return false;
        if (attacker instanceof EntityPlayer && !world.isRemote) dropEverything(1F);
        return false;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger("baseX", baseX);
        nbt.setInteger("baseY", baseY);
        nbt.setInteger("baseZ", baseZ);
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].isEmpty()) continue;
            NBTTagCompound t = new NBTTagCompound();
            t.setByte("slot", (byte) i);
            slots[i].writeToNBT(t);
            items.appendTag(t);
        }
        nbt.setTag("items", items);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        baseX = nbt.getInteger("baseX");
        baseY = nbt.getInteger("baseY");
        baseZ = nbt.getInteger("baseZ");
        slots = emptySlots();
        NBTTagList items = nbt.getTagList("items", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound t = items.getCompoundTagAt(i);
            int j = t.getByte("slot") & 255;
            if (j < slots.length) slots[j] = new ItemStack(t);
        }
    }
}
