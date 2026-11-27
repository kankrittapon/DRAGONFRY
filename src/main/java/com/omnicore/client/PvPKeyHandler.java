package com.omnicore.client;

import com.omnicore.config.OmniClientConfig;
import com.omnicore.network.ModNetwork;
import com.omnicore.network.TogglePvPPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
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

    public static final KeyMapping CURSOR_KEY = new KeyMapping(
            "key.omnicore.cursor_screen",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "key.categories.omnicore"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_PVP_KEY);
        event.register(CURSOR_KEY);
    }

    @Mod.EventBusSubscriber(modid = "omnicore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;

            while (TOGGLE_PVP_KEY.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new TogglePvPPacket());
            }
        }

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // Alt+C: สลับโหมด PvP ทันที
            if (mc.screen == null) {
                while (TOGGLE_PVP_KEY.consumeClick()) {
                    ModNetwork.CHANNEL.sendToServer(new TogglePvPPacket());
                }
            }

            // Ctrl: ปปลดล็อกเคอร์เซอร์เมาส์ด้วย Transparent Cursor Screen
            if (event.getAction() == GLFW.GLFW_PRESS) {
                if (mc.screen == null && CURSOR_KEY.matches(event.getKey(), event.getScanCode())) {
                    mc.setScreen(new PvPCursorScreen());
                }
            }
        }

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(
                Commands.literal("pvphud")
                    .executes(ctx -> {
                        OmniClientConfig.HudCorner corner = OmniClientConfig.HudCorner.TOP_LEFT;
                        try {
                            if (OmniClientConfig.INSTANCE != null && OmniClientConfig.INSTANCE.hudCorner != null) {
                                corner = OmniClientConfig.INSTANCE.hudCorner.get();
                            }
                        } catch (Exception ignored) {}
                        final OmniClientConfig.HudCorner current = corner;
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§6[OmniCore] §eตำแหน่งมุม PvP HUD ปัจจุบัน: §a" + PvPCursorScreen.formatCornerName(current)
                            + "\n§7ใช้งาน: /pvphud <top_left|top_right|bottom_left|bottom_right> หรือ /pvphud cycle"
                        ), false);
                        return 1;
                    })
                    .then(Commands.literal("cycle").executes(ctx -> {
                        PvPCursorScreen.cycleCorner(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(Commands.literal("top_left").executes(ctx -> setCorner(OmniClientConfig.HudCorner.TOP_LEFT, ctx.getSource())))
                    .then(Commands.literal("top_right").executes(ctx -> setCorner(OmniClientConfig.HudCorner.TOP_RIGHT, ctx.getSource())))
                    .then(Commands.literal("bottom_left").executes(ctx -> setCorner(OmniClientConfig.HudCorner.BOTTOM_LEFT, ctx.getSource())))
                    .then(Commands.literal("bottom_right").executes(ctx -> setCorner(OmniClientConfig.HudCorner.BOTTOM_RIGHT, ctx.getSource())))
            );
        }

        private static int setCorner(OmniClientConfig.HudCorner corner, net.minecraft.commands.CommandSourceStack source) {
            try {
                if (OmniClientConfig.INSTANCE != null && OmniClientConfig.INSTANCE.hudCorner != null) {
                    OmniClientConfig.INSTANCE.hudCorner.set(corner);
                    OmniClientConfig.SPEC.save();
                }
            } catch (Exception ignored) {}

            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.25f));
            }

            source.sendSuccess(() -> Component.literal("§6[OmniCore] §eย้ายตำแหน่ง PvP HUD ไปที่: §a" + PvPCursorScreen.formatCornerName(corner)), false);
            return 1;
        }
    }
}

