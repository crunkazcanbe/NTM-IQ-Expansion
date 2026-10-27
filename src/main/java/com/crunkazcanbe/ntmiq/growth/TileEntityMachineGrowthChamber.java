package com.crunkazcanbe.ntmiq.growth;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
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
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.List;

/**
 * Growth Chamber: grows salt/certus buds, budding blocks and flowers from a seed item + fluid. Slots: 0 battery, 1 input,
 * 2-3 upgrades, 4 output. Ported from NTM: IQ Edition (GPL-3; irislgtm, tam69 and the IQ team).
 */
public class TileEntityMachineGrowthChamber extends TileEntityMachineBase implements ITickable, IEnergyReceiverMK2, IFluidStandardReceiverMK2,
        IGUIProvider, IControlReceiver, IUpgradeInfoProvider {
    public long power;
    public long maxPower = 100_000;
    public final FluidTankNTM tank;
    public final ModuleMachineGrowthChamber module;
    public boolean didProcess = false;
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);
    private AudioWrapper audio;

    public TileEntityMachineGrowthChamber() {
        super(5);
        tank = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this);
        module = new ModuleMachineGrowthChamber(0, this, inventory, 1, 4, tank);
    }

    @Override
    public String getDefaultName() {
        return "container.ntmiq.growth_chamber";
    }

    public DirPos[] getConPos() {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new DirPos[] {
                new DirPos(x + 2, y, z, Library.POS_X), new DirPos(x - 2, y, z, Library.NEG_X),
                new DirPos(x, y, z + 2, Library.POS_Z), new DirPos(x, y, z - 2, Library.NEG_Z),
                new DirPos(x + 2, y + 2, z, Library.POS_X), new DirPos(x - 2, y + 2, z, Library.NEG_X),
                new DirPos(x, y + 2, z + 2, Library.POS_Z), new DirPos(x, y + 2, z - 2, Library.NEG_Z)
        };
    }

    @Override
    public void update() {
        if (!world.isRemote) {
            GenericRecipe recipe = module.getRecipe();
            if (recipe != null) maxPower = recipe.power * 100;
            maxPower = BobMathUtil.max(power, maxPower, 100_000);
            power = Library.chargeTEFromItems(inventory, 0, power, maxPower);
            upgradeManager.checkSlots(inventory, 2, 3);

            for (DirPos p : getConPos()) {
                trySubscribe(world, p);
                if (tank.getTankType() != Fluids.NONE) trySubscribe(tank.getTankType(), world, p);
            }

            int spd = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
            int od = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);
            int pwr = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
            double speed = 1D + spd / 3D + od;
            double pow = 1D - pwr * 0.25D + spd + od * 10D / 3D;

            module.update(speed, pow, true, ItemStack.EMPTY);
            didProcess = module.didProcess;
            if (module.markDirty) markDirty();
            networkPackNT(100);
        } else {
            if (didProcess && MainRegistry.proxy.me().getDistance(pos.getX(), pos.getY(), pos.getZ()) < 30) {
                if (audio == null) {
                    audio = createAudioLoop();
                    audio.startSound();
                } else if (!audio.isPlaying()) {
                    audio = rebootAudio(audio);
                }
                audio.keepAlive();
                audio.updateVolume(getVolume(2F));
            } else if (audio != null) {
                audio.stopSound();
                audio = null;
            }
        }
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return MainRegistry.proxy.getLoopedSound(IqGrowth.growthLoop, SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 1F, 15F, 1F, 20);
    }

    @Override public void onChunkUnload() { super.onChunkUnload(); stopAudio(); }
    @Override public void invalidate() { super.invalidate(); stopAudio(); }

    private void stopAudio() {
        if (audio != null) { audio.stopSound(); audio = null; }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true; // battery
        if (slot >= 2 && slot <= 3) return stack.getItem() instanceof ItemMachineUpgrade;
        return module.isItemValid(slot, stack);
    }

    @Override public boolean canExtractItem(int slot, ItemStack stack, int amount) { return slot == 4 || module.isSlotClogged(slot); }
    @Override public int[] getAccessibleSlotsFromSide(EnumFacing side) { return new int[] { 1, 4 }; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] { tank }; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] { tank }; }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        tank.serialize(buf);
        module.serialize(buf);
        buf.writeBoolean(didProcess);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        tank.deserialize(buf);
        module.deserialize(buf);
        didProcess = buf.readBoolean();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        power = nbt.getLong("power");
        maxPower = nbt.getLong("maxPower");
        tank.readFromNBT(nbt, "t");
        module.readFromNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setLong("power", power);
        nbt.setLong("maxPower", maxPower);
        tank.writeToNBT(nbt, "t");
        module.writeToNBT(nbt);
        return super.writeToNBT(nbt);
    }

    @Override public boolean hasPermission(EntityPlayer player) { return isUseableByPlayer(player); }

    @Override
    public void receiveControl(NBTTagCompound data) {
        if (data.hasKey("index") && data.hasKey("selection") && data.getInteger("index") == 0) {
            module.recipe = data.getString("selection");
            markChanged();
        }
    }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerMachineGrowthChamber(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIMachineGrowthChamber(player.inventory, this);
    }

    private AxisAlignedBB bb;

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (bb == null) bb = new AxisAlignedBB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 3, pos.getZ() + 2);
        return bb;
    }

    @Override @SideOnly(Side.CLIENT) public double getMaxRenderDistanceSquared() { return 65536D; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<String> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(IqGrowth.growthChamber));
        if (type == UpgradeType.SPEED) {
            info.add(TextFormatting.GREEN + I18nUtil.resolveKey(KEY_SPEED, "+" + (level * 100 / 3) + "%"));
            info.add(TextFormatting.RED + I18nUtil.resolveKey(KEY_CONSUMPTION, "+" + (level * 50) + "%"));
        }
        if (type == UpgradeType.POWER) info.add(TextFormatting.GREEN + I18nUtil.resolveKey(KEY_CONSUMPTION, "-" + (level * 25) + "%"));
        if (type == UpgradeType.OVERDRIVE) info.add((BobMathUtil.getBlink() ? TextFormatting.RED : TextFormatting.DARK_GRAY) + "YES");
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        upgrades.put(UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }
}
