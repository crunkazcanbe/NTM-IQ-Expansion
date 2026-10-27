package com.crunkazcanbe.ntmiq.magnetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/** IQ's three HbmKeybinds (B / M / H) + the strip's item model (crafter/stocker are CE baked items). */
@SideOnly(Side.CLIENT)
public final class MagneticClient {
    private MagneticClient() {}

    static final String CAT = "key.categories.hbm";
    static final KeyBinding CRAFTER = new KeyBinding(CAT + ".magneticCrafter", Keyboard.KEY_B, CAT);
    static final KeyBinding DEPOSIT = new KeyBinding(CAT + ".restockerMerge", Keyboard.KEY_M, CAT);
    static final KeyBinding RESTOCK = new KeyBinding(CAT + ".restockerRestock", Keyboard.KEY_H, CAT);

    static void preInit() {
        ClientRegistry.registerKeyBinding(CRAFTER);
        ClientRegistry.registerKeyBinding(DEPOSIT);
        ClientRegistry.registerKeyBinding(RESTOCK);
        MinecraftForge.EVENT_BUS.register(new MagneticClient.Events());
    }

    public static class Events {
        @SubscribeEvent
        public void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END || Minecraft.getMinecraft().player == null || Minecraft.getMinecraft().currentScreen != null) return;
            while (CRAFTER.isPressed()) MagneticPackets.NET.sendToServer(new MagneticPackets.KeyMessage(MagneticPackets.KEY_CRAFTER));
            while (DEPOSIT.isPressed()) MagneticPackets.NET.sendToServer(new MagneticPackets.KeyMessage(MagneticPackets.KEY_DEPOSIT));
            while (RESTOCK.isPressed()) MagneticPackets.NET.sendToServer(new MagneticPackets.KeyMessage(MagneticPackets.KEY_RESTOCK));
        }

        @SubscribeEvent
        public void models(ModelRegistryEvent e) {
            ModelLoader.setCustomModelResourceLocation(IqMagnetic.strip, 0, new ModelResourceLocation(IqMagnetic.strip.getRegistryName(), "inventory"));
        }
    }
}
