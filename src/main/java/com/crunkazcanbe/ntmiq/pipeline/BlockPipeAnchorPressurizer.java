package com.crunkazcanbe.ntmiq.pipeline;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.I18nUtil;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.ArrayList;
import java.util.List;

/** IQ's FluidPipeAnchorPressurizer: 2x1x2 base for the Industrial Pipe Anchor. Front = fluid I/O, back + bottom = compressed air. */
public class BlockPipeAnchorPressurizer extends BlockDummyable implements IFluidConnectorBlockMK2, ITooltipProvider, ILookOverlay {

    public BlockPipeAnchorPressurizer(String name) {
        super(Material.IRON, name);
        setSoundType(com.hbm.blocks.ModSoundTypes.pipe);
    }

    @Override
    public net.minecraft.tileentity.TileEntity createNewTileEntity(World world, int meta) {
        return meta >= 12 ? new TileEntityPipeAnchorPressurizer() : null;
    }

    @Override public int[] getDimensions() { return new int[] {0, 0, 1, 0, 1, 0}; }
    @Override public int getOffset() { return 0; }

    private TileEntityPipeAnchorPressurizer core(IBlockAccess world, BlockPos pos) {
        BlockPos core = this.findCore(world, pos);
        return core != null && world.getTileEntity(core) instanceof TileEntityPipeAnchorPressurizer p ? p : null;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        ItemStack held = player.getHeldItem(hand);
        if (!(held.getItem() instanceof IItemFluidIdentifier id)) return false;
        TileEntityPipeAnchorPressurizer press = core(world, pos);
        if (press == null) return false;
        FluidType type = id.getType(world, press.getPos().getX(), press.getPos().getY(), press.getPos().getZ(), held);
        if (type == Fluids.AIR) return false;
        press.tanks[0].setTankType(type);
        press.markDirty();
        return true;
    }

    @Override
    public boolean canConnect(FluidType type, IBlockAccess world, int x, int y, int z, ForgeDirection dir) {
        TileEntityPipeAnchorPressurizer press = core(world, new BlockPos(x, y, z));
        return press != null && press.canConnect(type, dir);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        this.addStandardInfo(tooltip);
    }

    @Override
    public void printHook(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
        TileEntityPipeAnchorPressurizer press = core(world, pos);
        if (press == null) return;
        ForgeDirection facing = press.facing();

        int maxProj = Integer.MIN_VALUE, minProj = Integer.MAX_VALUE;
        for (int[] off : press.getBlocks()) {
            int proj = off[0] * facing.offsetX + off[1] * facing.offsetZ;
            maxProj = Math.max(maxProj, proj);
            minProj = Math.min(minProj, proj);
        }
        int hitProj = (pos.getX() - press.getPos().getX()) * facing.offsetX + (pos.getZ() - press.getPos().getZ()) * facing.offsetZ;

        List<String> text = new ArrayList<>();
        if (hitProj == maxProj) {
            text.add(TextFormatting.GREEN + "->" + TextFormatting.RESET + "/" + TextFormatting.RED + "<- " + TextFormatting.RESET
                    + press.tanks[0].getTankType().getLocalizedName() + ": " + press.tanks[0].getFill() + "/" + press.tanks[0].getMaxFill() + "mB");
        } else if (hitProj == minProj) {
            text.add(TextFormatting.GREEN + "-> " + TextFormatting.RESET + Fluids.AIR.getLocalizedName() + ": "
                    + press.tanks[1].getFill() + "/" + press.tanks[1].getMaxFill() + "mB");
        }
        if (!text.isEmpty()) ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
    }
}
