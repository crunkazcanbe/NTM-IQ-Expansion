package com.crunkazcanbe.ntmiq.railgun;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.items.weapon.sedna.factory.XFactoryFolly;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.Vec3NT;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.vecmath.Vector2d;

/**
 * Railgun turret "Louis". Ported from NTM: IQ Edition (1.7.10, GPL-3) — original by irislgtm, Alexmaster75 / tam69 and the
 * IQ team. Charges a 100 MHE buffer from a 5 MHE storage (250 kHE/t), solves the launch angle for a target X/Z with a
 * linear-drag ballistic model (Lambert W + PD controller, one step per tick) and fires a sabot.
 * Control: GUI text field, Redstone-over-Radio. (IQ's OpenComputers callbacks are not ported.)
 */
public class TileEntityTurretRailgun extends TileEntityMachineBase implements ITickable, IGUIProvider, IEnergyReceiverMK2, IControlReceiver, IRORInteractive, IRORValueProvider {

    public final long maxPower = 5_000_000L;
    public long power;
    public final long consumption = 250_000L;
    public final long maxCharge = 100_000_000L;
    public long charge = 0L;

    public final float velYaw = 1F;
    public final float velPitch = 0.5F;
    public final float barrelLength = 5F;

    public double rotationPitch;
    public double rotationYaw;
    public double targetRotationYaw;

    public boolean active = false;
    public boolean charging = false;
    public boolean calculating = false;
    public ItemStack sabot = ItemStack.EMPTY;
    public int sabotType = -1;

    // ballistic solver state; v0/bc come from the loaded sabot, gravity and mass are fixed (IQ's tuning)
    public float v0 = 1000F;
    public double bc = 0.2D;
    public final double m = 1D;
    public double g = 20D;
    public double theta = Math.PI / 2D;
    public double dtheta = Math.toRadians(1D);
    public double error_old = 0D;
    public int iterations = 0;
    public double t_max = 0D;
    public double x_target = 0D;

    private AudioWrapper audioCharging, audioSpoolUp;
    private boolean chargeReadyAudio = false;

    // x/y of these vectors are world X/Z
    public Vector2d targetVector = null;
    public Vector2d targetDirectionVector = null;

    public double lastRotationYaw;
    public double lastRotationPitch;

    public boolean graphMode = true; // true = rotation graph, false = ballistics graph
    public String status = "";

    public TileEntityTurretRailgun() {
        super(4);
    }

    @Override
    public String getDefaultName() {
        return "container.turretRailgun";
    }

