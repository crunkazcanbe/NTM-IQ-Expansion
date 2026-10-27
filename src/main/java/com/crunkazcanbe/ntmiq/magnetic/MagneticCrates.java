package com.crunkazcanbe.ntmiq.magnetic;

import com.crunkazcanbe.ntmiq.Tags;
import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.items.block.ItemBlockStorageCrate;
import com.hbm.tileentity.machine.HandHeldTileEntityCrate;
import com.hbm.tileentity.machine.TileEntityCrate;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * IQ adds an {@code isMagnetic} field to CE's TileEntityCrateBase. Here it is a capability attached to every crate TE
 * instead (no mixin), saved in the TE's ForgeCaps. On break the dropped crate item gets IQ's "magnetic" item tag; on
 * placement that tag turns the new crate magnetic again. CE's held-crate GUI keeps unknown root tags, so it survives.
 */
public final class MagneticCrates {
    private MagneticCrates() {}

    public static final String TAG = "magnetic";
    public static final int RANGE = 8;

    /** the capability's data: just the flag */
    public static class Flag {
        public boolean magnetic;
    }

    @CapabilityInject(Flag.class)
    public static Capability<Flag> CAP = null;

    static void register() {
        CapabilityManager.INSTANCE.register(Flag.class, new Capability.IStorage<Flag>() {
            @Override public NBTBase writeNBT(Capability<Flag> c, Flag f, EnumFacing side) { return new NBTTagByte((byte) (f.magnetic ? 1 : 0)); }
            @Override public void readNBT(Capability<Flag> c, Flag f, EnumFacing side, NBTBase nbt) { f.magnetic = nbt instanceof NBTTagByte b && b.getByte() != 0; }
        }, Flag::new);
    }

    public static boolean isMagnetic(TileEntity te) {
        if (CAP == null || te == null || !te.hasCapability(CAP, null)) return false;
        return te.getCapability(CAP, null).magnetic;
    }

    public static void setMagnetic(TileEntity te, boolean on) {
        if (CAP != null && te != null && te.hasCapability(CAP, null)) {
            te.getCapability(CAP, null).magnetic = on;
            te.markDirty();
        }
    }

    static boolean isMagneticCrateItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemBlockStorageCrate && stack.hasTagCompound() && stack.getTagCompound().getBoolean(TAG);
    }

    /** IQ's ItemMagneticRestocker.getAllCrates: magnetic crate items in the inventory, then unlocked magnetic crates within 8 blocks */
    public static List<ItemStackHandler> getAllCrates(EntityPlayer player) {
        List<ItemStackHandler> list = new ArrayList<>();
        World world = player.world;

        for (ItemStack stack : player.inventory.mainInventory) {
            if (!isMagneticCrateItem(stack)) continue;
            if (!(((ItemBlockStorageCrate) stack.getItem()).getBlock() instanceof BlockStorageCrate block)) continue;
            if (!(block.createNewTileEntity(world, 0) instanceof TileEntityCrate template)) continue;
            HandHeldTileEntityCrate dummy = new HandHeldTileEntityCrate(template, stack);   // writes changes back into the item
            dummy.setWorld(world);
            dummy.setPos(player.getPosition());
            dummy.readNBT(stack.getTagCompound());
            list.add(dummy.inventory);
        }

        BlockPos p = player.getPosition();
        for (int cx = (p.getX() - RANGE) >> 4; cx <= (p.getX() + RANGE) >> 4; cx++)
            for (int cz = (p.getZ() - RANGE) >> 4; cz <= (p.getZ() + RANGE) >> 4; cz++) {
                if (!world.isBlockLoaded(new BlockPos(cx << 4, 0, cz << 4))) continue;   // never load chunks for this
                Chunk chunk = world.getChunk(cx, cz);
                for (TileEntity te : chunk.getTileEntityMap().values()) {
                    BlockPos q = te.getPos();
                    if (Math.abs(q.getX() - p.getX()) > RANGE || Math.abs(q.getY() - p.getY()) > RANGE || Math.abs(q.getZ() - p.getZ()) > RANGE) continue;
                    if (te instanceof TileEntityCrateBase crate && !te.isInvalid() && isMagnetic(te) && !crate.isLocked()) list.add(crate.inventory);
                }
            }
        return list;
    }

    // ---- events: capability attach + carrying the flag through break / place ----

    private static final ResourceLocation KEY = new ResourceLocation(Tags.MODID, "magnetic");
    private static final Set<String> brokenMagnetic = new HashSet<>();               // "dim:pos" broken this tick
    private static final WeakHashMap<EntityPlayer, Boolean> placingMagnetic = new WeakHashMap<>();

    public static class Events {
        @SubscribeEvent
        public void attach(AttachCapabilitiesEvent<TileEntity> e) {
            if (!(e.getObject() instanceof TileEntityCrateBase)) return;
            Flag flag = new Flag();
            e.addCapability(KEY, new ICapabilitySerializable<NBTTagByte>() {
                @Override public boolean hasCapability(Capability<?> c, EnumFacing f) { return c == CAP; }
                @Override public <T> T getCapability(Capability<T> c, EnumFacing f) { return c == CAP ? CAP.cast(flag) : null; }
                @Override public NBTTagByte serializeNBT() { return new NBTTagByte((byte) (flag.magnetic ? 1 : 0)); }
                @Override public void deserializeNBT(NBTTagByte nbt) { flag.magnetic = nbt.getByte() != 0; }
            });
        }

        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent e) {
            if (isMagnetic(e.getWorld().getTileEntity(e.getPos())))
                brokenMagnetic.add(e.getWorld().provider.getDimension() + ":" + e.getPos().toLong());
        }

        /** CE spawns the crate item (IPersistentNBT.breakBlock) right after the break event: tag it */
        @SubscribeEvent
        public void onDrop(EntityJoinWorldEvent e) {
            if (brokenMagnetic.isEmpty() || !(e.getEntity() instanceof EntityItem item)) return;
            ItemStack stack = item.getItem();
            if (!(stack.getItem() instanceof ItemBlockStorageCrate)) return;
            String key = e.getWorld().provider.getDimension() + ":" + new BlockPos(item).toLong();
            if (!brokenMagnetic.remove(key)) return;
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setBoolean(TAG, true);
            item.setItem(stack);
        }

        @SubscribeEvent
        public void tick(TickEvent.ServerTickEvent e) {
            if (e.phase == TickEvent.Phase.END) { brokenMagnetic.clear(); placingMagnetic.clear(); }
        }

        /** remember the flag before the item is used up by placing it */
        @SubscribeEvent
        public void onRightClick(PlayerInteractEvent.RightClickBlock e) {
            if (!e.getWorld().isRemote && isMagneticCrateItem(e.getItemStack())) placingMagnetic.put(e.getEntityPlayer(), true);
        }

        @SubscribeEvent
        public void onPlace(BlockEvent.PlaceEvent e) {
            if (placingMagnetic.remove(e.getPlayer()) == null) return;
            TileEntity te = e.getWorld().getTileEntity(e.getPos());
            if (te instanceof TileEntityCrateBase) setMagnetic(te, true);
        }

        @SubscribeEvent
        public void tooltip(ItemTooltipEvent e) {
            if (isMagneticCrateItem(e.getItemStack())) e.getToolTip().add(1, TextFormatting.AQUA + "(Magnetized)");
        }
    }

}
