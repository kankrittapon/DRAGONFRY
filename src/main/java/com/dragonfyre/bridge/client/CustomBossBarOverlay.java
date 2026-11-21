package com.dragonfyre.bridge.client;

import com.dragonfyre.bridge.network.WorldBossSyncPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CustomBossBarOverlay {
    private static final Map<UUID, WorldBossSyncPacket> ACTIVE_BOSSES = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> LAG_HP_RATIO = new ConcurrentHashMap<>();

    public static void updateBoss(WorldBossSyncPacket pkt) {
        if (!pkt.active() || pkt.currentHp() <= 0f) {
            ACTIVE_BOSSES.remove(pkt.bossUuid());
            LAG_HP_RATIO.remove(pkt.bossUuid());
        } else {
            ACTIVE_BOSSES.put(pkt.bossUuid(), pkt);
            LAG_HP_RATIO.putIfAbsent(pkt.bossUuid(), pkt.currentHp() / Math.max(1f, pkt.maxHp()));
        }
    }

    public static final IGuiOverlay BOSS_BAR = (gui, gfx, partialTick, screenW, screenH) -> {
        if (ACTIVE_BOSSES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Font font = mc.font;

        int barW = Math.min(280, screenW - 48);
        int barH = 16;
        int yOffset = 10;

        for (WorldBossSyncPacket boss : ACTIVE_BOSSES.values()) {
            float targetRatio = Math.max(0f, Math.min(1f, boss.currentHp() / Math.max(1f, boss.maxHp())));
            float prevLag = LAG_HP_RATIO.getOrDefault(boss.bossUuid(), targetRatio);
            float nextLag = prevLag > targetRatio ? Math.max(targetRatio, prevLag - 0.008f) : targetRatio;
            LAG_HP_RATIO.put(boss.bossUuid(), nextLag);

            int x = (screenW - barW) / 2;
            int borderCol = resolveThemeBorder(boss.barTheme());
            int fillCol = targetRatio < 0.25f ? 0xFFDC2626 : resolveThemeFill(boss.barTheme());

            double dist = Math.sqrt(mc.player.distanceToSqr(boss.x(), boss.y(), boss.z()));
            String timer = String.format("%02d:%02d", Math.max(0, boss.remainingSeconds()) / 60, Math.max(0, boss.remainingSeconds()) % 60);
            String title = boss.displayName() + " [" + boss.modSourceTag() + "]";

            RenderSystem.enableBlend();
            gfx.drawCenteredString(font, title, screenW / 2, yOffset, 0xFFF5F0E6);

            int barY = yOffset + 12;
            // Outer obsidian frame & metallic border
            gfx.fill(x - 2, barY - 2, x + barW + 2, barY + barH + 2, borderCol);
            gfx.fill(x, barY, x + barW, barY + barH, 0xE6100E17);

            // Delayed white/amber damage lag trail
            gfx.fill(x, barY, x + (int) (barW * nextLag), barY + barH, 0xFFFDE68A);
            // Active HP fill
            gfx.fill(x, barY, x + (int) (barW * targetRatio), barY + barH, fillCol);

            // Phase notches at 25%, 50%, 75%
            for (int p = 1; p <= 3; p++) {
                int nx = x + (barW * p) / 4;
                gfx.fill(nx, barY, nx + 1, barY + barH, 0xAA0C0B10);
            }

            String hpText = String.format("%,.0f / %,.0f HP", boss.currentHp(), boss.maxHp());
            gfx.drawCenteredString(font, hpText, screenW / 2, barY + Math.max(1, (barH - 8) / 2), 0xFFFFFFFF);

            String meta = String.format("⏳ %s  •  📍 (%d, %d, %d) %.0fm", timer, boss.x(), boss.y(), boss.z(), dist);
            gfx.drawCenteredString(font, meta, screenW / 2, barY + barH + 4, 0xFFE5C07B);

            RenderSystem.disableBlend();
            yOffset += barH + 30;
        }
    };

    private static int resolveThemeBorder(String theme) {
        return switch (theme == null ? "" : theme) {
            case "CRIMSON_OBSIDIAN" -> 0xFFEF4444;
            case "ARCANE_RUNE" -> 0xFFA855F7;
            case "EMERALD_VOID" -> 0xFF10B981;
            default -> 0xFFF59E0B;
        };
    }

    private static int resolveThemeFill(String theme) {
        return switch (theme == null ? "" : theme) {
            case "CRIMSON_OBSIDIAN" -> 0xFFB91C1C;
            case "ARCANE_RUNE" -> 0xFF9333EA;
            case "EMERALD_VOID" -> 0xFF059669;
            default -> 0xFFDC2626;
        };
    }
}
