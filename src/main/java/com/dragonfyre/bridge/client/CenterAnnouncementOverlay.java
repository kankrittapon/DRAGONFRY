package com.dragonfyre.bridge.client;

import com.dragonfyre.bridge.network.AnnouncementPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayDeque;
import java.util.Queue;

@Mod.EventBusSubscriber(modid = "dragonfyre_bridge", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CenterAnnouncementOverlay {
    private static final Queue<AnnouncementPacket> QUEUE = new ArrayDeque<>();
    private static AnnouncementPacket current = null;
    private static long startMillis = 0L;

    public static void pushAnnouncement(AnnouncementPacket pkt) {
        QUEUE.offer(pkt);
        if (current == null) {
            advanceNext();
        }
    }

    private static void advanceNext() {
        current = QUEUE.poll();
        if (current != null) {
            startMillis = System.currentTimeMillis();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && current.soundId() != null && !current.soundId().isBlank()) {
                ResourceLocation rl = ResourceLocation.tryParse(current.soundId());
                if (rl != null) {
                    SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(rl);
                    if (sound != null) {
                        mc.player.playSound(sound, 0.85f, 1.0f);
                    }
                }
            }
        }
    }

    public static final IGuiOverlay HUD_BANNER = (gui, gfx, partialTick, screenW, screenH) -> {
        if (current == null) return;
        long elapsed = System.currentTimeMillis() - startMillis;
        long totalMs = Math.max(1500L, current.durationTicks() * 50L);
        if (elapsed >= totalMs) {
            advanceNext();
            return;
        }

        float fadeIn = Math.min(1f, elapsed / 320f);
        float fadeOut = Math.min(1f, (totalMs - elapsed) / 400f);
        float alpha = Math.max(0f, Math.min(fadeIn, fadeOut));
        int aInt = (int) (alpha * 255f);
        if (aInt < 8) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        String headline = current.headline();
        String subtitle = current.subtitle();

        int textW = Math.max(font.width(headline), font.width(subtitle));
        int boxW = Math.min(screenW - 32, Math.max(210, textW + 44));
        int boxH = subtitle.isBlank() ? 30 : 44;
        int x = (screenW - boxW) / 2;

        float slideOffset = "SLIDE_DOWN".equals(current.animationStyle()) ? (1f - fadeIn) * -22f : 0f;
        int y = (int) (screenH * 0.14f + slideOffset);

        int bgCol = ((int) (alpha * 205) << 24) | 0x0E0C15;
        int accent = (aInt << 24) | (current.accentColorRgb() & 0xFFFFFF);
        int textMain = (aInt << 24) | 0xF5F0E6;
        int textSub = (aInt << 24) | 0xC4BED4;

        RenderSystem.enableBlend();
        // Obsidian banner panel + forged metallic accents
        gfx.fill(x, y, x + boxW, y + boxH, bgCol);
        gfx.fill(x, y, x + boxW, y + 2, accent);
        gfx.fill(x, y + boxH - 2, x + boxW, y + boxH, accent);
        gfx.fill(x, y, x + 3, y + boxH, accent);
        gfx.fill(x + boxW - 3, y, x + boxW, y + boxH, accent);

        gfx.drawCenteredString(font, headline, screenW / 2, y + 8, textMain);
        if (!subtitle.isBlank()) {
            gfx.drawCenteredString(font, subtitle, screenW / 2, y + 24, textSub);
        }
        RenderSystem.disableBlend();
    };

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("center_announcement_banner", HUD_BANNER);
        event.registerAboveAll("custom_world_boss_bar", CustomBossBarOverlay.BOSS_BAR);
    }
}
