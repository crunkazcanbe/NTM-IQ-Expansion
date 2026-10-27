package com.crunkazcanbe.ntmiq.drones;

import com.hbm.blocks.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared mining helpers for the F09 drones. In NTM: IQ Edition (1.7.10, GPL-3; irislgtm, tam69 and the IQ team) these
 * lived in EntityDroneBase; here they are static so the demolisher (a CE delivery drone) and the crate drones share them.
 */
final class DroneMining {
    private DroneMining() {}

    /** IQ: any ore-dictionary name starting with "ore" */
    static boolean isOre(World world, BlockPos pos, IBlockState state) {
        ItemStack stack;
        try {
            stack = state.getBlock().getItem(world, pos, state);
        } catch (RuntimeException ex) {
            return false;
        }
        if (stack.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(stack)) if (OreDictionary.getOreName(id).startsWith("ore")) return true;
        return false;
    }

    /** blocks no drone may dig: CE's drone network blocks, our quarry marker, bedrock, air, liquids */
    static boolean isProtected(IBlockState state) {
        Block b = state.getBlock();
        return b == Blocks.BEDROCK || state.getMaterial().isLiquid() || b == IqDrones.quarryMarker
                || b == ModBlocks.drone_crate || b == ModBlocks.drone_dock || b == ModBlocks.drone_crate_provider
                || b == ModBlocks.drone_crate_requester || b == ModBlocks.drone_waypoint || b == ModBlocks.drone_waypoint_request;
    }

    /**
     * IQ's getMinedDrops: normal drops, but HBM ores give raw ore. Port: fire Forge's harvest event so the add-on's
     * raw-ore handler (IqRecipes.RawOreDrops) and other mods see drone mining like a player break.
     */
    static List<ItemStack> drops(World world, BlockPos pos, IBlockState state) {
        NonNullList<ItemStack> drops = NonNullList.create();
        state.getBlock().getDrops(drops, world, pos, state, 0);
        float chance = ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, 0, 1.0F, false, null);
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : drops) if (!s.isEmpty() && world.rand.nextFloat() <= chance) out.add(s);
        return out;
    }

    /** break with particles + sound, no drops (the caller collects them) */
    static void destroy(World world, BlockPos pos, IBlockState state) {
        world.playEvent(2001, pos, Block.getStateId(state));
        world.setBlockToAir(pos);
    }

    static int breakerId(Entity e, BlockPos pos) {
        return e.getEntityId() * 1000003 + pos.getX() * 31 + pos.getY() * 17 + pos.getZ();
    }

    /** IQ's addToStorage: merge, then first empty slot. Returns false if anything is left over (stack keeps the rest). */
    static boolean addToStorage(ItemStack[] slots, ItemStack stack) {
        for (int i = 0; i < slots.length; i++) {
            if (!slots[i].isEmpty() && ItemStack.areItemsEqual(slots[i], stack) && ItemStack.areItemStackTagsEqual(slots[i], stack)) {
                int canAdd = Math.min(stack.getCount(), slots[i].getMaxStackSize() - slots[i].getCount());
                slots[i].grow(canAdd);
                stack.shrink(canAdd);
                if (stack.isEmpty()) return true;
            }
        }
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].isEmpty()) {
                slots[i] = stack.copy();
                stack.setCount(0);
                return true;
            }
        }
        return false;
    }
}
