package com.crunkazcanbe.ntmiq.conveyor;

import com.hbm.api.block.IToolable;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.I18nUtil;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.ArrayList;
import java.util.List;

/** IQ's MachineConveyorCombinator: a one-block conveyor that crafts 2x2 / 3x3 sets of identical items into their block. */
public class BlockConveyorCombinator extends BlockDummyable implements IConveyorBelt, ILookOverlay, IToolable, ITooltipProvider {

    public BlockConveyorCombinator(String name) {
        super(Material.IRON, name);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return meta >= 12 ? new TileEntityConveyorCombinator() : null;
    }

    @Override public int[] getDimensions() { return new int[] {0, 0, 0, 0, 0, 0}; }
    @Override public int getOffset() { return 0; }

    @Override
    public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side, float fX, float fY, float fZ, EnumHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        BlockPos core = findCore(world, new BlockPos(x, y, z));
        if (core == null || !(world.getTileEntity(core) instanceof TileEntityConveyorCombinator combinator)) return false;
        if (world.isRemote) return true;
        combinator.mode = combinator.mode == TileEntityConveyorCombinator.MODE_3x3 ? TileEntityConveyorCombinator.MODE_2x2 : TileEntityConveyorCombinator.MODE_3x3;
        combinator.markChanged();
        combinator.networkPackNT(50);
        world.playSound(null, x + 0.5, y + 0.5, z + 0.5, HBMSoundHandler.upgradePlug, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    static ForgeDirection travelDirection(World world, BlockPos machine) {
        int meta = world.getBlockState(machine).getBlock().getMetaFromState(world.getBlockState(machine)) - offset;
        return ForgeDirection.getOrientation(meta).getRotation(ForgeDirection.UP);
    }

    /** shared by both conveyor machines (same maths as CE's MachineConveyorPress) */
    static Vec3d travel(Vec3d snap, ForgeDirection dir, Vec3d itemPos, double speed) {
        Vec3d dest = new Vec3d(snap.x - dir.offsetX * speed, snap.y - dir.offsetY * speed, snap.z - dir.offsetZ * speed);
        Vec3d motion = dest.subtract(itemPos);
        double len = motion.length();
        return itemPos.add(motion.x / len * speed, motion.y / len * speed, motion.z / len * speed);
    }

    static Vec3d snap(ForgeDirection dir, BlockPos pos, Vec3d itemPos) {
        double posX = pos.getX() + 0.5, posZ = pos.getZ() + 0.5;
        if (dir.offsetX != 0) posX = Math.max(pos.getX(), Math.min(pos.getX() + 1, itemPos.x));
        if (dir.offsetZ != 0) posZ = Math.max(pos.getZ(), Math.min(pos.getZ() + 1, itemPos.z));
        return new Vec3d(posX, pos.getY() + 0.25, posZ);
    }

    @Override
    public Vec3d getTravelLocation(World world, int x, int y, int z, Vec3d itemPos, double speed) {
        BlockPos pos = new BlockPos(x, y, z);
        return travel(getClosestSnappingPosition(world, pos, itemPos), travelDirection(world, pos), itemPos, speed);
    }

    @Override
    public Vec3d getClosestSnappingPosition(World world, BlockPos pos, Vec3d itemPos) {
        return snap(travelDirection(world, pos), pos, itemPos);
    }

    @Override
    public boolean canItemStay(World world, int x, int y, int z, Vec3d itemPos) {
        return world.getBlockState(new BlockPos(x, y, z)).getBlock() == this;
    }

    @Override
    public void printHook(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        if (core == null || !(world.getTileEntity(core) instanceof TileEntityConveyorCombinator combinator)) return;
        List<String> text = new ArrayList<>();
        String key = getTranslationKey();
        String mode = combinator.mode == TileEntityConveyorCombinator.MODE_2x2 ? key + ".mode2x2" : key + ".mode3x3";
        text.add(I18nUtil.resolveKey(key + ".mode") + ": " + I18nUtil.resolveKey(mode));
        ILookOverlay.printGeneric(event, I18nUtil.resolveKey(key + ".name"), 0xffff00, 0x404000, text);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        this.addStandardInfo(tooltip);
    }
}
