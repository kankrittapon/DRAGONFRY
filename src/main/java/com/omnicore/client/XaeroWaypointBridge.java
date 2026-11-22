package com.omnicore.client;

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
            String label = "[BOSS] " + bossName;
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
                    String msg = String.format("xaero-waypoint:[BOSS] %s:B:%d:%d:%d:4:false:0:Internal-overworld-waypoints", safeName, x, y, z);
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
