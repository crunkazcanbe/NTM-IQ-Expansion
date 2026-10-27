package com.crunkazcanbe.ntmiq.sludge;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.List;

/**
 * IQ's TileEntityMachineSludgeProcessor ("Mass Centrifuge"), ported to CE 1.12.2. Slots: 0 battery, 1 blueprint,
 * 2-3 upgrades, 4 item in, 5-8 item out. 1 x 64k input tank, 4 x 64k output tanks.
 */
public class TileEntitySludgeProcessor extends TileEntityMachineBase implements ITickable, IEnergyReceiverMK2, IFluidStandardTransceiverMK2,
        IUpgradeInfoProvider, IControlReceiver, IGUIProvider {

    public FluidTankNTM[] inputTanks;
    public FluidTankNTM[] outputTanks;

    public long power;
    public long maxPower = 1_000_000;
    public boolean didProcess = false;

    public int anim;
    public int prevAnim;

    public ModuleSludgeProcessor sludgeProcessorModule;
    public UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntitySludgeProcessor() {
        super(0, true, true);
        inventory = new ItemStackHandler(9) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                markDirty();
            }
        };
        inputTanks = new FluidTankNTM[] { new FluidTankNTM(Fluids.NONE, 64_000).withOwner(this) };
        outputTanks = new FluidTankNTM[4];
        for (int i = 0; i < 4; i++) outputTanks[i] = new FluidTankNTM(Fluids.NONE, 64_000).withOwner(this);

        sludgeProcessorModule = new ModuleSludgeProcessor(0, this, inventory)
                .itemInput(4).itemOutput(5)
                .fluidInput(inputTanks[0]).fluidOutput(outputTanks[0], outputTanks[1], outputTanks[2], outputTanks[3]);
    }

    @Override
    public String getDefaultName() {
        return "container.machineSludgeProcessor";
    }

    @Override
    public void update() {
        if (maxPower <= 0) maxPower = 1_000_000L;

        if (!world.isRemote) {
            GenericRecipe recipe = sludgeProcessorModule.getRecipe();
            if (recipe != null) maxPower = recipe.power * 100;
            maxPower = BobMathUtil.max(power, maxPower, 1_000_000);
            power = Library.chargeTEFromItems(inventory, 0, power, maxPower);
            upgradeManager.checkSlots(inventory, 2, 3);

            for (DirPos pos : getConPos()) {
                trySubscribe(world, pos);
                for (FluidTankNTM tank : inputTanks) if (tank.getTankType() != Fluids.NONE) trySubscribe(tank.getTankType(), world, pos);
                for (FluidTankNTM tank : outputTanks) if (tank.getTankType() != Fluids.NONE && tank.getFill() > 0) tryProvide(tank, world, pos);
            }

            double speed = 1.0;
            double pow = 1.0;
            speed += Math.min(upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.SPEED), 3) / 3.0;
            speed += Math.min(upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.OVERDRIVE), 3);
            pow -= Math.min(upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.POWER), 3) * 0.25;
            pow += Math.min(upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.SPEED), 3);
            pow += Math.min(upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.OVERDRIVE), 3) * 10.0 / 3.0;

            sludgeProcessorModule.update(speed, pow, true, inventory.getStackInSlot(1));
            didProcess = sludgeProcessorModule.didProcess;
            if (sludgeProcessorModule.markDirty) markDirty();
            networkPackNT(100);
        } else {
            prevAnim = anim;
            if (didProcess) anim++;
        }
    }

    /** IQ's 8 connection points (both short ends, two per corner) */
    public DirPos[] getConPos() {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        switch (ForgeDirection.getOrientation(getBlockMetadata() - BlockDummyable.offset)) {
            case EAST: return new DirPos[] {
                    new DirPos(x + 2, y, z - 2, Library.NEG_Z), new DirPos(x + 3, y, z - 1, Library.POS_X),
                    new DirPos(x + 2, y, z + 3, Library.POS_Z), new DirPos(x + 3, y, z + 2, Library.POS_X),
                    new DirPos(x - 2, y, z - 2, Library.NEG_Z), new DirPos(x - 3, y, z - 1, Library.NEG_X),
                    new DirPos(x - 2, y, z + 3, Library.POS_Z), new DirPos(x - 3, y, z + 2, Library.NEG_X) };
            case WEST: return new DirPos[] {
                    new DirPos(x - 2, y, z - 3, Library.NEG_Z), new DirPos(x - 3, y, z - 2, Library.NEG_X),
                    new DirPos(x - 2, y, z + 2, Library.POS_Z), new DirPos(x - 3, y, z + 1, Library.NEG_X),
                    new DirPos(x + 2, y, z - 3, Library.NEG_Z), new DirPos(x + 3, y, z - 2, Library.POS_X),
                    new DirPos(x + 2, y, z + 2, Library.POS_Z), new DirPos(x + 3, y, z + 1, Library.POS_X) };
            case SOUTH: return new DirPos[] {
                    new DirPos(x - 3, y, z + 2, Library.NEG_X), new DirPos(x - 2, y, z + 3, Library.POS_Z),
                    new DirPos(x + 2, y, z + 2, Library.POS_X), new DirPos(x + 1, y, z + 3, Library.POS_Z),
                    new DirPos(x - 3, y, z - 2, Library.NEG_X), new DirPos(x - 2, y, z - 3, Library.NEG_Z),
                    new DirPos(x + 2, y, z - 2, Library.POS_X), new DirPos(x + 1, y, z - 3, Library.NEG_Z) };
            case NORTH: return new DirPos[] {
                    new DirPos(x - 2, y, z - 2, Library.NEG_X), new DirPos(x - 1, y, z - 3, Library.POS_Z),
                    new DirPos(x + 3, y, z - 2, Library.POS_X), new DirPos(x + 2, y, z - 3, Library.POS_Z),
                    new DirPos(x - 2, y, z + 2, Library.NEG_X), new DirPos(x - 1, y, z + 3, Library.POS_Z),
                    new DirPos(x + 3, y, z + 2, Library.POS_X), new DirPos(x + 2, y, z + 3, Library.POS_Z) };
            default: return new DirPos[0];
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.serialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(didProcess);
        sludgeProcessorModule.serialize(buf);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.deserialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        didProcess = buf.readBoolean();
        sludgeProcessorModule.deserialize(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        inputTanks[0].readFromNBT(nbt, "i0");
        for (int i = 0; i < 4; i++) outputTanks[i].readFromNBT(nbt, "o" + i);
        power = nbt.getLong("power");
        maxPower = nbt.getLong("maxPower");
        sludgeProcessorModule.readFromNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        inputTanks[0].writeToNBT(nbt, "i0");
        for (int i = 0; i < 4; i++) outputTanks[i].writeToNBT(nbt, "o" + i);
        nbt.setLong("power", power);
        nbt.setLong("maxPower", maxPower);
        sludgeProcessorModule.writeToNBT(nbt);
        return super.writeToNBT(nbt);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true;
        if (slot == 1 && stack.getItem() == ModItems.blueprints) return true;
        if (slot >= 2 && slot <= 3 && stack.getItem() instanceof ItemMachineUpgrade) return true;
        return sludgeProcessorModule.isItemValid(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return (slot >= 5 && slot <= 8) || sludgeProcessorModule.isSlotClogged(slot);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(EnumFacing side) {
        return new int[] {4, 5, 6, 7, 8};
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return outputTanks; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] {inputTanks[0], outputTanks[0], outputTanks[1], outputTanks[2], outputTanks[3]}; }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerSludgeProcessor(player.inventory, getCheckedInventory());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUISludgeProcessor(player.inventory, this);
    }

    @Override public boolean hasPermission(EntityPlayer player) { return isUseableByPlayer(player); }

    @Override
    public void receiveControl(NBTTagCompound data) {
        if (data.hasKey("index") && data.hasKey("selection") && data.getInteger("index") == 0) {
            sludgeProcessorModule.recipe = data.getString("selection");
            markChanged();
        }
    }

    AxisAlignedBB bb = null;

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (bb == null) bb = new AxisAlignedBB(pos.getX() - 2, pos.getY(), pos.getZ() - 2, pos.getX() + 3, pos.getY() + 5, pos.getZ() + 3);
        return bb;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }

    @Override
    public boolean canProvideInfo(ItemMachineUpgrade.UpgradeType type, int level, boolean extendedInfo) {
        return type == ItemMachineUpgrade.UpgradeType.SPEED || type == ItemMachineUpgrade.UpgradeType.POWER || type == ItemMachineUpgrade.UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(ItemMachineUpgrade.UpgradeType type, int level, List<String> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(IqSludge.block));
        if (type == ItemMachineUpgrade.UpgradeType.SPEED) {
            info.add(TextFormatting.GREEN + I18nUtil.resolveKey(KEY_SPEED, "+" + (level * 100 / 3) + "%"));
            info.add(TextFormatting.RED + I18nUtil.resolveKey(KEY_CONSUMPTION, "+" + (level * 50) + "%"));
        }
        if (type == ItemMachineUpgrade.UpgradeType.POWER) info.add(TextFormatting.GREEN + I18nUtil.resolveKey(KEY_CONSUMPTION, "-" + (level * 25) + "%"));
        if (type == ItemMachineUpgrade.UpgradeType.OVERDRIVE) info.add((BobMathUtil.getBlink() ? TextFormatting.RED : TextFormatting.DARK_GRAY) + "YES");
    }

    @Override
    public HashMap<ItemMachineUpgrade.UpgradeType, Integer> getValidUpgrades() {
        HashMap<ItemMachineUpgrade.UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(ItemMachineUpgrade.UpgradeType.SPEED, 3);
        upgrades.put(ItemMachineUpgrade.UpgradeType.POWER, 3);
        upgrades.put(ItemMachineUpgrade.UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }
}