    private AxisAlignedBB bb;

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (bb == null) bb = new AxisAlignedBB(pos.getX() - 3, pos.getY() - 1, pos.getZ() - 3, pos.getX() + 4, pos.getY() + 4, pos.getZ() + 4);
        return bb;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }

    private DirPos[] getConPos() {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        return new DirPos[] {
                new DirPos(x + 3, y, z, ForgeDirection.EAST),
                new DirPos(x - 3, y, z, ForgeDirection.WEST),
                new DirPos(x, y, z + 3, ForgeDirection.SOUTH),
                new DirPos(x, y, z - 3, ForgeDirection.NORTH)
        };
    }

    public static String sabotName(int type) {
        return switch (type) {
            case ItemAmmoRailgun.TUNGSTEN -> "TUNGSTEN";
            case ItemAmmoRailgun.DU -> "DU";
            case ItemAmmoRailgun.DESH -> "DESH";
            case ItemAmmoRailgun.NUKE -> "NUCLEAR";
            case ItemAmmoRailgun.STARMETAL -> "STARMETAL";
            case ItemAmmoRailgun.FLUID -> "FLUID";
            default -> "Empty";
        };
    }

    @Override
    public void update() {
        if (!world.isRemote) {
            this.power = Library.chargeTEFromItems(inventory, 0, this.power, this.getMaxPower());
            for (DirPos dir : getConPos()) this.trySubscribe(world, dir);

            this.status = "IDLE";
            this.sabotType = -1;
            this.sabot = getSabotLoaded();
            this.chargeReadyAudio = false;

            if (this.active && this.power > 0 && !this.sabot.isEmpty()) {
                this.sabotType = this.sabot.getMetadata();
                ItemAmmoRailgun.RailgunSabot data = ItemAmmoRailgun.type(this.sabotType);
                this.v0 = data.v0;
                this.bc = data.bc;

                if (this.targetVector != null) {
                    Vector2d delta = new Vector2d(targetVector.x - pos.getX(), targetVector.y - pos.getZ());
                    // IQ's simplified max range (fine for v0 > 500 and 0.1 < bc < 0.2); out-of-range targets are dropped
                    double maxRange = bc != 0.0D ? (v0 / bc * (0.8D + v0 * 0.0001D)) : (v0 * v0 / g);
                    if (delta.length() > maxRange) {
                        this.targetVector = null;
                    } else {
                        this.targetDirectionVector = new Vector2d(delta);
                        this.targetDirectionVector.normalize();

                        if (this.calculating) {
                            this.status = "COMPUTING";
                            this.x_target = delta.length();
                            this.calculating = calculateAngleTrajectory();
                            if (!this.calculating) {
                                this.charging = true; // angle found, commit to charging
                                this.targetRotationYaw = -Math.atan2(targetDirectionVector.y, targetDirectionVector.x);
                            }
                        }
                    }
                }

                if (this.charging) {
                    this.status = "CHARGING";
                    long d = Math.min(consumption, this.power);
                    if (this.charge < maxCharge && d > 0L) {
                        this.power -= d;
                        this.charge += d;
                    }
                    turnTowardsAngle(this.theta, this.targetRotationYaw);
                    if ((maxCharge - this.charge) / consumption <= 120) this.chargeReadyAudio = true;
                }

                if (this.charge >= maxCharge) this.fire(this.v0);
            }

            // slowly discharge the buffer if nothing is happening
            if (this.charge > 0L && !(this.charging || this.calculating)) this.charge *= 0.98;

            this.networkPackNT(250);
        } else {
            this.lastRotationPitch = this.rotationPitch;
            this.lastRotationYaw = this.rotationYaw;
            audioCharging = playAudio(audioCharging, HBMSoundHandler.alarmContainer, 1000F, this.charging, 100F);
            audioSpoolUp = playAudio(audioSpoolUp, IqRailgun.nukeCharge, 250F, this.chargeReadyAudio, 120F);
        }
    }

    @Override
    public void deserialize(ByteBuf buf) {
        double oldYaw = this.rotationYaw;
        super.deserialize(buf);
        this.active = buf.readBoolean();
        this.power = buf.readLong();
        this.charge = buf.readLong();
        this.targetVector = readVec2d(buf);
        this.targetDirectionVector = readVec2d(buf);
        this.charging = buf.readBoolean();
        this.rotationPitch = buf.readDouble();
        this.rotationYaw = buf.readDouble();
        this.calculating = buf.readBoolean();
        this.chargeReadyAudio = buf.readBoolean();
        this.graphMode = buf.readBoolean();
        this.status = ByteBufUtils.readUTF8String(buf);
        this.sabotType = buf.readInt();
        this.theta = buf.readDouble();
        this.dtheta = buf.readDouble();
        this.error_old = buf.readDouble();
        this.iterations = buf.readInt();
        this.t_max = buf.readDouble();
        this.x_target = buf.readDouble();
        this.v0 = buf.readFloat();
        this.bc = buf.readDouble();

        // fix the interpolation when the turret crosses the 360° line
        if (Math.abs(oldYaw - this.rotationYaw) > Math.PI) {
            if (this.lastRotationYaw < this.rotationYaw) this.lastRotationYaw += Math.PI * 2;
            else this.lastRotationYaw -= Math.PI * 2;
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.active);
        buf.writeLong(this.power);
        buf.writeLong(this.charge);
        writeVec2d(buf, this.targetVector);
        writeVec2d(buf, this.targetDirectionVector);
        buf.writeBoolean(this.charging);
        buf.writeDouble(this.rotationPitch);
        buf.writeDouble(this.rotationYaw);
        buf.writeBoolean(this.calculating);
        buf.writeBoolean(this.chargeReadyAudio);
        buf.writeBoolean(this.graphMode);
        ByteBufUtils.writeUTF8String(buf, this.status);
        buf.writeInt(this.sabotType);
        buf.writeDouble(this.theta);
        buf.writeDouble(this.dtheta);
        buf.writeDouble(this.error_old);
        buf.writeInt(this.iterations);
        buf.writeDouble(this.t_max);
        buf.writeDouble(this.x_target);
        buf.writeFloat(this.v0);
        buf.writeDouble(this.bc);
    }

    private static void writeVec2d(ByteBuf buf, Vector2d v) {
        buf.writeBoolean(v != null);
        if (v == null) return;
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
    }

    private static Vector2d readVec2d(ByteBuf buf) {
        return buf.readBoolean() ? new Vector2d(buf.readDouble(), buf.readDouble()) : null;
    }

    private AudioWrapper playAudio(AudioWrapper audio, net.minecraft.util.SoundEvent sound, float range, boolean condition, float volume) {
        if (condition) {
            if (audio == null) {
                audio = MainRegistry.proxy.getLoopedSound(sound, SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 1F, range, 1F, 20);
                audio.startSound();
            } else if (!audio.isPlaying()) {
                audio = rebootAudio(audio);
            }
            audio.keepAlive();
            audio.updateVolume(this.getVolume(volume));
        } else if (audio != null) {
            audio.stopSound();
            audio = null;
        }
        return audio;
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        stopAudio();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        stopAudio();
    }

    private void stopAudio() {
        if (audioCharging != null) { audioCharging.stopSound(); audioCharging = null; }
        if (audioSpoolUp != null) { audioSpoolUp.stopSound(); audioSpoolUp = null; }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.active = nbt.getBoolean("active");
        this.power = nbt.getLong("power");
        this.charge = nbt.getLong("charge");
        this.charging = nbt.getBoolean("charging");
        this.targetVector = nbt.hasKey("targetX") ? new Vector2d(nbt.getDouble("targetX"), nbt.getDouble("targetZ")) : null;
        this.rotationPitch = nbt.getDouble("rotationPitch");
        this.rotationYaw = nbt.getDouble("rotationYaw");
        this.theta = nbt.getDouble("theta");
        this.targetRotationYaw = nbt.getDouble("targetRotationYaw");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("active", this.active);
        nbt.setLong("power", this.power);
        nbt.setLong("charge", this.charge);
        nbt.setBoolean("charging", this.charging);
        if (this.targetVector != null) {
            nbt.setDouble("targetX", this.targetVector.x);
            nbt.setDouble("targetZ", this.targetVector.y);
        }
        nbt.setDouble("rotationPitch", this.rotationPitch);
        nbt.setDouble("rotationYaw", this.rotationYaw);
        nbt.setDouble("theta", this.theta);
        nbt.setDouble("targetRotationYaw", this.targetRotationYaw);
        return nbt;
    }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerTurretRailgun(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUITurretRailgun(player.inventory, this);
    }

    @Override public long getPower() { return this.power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return this.maxPower; }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.DOWN;
    }

    @Override
    public boolean hasPermission(EntityPlayer player) {
        return this.isUseableByPlayer(player);
    }

    @Override
    public void receiveControl(NBTTagCompound data) {
        if (data.hasKey("activeChange")) this.active = data.getBoolean("activeChange");
        if (data.hasKey("graphModeChange")) this.graphMode = data.getBoolean("graphModeChange");
        if (data.hasKey("dumpCharge") && this.charge > 0L) dumpCharge();
        if (data.hasKey("dataInput")) {
            String input = data.getString("dataInput").toLowerCase();
            String[] params = input.split(PARAM_SEPARATOR);
            if (!this.charging) {
                if (params.length == 2) { // "x:z" sets the target
                    try {
                        this.targetVector = new Vector2d(Integer.parseInt(params[0].trim()), Integer.parseInt(params[1].trim()));
                        return;
                    } catch (NumberFormatException ignored) { }
                } else if (input.equals("fire") && this.targetVector != null && !this.sabot.isEmpty()) {
                    commit();
                    return;
                }
            }
            params = input.split(NAME_SEPARATOR);
            if (params.length > 1) {
                try {
                    if (params[0].equals("pitch")) { this.theta = Math.toRadians(MathHelper.clamp(Integer.parseInt(params[1].trim()), -13, 90)); markChanged(); return; }
                    if (params[0].equals("yaw")) { this.targetRotationYaw = Math.toRadians(Integer.parseInt(params[1].trim()) % 360); markChanged(); return; }
                } catch (NumberFormatException ignored) { }
            }
            if (input.equals("abort") && (this.calculating || this.charging)) abort();
        }
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (i == 0) return stack.getItem() != IqRailgun.ammo; // battery
        return i <= 3 && stack.getItem() == IqRailgun.ammo;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(EnumFacing e) {
        return new int[] { 1, 2, 3 };
    }

    private void spawnBullet(ItemStack projectile, float velocity) {
        Vec3NT vec = new Vec3NT(barrelLength, 0D, 0D);
        vec.rotateAroundZRad(-this.rotationPitch);
        vec.rotateAroundYRad(this.rotationYaw);

        // muzzle beam (visual) — IQ reuses the Folly's beam config
        EntityBulletBeamBase beam = new EntityBulletBeamBase(world, XFactoryFolly.folly_sm, 0F);
        beam.posX = pos.getX() + 0.5 + vec.x;
        beam.posY = pos.getY() + vec.y + 2.25;
        beam.posZ = pos.getZ() + 0.5 + vec.z;
        beam.setRotationsFromVector(vec);
        beam.beamLength = MathHelper.clamp(velocity / 4D, 5D, 50D);

        EntityRailgunProjectile shell = new EntityRailgunProjectile(world);
        shell.setLocationAndAngles(beam.posX, beam.posY, beam.posZ, 0F, 0F);
        shell.shoot(vec.x, vec.y, vec.z, velocity / 20F, 0F);
        shell.setType(projectile.getMetadata());
        if (projectile.getMetadata() == ItemAmmoRailgun.FLUID) {
            shell.fluidId = ItemAmmoRailgun.getFluid(projectile).getID();
            shell.fluidFill = IqRailgun.ammo.getFill(projectile);
        }

        world.spawnEntity(beam);
        world.spawnEntity(shell);
    }

    private void turnTowardsAngle(double targetPitch, double targetYaw) {
        double turnYaw = Math.toRadians(this.velYaw);
        double turnPitch = Math.toRadians(this.velPitch);
        double pi2 = Math.PI * 2;

        if (Math.abs(this.rotationPitch - targetPitch) < turnPitch || Math.abs(this.rotationPitch - targetPitch) > pi2 - turnPitch) {
            this.rotationPitch = targetPitch;
        } else {
            this.rotationPitch += targetPitch > this.rotationPitch ? turnPitch : -turnPitch;
        }

        double deltaYaw = (targetYaw - this.rotationYaw) % pi2;
        int dir = 0;
        if (deltaYaw < -Math.PI) dir = 1;
        else if (deltaYaw < 0) dir = -1;
        else if (deltaYaw > Math.PI) dir = -1;
        else if (deltaYaw > 0) dir = 1;

        if (Math.abs(this.rotationYaw - targetYaw) < turnYaw || Math.abs(this.rotationYaw - targetYaw) > pi2 - turnYaw) this.rotationYaw = targetYaw;
        else this.rotationYaw += turnYaw * dir;

        this.rotationYaw = this.rotationYaw % pi2;
        this.rotationPitch = this.rotationPitch % pi2;
    }

    public ItemStack getSabotLoaded() {
        for (int i = 1; i < 4; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (s.getItem() == IqRailgun.ammo) return s;
        }
        return ItemStack.EMPTY;
    }

    private void consumeAmmo() {
        for (int i = 1; i < 4; i++) {
            if (inventory.getStackInSlot(i).getItem() == IqRailgun.ammo) {
                inventory.extractItem(i, 1, false);
                return;
            }
        }
    }

    private void commit() {
        this.calculating = true;
        this.theta = Math.PI / 2D;
        this.dtheta = Math.toRadians(1D);
        this.iterations = 0;
    }

    private void abort() {
        this.calculating = false;
        this.charging = false;
        world.playSound(null, pos, HBMSoundHandler.shutdown, SoundCategory.BLOCKS, 10F, 1F);
    }

    // IQ: fires at whatever the barrel points at, alignment is NOT checked — intended
    private void dumpCharge() {
        if (this.sabot.isEmpty()) this.sabot = getSabotLoaded();
        if (this.sabot.isEmpty()) { this.charge = 0L; return; }
        this.fire((float) (this.v0 * Math.sqrt((double) this.charge / (double) maxCharge)));
        world.playSound(null, pos, HBMSoundHandler.spark, SoundCategory.BLOCKS, 10F, 1F);
    }

    private void fire(float velocity) {
        this.spawnBullet(this.sabot, velocity);
        this.consumeAmmo();
        world.playSound(null, pos, HBMSoundHandler.loudestNoiseOnEarth, SoundCategory.BLOCKS, 100F, 1F);
        this.charge = 0L;
        this.charging = false;
    }

    public double x_i(double time, double mass, double velX, double bCoeff) {
        return mass * velX / bCoeff * (1D - Math.exp(-(bCoeff / mass * time)));
    }

    public double y_i(double time, double mass, double velY, double bCoeff, double gravity) {
        return mass / bCoeff * (velY + mass * gravity / bCoeff) * (1D - Math.exp(-(bCoeff / mass * time))) - mass * gravity * time / bCoeff;
    }

    // IQ: linear-drag optimal angle solver with a PD controller, one iteration per tick, capped at 600 (30 s)
    private boolean calculateAngleTrajectory() {
        double dt = 0.01D, kp = 0.001D, kd = 0.00001D;
        double tol = Math.toRadians(1000000D);
        double halfPi = Math.PI / 2D;
        double d10 = Math.toRadians(10D);

        if (Math.abs(Math.floor(this.dtheta * tol) / tol) != 0D && this.theta > 0D && this.iterations < 600) {
            double v0x = this.v0 * Math.cos(this.theta);
            double v0y = this.v0 * Math.sin(this.theta);
            double A = v0y + this.m * g / this.bc;
            double D = g / A;
            double k = -this.bc / (this.m * D);
            this.t_max = this.m / this.bc * lambertW0(k * Math.exp(k)) + 1D / D;
            double x = x_i(this.t_max, this.m, v0x, this.bc);

            this.theta -= this.dtheta;
            this.theta = MathHelper.clamp(this.theta, -Math.toRadians(13D), halfPi);
            double error = this.x_target - x;
            double derivative = (error - this.error_old) / dt;
            this.dtheta = Math.toRadians(kp * error + kd * derivative);
            this.dtheta = MathHelper.clamp(this.dtheta, -d10, d10);

            this.error_old = error;
            this.iterations++;
            return true;
        }
        return false;
    }

    /**
     * Upper branch of the Lambert W function. Barry, Barry & Culligan-Hensley, ACM TOMS Algorithm 743 (WAPR); Java port of
     * John Burkardt's MIT-licensed version, taken from IQ's BobMathUtil.lambert_w (branch 0 only, which is all Louis uses).
     */
    public static double lambertW0(double x) {
        double em = -Math.exp(-1.0);
        if (x < em) return Double.NaN;
        if (x == em) return -1.0;
        double c23 = 2.0 / 3.0, em2 = 2.0 / em, d12 = -em2;
        double tb = Math.pow(0.5, 52);
        double x0 = Math.pow(tb, 1.0 / 6.0) * 0.5;
        double x1 = (1.0 - 17.0 * Math.pow(tb, 2.0 / 7.0)) * em;
        double an3 = 8.0 / 3.0, an4 = 135.0 / 83.0, an5 = 166.0 / 39.0, an6 = 3167.0 / 3549.0;
        double s2 = Math.sqrt(2.0), s21 = 2.0 * s2 - 3.0, s22 = 4.0 - 3.0 * s2, s23 = s2 - 2.0;
        double delx = x - em;
        double value;

        if (Math.abs(x) <= x0) {
            return x / (1.0 + x / (1.0 + x / (2.0 + x / (0.6 + 0.34 * x))));
        } else if (x <= x1) {
            double reta = Math.sqrt(d12 * delx);
            return reta / (1.0 + reta / (3.0 + reta / (reta / (an4 + reta / (reta * an6 + an5)) + an3))) - 1.0;
        } else if (x <= 20.0) {
            double reta = s2 * Math.sqrt(1.0 - x / em);
            double an2 = 4.612634277343749 * Math.sqrt(Math.sqrt(reta + 1.09556884765625));
            value = reta / (1.0 + reta / (3.0 + (s21 * an2 + s22) * reta / (s23 * (an2 + reta)))) - 1.0;
        } else {
            double zl = Math.log(x);
            value = Math.log(x / Math.log(x / Math.pow(zl, Math.exp(-1.124491989777808 / (0.4225028202459761 + zl)))));
        }

        // one Newton-like refinement step
        double zn = Math.log(x / value) - value;
        double temp = 1.0 + value;
        double temp2 = 2.0 * temp * (temp + c23 * zn);
        return value * (1.0 + (zn / temp) * (temp2 - zn) / (temp2 - 2.0 * zn));
    }

    /* ---------------- Redstone-over-Radio ---------------- */

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "pitch",
                PREFIX_VALUE + "yaw",
                PREFIX_VALUE + "target",
                PREFIX_VALUE + "charge",
                PREFIX_VALUE + "loadedSabot",
                PREFIX_FUNCTION + "setActive" + NAME_SEPARATOR + "active (0 or 1)",
                PREFIX_FUNCTION + "pitch" + NAME_SEPARATOR + "angle (-13-90)",
                PREFIX_FUNCTION + "yaw" + NAME_SEPARATOR + "angle (0-360)",
                PREFIX_FUNCTION + "enqueue" + NAME_SEPARATOR + "x" + PARAM_SEPARATOR + "z",
                PREFIX_FUNCTION + "fire",
                PREFIX_FUNCTION + "abort",
                PREFIX_FUNCTION + "dump",
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        try {
            if ((PREFIX_FUNCTION + "setActive").equals(name) && params.length > 0) { this.active = Integer.parseInt(params[0]) == 1; markChanged(); }
            if ((PREFIX_FUNCTION + "pitch").equals(name) && params.length > 0) { this.theta = Math.toRadians(MathHelper.clamp(Integer.parseInt(params[0]), -13, 90)); markChanged(); }
            if ((PREFIX_FUNCTION + "yaw").equals(name) && params.length > 0) { this.targetRotationYaw = Math.toRadians(Integer.parseInt(params[0]) % 360); markChanged(); }
            if ((PREFIX_FUNCTION + "enqueue").equals(name) && params.length > 1) { this.targetVector = new Vector2d(Integer.parseInt(params[0]), Integer.parseInt(params[1])); markChanged(); }
        } catch (NumberFormatException ignored) { }
        if ((PREFIX_FUNCTION + "fire").equals(name) && this.targetVector != null && !this.charging) commit();
        if ((PREFIX_FUNCTION + "abort").equals(name)) abort();
        if ((PREFIX_FUNCTION + "dump").equals(name) && this.charge > 0L) dumpCharge();
        return null;
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "pitch").equals(name)) return "" + (int) Math.toDegrees(this.rotationPitch);
        if ((PREFIX_VALUE + "yaw").equals(name)) return "" + (int) Math.toDegrees(this.rotationYaw);
        if ((PREFIX_VALUE + "target").equals(name)) return this.targetVector != null ? ((int) this.targetVector.x + ":" + (int) this.targetVector.y) : "Not set";
        if ((PREFIX_VALUE + "charge").equals(name)) return "" + (int) (this.charge * 100 / this.maxCharge);
        if ((PREFIX_VALUE + "loadedSabot").equals(name)) return sabotName(this.sabotType);
        return null;
    }
}
