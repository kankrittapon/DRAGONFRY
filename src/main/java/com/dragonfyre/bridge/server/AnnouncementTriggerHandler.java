package com.dragonfyre.bridge.server;

import com.dragonfyre.bridge.network.AnnouncementPacket;
import com.dragonfyre.bridge.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
    private static final String NBT_ANNOUNCED = "df_loot_announced";

    public volatile boolean announcementsEnabled = true;
    public volatile String minLootRarity = "EPIC";
    public volatile String lootFilterCsv = "apotheosis:epic,apotheosis:mythic,apotheosis:ancient,EPIC";
    public volatile String mobKillFilterCsv = "cataclysm:*,iceandfire:*_dragon,twilightforest:*,mowziesmobs:*";

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public static void broadcastCustom(String headline, String subtitle, int colorRgb, String anim, String soundId, int durationTicks) {
        if (!INSTANCE.announcementsEnabled) return;
        ModNetwork.sendToAll(new AnnouncementPacket(headline, subtitle, colorRgb, anim, soundId, durationTicks));
    }

    public static void broadcastBossSpawn(String bossName, BlockPos pos, int minutes) {
        broadcastCustom(
                "🔥 World Boss " + bossName + " ปรากฏตัวแล้ว!",
                String.format("พิกัด (%d, %d, %d) • เปิด Xaero's World Map ดูจุดแดง • เหลือเวลา %d นาที", pos.getX(), pos.getY(), pos.getZ(), minutes),
                0xEF4444, "SLIDE_DOWN", "minecraft:entity.ender_dragon.growl", 140
        );
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
