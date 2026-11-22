package com.omnicore.server;

import com.omnicore.OmniCoreMod;
import com.omnicore.network.AnnouncementPacket;
import com.omnicore.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ระบบตรวจจับเหตุการณ์สำหรับป้ายประกาศกลางจอ:
 * - ตรวจจับไอเทมระดับ Epic/Mythic/Ancient จาก Apotheosis (Cisco Dragonfyre/Ultimate) และ Vanilla Rarity
 * - ตรวจจับการสังหารบอส/ม็อบพิเศษข้าม Mod ด้วย wildcard filter
 * - มีระบบ Cooldown & Dedup ป้องกันสแปมประกาศซ้ำตอนโยนของเก็บใหม่
 */
public class AnnouncementTriggerHandler {
    public static final AnnouncementTriggerHandler INSTANCE = new AnnouncementTriggerHandler();
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("DragonfyreAnnouncement");
    private static final String NBT_ANNOUNCED = "df_loot_announced";

    public volatile boolean announcementsEnabled = true;
    public volatile String minLootRarity = "EPIC";
    public volatile String lootFilterCsv = "apotheosis:epic,apotheosis:mythic,apotheosis:ancient,EPIC";
    public volatile String mobKillFilterCsv = "cataclysm:*,iceandfire:*_dragon,twilightforest:*,mowziesmobs:*";

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public static void broadcastCustom(String headline, String subtitle, int colorRgb, String anim, String soundId, int durationTicks) {
        if (!INSTANCE.announcementsEnabled) return;

        LOGGER.info("[OmniCore] " + headline + (subtitle != null && !subtitle.isBlank() ? " | " + subtitle : ""));

        // 1. Send mod packet (for clients with mod installed)
        try {
            ModNetwork.sendToAll(new AnnouncementPacket(headline, subtitle, colorRgb, anim, soundId, durationTicks));
        } catch (Exception ignored) {}

        // 2. Broadcast native Vanilla Title & Chat message (100% Server-side support for unmodded clients)
        MinecraftServer server = OmniCoreMod.SERVER;
        if (server != null) {
            Component headComp = Component.literal("§6" + headline);
            Component subComp = Component.literal("§e" + (subtitle != null ? subtitle : ""));

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                try {
                    player.connection.send(new ClientboundSetTitlesAnimationPacket(10, durationTicks, 20));
                    player.connection.send(new ClientboundSetTitleTextPacket(headComp));
                    if (subtitle != null && !subtitle.isBlank()) {
                        player.connection.send(new ClientboundSetSubtitleTextPacket(subComp));
                    }
                    player.sendSystemMessage(Component.literal("§e[World Boss] §f" + headline + (subtitle != null && !subtitle.isBlank() ? " §7(" + subtitle + ")" : "")));
                } catch (Exception ignored) {}
            }
        }
    }

    public static void broadcastBossSpawn(String bossName, BlockPos pos, int minutes) {
        broadcastCustom(
                "🔥 World Boss " + bossName + " ปรากฏตัวแล้ว!",
                String.format("พิกัด (%d, %d, %d) • เหลือเวลา %d นาที", pos.getX(), pos.getY(), pos.getZ(), minutes),
                0xEF4444, "SLIDE_DOWN", "minecraft:entity.ender_dragon.growl", 140
        );

        // ส่ง Xaero Waypoint format ลงแชทเซิร์ฟเวอร์ ให้ผู้เล่นคลิกปักหมุดลงแผนที่ได้ทันที
        MinecraftServer server = OmniCoreMod.SERVER;
        if (server != null) {
            String safeName = bossName.replace(" ", "_").replace(":", "-");
            String xaeroShare = String.format("xaero-waypoint:[BOSS] %s:☠:%d:%d:%d:4:false:0:Internal-overworld-waypoints", safeName, pos.getX(), pos.getY(), pos.getZ());
            server.getPlayerList().broadcastSystemMessage(Component.literal("§c[Xaero Map Pin] §f" + xaeroShare), false);
        }
    }

    public static void broadcastBossDefeated(String playerName, String bossName) {
        broadcastCustom(
                "👑 " + playerName + " ปราบ World Boss " + bossName + " สำเร็จ!",
                "จุดแดงบนแผนที่ถูกนำออกแล้ว • บันทึกสถิติขึ้น Google Sheets",
                0xF59E0B, "SCALE_POP", "minecraft:ui.toast.challenge_complete", 120
        );
    }

    @SubscribeEvent
    public void onItemPickup(EntityItemPickupEvent event) {
        if (!announcementsEnabled || event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem().getItem();
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getBoolean(NBT_ANNOUNCED)) return;

        String detectedRarity = detectApotheosisOrVanillaRarity(stack);
        if (detectedRarity == null) return;

        String cdKey = player.getUUID() + ":loot";
        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(cdKey, 0L) < 8000L) return;
        cooldowns.put(cdKey, now);

        tag.putBoolean(NBT_ANNOUNCED, true);
        String itemName = stack.getHoverName().getString();
        int color = detectedRarity.contains("MYTHIC") || detectedRarity.contains("ANCIENT") ? 0xF59E0B : 0xA855F7;

        broadcastCustom(
                "⚔ " + player.getName().getString() + " ได้รับไอเทมระดับ " + detectedRarity + ": " + itemName,
                "ตรวจพบไอเทมหายากในระบบเซิร์ฟเวอร์",
                color, "RUNE_GLOW", "minecraft:ui.toast.challenge_complete", 120
        );
        GasHotReloadManager.INSTANCE.logEventAsync("EPIC_LOOT", player.getName().getString(), detectedRarity + " - " + itemName);
    }

    private String detectApotheosisOrVanillaRarity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("affix_data")) {
            String apoRarity = tag.getCompound("affix_data").getString("rarity").toLowerCase(Locale.ROOT);
            for (String token : lootFilterCsv.split(",")) {
                String clean = token.trim().toLowerCase(Locale.ROOT);
                if (!clean.isEmpty() && apoRarity.contains(clean)) {
                    return apoRarity.replace("apotheosis:", "").toUpperCase(Locale.ROOT);
                }
            }
        }
        if (stack.getRarity() == Rarity.EPIC) {
            return "EPIC";
        }
        return null;
    }

    @SubscribeEvent
    public void onMobKilled(LivingDeathEvent event) {
        if (!announcementsEnabled || event.getEntity().level().isClientSide()) return;
        if (event.getEntity().getPersistentData().getBoolean(UniversalBossManager.NBT_BOSS_TAG)) return; // จัดการใน BossManager แล้ว
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ResourceLocation entityRl = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (entityRl == null) return;
        String idStr = entityRl.toString().toLowerCase(Locale.ROOT);

        if (matchesWildcardList(idStr, mobKillFilterCsv)) {
            String mobName = event.getEntity().getDisplayName().getString();
            broadcastCustom(
                    "☠ " + player.getName().getString() + " สังหารม็อบพิเศษ " + mobName + " สำเร็จ!",
                    "Entity: " + idStr,
                    0x38BDF8, "SLIDE_DOWN", "minecraft:entity.wither.death", 100
            );
            GasHotReloadManager.INSTANCE.logEventAsync("MOB_KILL", player.getName().getString(), mobName + " (" + idStr + ")");
        }
    }

    private boolean matchesWildcardList(String entityId, String csv) {
        for (String raw : csv.split(",")) {
            String p = raw.trim().toLowerCase(Locale.ROOT);
            if (p.isEmpty()) continue;
            if (p.equals("*") || p.equals(entityId)) return true;
            if (p.endsWith("*") && entityId.startsWith(p.substring(0, p.length() - 1))) return true;
            if (p.startsWith("*") && entityId.endsWith(p.substring(1))) return true;
        }
        return false;
    }
}
