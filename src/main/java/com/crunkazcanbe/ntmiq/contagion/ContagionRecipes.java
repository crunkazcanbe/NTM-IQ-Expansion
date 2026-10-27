package com.crunkazcanbe.ntmiq.contagion;

import com.crunkazcanbe.ntmiq.blood.ItemFluidSyringe;
import com.crunkazcanbe.ntmiq.blood.ItemMedicalSyringe;
import com.crunkazcanbe.ntmiq.fluids.IqFluids;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * The two bench steps of the lab loop. IQ does these in its Blood Centrifuge and Genome Sequencer, which are not part
 * of F02 here, so the add-on keeps them as crafting recipes: the chain (sample blood, separate the genome, write the
 * disk, brew the dose) stays playable and the same NBT a sequencer writes is produced.
 */
public final class ContagionRecipes {
    private ContagionRecipes() {}

    /** filled medical syringe + empty vial -> a blood vial carrying that disease, and the syringe comes back empty */
    public static class BloodVialDraw extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

        private static boolean isSyringe(ItemStack s) {
            return s.getItem() instanceof ItemMedicalSyringe && ItemFluidSyringe.fill(s) > 0
                    && IqContagion.pathogensOf(s).tagCount() > 0;
        }

        private static boolean isEmptyVial(ItemStack s) {
            return s.getItem() instanceof ItemGenomeVial && ItemFluidSyringe.fill(s) == 0 && GenomeSample.getKey(s) == null;
        }

        @Override
        public boolean matches(InventoryCrafting inv, World world) {
            return find(inv, true) >= 0 && find(inv, false) >= 0 && count(inv) == 2;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            int si = find(inv, true);
            if (si < 0) return ItemStack.EMPTY;
            ItemStack syringe = inv.getStackInSlot(si);
            NBTTagList pathogens = IqContagion.pathogensOf(syringe);
            NBTTagCompound entry = pathogens.getCompoundTagAt(0);

            ItemStack vial = ItemGenomeVial.filled(IqFluids.HUMAN_BLOOD, ItemGenomeVial.MAX_FLUID);
            NBTTagCompound nbt = vial.getTagCompound();
            nbt.setString("frame", entry.getString("frame"));
            if (entry.hasKey("mut", Constants.NBT.TAG_COMPOUND)) {
                NBTTagCompound mut = entry.getCompoundTag("mut");
                nbt.setTag("mut", mut.copy());
                if (mut.hasKey("genome")) nbt.setString("genome", mut.getString("genome"));
            }
            if (syringe.getTagCompound().hasKey(ItemMedicalSyringe.KEY_OWNER_NAME))
                nbt.setString(ItemMedicalSyringe.KEY_OWNER_NAME, syringe.getTagCompound().getString(ItemMedicalSyringe.KEY_OWNER_NAME));
            return vial;
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
            NonNullList<ItemStack> left = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
            int si = find(inv, true);
            if (si >= 0) {
                ItemStack empty = inv.getStackInSlot(si).copy();
                empty.setTagCompound(null);     // the sample went into the vial
                left.set(si, empty);
            }
            return left;
        }

        private static int find(InventoryCrafting inv, boolean syringe) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (!s.isEmpty() && (syringe ? isSyringe(s) : isEmptyVial(s))) return i;
            }
            return -1;
        }

        static int count(InventoryCrafting inv) {
            int n = 0;
            for (int i = 0; i < inv.getSizeInventory(); i++) if (!inv.getStackInSlot(i).isEmpty()) n++;
            return n;
        }

        @Override public boolean canFit(int width, int height) { return width * height >= 2; }
        @Override public ItemStack getRecipeOutput() { return ItemGenomeVial.filled(IqFluids.HUMAN_BLOOD, ItemGenomeVial.MAX_FLUID); }
        @Override public boolean isDynamic() { return true; }
    }

    /** floppy disk + genome sample vial -> the same disk with that section written onto it */
    public static class DiskWrite extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

        @Override
        public boolean matches(InventoryCrafting inv, World world) {
            return findDisk(inv) >= 0 && findSample(inv) >= 0 && BloodVialDraw.count(inv) == 2;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {
            int di = findDisk(inv), vi = findSample(inv);
            if (di < 0 || vi < 0) return ItemStack.EMPTY;
            ItemStack disk = inv.getStackInSlot(di).copy();
            ItemStack sample = inv.getStackInSlot(vi);
            ItemGenomeDisk.putSection(disk, GenomeSample.getKey(sample), GenomeSample.getValue(sample));
            return disk;
        }

        private static int findDisk(InventoryCrafting inv) {
            for (int i = 0; i < inv.getSizeInventory(); i++) if (inv.getStackInSlot(i).getItem() instanceof ItemGenomeDisk) return i;
            return -1;
        }

        private static int findSample(InventoryCrafting inv) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (s.getItem() instanceof ItemGenomeVial && GenomeSample.getKey(s) != null && GenomeSample.getValue(s) != null) return i;
            }
            return -1;
        }

        @Override public boolean canFit(int width, int height) { return width * height >= 2; }
        @Override public ItemStack getRecipeOutput() { return new ItemStack(IqContagion.floppyDisk); }
        @Override public boolean isDynamic() { return true; }
    }
}
