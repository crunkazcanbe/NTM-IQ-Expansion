package com.crunkazcanbe.ntmiq.magnetic;

import com.crunkazcanbe.ntmiq.Tags;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.ItemStackHandler;

import java.util.LinkedList;
import java.util.List;

/** Client -> server: the three IQ hotkeys, and IQ's MagneticCrafterRecipePacket (fill the grid from inventory + crates). */
public final class MagneticPackets {
    private MagneticPackets() {}

    public static final SimpleNetworkWrapper NET = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MODID + "_magnet");

    public static final int KEY_CRAFTER = 0, KEY_DEPOSIT = 1, KEY_RESTOCK = 2;

    static void register() {
        NET.registerMessage(KeyHandler.class, KeyMessage.class, 0, Side.SERVER);
        NET.registerMessage(RecipeHandler.class, RecipeMessage.class, 1, Side.SERVER);
    }

    public static class KeyMessage implements IMessage {
        int key;
        public KeyMessage() {}
        public KeyMessage(int key) { this.key = key; }
        @Override public void fromBytes(ByteBuf buf) { key = buf.readByte(); }
        @Override public void toBytes(ByteBuf buf) { buf.writeByte(key); }
    }

    public static class KeyHandler implements IMessageHandler<KeyMessage, IMessage> {
        @Override
        public IMessage onMessage(KeyMessage m, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                switch (m.key) {
                    case KEY_CRAFTER:
                        if (ItemMagneticCrafter.has(player, ItemMagneticCrafter.class)) player.openGui(Tags.MODID, IqMagnetic.GUI_CRAFTER, player.world, 0, 0, 0);
                        break;
                    case KEY_DEPOSIT:
                        if (ItemMagneticCrafter.has(player, ItemMagneticRestocker.class)) ItemMagneticRestocker.deposit(player, player.isSneaking());
                        break;
                    case KEY_RESTOCK:
                        if (ItemMagneticCrafter.has(player, ItemMagneticRestocker.class)) ItemMagneticRestocker.restock(player);
                        break;
                    default: break;
                }
            });
            return null;
        }
    }

    /** 9 grid cells, each a list of acceptable stacks (from the JEI recipe) */
    public static class RecipeMessage implements IMessage {
        ItemStack[][] items = new ItemStack[9][];
        public RecipeMessage() {}
        public RecipeMessage(ItemStack[][] items) { this.items = items; }

        @Override
        public void toBytes(ByteBuf buf) {
            for (int i = 0; i < 9; i++) {
                ItemStack[] options = items[i];
                buf.writeShort(options == null ? 0 : Math.min(options.length, 64));
                if (options != null) for (int j = 0; j < Math.min(options.length, 64); j++) ByteBufUtils.writeItemStack(buf, options[j]);
            }
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            for (int i = 0; i < 9; i++) {
                int n = Math.min(buf.readShort(), 64);
                items[i] = n == 0 ? null : new ItemStack[n];
                for (int j = 0; j < n; j++) items[i][j] = ByteBufUtils.readItemStack(buf);
            }
        }
    }

    public static class RecipeHandler implements IMessageHandler<RecipeMessage, IMessage> {
        @Override
        public IMessage onMessage(RecipeMessage m, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (player.openContainer instanceof ContainerMagneticCrafter c) fill(player, c, m.items);
            });
            return null;
        }

        private static void fill(EntityPlayerMP player, ContainerMagneticCrafter container, ItemStack[][] required) {
            InventoryCrafting grid = container.craftMatrix;
            List<ItemStack> limbo = new LinkedList<>();

            // empty the grid back into the inventory first
            for (int i = 0; i < 9; i++) {
                ItemStack existing = grid.getStackInSlot(i);
                if (existing.isEmpty()) continue;
                grid.setInventorySlotContents(i, ItemStack.EMPTY);
                if (!player.inventory.addItemStackToInventory(existing)) limbo.add(existing);
            }

            List<ItemStackHandler> crates = MagneticCrates.getAllCrates(player);
            boolean filled = false;
            for (int slot = 0; slot < 9; slot++) {
                ItemStack[] options = required[slot];
                if (options == null || options.length == 0) continue;
                ItemStack put = takeFromLimbo(options, limbo);
                if (put.isEmpty()) put = takeFromInventory(player, options);
                if (put.isEmpty()) for (ItemStackHandler crate : crates) if (!(put = takeFromCrate(crate, options)).isEmpty()) break;
                if (!put.isEmpty()) {
                    grid.setInventorySlotContents(slot, put);
                    filled = true;
                }
            }

            for (ItemStack stack : limbo) {
                EntityItem item = new EntityItem(player.world, player.posX, player.posY + 0.5D, player.posZ, stack);
                item.setPickupDelay(10);
                player.world.spawnEntity(item);
            }
            container.detectAndSendChanges();
            if (filled) ItemMagneticRestocker.playSound(player);
        }

        private static boolean matches(ItemStack stack, ItemStack[] options) {
            for (ItemStack o : options) if (o != null && !o.isEmpty() && stack.isItemEqual(o)) return true;
            return false;
        }

        private static ItemStack takeFromLimbo(ItemStack[] options, List<ItemStack> limbo) {
            for (ItemStack stack : limbo) {
                if (!matches(stack, options)) continue;
                ItemStack r = stack.splitStack(1);
                if (stack.isEmpty()) limbo.remove(stack);
                return r;
            }
            return ItemStack.EMPTY;
        }

        private static ItemStack takeFromInventory(EntityPlayerMP player, ItemStack[] options) {
            for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
                ItemStack stack = player.inventory.mainInventory.get(i);
                if (!stack.isEmpty() && matches(stack, options)) return player.inventory.decrStackSize(i, 1);
            }
            return ItemStack.EMPTY;
        }

        private static ItemStack takeFromCrate(ItemStackHandler crate, ItemStack[] options) {
            for (int i = 0; i < crate.getSlots(); i++) {
                ItemStack stack = crate.getStackInSlot(i);
                if (stack.isEmpty() || !matches(stack, options)) continue;
                ItemStack rest = stack.copy();
                ItemStack one = rest.splitStack(1);
                crate.setStackInSlot(i, rest);
                return one;
            }
            return ItemStack.EMPTY;
        }
    }
}
