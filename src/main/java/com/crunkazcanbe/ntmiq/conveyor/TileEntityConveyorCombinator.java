package com.crunkazcanbe.ntmiq.conveyor;

import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.machine.TileEntityMachineAutocrafter.InventoryCraftingAuto;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

/** IQ's TileEntityConveyorCombinator: groups identical items on the belt and crafts every full 2x2 / 3x3 set, hammer animation. */
public class TileEntityConveyorCombinator extends TileEntityLoadedBase implements ITickable {

    public static final int MODE_3x3 = 0;
    public static final int MODE_2x2 = 1;
    public static final String TAG_OWNER = "hbmConveyorCombinator";

    /** crafting result per single ingredient; EMPTY = no recipe (IQ caches misses too) */
    static final Map<ComparableStack, ItemStack> FROM_4 = new HashMap<>();
    static final Map<ComparableStack, ItemStack> FROM_9 = new HashMap<>();
    private static final InventoryCraftingAuto GRID = new InventoryCraftingAuto(3, 3);

    public int mode = MODE_3x3;
    public double hammer, renderHammer, lastHammer;
    private double syncHammer;
    private int turnProgress;
    private double phase, amplitude;
    private int activeTicks;

    @Override
    public void update() {
        if (!world.isRemote) {
            if (process()) {
                activeTicks = 10;
                world.playSound(null, pos, HBMSoundHandler.pressOperate, SoundCategory.BLOCKS, getVolume(0.5F), 1.25F);
            }
            animateHammer();
            networkPackNT(50);
        } else {
            lastHammer = renderHammer;
            if (turnProgress > 0) {
                renderHammer += (syncHammer - renderHammer) / turnProgress;
                --turnProgress;
            } else {
                renderHammer = syncHammer;
            }
        }
    }

    protected void animateHammer() {
        if (activeTicks > 0) activeTicks--;
        double target = activeTicks > 0 ? 1D : 0D;
        amplitude += (target - amplitude) * 0.2D;
        phase += 0.08D;
        if (phase >= 1D) phase -= 1D;
        hammer = (0.5D - 0.5D * Math.cos(phase * Math.PI * 2D)) * amplitude;
    }

    public int getRequired() {
        return mode == MODE_2x2 ? 4 : 9;
    }

    public boolean process() {
        List<EntityMovingItem> items = world.getEntitiesWithinAABB(EntityMovingItem.class,
                new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 0.5, pos.getZ() + 1));
        if (items.isEmpty()) return false;

        Map<ComparableStack, List<EntityMovingItem>> groups = new HashMap<>();
        for (EntityMovingItem item : items) {
            ItemStack stack = item.getItemStack();
            if (stack.isEmpty() || isOwnOutput(item)) continue;
            groups.computeIfAbsent(new ComparableStack(stack).makeSingular(), k -> new ArrayList<>()).add(item);
        }

        int needed = getRequired();
        boolean did = false;
        for (List<EntityMovingItem> list : groups.values()) {
            int total = 0;
            for (EntityMovingItem item : list) total += item.getItemStack().getCount();
            if (total < needed) continue;

            ItemStack output = getCombined(list.get(0).getItemStack(), needed);
            if (output.isEmpty()) continue;

            int sets = total / needed;
            int toConsume = sets * needed;
            for (EntityMovingItem item : list) {
                ItemStack stack = item.getItemStack();
                int take = Math.min(stack.getCount(), toConsume);
                toConsume -= take;
                if (take >= stack.getCount()) {
                    item.setDead();
                } else {
                    stack = stack.copy();
                    stack.shrink(take);
                    item.setItemStack(stack);
                }
                if (toConsume <= 0) break;
            }
            spawnOutput(output, sets * output.getCount());
            did = true;
        }
        return did;
    }

    public boolean isOwnOutput(EntityMovingItem item) {
        NBTTagCompound data = item.getEntityData();
        return data.hasKey(TAG_OWNER + "X") && data.getInteger(TAG_OWNER + "X") == pos.getX()
                && data.getInteger(TAG_OWNER + "Y") == pos.getY() && data.getInteger(TAG_OWNER + "Z") == pos.getZ();
    }

    protected void spawnOutput(ItemStack output, int amount) {
        int max = Math.max(1, output.getMaxStackSize());
        while (amount > 0) {
            int size = Math.min(amount, max);
            ItemStack stack = output.copy();
            stack.setCount(size);
            EntityMovingItem item = new EntityMovingItem(world);
            item.setPosition(pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5);
            item.setItemStack(stack);
            NBTTagCompound data = item.getEntityData();
            data.setInteger(TAG_OWNER + "X", pos.getX());
            data.setInteger(TAG_OWNER + "Y", pos.getY());
            data.setInteger(TAG_OWNER + "Z", pos.getZ());
            world.spawnEntity(item);
            amount -= size;
        }
    }

    public ItemStack getCombined(ItemStack ingredient, int size) {
        ComparableStack singular = new ComparableStack(ingredient).makeSingular();
        Map<ComparableStack, ItemStack> cache = size >= 9 ? FROM_9 : FROM_4;
        ItemStack cached = cache.get(singular);
        if (cached != null) return cached;

        GRID.clear();
        ItemStack one = ingredient.copy();
        one.setCount(1);
        int[] slots = size >= 9 ? new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8} : new int[] {0, 1, 3, 4};
        for (int i : slots) GRID.setInventorySlotContents(i, one.copy());
        ItemStack match = CraftingManager.findMatchingResult(GRID, world).copy();
        cache.put(singular, match);
        return match;
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(mode);
        buf.writeDouble(hammer);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readInt();
        syncHammer = buf.readDouble();
        turnProgress = 2;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        mode = nbt.getInteger("mode");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("mode", mode);
        return nbt;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 2, 2));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }
}
