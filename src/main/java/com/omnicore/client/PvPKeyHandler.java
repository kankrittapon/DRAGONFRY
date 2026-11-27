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
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

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

    public static final KeyMapping TOGGLE_HUD_KEY = new KeyMapping(
            "key.omnicore.toggle_hud",
            KeyConflictContext.IN_GAME,
            KeyModifier.ALT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.omnicore"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_PVP_KEY);
        event.register(CURSOR_KEY);
        event.register(TOGGLE_HUD_KEY);
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

            while (TOGGLE_HUD_KEY.consumeClick()) {
                OmniClientConfig.isHudHidden = !OmniClientConfig.isHudHidden;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                mc.player.displayClientMessage(
                    Component.literal("§6[OmniCore] §ePvP HUD: " + (OmniClientConfig.isHudHidden ? "§c[ซ่อน / Hidden]" : "§a[แสดง / Visible]")),
                    true
                );
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
                while (TOGGLE_HUD_KEY.consumeClick()) {
                    OmniClientConfig.isHudHidden = !OmniClientConfig.isHudHidden;
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    mc.player.displayClientMessage(
                        Component.literal("§6[OmniCore] §ePvP HUD: " + (OmniClientConfig.isHudHidden ? "§c[ซ่อน / Hidden]" : "§a[แสดง / Visible]")),
                        true
                    );
                }
            }

            // Ctrl: ปลดล็อกเคอร์เซอร์เมาส์ด้วย Transparent Cursor Screen
            if (event.getAction() == GLFW.GLFW_PRESS) {
                boolean isCtrl = event.getKey() == GLFW.GLFW_KEY_LEFT_CONTROL
                        || event.getKey() == GLFW.GLFW_KEY_RIGHT_CONTROL
                        || CURSOR_KEY.matches(event.getKey(), event.getScanCode());
                if (isCtrl) {
                    if (mc.screen == null || mc.screen.getClass().getSimpleName().equals("MergeFeelCursorScreen")) {
                        mc.setScreen(new PvPCursorScreen());
                    }
                }
            }
        }

        /**
         * Interoperability with MergeFeel:
         * If MergeFeel's cursor screen is open, render OmniCore PvP badge on it with full hover tooltip
         */
        @SubscribeEvent
        public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("MergeFeelCursorScreen")) {
                int screenW = mc.getWindow().getGuiScaledWidth();
                int screenH = mc.getWindow().getGuiScaledHeight();
                int mouseX = event.getMouseX();
                int mouseY = event.getMouseY();

                int x = PvPHudOverlay.getBadgeX(screenW);
                int y = PvPHudOverlay.getBadgeY(screenH);
                boolean hovered = mouseX >= x && mouseX <= x + PvPCursorScreen.BTN_W && mouseY >= y && mouseY <= y + PvPCursorScreen.BTN_H;

                PvPHudOverlay.renderPvPBadge(event.getGuiGraphics(), x, y, PvPCursorScreen.BTN_W, PvPCursorScreen.BTN_H, hovered, true);

                if (hovered) {
                    List<Component> tooltipLines = new ArrayList<>();
                    if (ClientPvPState.isPkFlagged()) {
                        tooltipLines.add(Component.literal("§c🗡️ ติดสถานะ Free PK! (ห้ามปิดจนกว่าเวลาจะหมด)"));
                    } else if (ClientPvPState.pvpEnabled) {
                        tooltipLines.add(Component.literal("§e⚔️ สถานะ: §cเปิดโหมด PVP"));
                    } else {
                        tooltipLines.add(Component.literal("§e🛡️ สถานะ: §aโหมดสงบ PVE"));
                    }
                    tooltipLines.add(Component.literal("§8-----------------------------"));
                    tooltipLines.add(Component.literal("§f• §aคลิกซ้าย: §7สลับโหมด PvP / PvE"));
                    tooltipLines.add(Component.literal("§f• §eคลิกขวา: §7ย้ายมุม HUD (§6" + PvPCursorScreen.formatCornerName(OmniClientConfig.currentCorner) + "§7)"));
                    tooltipLines.add(Component.literal("§f• §bAlt+H: §7ซ่อน/แสดง HUD (§b" + (OmniClientConfig.isHudHidden ? "ซ่อนอยู่" : "แสดงอยู่") + "§7)"));
                    event.getGuiGraphics().renderComponentTooltip(mc.font, tooltipLines, mouseX, mouseY);
                }
            }
        }

        /**
         * Global cursor screen click interceptor:
         * Catches mouse clicks directly on OmniCore PvP badge on ANY open transparent cursor screen
         * (Both PvPCursorScreen and MergeFeelCursorScreen)
         */
        @SubscribeEvent
        public static void onScreenMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null && (mc.screen instanceof PvPCursorScreen || mc.screen.getClass().getSimpleName().equals("MergeFeelCursorScreen"))) {
                int screenW = mc.getWindow().getGuiScaledWidth();
                int screenH = mc.getWindow().getGuiScaledHeight();
                double mouseX = event.getMouseX();
                double mouseY = event.getMouseY();
                int button = event.getButton();

                int x = PvPHudOverlay.getBadgeX(screenW);
                int y = PvPHudOverlay.getBadgeY(screenH);

                if (mouseX >= x && mouseX <= x + PvPCursorScreen.BTN_W && mouseY >= y && mouseY <= y + PvPCursorScreen.BTN_H) {
                    if (button == 0) { // Left-Click: Toggle PvP
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                        ModNetwork.CHANNEL.sendToServer(new TogglePvPPacket());
                        event.setCanceled(true);
                    } else if (button == 1) { // Right-Click: Cycle Corner
                        PvPCursorScreen.cycleCorner(mc);
                        event.setCanceled(true);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(
                Commands.literal("pvphud")
                    .executes(ctx -> {
                        final OmniClientConfig.HudCorner current = OmniClientConfig.currentCorner;
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§6[OmniCore] §eตำแหน่งมุม PvP HUD ปัจจุบัน: §a" + PvPCursorScreen.formatCornerName(current)
                            + " §7[สถานะ: " + (OmniClientConfig.isHudHidden ? "§cซ่อนอยู่§7]" : "§aแสดงผล§7]")
                            + "\n§7ใช้งาน: /pvphud <top_left|top_right|bottom_left|bottom_right> หรือ /pvphud <cycle|toggle>"
                        ), false);
                        return 1;
                    })
                    .then(Commands.literal("cycle").executes(ctx -> {
                        PvPCursorScreen.cycleCorner(Minecraft.getInstance());
                        return 1;
                    }))
                    .then(Commands.literal("toggle").executes(ctx -> {
                        OmniClientConfig.isHudHidden = !OmniClientConfig.isHudHidden;
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§6[OmniCore] §ePvP HUD: " + (OmniClientConfig.isHudHidden ? "§c[ซ่อน / Hidden]" : "§a[แสดง / Visible]")
                        ), false);
                        return 1;
                    }))
                    .then(Commands.literal("hide").executes(ctx -> {
                        OmniClientConfig.isHudHidden = !OmniClientConfig.isHudHidden;
                        Minecraft mc = Minecraft.getInstance();
                        if (mc != null) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "§6[OmniCore] §ePvP HUD: " + (OmniClientConfig.isHudHidden ? "§c[ซ่อน / Hidden]" : "§a[แสดง / Visible]")
                        ), false);
                        return 1;
                    }))
                    .then(Commands.literal("top_left").executes(ctx -> setCorner(OmniClientConfig.HudCorner.TOP_LEFT, ctx.getSource())))
                    .then(Commands.literal("top_right").executes(ctx -> setCorner(OmniClientConfig.HudCorner.TOP_RIGHT, ctx.getSource())))
                    .then(Commands.literal("bottom_left").executes(ctx -> setCorner(OmniClientConfig.HudCorner.BOTTOM_LEFT, ctx.getSource())))
                    .then(Commands.literal("bottom_right").executes(ctx -> setCorner(OmniClientConfig.HudCorner.BOTTOM_RIGHT, ctx.getSource())))
            );
        }

        private static int setCorner(OmniClientConfig.HudCorner corner, net.minecraft.commands.CommandSourceStack source) {
            OmniClientConfig.currentCorner = corner;
            try {
                if (OmniClientConfig.INSTANCE != null && OmniClientConfig.INSTANCE.hudCorner != null) {
                    OmniClientConfig.INSTANCE.hudCorner.set(corner);
                }
            } catch (Throwable ignored) {}

            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.25f));
            }

            source.sendSuccess(() -> Component.literal("§6[OmniCore] §eย้ายตำแหน่ง PvP HUD ไปที่: §a" + PvPCursorScreen.formatCornerName(corner)), false);
            return 1;
        }
    }
}


