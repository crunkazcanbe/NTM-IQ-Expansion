package com.crunkazcanbe.ntmiq.conveyor;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.ArrayList;
import java.util.List;

/** IQ's MachineConveyorSpraypainter: 2 tall; items ride through the top block and get painted with the tank's ink. */
public class BlockConveyorSpraypainter extends BlockDummyable implements IConveyorBelt, ILookOverlay, ITooltipProvider {

    public BlockConveyorSpraypainter(String name) {
        super(Material.IRON, name);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return meta >= 12 ? new TileEntityConveyorSpraypainter() : null;
    }

    @Override public int[] getDimensions() { return new int[] {1, 0, 0, 0, 0, 0}; }
    @Override public int getOffset() { return 0; }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (player.isSneaking() || !(held.getItem() instanceof IItemFluidIdentifier id)) return false;
        if (world.isRemote) return true;
        BlockPos core = findCore(world, pos);
        if (core == null || !(world.getTileEntity(core) instanceof TileEntityConveyorSpraypainter painter)) return false;
        FluidType type = id.getType(world, core.getX(), core.getY(), core.getZ(), held);
        painter.tank.setTankType(type);
        painter.markChanged();
        player.sendMessage(new TextComponentString("Changed type to ").setStyle(new Style().setColor(TextFormatting.YELLOW))
                .appendSibling(new TextComponentTranslation(type.getConditionalName())).appendSibling(new TextComponentString("!")));
        return true;
    }

    @Override
    public Vec3d getTravelLocation(World world, int x, int y, int z, Vec3d itemPos, double speed) {
        BlockPos pos = new BlockPos(x, y, z);
        return BlockConveyorCombinator.travel(getClosestSnappingPosition(world, pos, itemPos),
                BlockConveyorCombinator.travelDirection(world, pos.down()), itemPos, speed);
    }

    @Override
    public Vec3d getClosestSnappingPosition(World world, BlockPos pos, Vec3d itemPos) {
        return BlockConveyorCombinator.snap(BlockConveyorCombinator.travelDirection(world, pos.down()), pos, itemPos);
    }

    @Override
    public boolean canItemStay(World world, int x, int y, int z, Vec3d itemPos) {
        IBlockState below = world.getBlockState(new BlockPos(x, y - 1, z));
        return below.getBlock() == this && getMetaFromState(below) >= 12;
    }

    @Override
    public void printHook(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        if (core == null || !(world.getTileEntity(core) instanceof TileEntityConveyorSpraypainter painter)) return;
        List<String> text = new ArrayList<>();
        text.add(BobMathUtil.getShortNumber(painter.power) + "HE / " + BobMathUtil.getShortNumber(TileEntityConveyorSpraypainter.MAX_POWER) + "HE");
        text.add(TextFormatting.GREEN + "-> " + TextFormatting.RESET + painter.tank.getTankType().getLocalizedName() + ": "
                + painter.tank.getFill() + "/" + painter.tank.getMaxFill() + "mB");
        ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        this.addStandardInfo(tooltip);
    }
}
