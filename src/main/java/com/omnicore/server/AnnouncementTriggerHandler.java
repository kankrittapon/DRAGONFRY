package com.omnicore.server;

import com.omnicore.OmniCoreMod;
import com.omnicore.config.OmniConfig;
import com.omnicore.network.AnnouncementPacket;
import com.omnicore.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ระบบตรวจจับเหตุการณ์สำหรับป้ายประกาศกลางจอ (OmniCore Announcement & Loot Engine):
 * 1. Apotheosis Top-Tier Loot:
 *    - ลำดับความหายาก: Common -> Uncommon -> Rare -> Epic -> Mythic -> Ancient
 *    - ประกาศกลางจอ "เฉพาะ 2 ลำดับบนสุดเท่านั้น" (MYTHIC และ ANCIENT / รวม HEIRLOOM, ESOTERIC)
 *    - ลำดับทั่วไป (Common, Uncommon, Rare, Epic) และ Vanilla Item จะไม่ประกาศเด็ดขาด
 * 2. Artifact & Relic Discovery:
 *    - เมื่อผู้เล่นค้นพบ Artifact ชิ้นใหม่เป็นครั้งแรก -> ประกาศกลางจอ
 *    - ชิ้นซ้ำกันที่ได้เป็นครั้งที่สอง -> จะไม่ประกาศ (บันทึกใน Player Persistent Data)
 * 3. Boss / Special Mob Kills:
 *    - ตรวจจับการสังหารบอส/ม็อบพิเศษข้าม Mod ตาม Wildcard Filter
 */
public class AnnouncementTriggerHandler {
    public static final AnnouncementTriggerHandler INSTANCE = new AnnouncementTriggerHandler();
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("OmniCoreAnnounce");
    public static final String NBT_ANNOUNCED = "omnicore_loot_announced";
    public static final String NBT_PLAYER_DATA = "OmniCoreData";
    public static final String NBT_DISCOVERED_ARTIFACTS = "discovered_artifacts";

    public volatile boolean announcementsEnabled = true;
    public volatile boolean mobAnnounceEnabled = true;
    public volatile String minLootRarity = "MYTHIC";

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public boolean isAnnouncementsEnabled() {
        if (!announcementsEnabled) return false;
        try {
            if (OmniConfig.INSTANCE != null && OmniConfig.INSTANCE.announcementsEnabled != null) {
                return OmniConfig.INSTANCE.announcementsEnabled.get();
            }
        } catch (Exception ignored) {}
        return true;
    }

    public static void broadcastCustom(String headline, String subtitle, int colorRgb, String anim, String soundId, int durationTicks) {
        if (!INSTANCE.isAnnouncementsEnabled()) return;

        LOGGER.info("[OmniCore Announcement] " + headline + (subtitle != null && !subtitle.isBlank() ? " | " + subtitle : ""));

        // 1. Send mod packet (for clients with OmniCore installed -> RPG Banner Overlay)
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
                    player.sendSystemMessage(Component.literal("§e[OmniCore] §f" + headline + (subtitle != null && !subtitle.isBlank() ? " §7(" + subtitle + ")" : "")));
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
        if (!isAnnouncementsEnabled() || event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem().getItem();
        if (stack.isEmpty()) return;

        // 1. Tag-level deduplication: หากไอเทมชิ้นนี้เคยประกาศไปแล้ว (เช่น โยนแล้วเก็บใหม่) จะไม่ประกาศซ้ำเด็ดขาด
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getBoolean(NBT_ANNOUNCED)) return;

        // 2. ตรวจสอบว่าเป็น Artifact หรือ Relic หรือไม่ (ตรวจเช็คชิ้นซ้ำระดับ Player Persistent)
        if (isArtifactItem(stack)) {
            handleArtifactPickup(player, stack, tag);
            return;
        }

        // 3. ตรวจสอบว่าเป็นไอเทมระดับ Top-Tier ของ Apotheosis หรือไม่ (เฉพาะ 2 ลำดับบนสุด: MYTHIC, ANCIENT)
        String apoRarity = detectApotheosisRarity(stack);
        if (apoRarity != null && isTopTierApotheosisRarity(apoRarity)) {
            handleApotheosisPickup(player, stack, tag, apoRarity);
        }
        // หมายเหตุ: ไอเทมระดับ Common, Uncommon, Rare, Epic หรือไอเทม Vanilla ทั่วไปจะไม่ประกาศกลางจอเด็ดขาด
    }

