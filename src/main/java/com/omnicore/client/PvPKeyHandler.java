package com.omnicore.client;

import com.omnicore.network.ModNetwork;
import com.omnicore.network.TogglePvPPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "omnicore", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PvPKeyHandler {
    public static final KeyMapping TOGGLE_PVP_KEY = new KeyMapping(
            "key.omnicore.toggle_pvp",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.categories.omnicore"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_PVP_KEY);
    }

    @Mod.EventBusSubscriber(modid = "omnicore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeClientEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;

            while (TOGGLE_PVP_KEY.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new TogglePvPPacket());
            }
        }
    }
}
