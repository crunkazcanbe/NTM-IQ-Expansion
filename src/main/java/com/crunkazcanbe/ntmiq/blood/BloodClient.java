package com.crunkazcanbe.ntmiq.blood;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Item models + fluid tint (layer 1 = the fluid inside) for the F01 items. */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = "ntmiq")
public final class BloodClient {
    private BloodClient() {}

    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (Item i : new Item[] { IqBlood.syringe, IqBlood.combatSyringe, IqBlood.medicalSyringe, IqBlood.notchSyringe, IqBlood.autoDrip, IqBlood.bloodMonitor })
            ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
    }

    @SubscribeEvent
    public static void colors(ColorHandlerEvent.Item e) {
        e.getItemColors().registerItemColorHandler((stack, tint) -> {
            if (tint != 1 || ItemFluidSyringe.fill(stack) <= 0) return 0xFFFFFF;
            FluidType t = ItemFluidSyringe.type(stack);
            return t.getColor() < 0 ? 0xFFFFFF : t.getColor();
        }, IqBlood.items());
    }
}
