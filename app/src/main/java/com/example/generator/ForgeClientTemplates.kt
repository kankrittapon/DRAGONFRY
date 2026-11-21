package com.example.generator

import com.example.data.local.StudioConfigEntity

object ForgeClientTemplates {

    fun generateModNetwork(config: StudioConfigEntity): GeneratedFile {
        val pkg = config.packageName.trim().ifBlank { "com.dragonfyre.bridge" }
        val pkgPath = pkg.replace('.', '/')
        val modId = config.modId.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        val content = """
package $pkg.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("$modId", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(AnnouncementPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AnnouncementPacket::encode)
                .decoder(AnnouncementPacket::decode)
                .consumerMainThread(AnnouncementPacket::handle)
                .add();

        CHANNEL.messageBuilder(WorldBossSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WorldBossSyncPacket::encode)
                .decoder(WorldBossSyncPacket::decode)
                .consumerMainThread(WorldBossSyncPacket::handle)
                .add();
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
        """.trimIndent()

        return GeneratedFile(
            path = "src/main/java/$pkgPath/network/ModNetwork.java",
            displayName = "ModNetwork.java",
            category = GeneratedFileCategory.FORGE_CLIENT,
            language = "java",
            descriptionTh = "ระบบ Forge SimpleChannel ส่ง Packet ป้ายประกาศและสถานะ World Boss จาก Server ไปยัง Client",
            content = content
        )
    }

    fun generatePackets(config: StudioConfigEntity): List<GeneratedFile> {
        val pkg = config.packageName.trim().ifBlank { "com.dragonfyre.bridge" }
        val pkgPath = pkg.replace('.', '/')

        val announcePkt = """
package $pkg.network;

import $pkg.client.CenterAnnouncementOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record AnnouncementPacket(
        String headline,
        String subtitle,
        int accentColorRgb,
        String animationStyle,
        String soundId,
        int durationTicks
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(headline, 256);
        buf.writeUtf(subtitle, 256);
        buf.writeInt(accentColorRgb);
        buf.writeUtf(animationStyle, 32);
        buf.writeUtf(soundId, 128);
        buf.writeInt(durationTicks);
    }

    public static AnnouncementPacket decode(FriendlyByteBuf buf) {
        return new AnnouncementPacket(
                buf.readUtf(256),
                buf.readUtf(256),
                buf.readInt(),
                buf.readUtf(32),
                buf.readUtf(128),
                buf.readInt()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                CenterAnnouncementOverlay.pushAnnouncement(this)
        ));
        ctx.get().setPacketHandled(true);
    }
}
        """.trimIndent()

        val bossPkt = """
package $pkg.network;

import $pkg.client.CustomBossBarOverlay;
import $pkg.client.XaeroWaypointBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record WorldBossSyncPacket(
        UUID bossUuid,
        String displayName,
        String modSourceTag,
        float currentHp,
        float maxHp,
        int x,
        int y,
        int z,
        String dimensionId,
        int remainingSeconds,
        String affixCsv,
        String barTheme,
        boolean xaeroRedMarker,
        boolean active
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(bossUuid);
        buf.writeUtf(displayName, 128);
        buf.writeUtf(modSourceTag, 64);
        buf.writeFloat(currentHp);
        buf.writeFloat(maxHp);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeUtf(dimensionId, 96);
        buf.writeInt(remainingSeconds);
        buf.writeUtf(affixCsv, 128);
        buf.writeUtf(barTheme, 32);
        buf.writeBoolean(xaeroRedMarker);
        buf.writeBoolean(active);
    }

    public static WorldBossSyncPacket decode(FriendlyByteBuf buf) {
        return new WorldBossSyncPacket(
                buf.readUUID(),
                buf.readUtf(128),
                buf.readUtf(64),
                buf.readFloat(),
                buf.readFloat(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(96),
                buf.readInt(),
                buf.readUtf(128),
                buf.readUtf(32),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            CustomBossBarOverlay.updateBoss(this);
            if (xaeroRedMarker) {
                if (active) {
                    XaeroWaypointBridge.upsertRedBossWaypoint(bossUuid, displayName, x, y, z, dimensionId);
                } else {
                    XaeroWaypointBridge.removeBossWaypoint(bossUuid);
                }
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
        """.trimIndent()

        return listOf(
            GeneratedFile(
                path = "src/main/java/$pkgPath/network/AnnouncementPacket.java",
                displayName = "AnnouncementPacket.java",
                category = GeneratedFileCategory.FORGE_CLIENT,
                language = "java",
                descriptionTh = "Packet ส่งข้อมูลป้ายประกาศกลางจอพร้อมสี แอนิเมชัน และเสียงเอฟเฟกต์",
                content = announcePkt
            ),
            GeneratedFile(
                path = "src/main/java/$pkgPath/network/WorldBossSyncPacket.java",
                displayName = "WorldBossSyncPacket.java",
                category = GeneratedFileCategory.FORGE_CLIENT,
                language = "java",
                descriptionTh = "Packet อัปเดตเลือดบอส พิกัด เวลาที่เหลือ และสั่งสร้าง/ลบจุดแดงบน Xaero's World Map",
                content = bossPkt
            )
        )
    }