    /**
     * จัดการการเก็บไอเทม Artifact:
     * - ได้ครั้งแรก -> ประกาศกลางจอ
     * - ชิ้นซ้ำกันที่ได้ครั้งที่สอง -> จะไม่ประกาศเด็ดขาด
     */
    private void handleArtifactPickup(ServerPlayer player, ItemStack stack, CompoundTag tag) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String artifactKey = rl != null ? rl.toString().toLowerCase(Locale.ROOT) : stack.getItem().toString().toLowerCase(Locale.ROOT);

        // ดึงประวัติ Artifact ที่ผู้เล่นคนนี้เคยค้นพบแล้ว (Persistent ข้าม Restart/Relog)
        CompoundTag playerData = player.getPersistentData();
        CompoundTag omniData = playerData.getCompound(NBT_PLAYER_DATA);
        if (!playerData.contains(NBT_PLAYER_DATA)) {
            playerData.put(NBT_PLAYER_DATA, omniData);
        }

        ListTag discoveredList = omniData.getList(NBT_DISCOVERED_ARTIFACTS, Tag.TAG_STRING);
        boolean alreadyDiscovered = false;
        for (int i = 0; i < discoveredList.size(); i++) {
            if (discoveredList.getString(i).equalsIgnoreCase(artifactKey)) {
                alreadyDiscovered = true;
                break;
            }
        }

        // มาร์กที่ตัวไอเทมไว้เสมอ เพื่อป้องกันการโยนแล้วเก็บใหม่
        tag.putBoolean(NBT_ANNOUNCED, true);

        // หากผู้เล่นเคยค้นพบ Artifact ชิ้นนี้ไปแล้ว -> "ชิ้นซ้ำกันที่ได้ครั้งที่สองจะไม่ประกาศ"
        if (alreadyDiscovered) {
            return;
        }

        // บันทึกว่าผู้เล่นค้นพบ Artifact ชิ้นนี้แล้วเป็นครั้งแรก
        discoveredList.add(StringTag.valueOf(artifactKey));
        omniData.put(NBT_DISCOVERED_ARTIFACTS, discoveredList);

