package com.crunkazcanbe.ntmiq.railgun;

import com.crunkazcanbe.ntmiq.IqContent;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.util.BobMathUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

/**
 * Railgun sabots for "Louis". Ported from NTM: IQ Edition (1.7.10, GPL-3) — original by irislgtm, Alexmaster75 / tam69 and
 * the IQ team. Stats (v0, ballistic coefficient) and impact effects are IQ's.
 * Port change: the Fluid sabot actually stores its fluid on the stack (IQ's was a WIP that kept it on a shared singleton).
 */
public class ItemAmmoRailgun extends Item implements IFillableItem {

    public static final int TUNGSTEN = 0, DU = 1, NUKE = 2, DESH = 3, STARMETAL = 4, FLUID = 5;
    public static final short FLUID_CAPACITY = 1_000;
    public static final RailgunSabot[] TYPES = new RailgunSabot[6];

    public ItemAmmoRailgun() {
        setHasSubtypes(true);
        setRegistryName("ammo_railgun");
        setTranslationKey("ntmiq.ammo_railgun");
        setCreativeTab(IqContent.TAB);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) return;
        for (int i = 0; i < TYPES.length; i++) items.add(new ItemStack(this, 1, i));
    }

    public static RailgunSabot type(int meta) {
        return TYPES[Math.abs(meta) % TYPES.length];
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return "item.ntmiq.ammo_railgun_" + type(stack.getMetadata()).name;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> list, ITooltipFlag flag) {
        String r = TextFormatting.RED + "";
        String y = TextFormatting.YELLOW + "";
        switch (stack.getMetadata()) {
            case TUNGSTEN -> list.add(y + "Tungsten core APFSDS");
            case DU -> list.add(y + "Depleted uranium core APFSDS");
            case NUKE -> list.add(r + "bigass nuke FSDS");
            case DESH -> { list.add(y + "First impact results in a ricochet."); list.add(y + "APFSDS"); }
            case STARMETAL -> list.add(y + "More aerodynamic T-APDSFS");
            case FLUID -> {
                list.add(y + "Fluid filled APDSFS");
                list.add(y + "> " + getFluid(stack).getLocalizedName());
                list.add(y + "> " + getFill(stack) + "/" + FLUID_CAPACITY + "mB");
            }
            default -> { }
        }
        RailgunSabot t = type(stack.getMetadata());
        list.add("===================================");
        list.add(y + "Initial velocity: " + BobMathUtil.getShortNumber((long) t.v0) + "m/s");
        list.add(y + "Ballistic coeff.: " + t.bc + "kgf/m²");
    }

    /* ---------------- fluid sabot (meta 5 only) ---------------- */

    public static FluidType getFluid(ItemStack stack) {
        return stack.hasTagCompound() ? Fluids.fromID(stack.getTagCompound().getShort("type")) : Fluids.NONE;
    }

    private static void setFill(ItemStack stack, FluidType type, int fill) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setShort("type", (short) (fill > 0 ? type.getID() : Fluids.NONE.getID()));
        stack.getTagCompound().setShort("fill", (short) fill);
    }

    @Override
    public int getFill(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound().getShort("fill") : 0;
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return stack.getMetadata() == FLUID && stack.getCount() == 1 && (type == getFluid(stack) || getFill(stack) == 0)
                && !type.isAntimatter() && type.isDispersable();
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack)) return amount;
        int toFill = Math.min(FLUID_CAPACITY - getFill(stack), amount);
        setFill(stack, type, getFill(stack) + toFill);
        return amount - toFill;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return stack.getMetadata() == FLUID && getFill(stack) > 0 && getFluid(stack) == type;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        if (!providesFluid(type, stack)) return 0;
        int out = Math.min(amount, getFill(stack));
        setFill(stack, type, getFill(stack) - out);
        return out;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return getFluid(stack);
    }

    /* ---------------- sabot types ---------------- */

    public abstract static class RailgunSabot {
        public final String name;
        public final ResourceLocation texture;
        public final double bc;   // ballistic coefficient [kgf*m^-2]
        public final float v0;    // initial velocity [m/s]

        RailgunSabot(String name, double bc, float v0) {
            this.name = name;
            this.texture = new ResourceLocation("ntmiq", "textures/models/projectiles/railgun_" + name + ".png");
            this.bc = bc;
            this.v0 = v0;
        }

        public abstract void onImpact(EntityRailgunProjectile sabot, RayTraceResult mop);
    }

    public static void standardExplosion(EntityRailgunProjectile shell, RayTraceResult mop, float size, float rangeMod) {
        Vec3d vec = new Vec3d(shell.motionX, shell.motionY, shell.motionZ).normalize();
        ExplosionVNT xnt = new ExplosionVNT(shell.world, mop.hitVec.x - vec.x, mop.hitVec.y - vec.y, mop.hitVec.z - vec.z, size);
        xnt.setBlockAllocator(new BlockAllocatorStandard(48));
        xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop().withBlockEffect(new BlockMutatorDebris(ModBlocks.block_slag, 1)));
        xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(rangeMod));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
        shell.killAndClear();
    }

    private static void smallFx(EntityRailgunProjectile s, RayTraceResult mop) {
        ExplosionCreator.composeEffect(s.world, mop.hitVec.x, mop.hitVec.y, mop.hitVec.z, 10, 2F, 0.5F, 25F, 5, 0, 20, 0.75F, 1F, -2F, 150);
    }

    private static void bigFx(EntityRailgunProjectile s, RayTraceResult mop) {
        ExplosionCreator.composeEffect(s.world, mop.hitVec.x, mop.hitVec.y, mop.hitVec.z, 10, 2F, 0.5F, 25F, 7, 0, 20, 0.85F, 1F, -2F, 200);
    }

    static {
        TYPES[TUNGSTEN] = new RailgunSabot("tungsten", 0.2D, 1000F) {
            public void onImpact(EntityRailgunProjectile s, RayTraceResult mop) { standardExplosion(s, mop, 10F, 3F); smallFx(s, mop); }
        };
        TYPES[DU] = new RailgunSabot("du", 0.2D, 1100F) {
            public void onImpact(EntityRailgunProjectile s, RayTraceResult mop) { standardExplosion(s, mop, 15F, 3F); bigFx(s, mop); }
        };
        TYPES[NUKE] = new RailgunSabot("nuke", 0.2D, 750F) {
            public void onImpact(EntityRailgunProjectile s, RayTraceResult mop) {
                World w = s.world;
                double x = mop.hitVec.x, y = mop.hitVec.y, z = mop.hitVec.z;
                EntityNukeExplosionMK3 ex = new EntityNukeExplosionMK3(w);
                ex.posX = x + 0.5; ex.posY = y + 0.5; ex.posZ = z + 0.5;
                ex.destructionRange = 50;
                ex.speed = BombConfig.blastSpeed;
                ex.coefficient = 1.0F;
                ex.waste = false;
                w.spawnEntity(ex);
                w.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 100000.0F, 1.0F);
                EntityCloudFleijaRainbow cloud = new EntityCloudFleijaRainbow(w, 50);
                cloud.posX = x; cloud.posY = y; cloud.posZ = z;
                w.spawnEntity(cloud);
                w.spawnEntity(EntityNukeExplosionMK5.statFac(w, BombConfig.missileRadius, x, y, z));
                EntityNukeTorex.statFac(w, x, y, z, 150);
                s.killAndClear();
            }
        };
        TYPES[DESH] = new RailgunSabot("desh", 0.2D, 1000F) {
            public void onImpact(EntityRailgunProjectile s, RayTraceResult mop) {
                if (!s.ricocheted && mop.typeOfHit == RayTraceResult.Type.BLOCK && mop.sideHit != null) {
                    s.ricocheted = true; // IQ kept this flag on the shared sabot type, so only the first desh shell ever bounced
                    Vec3d face = new Vec3d(mop.sideHit.getDirectionVec());
                    Vec3d vel = new Vec3d(s.motionX, s.motionY, s.motionZ);
                    double angle = Math.toRadians(BobMathUtil.getCrossAngle(vel, face));
                    s.motionX *= -1D;
                    s.motionY = s.motionY * -1D * Math.sin(2D * angle);
                    s.motionZ *= -1D;
                    s.setPosition(mop.hitVec.x, mop.hitVec.y, mop.hitVec.z);
                    s.world.playSound(null, s.posX, s.posY, s.posZ, HBMSoundHandler.ricochet, SoundCategory.HOSTILE, 40F, 1F);
                } else {
                    standardExplosion(s, mop, 15F, 3F);
                    bigFx(s, mop);
                }
            }
        };
        TYPES[STARMETAL] = new RailgunSabot("starmetal", 0.1D, 1500F) {
            public void onImpact(EntityRailgunProjectile s, RayTraceResult mop) { standardExplosion(s, mop, 15F, 3F); bigFx(s, mop); }
        };
        TYPES[FLUID] = new RailgunSabot("fluid", 0.2D, 1000F) {
            public void onImpact(EntityRailgunProjectile s, RayTraceResult mop) {
                smallFx(s, mop);
                FluidType type = Fluids.fromID(s.fluidId);
                if (type.isDispersable() && s.fluidFill > 0) {
                    EntityMist mist = new EntityMist(s.world);
                    mist.setType(type);
                    mist.setPosition(s.posX, s.posY, s.posZ);
                    mist.setArea(10, 5);
                    mist.setDuration(s.fluidFill * 200 / FLUID_CAPACITY);
                    s.world.spawnEntity(mist);
                }
                s.killAndClear();
            }
        };
    }
}