    fun generateClientOverlays(config: StudioConfigEntity): List<GeneratedFile> {
        val pkg = config.packageName.trim().ifBlank { "com.dragonfyre.bridge" }
        val pkgPath = pkg.replace('.', '/')
        val modId = config.modId.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        val prefix = config.xaeroWaypointPrefix.replace("\"", "\\\"")

        val announcementOverlay = """
package $pkg.client;

import $pkg.network.AnnouncementPacket;
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

@Mod.EventBusSubscriber(modid = "$modId", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
        """.trimIndent()

        val bossBarOverlay = """
package $pkg.client;

import $pkg.network.WorldBossSyncPacket;
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
        int barH = ${config.bossBarHeightDp.coerceIn(10, 24)};
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
            ${if (config.enableDamageLagTrail) "gfx.fill(x, barY, x + (int) (barW * nextLag), barY + barH, 0xFFFDE68A);" else "// Damage lag trail disabled"}
            // Active HP fill
            gfx.fill(x, barY, x + (int) (barW * targetRatio), barY + barH, fillCol);

            // Phase notches at 25%, 50%, 75%
            ${if (config.showPhaseNotches) """
            for (int p = 1; p <= 3; p++) {
                int nx = x + (barW * p) / 4;
                gfx.fill(nx, barY, nx + 1, barY + barH, 0xAA0C0B10);
            }
            """.trimIndent() else "// Phase notches disabled"}

            String hpText = String.format("%,.0f / %,.0f HP", boss.currentHp(), boss.maxHp());
            ${if (config.showExactHp) "gfx.drawCenteredString(font, hpText, screenW / 2, barY + Math.max(1, (barH - 8) / 2), 0xFFFFFFFF);" else "// Exact HP hidden"}

            String meta = String.format("⏳ %s  •  📍 (%d, %d, %d) %.0fm", timer, boss.x(), boss.y(), boss.z(), dist);
            ${if (config.showDistanceAndCoords || config.showCountdownTimer) "gfx.drawCenteredString(font, meta, screenW / 2, barY + barH + 4, 0xFFE5C07B);" else ""}

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
        """.trimIndent()

        val xaeroBridge = """
package $pkg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Soft-Dependency Bridge สำหรับ Xaero's Minimap & World Map (1.20.1)
 * สร้างจุดแดงชั่วคราว (Temporary Red Waypoint) ทันทีที่ World Boss เกิด
 * และลบทิ้งอัตโนมัติเมื่อบอสตายหรือหมดเวลา โดยไม่ Crash แม้เวอร์ชัน Xaero เปลี่ยน
 */
public class XaeroWaypointBridge {
    private static final Map<UUID, Object> CREATED_WAYPOINTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_CHAT_FALLBACK = new ConcurrentHashMap<>();

    public static void upsertRedBossWaypoint(UUID bossId, String bossName, int x, int y, int z, String dimId) {
        boolean hasXaero = ModList.get().isLoaded("xaerominimap") || ModList.get().isLoaded("xaeroworldmap");
        if (!hasXaero) return;

        try {
            // Reflection bridge เข้าถึง XaeroMinimapSession -> WaypointsManager -> Current Set
            Class<?> sessionCls = Class.forName("xaero.common.XaeroMinimapSession");
            Object session = sessionCls.getMethod("getCurrentSession").invoke(null);
            if (session == null) return;

            Object wpManager = session.getClass().getMethod("getWaypointsManager").invoke(session);
            Object currentWorld = wpManager.getClass().getMethod("getCurrentWorld").invoke(wpManager);
            if (currentWorld == null) return;

            Object currentSet = currentWorld.getClass().getMethod("getCurrentSet").invoke(currentWorld);
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) currentSet.getClass().getMethod("getList").invoke(currentSet);

            // ลบหมุดเก่าของบอสตัวเดิมก่อนอัปเดตพิกัดใหม่
            Object existing = CREATED_WAYPOINTS.remove(bossId);
            if (existing != null) {
                list.remove(existing);
            }

            // สร้าง Temporary Crimson Red Waypoint (color = 4 คือสีแดงสด, temporary = true)
            Class<?> wpCls = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            Constructor<?> ctor = wpCls.getConstructor(int.class, int.class, int.class, String.class, String.class, int.class, int.class, boolean.class);
            String label = "$prefix " + bossName;
            Object wp = ctor.newInstance(x, y, z, label, "B", 4, 0, true);
            list.add(wp);
            CREATED_WAYPOINTS.put(bossId, wp);
        } catch (Throwable reflectionMismatch) {
            // Fallback ที่ Xaero's Minimap ทุกรุ่นรองรับผ่านระบบ shared waypoint format
            long now = System.currentTimeMillis();
            if (now - LAST_CHAT_FALLBACK.getOrDefault(bossId, 0L) > 30000L) {
                LAST_CHAT_FALLBACK.put(bossId, now);
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    String safeName = bossName.replace(":", "-");
                    String msg = String.format("xaero-waypoint:$prefix %s:B:%d:%d:%d:4:false:0:Internal-overworld-waypoints", safeName, x, y, z);
                    mc.player.displayClientMessage(Component.literal("§c[World Boss Pin] §f" + msg), false);
                }
            }
        }
    }

    public static void removeBossWaypoint(UUID bossId) {
        Object existing = CREATED_WAYPOINTS.remove(bossId);
        LAST_CHAT_FALLBACK.remove(bossId);
        if (existing == null) return;
        try {
            Class<?> sessionCls = Class.forName("xaero.common.XaeroMinimapSession");
            Object session = sessionCls.getMethod("getCurrentSession").invoke(null);
            if (session == null) return;
            Object wpManager = session.getClass().getMethod("getWaypointsManager").invoke(session);
            Object currentWorld = wpManager.getClass().getMethod("getCurrentWorld").invoke(wpManager);
            if (currentWorld == null) return;
            Object currentSet = currentWorld.getClass().getMethod("getCurrentSet").invoke(currentWorld);
            Method getList = currentSet.getClass().getMethod("getList");
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) getList.invoke(currentSet);
            list.remove(existing);
        } catch (Throwable ignored) {
        }
    }
}
        """.trimIndent()

        return listOf(
            GeneratedFile(
                path = "src/main/java/$pkgPath/client/CenterAnnouncementOverlay.java",
                displayName = "CenterAnnouncementOverlay.java",
                category = GeneratedFileCategory.FORGE_CLIENT,
                language = "java",
                descriptionTh = "วาดป้ายประกาศกลางจอแบบ RPG พร้อมคิวข้อความ แอนิเมชัน Slide/Glow และเสียงประกอบ",
                content = announcementOverlay
            ),
            GeneratedFile(
                path = "src/main/java/$pkgPath/client/CustomBossBarOverlay.java",
                displayName = "CustomBossBarOverlay.java",
                category = GeneratedFileCategory.FORGE_CLIENT,
                language = "java",
                descriptionTh = "วาดหลอดเลือดบอส Custom พร้อม Damage Trail, ขีดแบ่งเฟส, เลือดจริง, เวลาถอยหลัง และระยะห่าง",
                content = bossBarOverlay
            ),
            GeneratedFile(
                path = "src/main/java/$pkgPath/client/XaeroWaypointBridge.java",
                displayName = "XaeroWaypointBridge.java",
                category = GeneratedFileCategory.FORGE_CLIENT,
                language = "java",
                descriptionTh = "เชื่อม Xaero's World Map & Minimap ปักจุดแดงบอสอัตโนมัติและลบเมื่อจบไฟต์ (Soft-Dependency)",
                content = xaeroBridge
            )
        )
    }
}