        // Cooldown ป้องกันสแปมกรณีเปิดหีบแล้วเก็บรวดเร็วหลายชิ้นติดกัน
        String cdKey = player.getUUID() + ":artifact";
        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(cdKey, 0L) < 5000L) return;
        cooldowns.put(cdKey, now);

        String itemName = stack.getHoverName().getString();
        broadcastCustom(
                "👑 " + player.getName().getString() + " ค้นพบ Artifact ชิ้นใหม่: " + itemName,
                "บันทึกการค้นพบ Artifact ลงในคอลเลกชันแล้ว (" + artifactKey + ")",
                0xFBBF24, "SCALE_POP", "minecraft:ui.toast.challenge_complete", 130
        );

        GasHotReloadManager.INSTANCE.logEventAsync("ARTIFACT_DISCOVERY", player.getName().getString(), itemName + " [" + artifactKey + "]");
    }

    /**
     * จัดการการเก็บไอเทมระดับ Top-Tier ของ Apotheosis (Mythic, Ancient, ฯลฯ)
     */
    private void handleApotheosisPickup(ServerPlayer player, ItemStack stack, CompoundTag tag, String rarity) {
        String cdKey = player.getUUID() + ":loot";
        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(cdKey, 0L) < 8000L) return;
        cooldowns.put(cdKey, now);

        tag.putBoolean(NBT_ANNOUNCED, true);
        String itemName = stack.getHoverName().getString();

        int color = 0xF59E0B; // Gold/Orange สำหรับ Mythic
        String anim = "RUNE_GLOW";
        if (rarity.equals("ANCIENT") || rarity.equals("ESOTERIC") || rarity.equals("HEIRLOOM")) {
            color = 0xEF4444; // Crimson/Rainbow สำหรับ Ancient และระดับสูงสุด
            anim = "SCALE_POP";
        }

        broadcastCustom(
                "⚔ " + player.getName().getString() + " ได้รับไอเทมระดับ " + rarity + ": " + itemName,
                "ตรวจพบไอเทมระดับตำนานในระบบเซิร์ฟเวอร์",
                color, anim, "minecraft:ui.toast.challenge_complete", 120
        );

        GasHotReloadManager.INSTANCE.logEventAsync("EPIC_LOOT", player.getName().getString(), rarity + " - " + itemName);
    }

    /**
     * ตรวจสอบว่าไอเทมเป็นชิ้นส่วนประเภท Artifact หรือ Relic หรือไม่
     */
    public boolean isArtifactItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl != null) {
            String ns = rl.getNamespace().toLowerCase(Locale.ROOT);
            String path = rl.getPath().toLowerCase(Locale.ROOT);
            String fullId = rl.toString().toLowerCase(Locale.ROOT);

            // 1. Mod Namespaces สำหรับ Artifact & Relic
            if (ns.equals("relics") || ns.equals("artifacts") || ns.equals("betterarcheology")) {
                if (path.contains("artifact") || path.contains("relic") || ns.equals("relics") || ns.equals("artifacts")) {
                    return true;
                }
            }

            // 2. ตรวจสอบตาม Item Filters จาก Config
            String filterCsv = "relics:*,artifacts:*,*artifact*,*relic*";
            try {
                if (OmniConfig.INSTANCE != null && OmniConfig.INSTANCE.artifactItemFilters != null) {
                    filterCsv = OmniConfig.INSTANCE.artifactItemFilters.get();
                }
            } catch (Exception ignored) {}

            if (matchesWildcardList(fullId, filterCsv)) {
                return true;
            }
        }

        // 3. ตรวจสอบระดับความหายากชื่อ ARTIFACT จาก Apotheotic Additions
        String apoRarity = detectApotheosisRarity(stack);
        return apoRarity != null && apoRarity.equalsIgnoreCase("ARTIFACT");
    }

    /**
     * ดึงระดับความหายากจาก Apotheosis (Affix Items, Gems และ Tag)
     */
    public static String detectApotheosisRarity(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;

        // 1. ตรวจจาก Affix Data: affix_data.rarity
        if (tag.contains("affix_data")) {
            CompoundTag affixData = tag.getCompound("affix_data");
            if (affixData.contains("rarity")) {
                String r = affixData.getString("rarity").trim().toLowerCase(Locale.ROOT);
                return cleanRarityName(r);
            }
        }

        // 2. ตรวจจาก Gem Item: gem & rarity
        if (tag.contains("gem") && tag.contains("rarity")) {
            String r = tag.getString("rarity").trim().toLowerCase(Locale.ROOT);
            return cleanRarityName(r);
        }

        // 3. ตรวจจาก Root Tag rarity (ถ้ามี)
        if (tag.contains("rarity") && tag.getTagType("rarity") == Tag.TAG_STRING) {
            String r = tag.getString("rarity").trim().toLowerCase(Locale.ROOT);
            if (!r.isEmpty()) {
                return cleanRarityName(r);
            }
        }

        return null;
    }

    private static String cleanRarityName(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        if (raw.contains(":")) {
            raw = raw.substring(raw.indexOf(":") + 1);
        }
        return raw.toUpperCase(Locale.ROOT);
    }

    /**
     * กรองเฉพาะ 2 ลำดับบนสุดของ Apotheosis เท่านั้น:
     * - ลำดับต่ำกว่า (Common, Uncommon, Rare, Epic) จะคืนค่า false เสมอ
     * - 2 ลำดับบนสุด (Mythic, Ancient รวมทั้ง Heirloom, Esoteric) จะคืนค่า true
     */
    private boolean isTopTierApotheosisRarity(String rarity) {
        if (rarity == null) return false;

        // ดักตัดลำดับทั่วไปออก 100%
        if (rarity.equals("COMMON") || rarity.equals("UNCOMMON") || rarity.equals("RARE") || rarity.equals("EPIC")) {
            return false;
        }

        // หากเป็น ARTIFACT จะถูกส่งไปประมวลผลในระบบ Artifact (ชิ้นแรกประกาศ ชิ้นซ้ำไม่ประกาศ)
        if (rarity.equals("ARTIFACT")) {
            return false;
        }

        String allowedCsv = "MYTHIC,ANCIENT,HEIRLOOM,ESOTERIC";
        try {
            if (OmniConfig.INSTANCE != null && OmniConfig.INSTANCE.apotheosisAnnounceRarities != null) {
                allowedCsv = OmniConfig.INSTANCE.apotheosisAnnounceRarities.get();
            }
        } catch (Exception ignored) {}

        for (String token : allowedCsv.split(",")) {
            if (rarity.equalsIgnoreCase(token.trim())) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onMobKilled(LivingDeathEvent event) {
        if (!isAnnouncementsEnabled() || !mobAnnounceEnabled || event.getEntity().level().isClientSide()) return;
        if (event.getEntity().getPersistentData().getBoolean(UniversalBossManager.NBT_BOSS_TAG)) return; // จัดการใน BossManager แล้ว
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        net.minecraft.world.entity.LivingEntity entity = event.getEntity();
        ResourceLocation entityRl = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityRl == null) return;
        String idStr = entityRl.toString().toLowerCase(Locale.ROOT);

        // 1. Blacklist Check: ดักตัดม็อบลูกน้อง, มินเนียน, ซัมมอน, สัตว์เล็ก, นก, ลิง, กบ ออก 100%
        String blacklistCsv = "*minion*,*baby*,*drone*,*larva*,*servant*,*summon*,*crane*,*fitter*,*raptor*,*grottol*,*lantern*,*foliaath*,*umvuthana*,twilightforest:raven,twilightforest:squirrel,twilightforest:bunny,twilightforest:tiny_bird,twilightforest:kobold,twilightforest:firefly,twilightforest:mosquito_swarm,twilightforest:slime_beetle,twilightforest:redcap*";
        try {
            if (OmniConfig.INSTANCE != null && OmniConfig.INSTANCE.mobKillBlacklist != null) {
                blacklistCsv = OmniConfig.INSTANCE.mobKillBlacklist.get();
            }
        } catch (Exception ignored) {}

        if (matchesWildcardList(idStr, blacklistCsv)) {
            return;
        }

        // 2. Health Threshold Check: บอสจริงต้องมีเลือดสูง (ค่ามาตรฐาน >= 80 HP)
        double minHp = 80.0;
        try {
            if (OmniConfig.INSTANCE != null && OmniConfig.INSTANCE.mobKillMinHealth != null) {
                minHp = OmniConfig.INSTANCE.mobKillMinHealth.get();
            }
        } catch (Exception ignored) {}

        float maxHealth = entity.getMaxHealth();
        // หากเลือดไม่ถึงเกณฑ์ขั้นต่ำ และไม่ใช่บอสหลักที่ระบุชื่อชัดเจน -> ไม่ประกาศเด็ดขาด
        if (maxHealth < minHp && !isMajorBossExplicit(idStr)) {
            return;
        }

        // 3. Whitelist Filter Check: กรองเฉพาะบอสแท้จริง
        String mobFilterCsv = "cataclysm:*,iceandfire:*_dragon,iceandfire:gorgon,iceandfire:cyclops,iceandfire:hydra,mowziesmobs:ferrous_wroughtnaut,mowziesmobs:frostmaw,mowziesmobs:barako,twilightforest:naga,twilightforest:lich,twilightforest:minoshroom,twilightforest:hydra,twilightforest:knight_phantom,twilightforest:ur_ghast,twilightforest:alpha_yeti,twilightforest:snow_queen";
        try {
            if (OmniConfig.INSTANCE != null && OmniConfig.INSTANCE.mobKillFilter != null) {
                mobFilterCsv = OmniConfig.INSTANCE.mobKillFilter.get();
            }
        } catch (Exception ignored) {}

        if (matchesWildcardList(idStr, mobFilterCsv)) {
            // 4. Anti-Spam Cooldown ต่อชนิดม็อบ (อย่างน้อย 15 วินาทีต่อ 1 ชนิดม็อบ)
            String cdKey = "mobkill:" + idStr;
            long now = System.currentTimeMillis();
            if (now - cooldowns.getOrDefault(cdKey, 0L) < 15_000L) {
                return;
            }
            cooldowns.put(cdKey, now);

            String mobName = entity.getDisplayName().getString();
            broadcastCustom(
                    "☠ " + player.getName().getString() + " สังหารบอส " + mobName + " สำเร็จ!",
                    "Boss: " + idStr + " (HP: " + Math.round(maxHealth) + ")",
                    0x38BDF8, "SLIDE_DOWN", "minecraft:entity.wither.death", 110
            );
            GasHotReloadManager.INSTANCE.logEventAsync("BOSS_KILL", player.getName().getString(), mobName + " (" + idStr + ")");
        }
    }

    private boolean isMajorBossExplicit(String idStr) {
        return idStr.equals("twilightforest:knight_phantom") ||
               idStr.equals("iceandfire:siren") ||
               idStr.contains("boss");
    }

    private boolean matchesWildcardList(String id, String csv) {
        for (String raw : csv.split(",")) {
            String p = raw.trim().toLowerCase(Locale.ROOT);
            if (p.isEmpty()) continue;
            if (p.equals("*") || p.equals(id)) return true;
            if (p.endsWith("*") && id.startsWith(p.substring(0, p.length() - 1))) return true;
            if (p.startsWith("*") && id.endsWith(p.substring(1))) return true;
        }
        return false;
    }
}
