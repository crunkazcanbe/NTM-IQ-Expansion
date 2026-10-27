package com.crunkazcanbe.ntmiq.husks;

import java.util.HashMap;
import java.util.List;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
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

/**
 * IQ's TileEntityMachineHaemodialysis "DIA-1000" (NTM: IQ Edition, GPL-3; irislgtm / tam69 / IQ team), ported to CE.
 * Slots: 0 battery, 1-2 upgrades. 3 x 24k input tanks, 2 x 24k output tanks, 8 connectors around the 3x3 base.
 */
public class TileEntityHaemodialysis extends TileEntityMachineBase implements ITickable, IEnergyReceiverMK2, IFluidStandardTransceiverMK2,
        IUpgradeInfoProvider, IControlReceiver, IGUIProvider {

    public final FluidTankNTM[] inputTanks = new FluidTankNTM[3];
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[2];
    public long power;
    public long maxPower = 1_000_000;
    public boolean didProcess;

    public final ModuleHaemodialysis module;
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityHaemodialysis() {
        super(3, true, true);
        for (int i = 0; i < 3; i++) inputTanks[i] = new FluidTankNTM(Fluids.NONE, 24_000).withOwner(this);
        for (int i = 0; i < 2; i++) outputTanks[i] = new FluidTankNTM(Fluids.NONE, 24_000).withOwner(this);
        module = new ModuleHaemodialysis(0, this, inventory, inputTanks, outputTanks);
    }

    @Override public String getDefaultName() { return "container.machineHaemodialysis"; }

    @Override
    public void update() {
        if (world.isRemote) return;
        GenericRecipe recipe = module.getRecipe();
        maxPower = BobMathUtil.max(power, recipe != null ? recipe.power * 100 : 0, 1_000_000);
        power = Library.chargeTEFromItems(inventory, 0, power, maxPower);
        upgradeManager.checkSlots(inventory, 1, 2);

        for (DirPos p : getConPos()) {
            trySubscribe(world, p);
            for (FluidTankNTM t : inputTanks) if (t.getTankType() != Fluids.NONE) trySubscribe(t.getTankType(), world, p);
            for (FluidTankNTM t : outputTanks) if (t.getTankType() != Fluids.NONE && t.getFill() > 0) tryProvide(t, world, p);
        }

        double speed = 1D + Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3D + Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);
        double pow = 1D - Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25D + Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3)
                + Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 10D / 3D;
        module.update(speed, pow, true, ItemStack.EMPTY);
        didProcess = module.didProcess;
        if (module.markDirty) markDirty();
        networkPackNT(100);
    }

    public DirPos[] getConPos() {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new DirPos[] {
                new DirPos(x + 2, y, z + 1, Library.POS_X), new DirPos(x + 2, y, z - 1, Library.POS_X),
                new DirPos(x - 2, y, z + 1, Library.NEG_X), new DirPos(x - 2, y, z - 1, Library.NEG_X),
                new DirPos(x + 1, y, z + 2, Library.POS_Z), new DirPos(x - 1, y, z + 2, Library.POS_Z),
                new DirPos(x + 1, y, z - 2, Library.NEG_Z), new DirPos(x - 1, y, z - 2, Library.NEG_Z) };
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM t : inputTanks) t.serialize(buf);
        for (FluidTankNTM t : outputTanks) t.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(didProcess);
        module.serialize(buf);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM t : inputTanks) t.deserialize(buf);
        for (FluidTankNTM t : outputTanks) t.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        didProcess = buf.readBoolean();
        module.deserialize(buf);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        for (int i = 0; i < 3; i++) inputTanks[i].readFromNBT(nbt, "i" + i);
        for (int i = 0; i < 2; i++) outputTanks[i].readFromNBT(nbt, "o" + i);
        power = nbt.getLong("power");
        maxPower = nbt.getLong("maxPower");
        module.readFromNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        for (int i = 0; i < 3; i++) inputTanks[i].writeToNBT(nbt, "i" + i);
        for (int i = 0; i < 2; i++) outputTanks[i].writeToNBT(nbt, "o" + i);
        nbt.setLong("power", power);
        nbt.setLong("maxPower", maxPower);
        module.writeToNBT(nbt);
        return super.writeToNBT(nbt);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 || stack.getItem() instanceof ItemMachineUpgrade;
    }

    @Override public boolean canExtractItem(int slot, ItemStack stack, int amount) { return slot >= 1; }
    @Override public int[] getAccessibleSlotsFromSide(EnumFacing side) { return new int[] { 0 }; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return outputTanks; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] { inputTanks[0], inputTanks[1], inputTanks[2], outputTanks[0], outputTanks[1] }; }

    @Override
    public Container provideContainer(int id, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerHaemodialysis(player.inventory, getCheckedInventory());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int id, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIHaemodialysis(player.inventory, this);
    }

    @Override public boolean hasPermission(EntityPlayer player) { return isUseableByPlayer(player); }

    @Override
    public void receiveControl(NBTTagCompound data) {
        if (data.hasKey("index") && data.hasKey("selection") && data.getInteger("index") == 0) {
            module.recipe = data.getString("selection");
            markChanged();
        }
    }

    AxisAlignedBB bb;

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (bb == null) bb = new AxisAlignedBB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 4, pos.getZ() + 2);
        return bb;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() { return 65536.0D; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<String> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(IqHusks.haemodialysis));
        if (type == UpgradeType.SPEED) {
            info.add(TextFormatting.GREEN + I18nUtil.resolveKey(KEY_SPEED, "+" + (level * 100 / 3) + "%"));
            info.add(TextFormatting.RED + I18nUtil.resolveKey(KEY_CONSUMPTION, "+" + (level * 50) + "%"));
        }
        if (type == UpgradeType.POWER) info.add(TextFormatting.GREEN + I18nUtil.resolveKey(KEY_CONSUMPTION, "-" + (level * 25) + "%"));
        if (type == UpgradeType.OVERDRIVE) info.add((BobMathUtil.getBlink() ? TextFormatting.RED : TextFormatting.DARK_GRAY) + "YES");
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> u = new HashMap<>();
        u.put(UpgradeType.SPEED, 3);
        u.put(UpgradeType.POWER, 3);
        u.put(UpgradeType.OVERDRIVE, 3);
        return u;
    }
}
