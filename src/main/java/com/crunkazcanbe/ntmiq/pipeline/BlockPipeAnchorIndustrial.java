package com.crunkazcanbe.ntmiq.pipeline;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.tileentity.network.TileEntityPipeBaseNT;
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
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.ArrayList;
import java.util.List;

/** IQ's FluidPipeAnchorIndustrial: 2x2x2 anchor, link two of them with an HBM wrench (click the core blocks), up to 100 m. */
public class BlockPipeAnchorIndustrial extends BlockDummyable implements ITooltipProvider, ILookOverlay {

    public BlockPipeAnchorIndustrial(String name) {
        super(Material.IRON, name);
        setSoundType(com.hbm.blocks.ModSoundTypes.pipe);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return meta >= 12 ? new TileEntityPipeAnchorIndustrial() : null;
    }

    @Override public int[] getDimensions() { return new int[] {1, 0, 1, 0, 1, 0}; }
    @Override public int getOffset() { return 0; }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        ItemStack held = player.getHeldItem(hand);
        if (!(held.getItem() instanceof IItemFluidIdentifier id)) return false;
        BlockPos core = this.findCore(world, pos);
        if (core == null || !(world.getTileEntity(core) instanceof TileEntityPipeBaseNT pipe)) return false;
        FluidType type = id.getType(world, core.getX(), core.getY(), core.getZ(), held);
        if (pipe.getType() == type) return false;
        pipe.setType(type);
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        this.addStandardInfo(tooltip);
    }

    @Override
    public void printHook(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
        BlockPos core = this.findCore(world, pos);
        if (core == null || !(world.getTileEntity(core) instanceof TileEntityPipeBaseNT duct)) return;
        List<String> text = new ArrayList<>();
        text.add("&[" + duct.getType().getColor() + "&]" + duct.getType().getLocalizedName());
        if (!pos.equals(core)) text.add(I18nUtil.resolveKey("tile.ntmiq.pipe_anchor_industrial.core"));
        ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
    }
}
